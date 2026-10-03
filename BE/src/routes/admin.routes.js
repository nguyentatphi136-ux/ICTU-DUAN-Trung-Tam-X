const express = require('express');
const { ROLES, ROLE_LABELS, isValidRole } = require('../constants/roles');
const {
  assignRole,
  findUserByEmail,
  findUserById,
  hasRole,
  revokeRole,
  toPublicUser,
  updateUser,
} = require('../data/users');
const { authenticate, authorize } = require('../middleware/auth');

const router = express.Router();
const allowedStatuses = new Set(['active', 'inactive', 'locked']);
const editableFields = new Set(['email', 'name', 'phone', 'status']);

router.use(authenticate, authorize(ROLES.ADMIN));

router.param('id', (req, res, next, id) => {
  req.targetUser = findUserById(id);
  if (!req.targetUser) {
    return res.status(404).json({ success: false, message: 'User not found' });
  }
  return next();
});

router.put('/users/:id', (req, res) => {
  const user = req.targetUser;

  const body = req.body;
  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    return res.status(400).json({ success: false, message: 'Request body phải là một object JSON' });
  }

  const fields = Object.keys(body);
  if (fields.length === 0) {
    return res.status(400).json({ success: false, message: 'Vui lòng cung cấp thông tin cần cập nhật' });
  }

  if (fields.includes('role') || fields.includes('roles')) {
    return res.status(400).json({
      success: false,
      message: 'Vai trò được phân bổ và thu hồi qua /admin/users/:id/roles',
    });
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

  if (Object.hasOwn(body, 'status')) {
    if (!allowedStatuses.has(body.status)) {
      return res.status(400).json({ success: false, message: 'Trạng thái không hợp lệ' });
    }
    if (user.id === req.user.id && body.status !== 'active') {
      return res.status(409).json({
        success: false,
        code: 'CANNOT_DEACTIVATE_SELF',
        message: 'Không thể tự khoá hoặc ngừng hoạt động tài khoản của chính mình',
      });
    }
    changes.status = body.status;
  }

  const updatedUser = updateUser(user, changes);
  return res.status(200).json({
    success: true,
    message: 'User updated successfully',
    user: toPublicUser(updatedUser),
  });
});

router.get('/roles', (req, res) => {
  return res.status(200).json({
    success: true,
    roles: Object.values(ROLES).map((role) => ({ role, label: ROLE_LABELS[role] })),
  });
});

router.get('/users/:id/roles', (req, res) => {
  return res.status(200).json({
    success: true,
    userId: req.targetUser.id,
    roles: [...req.targetUser.roles],
  });
});

router.post('/users/:id/roles', (req, res) => {
  const user = req.targetUser;
  const role = req.body?.role;

  if (!isValidRole(role)) {
    return res.status(400).json({ success: false, message: 'Vai trò không hợp lệ' });
  }

  if (hasRole(user, role)) {
    return res.status(409).json({
      success: false,
      code: 'ROLE_ALREADY_ASSIGNED',
      message: `Người dùng đã có vai trò ${ROLE_LABELS[role]}`,
    });
  }

  assignRole(user, role);
  return res.status(201).json({
    success: true,
    message: `Đã phân bổ vai trò ${ROLE_LABELS[role]}`,
    user: toPublicUser(user),
  });
});

// roleId là mã vai trò trong danh mục ROLES, ví dụ /admin/users/3/roles/Accountant.
router.delete('/users/:id/roles/:roleId', (req, res) => {
  const user = req.targetUser;
  const role = req.params.roleId;

  if (!isValidRole(role)) {
    return res.status(400).json({ success: false, message: 'Vai trò không hợp lệ' });
  }

  if (!hasRole(user, role)) {
    return res.status(404).json({
      success: false,
      code: 'ROLE_NOT_ASSIGNED',
      message: `Người dùng không có vai trò ${ROLE_LABELS[role]}`,
    });
  }

  // Người thao tác luôn là một Admin còn hoạt động; chặn tự thu hồi (cùng với
  // chặn tự khoá ở PUT) đảm bảo hệ thống không bao giờ mất Admin cuối cùng.
  if (role === ROLES.ADMIN && user.id === req.user.id) {
    return res.status(409).json({
      success: false,
      code: 'CANNOT_REVOKE_OWN_ADMIN',
      message: 'Không thể tự thu hồi vai trò quản trị của chính mình',
    });
  }

  revokeRole(user, role);
  return res.status(200).json({
    success: true,
    message: `Đã thu hồi vai trò ${ROLE_LABELS[role]}`,
    user: toPublicUser(user),
  });
});

module.exports = router;
