const { ROLES } = require('./roles');

// Danh mục toàn bộ 8 vai trò nghiệp vụ của hệ thống theo đặc tả User Story IDTTX-20 & README
const ALL_ROLES = Object.freeze({
  ADMIN: ROLES.ADMIN || 'Admin',
  TRAINING_MANAGER: ROLES.TRAINING_MANAGER || 'TrainingManager',
  ADMISSIONS: ROLES.ADMISSIONS || 'Admissions',
  INSTRUCTOR: ROLES.INSTRUCTOR || 'Instructor',
  TEACHING_ASSISTANT: ROLES.TEACHING_ASSISTANT || 'TeachingAssistant',
  ACCOUNTANT: ROLES.ACCOUNTANT || 'Accountant',
  STUDENT: ROLES.STUDENT || 'Student',
  GUEST: 'Guest',
});

// Danh mục quyền hạn chi tiết (Permissions)
const PERMISSIONS = Object.freeze({
  // Phân hệ Quản trị & Tài khoản
  USER_VIEW: 'USER_VIEW',
  USER_EDIT: 'USER_EDIT',
  USER_LOCK: 'USER_LOCK',
  ROLE_VIEW: 'ROLE_VIEW',
  ROLE_ASSIGN: 'ROLE_ASSIGN',

  // Phân hệ Điểm số (Grade) - Giảng viên được sửa, Kế toán KHÔNG được sửa
  GRADE_VIEW: 'GRADE_VIEW',
  GRADE_EDIT: 'GRADE_EDIT',

  // Phân hệ Học phí (Tuition) - Kế toán được sửa, Giảng viên KHÔNG được sửa
  TUITION_VIEW: 'TUITION_VIEW',
  TUITION_EDIT: 'TUITION_EDIT',

  // Phân hệ Đào tạo & Lớp học
  CLASS_MANAGE: 'CLASS_MANAGE',
  SCHEDULE_MANAGE: 'SCHEDULE_MANAGE',

  // Phân hệ Tuyển sinh
  LEAD_MANAGE: 'LEAD_MANAGE',

  // Cổng thông tin công khai
  PUBLIC_VIEW: 'PUBLIC_VIEW',
});

// Ma trận quyền hạn cho 8 vai trò nghiệp vụ
// Quy tắc cốt lõi:
// 1. Giảng viên (Instructor): CÓ GRADE_EDIT, TUYỆT ĐỐI KHÔNG CÓ TUITION_EDIT
// 2. Kế toán (Accountant): CÓ TUITION_EDIT, TUYỆT ĐỐI KHÔNG CÓ GRADE_EDIT
// 3. Quản trị hệ thống (Admin): Toàn quyền
// 4. Học viên (Student): Chỉ xem (GRADE_VIEW, TUITION_VIEW)
const ROLE_PERMISSIONS = Object.freeze({
  [ALL_ROLES.ADMIN]: Object.values(PERMISSIONS),

  [ALL_ROLES.TRAINING_MANAGER]: [
    PERMISSIONS.CLASS_MANAGE,
    PERMISSIONS.SCHEDULE_MANAGE,
    PERMISSIONS.GRADE_VIEW,
    PERMISSIONS.GRADE_EDIT,
    PERMISSIONS.USER_VIEW,
    PERMISSIONS.PUBLIC_VIEW,
  ],

  [ALL_ROLES.INSTRUCTOR]: [
    PERMISSIONS.GRADE_VIEW,
    PERMISSIONS.GRADE_EDIT, // Giảng viên được sửa điểm
    // KHÔNG CÓ TUITION_EDIT
    PERMISSIONS.PUBLIC_VIEW,
  ],

  [ALL_ROLES.TEACHING_ASSISTANT]: [
    PERMISSIONS.GRADE_VIEW,
    PERMISSIONS.PUBLIC_VIEW,
  ],

  [ALL_ROLES.ADMISSIONS]: [
    PERMISSIONS.LEAD_MANAGE,
    PERMISSIONS.PUBLIC_VIEW,
  ],

  [ALL_ROLES.ACCOUNTANT]: [
    PERMISSIONS.TUITION_VIEW,
    PERMISSIONS.TUITION_EDIT, // Kế toán được sửa học phí
    PERMISSIONS.GRADE_VIEW,   // Kế toán chỉ xem điểm, KHÔNG ĐƯỢC SỬA ĐIỂM
    PERMISSIONS.PUBLIC_VIEW,
  ],

  [ALL_ROLES.STUDENT]: [
    PERMISSIONS.GRADE_VIEW,
    PERMISSIONS.TUITION_VIEW,
    PERMISSIONS.PUBLIC_VIEW,
  ],

  [ALL_ROLES.GUEST]: [
    PERMISSIONS.PUBLIC_VIEW,
  ],
});

/**
 * Lấy toàn bộ danh sách quyền hạn hợp nhất từ danh sách vai trò của người dùng
 * @param {string[]} roles
 * @returns {Set<string>}
 */
function getPermissionsForRoles(roles = []) {
  const permissions = new Set();
  for (const role of roles) {
    const rolePerms = ROLE_PERMISSIONS[role];
    if (Array.isArray(rolePerms)) {
      rolePerms.forEach((p) => permissions.add(p));
    }
  }
  return permissions;
}

/**
 * Kiểm tra xem danh sách vai trò có chứa quyền yêu cầu hay không
 * @param {string[]} roles
 * @param {string} permission
 * @returns {boolean}
 */
function hasPermission(roles = [], permission) {
  if (!roles || roles.length === 0 || !permission) return false;
  return getPermissionsForRoles(roles).has(permission);
}

module.exports = {
  ALL_ROLES,
  PERMISSIONS,
  ROLE_PERMISSIONS,
  getPermissionsForRoles,
  hasPermission,
};
