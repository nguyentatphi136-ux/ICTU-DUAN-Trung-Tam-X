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

module.exports = { authenticate, authorize };
