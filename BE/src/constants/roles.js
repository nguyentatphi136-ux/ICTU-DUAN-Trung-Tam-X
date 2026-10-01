// Danh mục vai trò thống nhất với frontend (frontend/app.js -> ROLES).
const ROLES = Object.freeze({
  ADMIN: 'Admin',
  TRAINING_MANAGER: 'TrainingManager',
  INSTRUCTOR: 'Instructor',
  TEACHING_ASSISTANT: 'TeachingAssistant',
  ADMISSIONS: 'Admissions',
  ACCOUNTANT: 'Accountant',
  STUDENT: 'Student',
});

const ROLE_LABELS = Object.freeze({
  [ROLES.ADMIN]: 'Quản trị hệ thống',
  [ROLES.TRAINING_MANAGER]: 'Quản lý đào tạo',
  [ROLES.INSTRUCTOR]: 'Giảng viên',
  [ROLES.TEACHING_ASSISTANT]: 'Trợ giảng',
  [ROLES.ADMISSIONS]: 'Tư vấn tuyển sinh',
  [ROLES.ACCOUNTANT]: 'Kế toán',
  [ROLES.STUDENT]: 'Học viên',
});

const allRoles = new Set(Object.values(ROLES));

function isValidRole(role) {
  return typeof role === 'string' && allRoles.has(role);
}

module.exports = { ROLES, ROLE_LABELS, isValidRole };
