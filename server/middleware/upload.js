const multer = require('multer');
const path = require('path');
const fs = require('fs');
const { encryptFile } = require('../utils/fileEncryption');

// Tạo thư mục uploads nếu chưa có
const uploadsDir = path.join(__dirname, '../uploads/avatars');
if (!fs.existsSync(uploadsDir)) {
    fs.mkdirSync(uploadsDir, { recursive: true });
}

// Cấu hình storage
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, uploadsDir);
    },
    filename: function (req, file, cb) {
        // Tên file: studentId_timestamp.extension
        const studentId = req.params.studentId || req.body.studentId || 'unknown';
        const timestamp = Date.now();
        const ext = path.extname(file.originalname);
        const filename = `${studentId}_${timestamp}${ext}`;
        cb(null, filename);
    }
});

// File filter - chỉ chấp nhận ảnh
const fileFilter = (req, file, cb) => {
    const allowedTypes = /jpeg|jpg|png|gif|webp|svg/;
    const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
    const mimetype = allowedTypes.test(file.mimetype);

    if (mimetype && extname) {
        return cb(null, true);
    } else {
        cb(new Error('Chỉ chấp nhận file ảnh (JPG, PNG, GIF, WEBP, SVG)'));
    }
};

// Cấu hình multer
const upload = multer({
    storage: storage,
    limits: {
        fileSize: 5 * 1024 * 1024 // 5MB max
    },
    fileFilter: fileFilter
});

/**
 * Middleware to encrypt uploaded file
 * Call this AFTER multer processes the upload
 */
async function encryptUploadedFile(req, res, next) {
    if (!req.file) {
        return next();
    }

    try {
        const originalPath = req.file.path;
        const tempPath = originalPath + '.tmp';

        // Encrypt the file (original -> temp)
        await encryptFile(originalPath, tempPath);

        // Replace original with encrypted version
        fs.unlinkSync(originalPath);
        fs.renameSync(tempPath, originalPath);

        next();
    } catch (error) {
        console.error('[ENCRYPTION] Failed to encrypt uploaded file:', error.message);
        // Clean up the unencrypted file
        if (req.file && req.file.path) {
            try {
                fs.unlinkSync(req.file.path);
            } catch (cleanupError) {
                console.error('[ENCRYPTION] Failed to cleanup unencrypted file:', cleanupError.message);
            }
        }
        return res.status(500).json({
            success: false,
            message: 'Failed to encrypt uploaded file',
            error: error.message
        });
    }
}

module.exports = upload;
module.exports.encryptUploadedFile = encryptUploadedFile;

