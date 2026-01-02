const { Card } = require('../models');
const crypto = require('crypto');
const jwt = require('jsonwebtoken');

// Admin student ID - bypass authentication
const ADMIN_STUDENT_ID = 'CT060132';

/**
 * Login với RSA signature
 * Client đã verify RSA ở client-side, giờ server verify lại để tạo token
 */
exports.login = async (req, res) => {
    try {
        const { studentId, challenge, signature } = req.body;

        if (!studentId || !challenge || !signature) {
            return res.status(400).json({
                success: false,
                message: 'Thiếu thông tin: studentId, challenge, signature'
            });
        }

        // Admin bypass - không cần verify RSA (case-insensitive)
        if (studentId.toUpperCase() === ADMIN_STUDENT_ID) {
            console.log(`[AUTH] Admin login: ${studentId}`);
            const token = jwt.sign(
                { 
                    studentId: studentId,
                    role: 'admin',
                    iat: Math.floor(Date.now() / 1000)
                },
                process.env.JWT_SECRET || 'library_card_secret_key',
                { expiresIn: '24h' }
            );

            return res.json({
                success: true,
                message: 'Admin login successful',
                data: {
                    token: token,
                    studentId: studentId,
                    role: 'admin'
                }
            });
        }

        // Lấy public key từ database
        const card = await Card.findOne({ where: { studentId } });
        if (!card) {
            return res.status(404).json({
                success: false,
                message: 'Thẻ không tồn tại'
            });
        }

        if (!card.rsaPublicKey) {
            return res.status(400).json({
                success: false,
                message: 'Thẻ chưa có RSA public key'
            });
        }

        // Verify RSA signature
        try {
            const publicKey = crypto.createPublicKey({
                key: card.rsaPublicKey,
                format: 'pem'
            });

            // Method 1: Try Signature API (SHA1withRSA)
            try {
                const verify = crypto.createVerify('SHA1');
                verify.update(Buffer.from(challenge, 'base64'));
                verify.end();

                const isValid = verify.verify(publicKey, Buffer.from(signature, 'base64'));

                if (!isValid) {
                    console.log(`[AUTH] RSA signature verification failed for student: ${studentId}`);
                    return res.status(401).json({
                        success: false,
                        message: 'RSA signature verification failed'
                    });
                }

                console.log(`[AUTH] RSA signature verification successful for student: ${studentId}`);
            } catch (sigError) {
                // Fallback: Manual verification
                console.log(`[AUTH] Signature API failed, trying manual verification: ${sigError.message}`);
                
                // Hash challenge với SHA-1
                const sha1 = crypto.createHash('sha1');
                sha1.update(Buffer.from(challenge, 'base64'));
                const challengeHash = sha1.digest();

                // Decrypt signature với public key
                const decrypted = crypto.publicDecrypt(
                    {
                        key: publicKey,
                        padding: crypto.constants.RSA_PKCS1_PADDING
                    },
                    Buffer.from(signature, 'base64')
                );

                // Verify PKCS#1 v1.5 padding format
                if (decrypted.length < 35) {
                    return res.status(401).json({
                        success: false,
                        message: 'Invalid signature format'
                    });
                }

                if (decrypted[0] !== 0x00 || decrypted[1] !== 0x01) {
                    return res.status(401).json({
                        success: false,
                        message: 'Invalid PKCS#1 padding header'
                    });
                }

                // Find 0x00 separator after PS
                let sepIndex = -1;
                for (let i = 2; i < decrypted.length; i++) {
                    if (decrypted[i] === 0x00) {
                        sepIndex = i;
                        break;
                    } else if (decrypted[i] !== 0xFF) {
                        return res.status(401).json({
                            success: false,
                            message: 'Invalid PS padding'
                        });
                    }
                }

                if (sepIndex === -1 || sepIndex < 10) {
                    return res.status(401).json({
                        success: false,
                        message: 'PS padding too short'
                    });
                }

                // Extract DigestInfo and hash
                const digestInfoStart = sepIndex + 1;
                const expectedDigestInfo = Buffer.from([
                    0x30, 0x21, 0x30, 0x09, 0x06, 0x05,
                    0x2B, 0x0E, 0x03, 0x02, 0x1A, 0x05,
                    0x00, 0x04, 0x14
                ]);

                if (digestInfoStart + expectedDigestInfo.length + 20 > decrypted.length) {
                    return res.status(401).json({
                        success: false,
                        message: 'DigestInfo + hash exceeds decrypted data'
                    });
                }

                // Verify DigestInfo
                for (let i = 0; i < expectedDigestInfo.length; i++) {
                    if (decrypted[digestInfoStart + i] !== expectedDigestInfo[i]) {
                        return res.status(401).json({
                            success: false,
                            message: 'Invalid DigestInfo'
                        });
                    }
                }

                // Extract hash
                const hashStart = digestInfoStart + expectedDigestInfo.length;
                const extractedHash = decrypted.slice(hashStart, hashStart + 20);

                // Compare hashes
                if (!challengeHash.equals(extractedHash)) {
                    return res.status(401).json({
                        success: false,
                        message: 'Hash mismatch'
                    });
                }

                console.log(`[AUTH] Manual RSA signature verification successful for student: ${studentId}`);
            }

            // Tạo JWT token
            const token = jwt.sign(
                { 
                    studentId: studentId,
                    role: 'user',
                    iat: Math.floor(Date.now() / 1000)
                },
                process.env.JWT_SECRET || 'library_card_secret_key',
                { expiresIn: '24h' }
            );

            return res.json({
                success: true,
                message: 'Login successful',
                data: {
                    token: token,
                    studentId: studentId,
                    role: 'user'
                }
            });

        } catch (verifyError) {
            console.error(`[AUTH] RSA verification error for student ${studentId}:`, verifyError);
            return res.status(401).json({
                success: false,
                message: 'RSA signature verification failed: ' + verifyError.message
            });
        }

    } catch (error) {
        console.error('[AUTH] Login error:', error);
        return res.status(500).json({
            success: false,
            message: 'Internal server error',
            error: process.env.NODE_ENV === 'development' ? error.message : {}
        });
    }
};

/**
 * Verify token (dùng trong middleware)
 */
exports.verifyToken = (req, res, next) => {
    try {
        const authHeader = req.headers['authorization'];
        const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN

        if (!token) {
            return res.status(401).json({
                success: false,
                message: 'No token provided'
            });
        }

        // Admin bypass
        const decoded = jwt.decode(token);
        if (decoded && decoded.studentId === ADMIN_STUDENT_ID) {
            req.authenticatedStudentId = decoded.studentId;
            req.authenticatedRole = 'admin';
            return next();
        }

        // Verify token
        jwt.verify(token, process.env.JWT_SECRET || 'library_card_secret_key', (err, decoded) => {
            if (err) {
                return res.status(401).json({
                    success: false,
                    message: 'Invalid or expired token'
                });
            }

            req.authenticatedStudentId = decoded.studentId;
            req.authenticatedRole = decoded.role || 'user';
            next();
        });

    } catch (error) {
        console.error('[AUTH] Token verification error:', error);
        return res.status(500).json({
            success: false,
            message: 'Token verification error'
        });
    }
};

/**
 * Middleware: Verify token và check authorization
 * Admin có thể truy cập tất cả, user chỉ truy cập được data của mình
 */
exports.authenticate = (req, res, next) => {
    try {
        const authHeader = req.headers['authorization'];
        const token = authHeader && authHeader.split(' ')[1];

        if (!token) {
            return res.status(401).json({
                success: false,
                message: 'No token provided'
            });
        }

        // Decode token để check admin
        const decoded = jwt.decode(token);
        if (decoded && decoded.studentId === ADMIN_STUDENT_ID) {
            req.authenticatedStudentId = decoded.studentId;
            req.authenticatedRole = 'admin';
            return next();
        }

        // Verify token
        jwt.verify(token, process.env.JWT_SECRET || 'library_card_secret_key', (err, decoded) => {
            if (err) {
                return res.status(401).json({
                    success: false,
                    message: 'Invalid or expired token'
                });
            }

            req.authenticatedStudentId = decoded.studentId;
            req.authenticatedRole = decoded.role || 'user';

            // Check authorization: user chỉ truy cập được data của mình
            const requestedStudentId = req.params.studentId;
            if (requestedStudentId && decoded.studentId !== requestedStudentId && decoded.role !== 'admin') {
                return res.status(403).json({
                    success: false,
                    message: 'Forbidden: You can only access your own data'
                });
            }

            next();
        });

    } catch (error) {
        console.error('[AUTH] Authentication error:', error);
        return res.status(500).json({
            success: false,
            message: 'Authentication error'
        });
    }
};

/**
 * Middleware: Chỉ admin mới được truy cập
 * Dùng cho các API quản lý như tạo/sửa/xóa sách, quản lý thẻ, etc.
 */
exports.authenticateAdmin = (req, res, next) => {
    try {
        const authHeader = req.headers['authorization'];
        const token = authHeader && authHeader.split(' ')[1];

        if (!token) {
            return res.status(401).json({
                success: false,
                message: 'No token provided'
            });
        }

        // Verify token
        jwt.verify(token, process.env.JWT_SECRET || 'library_card_secret_key', (err, decoded) => {
            if (err) {
                return res.status(401).json({
                    success: false,
                    message: 'Invalid or expired token'
                });
            }

            // Check if admin
            const isAdmin = decoded.studentId === ADMIN_STUDENT_ID || decoded.role === 'admin';
            
            if (!isAdmin) {
                return res.status(403).json({
                    success: false,
                    message: 'Forbidden: Admin access required'
                });
            }

            req.authenticatedStudentId = decoded.studentId;
            req.authenticatedRole = 'admin';
            next();
        });

    } catch (error) {
        console.error('[AUTH] Admin authentication error:', error);
        return res.status(500).json({
            success: false,
            message: 'Authentication error'
        });
    }
};

