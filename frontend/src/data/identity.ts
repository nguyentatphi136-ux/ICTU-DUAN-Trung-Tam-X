/**
 * Đưa tên về dạng hệ thống lưu: chữ in hoa không dấu, một khoảng trắng giữa các từ.
 * Dùng để tìm kiếm không phân biệt dấu và hoa thường, ví dụ "trần thu hà" khớp "TRAN THU HA".
 */
export const toSystemName = (s: string) =>
  s
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/gi, 'D')
    .toUpperCase()
    .trim()
    .replace(/\s+/g, ' ');
