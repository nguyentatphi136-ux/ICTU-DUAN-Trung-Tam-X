// Danh mục và định dạng mã, tài khoản học viên (S3-03 / IDTTX-211)
const STUDENT_STATUSES = Object.freeze([
  'Mới nhập học',
  'Đang học',
  'Bảo lưu',
  'Đã tốt nghiệp',
  'Thôi học',
]);

const DEFAULT_STUDENT_STATUS = 'Mới nhập học';

/**
 * Thêm số 0 vào đầu để đủ độ dài (ví dụ: pad(7, 3) => "007")
 */
function pad(n, width = 3) {
  return String(n).padStart(width, '0');
}

/**
 * Trích xuất số thứ tự từ mã học viên (HV260143 => 143)
 */
function seq(code) {
  if (!code || typeof code !== 'string') return 0;
  return Number(code.slice(4)) || 0;
}

/**
 * Sinh email tài khoản đăng nhập học viên theo định dạng TMS
 * Ví dụ số 143 => S25200212143@tms.vn
 */
function loginOf(sequenceNumber) {
  const n = Number(sequenceNumber) || 0;
  return `S25200212${pad(n % 1000, 3)}@tms.vn`;
}

/**
 * Kiểm tra trạng thái học viên hợp lệ
 */
function isValidStudentStatus(status) {
  return typeof status === 'string' && STUDENT_STATUSES.includes(status.trim());
}

/**
 * Số di động Việt Nam: 10 chữ số, đầu 03, 05, 07, 08, 09 (chuẩn UserDAO.VN_PHONE_PATTERN)
 */
const PHONE_RE = /^0[35789]\d{8}$/;
const PHONE_MSG = 'Số điện thoại di động gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09';

/**
 * Kiểm tra định dạng số điện thoại Việt Nam hợp lệ
 */
function isValidPhone(phone) {
  if (!phone || typeof phone !== 'string') return false;
  const digits = phone.replace(/[\s.-]/g, '');
  return PHONE_RE.test(digits);
}

/**
 * Rút trích chỉ các chữ số từ chuỗi điện thoại
 */
function cleanDigits(phone) {
  if (!phone || typeof phone !== 'string') return '';
  return phone.replace(/[\s.-]/g, '');
}

/**
 * Kiểm tra định dạng email
 */
function isValidEmail(email) {
  if (!email || typeof email !== 'string') return false;
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}

module.exports = {
  DEFAULT_STUDENT_STATUS,
  PHONE_MSG,
  PHONE_RE,
  STUDENT_STATUSES,
  cleanDigits,
  isValidEmail,
  isValidPhone,
  isValidStudentStatus,
  loginOf,
  pad,
  seq,
};

