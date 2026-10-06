// Kiểm tra định dạng dùng chung cho các biểu mẫu. Máy chủ vẫn phải kiểm lại.
export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
/** Số điện thoại Việt Nam: 10 chữ số, bắt đầu bằng 0 (bỏ khoảng trắng trước khi kiểm). */
export const PHONE_RE = /^0\d{9}$/;
