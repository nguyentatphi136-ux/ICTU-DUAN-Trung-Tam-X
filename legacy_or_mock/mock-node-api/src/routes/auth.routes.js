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

// S1-03 AC1, AC2, AC3: Quên mật khẩu qua email - Anti-probing, Constant-time, 15m Rate Limiting
const resetTokens = new Map();
const rateLimitStore = new Map();
const RATE_LIMIT_COOLDOWN_MS = 15 * 60 * 1000; // 15 phút

router.post('/forgot-password', async (req, res, next) => {
  const startTime = Date.now();
  try {
    const { email } = req.body || {};
    if (!email || !email.includes('@')) {
      return res.status(400).json({
        success: false,
        code: 'VALIDATION_ERROR',
        message: 'Vui lòng nhập địa chỉ email hợp lệ',
      });
    }

    const normalizedEmail = email.trim().toLowerCase();

    // Giới hạn 1 email chỉ gửi được 1 lần sau 15 phút (Rate Limiting)
    const lastSent = rateLimitStore.get(normalizedEmail);
    if (lastSent && (Date.now() - lastSent < RATE_LIMIT_COOLDOWN_MS)) {
      const remainingMinutes = Math.ceil((RATE_LIMIT_COOLDOWN_MS - (Date.now() - lastSent)) / 60000);
      return res.status(429).json({
        success: false,
        code: 'RATE_LIMITED',
        message: `Email này chỉ có thể nhận liên kết đặt lại mật khẩu 1 lần mỗi 15 phút để bảo vệ hệ thống. Vui lòng thử lại sau ${remainingMinutes} phút.`,
      });
    }
    rateLimitStore.set(normalizedEmail, Date.now());

    const user = users.find((u) => u.email === normalizedEmail);

    if (user) {
      const token = 'rst_' + Math.random().toString(36).substring(2, 10) + Date.now().toString(36);
      resetTokens.set(token, {
        userId: user.id,
        email: user.email,
        expiresAt: Date.now() + 30 * 60 * 1000, // 30 phút
        isUsed: false,
      });
    }

    // Bảo vệ chống tấn công đo lường thời gian (Constant-time ~ 200ms)
    const elapsed = Date.now() - startTime;
    if (elapsed < 200) {
      await new Promise((resolve) => setTimeout(resolve, 200 - elapsed));
    }

    // Luôn trả về cùng một thông báo chung dù email có tồn tại hay không (Anti-probing)
    return res.status(200).json({
      success: true,
      code: 'AUTH_RESET_ACCEPTED',
      message: 'Nếu địa chỉ email tồn tại trên hệ thống, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu qua email trong vài phút.',
    });
  } catch (error) {
    return res.status(200).json({
      success: true,
      message: 'Nếu địa chỉ email tồn tại trên hệ thống, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu qua email trong vài phút.',
    });
  }
});

module.exports = router;

