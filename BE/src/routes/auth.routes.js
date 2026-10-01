const { randomBytes, scrypt, timingSafeEqual } = require('node:crypto');
const { promisify } = require('node:util');
const express = require('express');
const jwt = require('jsonwebtoken');
const { updatePassword, users } = require('../data/users');

const router = express.Router();
const scryptAsync = promisify(scrypt);
const invalidCredentialsMessage = 'Email hoặc mật khẩu không chính xác';
const invalidNewPasswordMessage = 'Mật khẩu mới phải có tối thiểu 8 ký tự, bao gồm cả chữ và số!';
const newPasswordPattern = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;
const dummySalt = randomBytes(16);
const dummyHash = randomBytes(64);

function requireAuthentication(req, res, next) {
  const [scheme, token] = (req.get('authorization') || '').split(' ');
  if (scheme !== 'Bearer' || !token) {
    return res.status(401).json({ success: false, message: 'Vui lòng đăng nhập để tiếp tục' });
  }

  try {
    const claims = jwt.verify(token, process.env.JWT_SECRET || 'development-only-change-this-secret', {
      algorithms: ['HS256'],
    });
    const user = users.find((candidate) => candidate.id === claims.sub);
    if (!user) {
      return res.status(401).json({ success: false, message: 'Tài khoản trong phiên đăng nhập không tồn tại' });
    }
    req.authenticatedUser = user;
    return next();
  } catch {
    return res.status(401).json({ success: false, message: 'Phiên đăng nhập không hợp lệ hoặc đã hết hạn' });
  }
}

async function verifyPassword(password, user) {
  const salt = user ? Buffer.from(`idttx-44:${user.id}`) : dummySalt;
  const expectedHash = user ? user.passwordHash : dummyHash;
  const actualHash = await scryptAsync(password, salt, expectedHash.length);
  return timingSafeEqual(actualHash, expectedHash);
}

router.post('/change-password', requireAuthentication, (req, res) => {
  const { newPassword } = req.body || {};

  if (typeof newPassword !== 'string' || !newPassword) {
    return res.status(400).json({ success: false, message: 'Mật khẩu mới là bắt buộc' });
  }

  if (!newPasswordPattern.test(newPassword)) {
    return res.status(400).json({
      success: false,
      code: 'INVALID_NEW_PASSWORD',
      message: invalidNewPasswordMessage,
    });
  }

  updatePassword(req.authenticatedUser.id, newPassword);
  return res.status(200).json({ success: true, message: 'Đổi mật khẩu thành công' });
});

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

    const token = jwt.sign(
      { role: user.role },
      process.env.JWT_SECRET || 'development-only-change-this-secret',
      {
        subject: user.id,
        expiresIn: process.env.JWT_EXPIRES_IN || '1h',
        algorithm: 'HS256',
      },
    );

    return res.status(200).json({
      success: true,
      message: 'Đăng nhập thành công',
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        role: user.role,
      },
      token,
    });
  } catch (error) {
    return next(error);
  }
});

module.exports = router;
