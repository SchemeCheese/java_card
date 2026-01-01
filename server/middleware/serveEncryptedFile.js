const { decryptBuffer } = require('../utils/fileEncryption');
const fs = require('fs').promises;
const path = require('path');

/**
 * Middleware to serve encrypted avatar files
 * Decrypts file on-the-fly and streams to client
 */
async function serveEncryptedAvatar(req, res) {
    try {
        const { studentId } = req.params;
        
        // Get card to find avatar filename
        const { Card } = require('../models');
        const card = await Card.findOne({ where: { studentId } });
        
        if (!card) {
            return res.status(404).json({
                success: false,
                message: 'Avatar not found'
            });
        }
        
        if (!card.imagePath) {
            return res.status(404).json({
                success: false,
                message: 'Avatar not found'
            });
        }
        
        // Read encrypted file
        const avatarPath = path.join(__dirname, '..', card.imagePath);
        
        try {
            // Check if file exists
            const fs = require('fs');
            if (!fs.existsSync(avatarPath)) {
                return res.status(404).json({
                    success: false,
                    message: 'Avatar file not found on disk'
                });
            }
            
            const encryptedData = await fs.promises.readFile(avatarPath);
            
            // Decrypt the file
            const decryptedData = decryptBuffer(encryptedData);
            
            // Determine content type from original filename
            const ext = path.extname(card.imagePath).toLowerCase();
            const contentTypes = {
                '.jpg': 'image/jpeg',
                '.jpeg': 'image/jpeg',
                '.png': 'image/png',
                '.gif': 'image/gif',
                '.webp': 'image/webp',
                '.svg': 'image/svg+xml'
            };
            
            const contentType = contentTypes[ext] || 'application/octet-stream';
            
            // Set headers
            res.setHeader('Content-Type', contentType);
            res.setHeader('Content-Length', decryptedData.length);
            res.setHeader('Cache-Control', 'public, max-age=86400'); // Cache for 1 day
            
            // Send decrypted data
            res.send(decryptedData);
            
        } catch (fileError) {
            console.error('[AVATAR] Error reading/decrypting avatar:', fileError.message);
            return res.status(500).json({
                success: false,
                message: 'Error loading avatar',
                error: fileError.message
            });
        }
        
    } catch (error) {
        console.error('[AVATAR] Error serving avatar:', error.message);
        res.status(500).json({
            success: false,
            message: 'Error serving avatar',
            error: error.message
        });
    }
}

module.exports = { serveEncryptedAvatar };
