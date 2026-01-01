const crypto = require('crypto');
const fs = require('fs').promises;
const path = require('path');

// Encryption algorithm
const ALGORITHM = 'aes-256-gcm';
const IV_LENGTH = 16; // For AES, this is always 16 bytes
const AUTH_TAG_LENGTH = 16; // GCM auth tag is 16 bytes
const KEY_LENGTH = 32; // 256 bits = 32 bytes

/**
 * Get encryption key from environment variable
 * @returns {Buffer} Encryption key
 */
function getEncryptionKey() {
    const keyHex = process.env.FILE_ENCRYPTION_KEY;
    
    if (!keyHex) {
        throw new Error('FILE_ENCRYPTION_KEY environment variable is not set');
    }
    
    const key = Buffer.from(keyHex, 'hex');
    
    if (key.length !== KEY_LENGTH) {
        throw new Error(`FILE_ENCRYPTION_KEY must be ${KEY_LENGTH * 2} hex characters (${KEY_LENGTH} bytes)`);
    }
    
    return key;
}

/**
 * Encrypt a buffer using AES-256-GCM
 * @param {Buffer} buffer - Data to encrypt
 * @returns {Buffer} Encrypted data with IV and auth tag prepended
 */
function encryptBuffer(buffer) {
    try {
        const key = getEncryptionKey();
        const iv = crypto.randomBytes(IV_LENGTH);
        
        const cipher = crypto.createCipheriv(ALGORITHM, key, iv);
        
        const encrypted = Buffer.concat([
            cipher.update(buffer),
            cipher.final()
        ]);
        
        const authTag = cipher.getAuthTag();
        
        // Format: [IV (16 bytes)][Auth Tag (16 bytes)][Encrypted Data]
        return Buffer.concat([iv, authTag, encrypted]);
    } catch (error) {
        throw new Error(`Encryption failed: ${error.message}`);
    }
}

/**
 * Decrypt a buffer using AES-256-GCM
 * @param {Buffer} encryptedBuffer - Encrypted data with IV and auth tag
 * @returns {Buffer} Decrypted data
 */
function decryptBuffer(encryptedBuffer) {
    try {
        const key = getEncryptionKey();
        
        // Extract IV, auth tag, and encrypted data
        const iv = encryptedBuffer.slice(0, IV_LENGTH);
        const authTag = encryptedBuffer.slice(IV_LENGTH, IV_LENGTH + AUTH_TAG_LENGTH);
        const encrypted = encryptedBuffer.slice(IV_LENGTH + AUTH_TAG_LENGTH);
        
        const decipher = crypto.createDecipheriv(ALGORITHM, key, iv);
        decipher.setAuthTag(authTag);
        
        const decrypted = Buffer.concat([
            decipher.update(encrypted),
            decipher.final()
        ]);
        
        return decrypted;
    } catch (error) {
        throw new Error(`Decryption failed: ${error.message}`);
    }
}

/**
 * Encrypt a file
 * @param {string} inputPath - Path to input file
 * @param {string} outputPath - Path to output encrypted file
 */
async function encryptFile(inputPath, outputPath) {
    try {
        const data = await fs.readFile(inputPath);
        const encrypted = encryptBuffer(data);
        await fs.writeFile(outputPath, encrypted);
    } catch (error) {
        throw new Error(`File encryption failed: ${error.message}`);
    }
}

/**
 * Decrypt a file
 * @param {string} inputPath - Path to encrypted file
 * @param {string} outputPath - Path to output decrypted file
 */
async function decryptFile(inputPath, outputPath) {
    try {
        const encryptedData = await fs.readFile(inputPath);
        const decrypted = decryptBuffer(encryptedData);
        await fs.writeFile(outputPath, decrypted);
    } catch (error) {
        throw new Error(`File decryption failed: ${error.message}`);
    }
}

/**
 * Generate a new encryption key
 * @returns {string} Hex-encoded encryption key
 */
function generateKey() {
    return crypto.randomBytes(KEY_LENGTH).toString('hex');
}

module.exports = {
    encryptBuffer,
    decryptBuffer,
    encryptFile,
    decryptFile,
    generateKey,
    IV_LENGTH,
    AUTH_TAG_LENGTH
};
