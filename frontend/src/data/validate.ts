// Kiểm tra định dạng dùng chung cho các biểu mẫu. Máy chủ vẫn phải kiểm lại.
export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
/** Số di động Việt Nam: 10 chữ số, đầu 03, 05, 07, 08, 09 (giống UserDAO.VN_PHONE_PATTERN ở máy chủ). */
export const PHONE_RE = /^0[35789]\d{8}$/;
export const PHONE_MSG = 'Số điện thoại di động gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09';
/** Bỏ khoảng trắng, dấu chấm, gạch nối trước khi kiểm hoặc so sánh. */
export const digits = (phone: string) => phone.replace(/[\s.-]/g, '');
export const isPhone = (phone: string) => PHONE_RE.test(digits(phone));
