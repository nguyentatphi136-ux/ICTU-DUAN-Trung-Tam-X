const jwt = require('jsonwebtoken');
const jwtConfig = require('../config/jwt');
const { isValidRole } = require('../constants/roles');
const { findUserById } = require('../data/users');

// Xác thực JWT rồi nạp người dùng từ kho dữ liệu. Vai trò và trạng thái luôn
// lấy từ kho chứ không lấy từ token, nên thu hồi vai trò hoặc khoá tài khoản
// có hiệu lực ngay ở request kế tiếp, không phải chờ token hết hạn.
function authenticate(req, res, next) {
  const [scheme, token] = (req.get('authorization') || '').split(' ');
  if (scheme !== 'Bearer' || !token) {
    return res.status(401).json({ success: false, message: 'Vui lòng đăng nhập để tiếp tục' });
  }

  let claims;
  try {
    claims = jwt.verify(token, jwtConfig.secret, { algorithms: [jwtConfig.algorithm] });
  } catch {
    return res.status(401).json({ success: false, message: 'Phiên đăng nhập không hợp lệ hoặc đã hết hạn' });
  }

  const user = findUserById(claims.sub);
  if (!user) {
    return res.status(401).json({ success: false, message: 'Phiên đăng nhập không hợp lệ hoặc đã hết hạn' });
  }

  if (user.status !== 'active') {
    return res.status(403).json({
      success: false,
      code: 'ACCOUNT_INACTIVE',
      message: 'Tài khoản đã bị khoá hoặc ngừng hoạt động',
    });
  }

  req.user = user;
  return next();
}

// Cho qua nếu người dùng có ít nhất một trong các vai trò được phép.
// Dùng sau authenticate: router.use(authenticate, authorize(ROLES.ADMIN)).
function authorize(...allowedRoles) {
  if (allowedRoles.length === 0 || !allowedRoles.every(isValidRole)) {
    throw new Error(`authorize() nhận danh sách vai trò không hợp lệ: ${JSON.stringify(allowedRoles)}`);
  }

  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ success: false, message: 'Vui lòng đăng nhập để tiếp tục' });
    }

    if (!req.user.roles.some((role) => allowedRoles.includes(role))) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: 'Bạn không có quyền thực hiện thao tác này',
      });
    }

    return next();
  };
}

const { PERMISSIONS, hasPermission } = require('../constants/permissions');
const { ROLES } = require('../constants/roles');

/**
 * Middleware kiểm tra quyền hạn chi tiết (Permission-based Authorization)
 * @param {string} permissionCode Mã quyền trong PERMISSIONS
 * @param {string} [customDeniedMessage] Thông báo lỗi tiếng Việt tùy chỉnh
 */
function requirePermission(permissionCode, customDeniedMessage) {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ success: false, message: 'Vui lòng đăng nhập để tiếp tục' });
    }

    if (!hasPermission(req.user.roles, permissionCode)) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: customDeniedMessage || 'Bạn không có quyền thực hiện thao tác này',
      });
    }

    return next();
  };
}

/**
 * Middleware mặc định từ chối ở tầng máy chủ (Default-deny principle)
 * Mọi yêu cầu không thỏa mãn quyền đều bị từ chối rõ ràng bằng tiếng Việt
 */
function defaultDeny(customMessage) {
  return (req, res) => {
    return res.status(403).json({
      success: false,
      code: 'DEFAULT_DENIED',
      message: customMessage || 'Truy cập bị từ chối: Chức năng chưa được cấp quyền ở tầng máy chủ',
    });
  };
}

/**
 * Middleware nghiệp vụ cho điểm số:
 * Đảm bảo Giảng viên & Quản lý đào tạo & Admin sửa được điểm.
 * Kế toán và các vai trò khác bị từ chối với thông báo tiếng Việt cụ thể.
 */
function checkGradePermission(action = 'view') {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ success: false, message: 'Vui lòng đăng nhập để tiếp tục' });
    }

    if (action === 'edit') {
      const isAccountantOnly = req.user.roles.includes(ROLES.ACCOUNTANT) &&
        !req.user.roles.includes(ROLES.ADMIN) &&
        !req.user.roles.includes(ROLES.INSTRUCTOR) &&
        !req.user.roles.includes(ROLES.TRAINING_MANAGER);

      if (isAccountantOnly) {
        return res.status(403).json({
          success: false,
          code: 'FORBIDDEN',
          message: 'Kế toán không có quyền chỉnh sửa điểm số học viên',
        });
      }

      if (!hasPermission(req.user.roles, PERMISSIONS.GRADE_EDIT)) {
        return res.status(403).json({
          success: false,
          code: 'FORBIDDEN',
          message: 'Bạn không có quyền chỉnh sửa điểm số học viên',
        });
      }
    }

    return next();
  };
}

/**
 * Middleware nghiệp vụ cho học phí:
 * Đảm bảo Kế toán & Admin sửa được học phí.
 * Giảng viên và các vai trò khác bị từ chối với thông báo tiếng Việt cụ thể.
 */
function checkTuitionPermission(action = 'view') {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ success: false, message: 'Vui lòng đăng nhập để tiếp tục' });
    }

    if (action === 'edit') {
      const isInstructorOnly = (req.user.roles.includes(ROLES.INSTRUCTOR) || req.user.roles.includes(ROLES.TEACHING_ASSISTANT)) &&
        !req.user.roles.includes(ROLES.ADMIN) &&
        !req.user.roles.includes(ROLES.ACCOUNTANT);

      if (isInstructorOnly) {
        return res.status(403).json({
          success: false,
          code: 'FORBIDDEN',
          message: 'Giảng viên không có quyền chỉnh sửa thông tin học phí',
        });
      }

      if (!hasPermission(req.user.roles, PERMISSIONS.TUITION_EDIT)) {
        return res.status(403).json({
          success: false,
          code: 'FORBIDDEN',
          message: 'Bạn không có quyền chỉnh sửa thông tin học phí',
        });
      }
    }

    return next();
  };
}

module.exports = {
  authenticate,
  authorize,
  checkGradePermission,
  checkTuitionPermission,
  defaultDeny,
  requirePermission,
};
