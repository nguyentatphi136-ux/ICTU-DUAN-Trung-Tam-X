const express = require('express');
const { ROLES, ROLE_LABELS, isValidRole } = require('../constants/roles');
const {
  assignRole,
  createUser,
  findUserByEmail,
  findUserById,
  hasRole,
  revokeRole,
  toPublicUser,
  updateUser,
  users,
} = require('../data/users');
const { authenticate, authorize } = require('../middleware/auth');

const router = express.Router();
const allowedStatuses = new Set(['active', 'inactive', 'locked']);
const editableFields = new Set(['email', 'name', 'phone', 'status', 'lockedReason', 'reason']);

router.use(authenticate, authorize(ROLES.ADMIN));

router.get('/users', (req, res) => {
  const { q, keyword, role, status, page = 1, limit = 20 } = req.query;
  const kw = (q || keyword || '').trim().toLowerCase();
  const filterRole = role ? role.trim() : null;
  const filterStatus = status ? status.trim().toLowerCase() : null;

  let filtered = users;
  if (kw) {
    filtered = filtered.filter(
      (u) =>
        u.name.toLowerCase().includes(kw) ||
        u.email.toLowerCase().includes(kw) ||
        (u.phone && u.phone.includes(kw))
    );
  }
  if (filterRole && filterRole !== 'ALL') {
    filtered = filtered.filter((u) => u.roles.some((r) => r.toLowerCase() === filterRole.toLowerCase()));
  }
  if (filterStatus && filterStatus !== 'all') {
    filtered = filtered.filter((u) => u.status === filterStatus);
  }

  const totalCount = filtered.length;
  const pageNum = Math.max(1, parseInt(page, 10) || 1);
  const pageSize = Math.max(1, parseInt(limit, 10) || 20);
  const totalPages = Math.ceil(totalCount / pageSize) || 1;
  const offset = (pageNum - 1) * pageSize;
  const items = filtered.slice(offset, offset + pageSize).map(toPublicUser);

  return res.status(200).json({
    success: true,
    totalCount,
    page: pageNum,
    pageSize,
    totalPages,
    items,
  });
});

router.post('/users', (req, res) => {
  const body = req.body || {};
  const email = (body.email || '').trim().toLowerCase();
  const name = (body.name || body.fullName || '').trim();
  const phone = (body.phone || '').trim();
  const rawRoles = Array.isArray(body.roles) ? body.roles : (body.role ? [body.role] : [ROLES.STUDENT]);

  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return res.status(400).json({ success: false, message: 'Địa chỉ email không hợp lệ' });
  }
  if (!name) {
    return res.status(400).json({ success: false, message: 'Vui lòng nhập họ và tên người dùng' });
  }

  if (findUserByEmail(email)) {
    return res.status(409).json({
      success: false,
      code: 'EMAIL_ALREADY_EXISTS',
      message: 'Email này đã tồn tại trong hệ thống, vui lòng chọn email khác!',
    });
  }

  const tempPassword = body.tempPassword || body.password || 'Edu@123456';
  const newId = String(users.length + 1);
  const newUser = createUser(newId, email, name, rawRoles, tempPassword);
  newUser.phone = phone;
  users.push(newUser);

  return res.status(201).json({
    success: true,
    message: 'Tạo tài khoản thành công. Email kích hoạt kèm mật khẩu tạm đã được gửi.',
    user: toPublicUser(newUser),
    tempPassword,
  });
});

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
    const rawStatus = typeof body.status === 'string' ? body.status.toLowerCase() : body.status;
    if (!allowedStatuses.has(rawStatus)) {
      return res.status(400).json({ success: false, message: 'Trạng thái không hợp lệ' });
    }
    if (user.id === req.user.id && rawStatus !== 'active') {
      return res.status(409).json({
        success: false,
        code: 'CANNOT_DEACTIVATE_SELF',
        message: 'Không thể tự khoá hoặc ngừng hoạt động tài khoản của chính mình',
      });
    }
    changes.status = rawStatus;
    if (rawStatus === 'locked') {
      changes.lockedReason = (body.lockedReason || body.reason || 'Khoá tài khoản bởi quản trị viên').trim();
    } else {
      changes.lockedReason = null;
    }
  }

  const updatedUser = updateUser(user, changes);
  const assignedClasses = user.roles.includes('Instructor') || user.roles.includes('TeachingAssistant')
    ? [{ classCode: 'JV01', className: 'Lớp Lập trình Java K12', roleInClass: 'Giảng viên chính' }]
    : [];

  return res.status(200).json({
    success: true,
    message: changes.status === 'locked'
      ? 'Khoá tài khoản thành công. Toàn bộ phiên đăng nhập đã được thu hồi.'
      : 'User updated successfully',
    user: toPublicUser(updatedUser),
    assignedClasses,
    requiresHandover: assignedClasses.length > 0,
  });
});

router.put('/users/:id/status', (req, res, next) => {
  const status = typeof req.body?.status === 'string' ? req.body.status.toLowerCase() : req.body?.status;
  if (status === 'locked' && !req.body?.lockedReason && !req.body?.reason) {
    return res.status(400).json({
      success: false,
      code: 'REASON_REQUIRED',
      message: 'Bắt buộc phải ghi rõ lý do khi khoá tài khoản.',
    });
  }
  return router.handle(Object.assign(req, { url: `/users/${req.params.id}` }), res, next);
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
