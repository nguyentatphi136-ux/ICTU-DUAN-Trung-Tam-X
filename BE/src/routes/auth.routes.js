const { randomBytes, scrypt, timingSafeEqual } = require('node:crypto');
const { promisify } = require('node:util');
const express = require('express');
const jwt = require('jsonwebtoken');
const jwtConfig = require('../config/jwt');
const { toPublicUser, users } = require('../data/users');
const { authenticate } = require('../middleware/auth');

const router = express.Router();
const scryptAsync = promisify(scrypt);
const invalidCredentialsMessage = 'Email hoặc mật khẩu không chính xác';
const dummySalt = randomBytes(16);
const dummyHash = randomBytes(64);

async function verifyPassword(password, user) {
  const salt = user ? Buffer.from(`idttx-44:${user.id}`) : dummySalt;
  const expectedHash = user ? user.passwordHash : dummyHash;
  const actualHash = await scryptAsync(password, salt, expectedHash.length);
  return timingSafeEqual(actualHash, expectedHash);
}

router.post('/login', async (req, res, next) => {
  try {
    const { email, password } = req.body || {};

    if (typeof email !== 'string' || typeof password !== 'string' || !email.trim() || !password) {
      return res.status(400).json({
        success: false,
        message: 'Email và mật khẩu là bắt buộc',
      });
    }

    const normalizedEmail = email.trim().toLowerCase();
    const user = users.find((candidate) => candidate.email === normalizedEmail);
    const passwordIsValid = await verifyPassword(password, user);

    if (!user || !passwordIsValid) {
      return res.status(401).json({
        success: false,
        message: invalidCredentialsMessage,
      });
    }

    // roles trong token chỉ để client hiển thị; quyền truy cập luôn được
    // kiểm tra lại từ kho người dùng bởi middleware authenticate/authorize.
    const token = jwt.sign({ roles: user.roles }, jwtConfig.secret, {
      subject: user.id,
      expiresIn: jwtConfig.expiresIn,
      algorithm: jwtConfig.algorithm,
    });

    return res.status(200).json({
      success: true,
      message: 'Đăng nhập thành công',
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        roles: user.roles,
      },
      token,
    });
  } catch (error) {
    return next(error);
  }
});

router.get('/me', authenticate, (req, res) => {
  return res.status(200).json({ success: true, user: toPublicUser(req.user) });
});

module.exports = router;
