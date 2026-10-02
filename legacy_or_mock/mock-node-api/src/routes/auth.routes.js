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

const MAX_FAILED_ATTEMPTS = 5;
const LOCKOUT_MINUTES = 15;
const LOCKOUT_DURATION_MS = LOCKOUT_MINUTES * 60 * 1000;
const failedLoginAttempts = new Map(); // normalizedEmail -> { count, lockedUntil }

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

    // S1-01 AC3: Khóa tạm 15 phút sau 5 lần sai liên tiếp
    const now = Date.now();
    const attemptRecord = failedLoginAttempts.get(normalizedEmail);
    if (attemptRecord && attemptRecord.lockedUntil && attemptRecord.lockedUntil > now) {
      const remainingMinutes = Math.ceil((attemptRecord.lockedUntil - now) / (60 * 1000));
      return res.status(423).json({
        success: false,
        code: 'ACCOUNT_TEMPORARILY_LOCKED',
        message: `Tài khoản tạm thời bị khóa 15 phút do nhập sai 5 lần liên tiếp. Vui lòng thử lại sau ${remainingMinutes} phút.`,
        remainingMinutes,
      });
    }

    const user = users.find((candidate) => candidate.email === normalizedEmail);
    const passwordIsValid = await verifyPassword(password, user);

    if (!user || !passwordIsValid) {
      let record = attemptRecord;
      if (!record || (record.lockedUntil && record.lockedUntil <= now)) {
        record = { count: 0, lockedUntil: null };
      }
      record.count += 1;
      if (record.count >= MAX_FAILED_ATTEMPTS) {
        record.lockedUntil = now + LOCKOUT_DURATION_MS;
        failedLoginAttempts.set(normalizedEmail, record);
        return res.status(423).json({
          success: false,
          code: 'ACCOUNT_TEMPORARILY_LOCKED',
          message: 'Tài khoản đã bị tạm khóa 15 phút do nhập sai 5 lần liên tiếp. Vui lòng thử lại sau 15 phút.',
          remainingMinutes: LOCKOUT_MINUTES,
        });
      } else {
        failedLoginAttempts.set(normalizedEmail, record);
        const remainingAttempts = MAX_FAILED_ATTEMPTS - record.count;
        return res.status(401).json({
          success: false,
          message: invalidCredentialsMessage,
          remainingAttempts,
        });
      }
    }

    // Đăng nhập thành công -> Reset bộ đếm thất bại
    failedLoginAttempts.delete(normalizedEmail);

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

async function hashPassword(password, userId) {
  const salt = Buffer.from(`idttx-44:${userId}`);
  return await scryptAsync(password, salt, 64);
}

router.get('/me', authenticate, (req, res) => {
  return res.status(200).json({ success: true, user: toPublicUser(req.user) });
});

router.post('/change-password', authenticate, async (req, res, next) => {
  try {
    const { currentPassword, newPassword, confirmPassword } = req.body || {};

    if (!currentPassword) {
      return res.status(400).json({
        success: false,
        code: 'VALIDATION_ERROR',
        message: 'Vui lòng nhập mật khẩu hiện tại',
      });
    }

    if (!newPassword || newPassword.length < 8) {
      return res.status(400).json({
        success: false,
        code: 'VALIDATION_ERROR',
        message: 'Mật khẩu mới phải có tối thiểu 8 ký tự',
      });
    }

    if (!/[a-zA-Z]/.test(newPassword) || !/[0-9]/.test(newPassword)) {
      return res.status(400).json({
        success: false,
        code: 'VALIDATION_ERROR',
        message: 'Mật khẩu mới phải bao gồm cả chữ cái và chữ số',
      });
    }

    if (confirmPassword !== undefined && newPassword !== confirmPassword) {
      return res.status(400).json({
        success: false,
        code: 'VALIDATION_ERROR',
        message: 'Xác nhận mật khẩu mới không trùng khớp',
      });
    }

    if (currentPassword === newPassword) {
      return res.status(400).json({
        success: false,
        code: 'VALIDATION_ERROR',
        message: 'Mật khẩu mới không được trùng với mật khẩu hiện tại',
      });
    }

    const passwordIsValid = await verifyPassword(currentPassword, req.user);
    if (!passwordIsValid) {
      return res.status(400).json({
        success: false,
        code: 'INVALID_CURRENT_PASSWORD',
        message: 'Mật khẩu hiện tại không chính xác',
      });
    }

    req.user.passwordHash = await hashPassword(newPassword, req.user.id);
    req.user.passwordChangedAt = Date.now();

    return res.status(200).json({
      success: true,
      code: 'AUTH_CHANGE_PASSWORD_SUCCESS',
      message: 'Đổi mật khẩu thành công. Các phiên đăng nhập trên thiết bị khác đã được thu hồi.',
      user: toPublicUser(req.user),
    });
  } catch (error) {
    return next(error);
  }
});

module.exports = router;
