const express = require('express');
const jwt = require('jsonwebtoken');
const { findUserByEmail, findUserById, updateUser } = require('../data/users');

const router = express.Router();
const allowedRoles = new Set(['Admin', 'Teacher', 'Student']);
const allowedStatuses = new Set(['active', 'inactive', 'locked']);
const editableFields = new Set(['email', 'name', 'phone', 'role', 'status']);

function requireAdmin(req, res, next) {
  const [scheme, token] = (req.get('authorization') || '').split(' ');
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

function publicUser(user) {
  return {
    id: user.id,
    email: user.email,
    name: user.name,
    phone: user.phone,
    role: user.role,
    status: user.status,
  };
}

router.put('/users/:id', requireAdmin, (req, res) => {
  const user = findUserById(req.params.id);
  if (!user) {
    return res.status(404).json({ success: false, message: 'User not found' });
  }

  const body = req.body;
  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    return res.status(400).json({ success: false, message: 'Request body phải là một object JSON' });
  }

  const fields = Object.keys(body);
  if (fields.length === 0) {
    return res.status(400).json({ success: false, message: 'Vui lòng cung cấp thông tin cần cập nhật' });
  }

  const unknownField = fields.find((field) => !editableFields.has(field));
  if (unknownField) {
    return res.status(400).json({ success: false, message: `Trường không được phép cập nhật: ${unknownField}` });
  }

  const changes = {};
  if (Object.hasOwn(body, 'email')) {
    if (typeof body.email !== 'string' || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(body.email.trim())) {
      return res.status(400).json({ success: false, message: 'Email không hợp lệ' });
    }
    if (findUserByEmail(body.email, user.id)) {
      return res.status(409).json({
        success: false,
        code: 'EMAIL_ALREADY_EXISTS',
        message: 'Email này đã tồn tại trong hệ thống, vui lòng chọn email khác!',
      });
    }
    changes.email = body.email;
  }

  if (Object.hasOwn(body, 'name')) {
    if (typeof body.name !== 'string' || !body.name.trim()) {
      return res.status(400).json({ success: false, message: 'Họ tên không hợp lệ' });
    }
    changes.name = body.name;
  }

  if (Object.hasOwn(body, 'phone')) {
    if (typeof body.phone !== 'string') {
      return res.status(400).json({ success: false, message: 'Số điện thoại không hợp lệ' });
    }
    changes.phone = body.phone;
  }

  if (Object.hasOwn(body, 'role')) {
    if (!allowedRoles.has(body.role)) {
      return res.status(400).json({ success: false, message: 'Vai trò không hợp lệ' });
    }
    changes.role = body.role;
  }

  if (Object.hasOwn(body, 'status')) {
    if (!allowedStatuses.has(body.status)) {
      return res.status(400).json({ success: false, message: 'Trạng thái không hợp lệ' });
    }
    changes.status = body.status;
  }

  const updatedUser = updateUser(user, changes);
  return res.status(200).json({
    success: true,
    message: 'User updated successfully',
    user: publicUser(updatedUser),
  });
});

module.exports = router;
