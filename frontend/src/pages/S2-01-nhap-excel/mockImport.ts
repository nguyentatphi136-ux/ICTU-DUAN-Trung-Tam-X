// Kết quả kiểm tra giả cho bước xem trước của S2-01 (50 dòng, 4 dòng lỗi như trong thiết kế).
// Khi tích hợp: máy chủ đọc tệp, kiểm từng dòng (thiếu cột, email trùng, số điện thoại, vai trò) và trả về danh sách này.
export type ImportRow = { line: number; name: string; email: string; phone: string; role: string; error?: string };

const FIXED: Record<number, Omit<ImportRow, 'line'>> = {
  5: { name: 'LE MINH PHUC', email: 'S25200212105@tms.vn', phone: '0903 441 276', role: 'Học viên' },
  6: { name: 'TRAN THI TRANG', email: 'S25200212106@tms.vn', phone: '0938 120 554', role: 'Học viên' },
  7: { name: '', email: 'S25200212107@tms.vn', phone: '0912 880 142', role: 'Học viên', error: 'Thiếu họ tên' },
  8: { name: 'PHAM QUOC KHAI', email: 'S25200212108@tms.vn', phone: '0965 702 318', role: 'Học viên' },
  11: { name: 'HOANG GIA LINH', email: 'S25200212111@tms.vn', phone: '0919 663 207', role: 'Học viên' },
  12: { name: 'DANG NGOC VY', email: 'S25200212101@tms.vn', phone: '0944 205 771', role: 'Học viên', error: 'Email trùng với tài khoản đã có' },
  22: { name: 'VU DUC TU', email: 'TA25200212022@tms.vn', phone: '0981 254 690', role: 'Trợ giảng' },
  23: { name: 'BUI THANH SON', email: 'S25200212131@tms.vn', phone: '912 55 60', role: 'Học viên', error: 'Số điện thoại sai định dạng' },
  31: { name: 'NGO BAO KHOA', email: 'S25200212132@tms.vn', phone: '0977 310 468', role: 'Sinh viên', error: 'Vai trò không tồn tại' },
};

export function previewRows(): ImportRow[] {
  return [...Array(50)].map((_, i) => {
    const line = i + 1;
    const n = 200 + line;
    return { line, ...(FIXED[line] ?? { name: `HOC VIEN K15 ${line}`, email: `S25200212${n}@tms.vn`, phone: `0905 ${String(100 + line)} ${String(300 + line)}`, role: 'Học viên' }) };
  });
}
