const { randomBytes, scrypt, timingSafeEqual } = require('node:crypto');
const { promisify } = require('node:util');
const express = require('express');
const jwt = require('jsonwebtoken');
const {
  addUser,
  findUserByEmail,
  findUserById,
  updateUser,
  users,
} = require('../data/users');

const router = express.Router();
const scryptAsync = promisify(scrypt);
const invalidCredentialsMessage = 'Email hoặc mật khẩu không chính xác';
const duplicateEmailMessage = 'Email này đã tồn tại trong hệ thống, vui lòng chọn email khác!';
const dummySalt = randomBytes(16);
const dummyHash = randomBytes(64);
const allowedRoles = new Set(['Admin', 'Teacher', 'Student']);

function publicUser(user) {
  return { id: user.id, email: user.email, name: user.name, role: user.role };
}

function requireAdmin(req, res, next) {
  const authorization = req.get('authorization') || '';
  const [scheme, token] = authorization.split(' ');

  if (scheme !== 'Bearer' || !token) {
    return res.status(401).json({ success: false, message: 'Vui lòng đăng nhập để tiếp tục' });
  }

  try {
    const claims = jwt.verify(token, process.env.JWT_SECRET || 'development-only-change-this-secret', {
      algorithms: ['HS256'],
    });
    if (claims.role !== 'Admin') {
      return res.status(403).json({ success: false, message: 'Bạn không có quyền thực hiện thao tác này' });
    }
    return next();
  } catch {
    return res.status(401).json({ success: false, message: 'Phiên đăng nhập không hợp lệ hoặc đã hết hạn' });
  }
}

router.post('/register', (req, res) => {
  const { email, name, password } = req.body || {};
  if (
    typeof email !== 'string' ||
    !email.trim() ||
    typeof name !== 'string' ||
    !name.trim() ||
    typeof password !== 'string' ||
    !password
  ) {
    return res.status(400).json({
      success: false,
      message: 'Email, họ tên và mật khẩu là bắt buộc',
    });
  }

  if (findUserByEmail(email)) {
    return res.status(409).json({
      success: false,
      code: 'EMAIL_ALREADY_EXISTS',
      message: duplicateEmailMessage,
    });
  }

  const user = addUser({ email, name: name.trim(), role: 'Student', password });
  return res.status(201).json({
    success: true,
    message: 'Tạo tài khoản thành công',
    user: publicUser(user),
  });
});

router.patch('/users/:id', requireAdmin, (req, res) => {
  const user = findUserById(req.params.id);
  if (!user) {
    return res.status(404).json({ success: false, message: 'Không tìm thấy tài khoản' });
  }

  const { email, name, role, password } = req.body || {};
  const hasChanges = [email, name, role, password].some((value) => value !== undefined);
  if (!hasChanges) {
    return res.status(400).json({ success: false, message: 'Vui lòng cung cấp thông tin cần cập nhật' });
  }
  if (email !== undefined && (typeof email !== 'string' || !email.trim())) {
    return res.status(400).json({ success: false, message: 'Email không hợp lệ' });
  }
  if (name !== undefined && (typeof name !== 'string' || !name.trim())) {
    return res.status(400).json({ success: false, message: 'Họ tên không hợp lệ' });
  }
  if (role !== undefined && !allowedRoles.has(role)) {
    return res.status(400).json({ success: false, message: 'Vai trò không hợp lệ' });
  }
  if (password !== undefined && (typeof password !== 'string' || !password)) {
    return res.status(400).json({ success: false, message: 'Mật khẩu không hợp lệ' });
  }

  if (email !== undefined && findUserByEmail(email, user.id)) {
    return res.status(409).json({
      success: false,
      code: 'EMAIL_ALREADY_EXISTS',
      message: duplicateEmailMessage,
    });
  }

  updateUser(user, { email, name, role, password });
  return res.status(200).json({
    success: true,
    message: 'Cập nhật tài khoản thành công',
    user: publicUser(user),
  });
});

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
