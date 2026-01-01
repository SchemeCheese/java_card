const jwt = require('jsonwebtoken');
const { logger } = require('../config/logger');

/**
 * OAuth 2.0 Authentication Middleware for Webhook
 */
class OAuthMiddleware {
  /**
   * POST /api/oauth/token
   * Sepay sẽ gọi endpoint này để lấy access token
   */
  async issueToken(req, res) {
    try {
      // DEBUG: Log ALL request details
      logger.error('='.repeat(20) + ' OAUTH DEBUG ' + '='.repeat(20));
      logger.error(`[OAuth Debug] Headers: ${JSON.stringify(req.headers, null, 2)}`);
      logger.error(`[OAuth Debug] Body: ${JSON.stringify(req.body, null, 2)}`);
      logger.error('='.repeat(53));

      // Extract credentials (support both snake_case and camelCase)
      let { client_id, client_secret, clientId, clientSecret } = req.body;
      
      // Normalize to snake_case
      client_id = client_id || clientId;
      client_secret = client_secret || clientSecret;
      
      // If not in body, try headers
      if (!client_id || !client_secret) {
        // Try custom headers
        client_id = client_id || req.headers['client_id'] || req.headers['x-client-id'];
        client_secret = client_secret || req.headers['client_secret'] || req.headers['x-client-secret'];
        
        // Try Basic Auth (Standard OAuth 2.0 method)
        const authHeader = req.headers['authorization'];
        if (authHeader && authHeader.startsWith('Basic ')) {
          try {
            const base64Credentials = authHeader.substring(6);
            const credentials = Buffer.from(base64Credentials, 'base64').toString('ascii');
            const [id, secret] = credentials.split(':');
            client_id = client_id || id;
            client_secret = client_secret || secret;
            logger.error('[OAuth Debug] Found credentials in Basic Auth header');
          } catch (e) {
            logger.error('[OAuth Debug] Failed to parse Basic Auth header:', e);
          }
        }
      }
      
      // DEBUG: Log received request
      logger.error(`[OAuth Debug] Final credentials: client_id=${client_id}, client_secret=${client_secret ? '***' : 'missing'}`);
      
      // Removed grant_type check per request
      
      // Validate credentials
      const validClientId = process.env.OAUTH_CLIENT_ID;
      const validClientSecret = process.env.OAUTH_CLIENT_SECRET;
      
      if (!validClientId || !validClientSecret) {
        logger.error('[OAuth Debug] Credentials not found in .env');
        console.error('OAuth credentials not configured in .env');
        return res.status(500).json({
          error: 'server_error',
          error_description: 'OAuth not configured'
        });
      }
      
      if (client_id !== validClientId || client_secret !== validClientSecret) {
        logger.error(`[OAuth Debug] Credential mismatch!
          Expected Client ID: ${validClientId}
          Received Client ID: ${client_id}
          Expected Secret: ${validClientSecret} (Length: ${validClientSecret.length})
          Received Secret: ${client_secret} (Length: ${client_secret ? client_secret.length : 0})
        `);
        
        return res.status(401).json({
          error: 'invalid_client',
          error_description: 'Invalid client credentials'
        });
      }
      
      // Generate access token (JWT)
      const jwtSecret = process.env.JWT_SECRET || process.env.WEBHOOK_SECRET;
      const token = jwt.sign(
        { 
          client_id: client_id,
          scope: 'webhook',
          iat: Math.floor(Date.now() / 1000)
        },
        jwtSecret,
        { 
          expiresIn: '1h', // Token hết hạn sau 1 giờ
          issuer: 'library-card-api'
        }
      );
      
      res.json({
        success: true,
        access_token: token,
        token_type: 'Bearer',
        expires_in: 3600, // 1 hour in seconds
        scope: 'webhook'
      });
      
      console.log('✅ OAuth token issued for client:', client_id);
      
    } catch (error) {
      console.error('OAuth token error:', error);
      res.status(500).json({
        error: 'server_error',
        error_description: 'Internal server error'
      });
    }
  }
  
  /**
   * Middleware để verify OAuth token cho webhook
   */
  verifyToken(req, res, next) {
    try {
      const authHeader = req.headers['authorization'];
      
      // DEBUG: Log webhook request header
      logger.info(`[Webhook Debug] Verifying token. Auth Header: ${authHeader ? 'Present' : 'Missing'}`);
      
      if (!authHeader || !authHeader.startsWith('Bearer ')) {
        logger.error(`[Webhook Debug] Missing or invalid auth header: ${authHeader}`);
        return res.status(401).json({
          error: 'invalid_token',
          error_description: 'Missing or invalid authorization header'
        });
      }
      
      const token = authHeader.substring(7); // Remove 'Bearer '
      const jwtSecret = process.env.JWT_SECRET || process.env.WEBHOOK_SECRET;
      
      // Verify JWT
      const decoded = jwt.verify(token, jwtSecret);
      
      // Check scope
      if (decoded.scope !== 'webhook') {
        return res.status(403).json({
          error: 'insufficient_scope',
          error_description: 'Token does not have webhook scope'
        });
      }
      
      // Attach decoded token to request
      req.oauth = decoded;
      
      next();
      
    } catch (error) {
      if (error.name === 'TokenExpiredError') {
        return res.status(401).json({
          error: 'invalid_token',
          error_description: 'Token has expired'
        });
      }
      
      if (error.name === 'JsonWebTokenError') {
        return res.status(401).json({
          error: 'invalid_token',
          error_description: 'Invalid token'
        });
      }
      
      console.error('Token verification error:', error);
      res.status(500).json({
        error: 'server_error',
        error_description: 'Internal server error'
      });
    }
  }

  /**
   * Middleware xác thực bằng API Key (Header: "Authorization: Apikey <API_KEY>")
   */
  verifyApiKey(req, res, next) {
    try {
      const authHeader = req.headers['authorization'];
      const { logger } = require('../config/logger');

      // DEBUG: Log webhook request header
      logger.info(`[Webhook Auth] Verifying API Key. Header: ${authHeader}`);

      if (!authHeader || !authHeader.startsWith('Apikey ')) {
        logger.error(`[Webhook Auth] Missing or invalid auth header format. Expected 'Apikey <KEY>'`);
        return res.status(401).json({
          error: 'invalid_token',
          message: 'Missing or invalid authorization header. Expected "Apikey YOUR_API_KEY"'
        });
      }

      const apiKey = authHeader.substring(7).trim(); // Remove 'Apikey '
      const validApiKey = process.env.SEPAY_API_KEY;

      if (!validApiKey) {
        logger.error('[Webhook Auth] Server API Key not configured');
        return res.status(500).json({
          error: 'server_configuration_error',
          message: 'API Key not configured on server'
        });
      }

      if (apiKey !== validApiKey) {
        logger.error(`[Webhook Auth] Invalid API Key received: ${apiKey.substring(0, 5)}...`);
        return res.status(401).json({
          error: 'invalid_token',
          message: 'Invalid API Key'
        });
      }

      logger.info('[Webhook Auth] API Key verified successfully');
      next();

    } catch (error) {
      console.error('API Key verification error:', error);
      res.status(500).json({
        error: 'server_error',
        message: 'Internal server error during authentication'
      });
    }
  }
}

module.exports = new OAuthMiddleware();
