const Payment = require('../models/Payment');
const EventEmitter = require('events');

/**
 * SSE Manager để quản lý connections và gửi events
 */
class SSEManager extends EventEmitter {
  constructor() {
    super();
    this.clients = new Map(); // paymentId -> Set of response objects
  }

  /**
   * Thêm client vào listening list
   */
  addClient(paymentId, res) {
    if (!this.clients.has(paymentId)) {
      this.clients.set(paymentId, new Set());
    }
    this.clients.get(paymentId).add(res);
    
    console.log(`[SSE] Client connected for payment ${paymentId}. Total clients: ${this.clients.get(paymentId).size}`);
  }

  /**
   * Remove client khi disconnect
   */
  removeClient(paymentId, res) {
    if (this.clients.has(paymentId)) {
      this.clients.get(paymentId).delete(res);
      
      // Cleanup nếu không còn client nào
      if (this.clients.get(paymentId).size === 0) {
        this.clients.delete(paymentId);
      }
      
      console.log(`[SSE] Client disconnected for payment ${paymentId}`);
    }
  }

  /**
   * Gửi event tới tất cả clients đang listen payment này
   */
  sendEvent(paymentId, eventType, data) {
    if (!this.clients.has(paymentId)) {
      console.log(`[SSE] No clients listening for payment ${paymentId}`);
      return;
    }

    const clients = this.clients.get(paymentId);
    const message = `event: ${eventType}\ndata: ${JSON.stringify(data)}\n\n`;

    clients.forEach((res) => {
      try {
        res.write(message);
      } catch (error) {
        console.error(`[SSE] Error sending to client:`, error.message);
        this.removeClient(paymentId, res);
      }
    });

    console.log(`[SSE] Sent ${eventType} event to ${clients.size} client(s) for payment ${paymentId}`);
  }

  /**
   * Gửi payment status update
   */
  sendPaymentUpdate(paymentId, status, payment) {
    this.sendEvent(paymentId, 'payment-status', {
      paymentId,
      status,
      paidAt: payment?.paidAt,
      updatedAt: payment?.updatedAt
    });
  }

  /**
   * Gửi heartbeat để keep connection alive
   */
  sendHeartbeat(paymentId) {
    this.sendEvent(paymentId, 'heartbeat', { timestamp: new Date().toISOString() });
  }
}

// Singleton instance
const sseManager = new SSEManager();

/**
 * SSE Controller
 */
class SSEController {
  /**
   * GET /api/payment/:paymentId/stream
   * SSE endpoint để client subscribe payment updates
   */
  async streamPaymentStatus(req, res) {
    const { paymentId } = req.params;

    try {
      // Verify payment exists
      const payment = await Payment.findByPk(paymentId);
      if (!payment) {
        return res.status(404).json({
          success: false,
          message: 'Payment not found'
        });
      }

      // Setup SSE headers
      res.setHeader('Content-Type', 'text/event-stream');
      res.setHeader('Cache-Control', 'no-cache');
      res.setHeader('Connection', 'keep-alive');
      res.setHeader('X-Accel-Buffering', 'no'); // Disable nginx buffering

      // Send initial connection message
      res.write(`event: connected\ndata: ${JSON.stringify({ paymentId, status: payment.status })}\n\n`);

      // Add client to manager
      sseManager.addClient(paymentId, res);

      // Send heartbeat every 30s to keep connection alive
      const heartbeatInterval = setInterval(() => {
        sseManager.sendHeartbeat(paymentId);
      }, 30000);

      // Cleanup on client disconnect
      req.on('close', () => {
        clearInterval(heartbeatInterval);
        sseManager.removeClient(paymentId, res);
        res.end();
      });

    } catch (error) {
      console.error('[SSE] Stream error:', error);
      res.status(500).json({
        success: false,
        message: 'SSE stream error'
      });
    }
  }
}

module.exports = {
  sseController: new SSEController(),
  sseManager
};
