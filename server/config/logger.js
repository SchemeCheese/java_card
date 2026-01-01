'use strict';

const fs = require('fs');
const path = require('path');
const util = require('util');
const moment = require('moment');
const { createLogger, format, transports } = require('winston');
const DailyRotateFile = require('winston-daily-rotate-file');

// ---- Paths & helpers --------------------------------------------------------
const logsRoot = path.join(__dirname, '..', 'logs');

function ensureDir(p) {
    try {
        fs.mkdirSync(p, { recursive: true });
    } catch (_) { }
}

function monthKey(d = new Date()) {
    // YYYY-MM
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    return `${y}-${m}`;
}

function filterLevels(levels) {
    return format((info) => (levels.includes(info.level) ? info : false))();
}

const logPrintf = format.printf(({ timestamp, level, message, stack }) => {
    const base = `${timestamp} ${level.toUpperCase()} ${message}`;
    return stack ? `${base}\n${stack}` : base;
});

const logPrintfNoTime = format.printf(({ level, message, stack }) => {
    const base = `${level.toUpperCase()} ${message}`;
    return stack ? `${base}\n${stack}` : base;
});

// ---- Factory to (re)build transports for a given month ----------------------
function buildMonthlyTransports(mKey) {
    const monthDir = path.join(logsRoot, mKey);
    ensureDir(monthDir);

    const commonFormat = format.combine(
        format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss.SSS' }),
        format.errors({ stack: true }),
        format.splat(),
        logPrintf
    );

    const outT = new DailyRotateFile({
        dirname: monthDir,                                 // <-- folder YYYY-MM
        filename: '%DATE%-out.log',                        // <-- file YYYY-MM-DD-out.log
        datePattern: 'YYYY-MM-DD',
        zippedArchive: false,
        level: 'silly',
        format: format.combine(filterLevels(['info', 'http', 'verbose', 'debug', 'silly', 'warn'])), // everything except pure 'error'
    });

    const errT = new DailyRotateFile({
        dirname: monthDir,
        filename: '%DATE%-err.log',
        datePattern: 'YYYY-MM-DD',
        zippedArchive: false,
        level: 'silly',
        format: format.combine(filterLevels(['error'])),
    });

    // Ensure nested dirs on rotation/new file
    [outT, errT].forEach((t) => {
        if (typeof t.on === 'function') {
            t.on('new', (filename) => ensureDir(path.dirname(filename)));
            t.on('rotate', (_oldFile, newFile) => ensureDir(path.dirname(newFile)));
        }
    });

    // Wrap each transport with the common formatter
    outT.format = format.combine(commonFormat, outT.format);
    errT.format = format.combine(commonFormat, errT.format);

    return { outT, errT };
}

// ---- Create logger with current month transports ----------------------------
ensureDir(logsRoot);
let currentMonth = monthKey();
let { outT, errT } = buildMonthlyTransports(currentMonth);

const logger = createLogger({
    level: process.env.LOG_LEVEL || 'info',
    transports: [outT, errT],
});

// ---- Add console transport để log ra terminal/server -----------------------------------
logger.add(new transports.Console({
    format: format.combine(
        format.errors({ stack: true }),
        format.splat(),
        logPrintfNoTime
    ),
}));

// ---- Safe setTimeout function để xử lý timeout lớn hơn giới hạn Node.js ---------
function safeSetTimeout(fn, delay) {
  const MAX_TIMEOUT = 2147483647; // ~24.8 ngày - giới hạn của Node.js setTimeout
  if (delay > MAX_TIMEOUT) {
    return setTimeout(() => {
      safeSetTimeout(fn, delay - MAX_TIMEOUT);
    }, MAX_TIMEOUT);
  }
  return setTimeout(fn, delay);
}

// ---- Monthly rotation scheduler (sửa để tránh timeout overflow) -------------------
function msUntilNextMonth() {
  const now = moment();
  // Lấy 00:00:05 ngày đầu tháng kế tiếp
  const nextMonth = now.clone().add(1, 'month').startOf('month').add(5, 'seconds');
  const diff = nextMonth.diff(now, 'milliseconds');
  
  // Giới hạn timeout để tránh 32-bit overflow
  const MAX_TIMEOUT = 2147483647; // 32-bit signed integer limit (2^31 - 1)
  const MIN_TIMEOUT = 2000; // 2 giây
  
  // Kiểm tra tính hợp lệ của diff
  if (!Number.isFinite(diff) || diff <= 0) {
    console.warn(`Invalid timeout calculated (${diff}ms), using minimum timeout (${MIN_TIMEOUT}ms)`);
    return MIN_TIMEOUT;
  }
  
  return Math.max(diff, MIN_TIMEOUT);
}

let monthlyTimer = null;
let fallbackTimer = null;

function refreshTransportsIfMonthChanged() {
  const nowKey = monthKey();        // YYYY-MM hiện tại
  if (nowKey === currentMonth) return;

  // Gỡ transport cũ
  try {
    logger.remove(outT);
  } catch (_) {}
  try {
    logger.remove(errT);
  } catch (_) {}

  if (typeof outT.close === 'function') {
    try { outT.close(); } catch (_) {}
  }
  if (typeof errT.close === 'function') {
    try { errT.close(); } catch (_) {}
  }

  // Tạo transport mới cho tháng mới
  const built = buildMonthlyTransports(nowKey);
  outT = built.outT;
  errT = built.errT;

  logger.add(outT);
  logger.add(errT);

  currentMonth = nowKey;
  logger.info(`Log directory rotated to month ${currentMonth}`);
}

function scheduleMonthlyRotation() {
  const delay = msUntilNextMonth();
  if (monthlyTimer) clearTimeout(monthlyTimer);

  monthlyTimer = safeSetTimeout(() => {
    try {
      refreshTransportsIfMonthChanged();
    } catch (error) {
      console.error('Error in monthly rotation:', error);
    } finally {
      scheduleMonthlyRotation(); // Schedule next rotation
    }
  }, delay);

  // fallback: 6 giờ gọi 1 lần, phòng khi bị lệch giờ hệ thống
  if (fallbackTimer) clearInterval(fallbackTimer);
  fallbackTimer = setInterval(() => {
    try {
      refreshTransportsIfMonthChanged();
    } catch (error) {
      console.error('Error in fallback rotation:', error);
    }
  }, 6 * 60 * 60 * 1000); // 6 giờ
}

// Gọi khi khởi động app
scheduleMonthlyRotation();

// ---- Console monkey-patch ---------------------------------------------------
const original = {
    log: console.log.bind(console),
    info: console.info ? console.info.bind(console) : console.log.bind(console),
    warn: console.warn.bind(console),
    error: console.error.bind(console),
    debug: console.debug ? console.debug.bind(console) : console.log.bind(console),
};

const hasConsoleTransport = logger.transports.some((t) => t instanceof transports.Console);

console.log = (...args) => {
    const msg = util.format(...args);
    logger.info(msg);
    if (!hasConsoleTransport) original.log(...args);
};
console.info = (...args) => {
    const msg = util.format(...args);
    logger.info(msg);
    if (!hasConsoleTransport) original.info(...args);
};
console.warn = (...args) => {
    const msg = util.format(...args);
    logger.warn(msg);
    if (!hasConsoleTransport) original.warn(...args);
};
console.error = (...args) => {
    const msg = util.format(...args);
    logger.error(msg);
    if (!hasConsoleTransport) original.error(...args);
};
console.debug = (...args) => {
    const msg = util.format(...args);
    logger.debug(msg);
    if (!hasConsoleTransport) original.debug(...args);
};

// ---- Morgan stream (HTTP access) -------------------------------------------
const morganStream = {
    write: (message) => {
        logger.info(message.trim());
    },
};

module.exports = { logger, morganStream };
