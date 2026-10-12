// Kiểm từng dòng nhập (S2-01) khi chưa có backend, cùng quy tắc với AdminDAO.evaluateImportRow ở máy chủ:
// lỗi (thiếu, sai định dạng, vai trò lạ) > trùng (email, số điện thoại trong tệp hoặc đã có tài khoản) > hợp lệ.
import { ROLES } from '../../data/permissions';
import { saveAll } from '../../data/store';
import { USERS } from '../../data/users';
import { digits, EMAIL_RE, PHONE_RE } from '../../data/validate';

export type RowStatus = 'ok' | 'duplicate' | 'error';
export type ImportRow = { line: number; name: string; email: string; phone: string; role: string; birth: string; status: RowStatus; issues: string[] };

export const MAX_ROWS = 5000;
export const COLUMNS = ['Họ và tên', 'Email', 'Số điện thoại', 'Vai trò', 'Ngày sinh', 'Giới tính', 'Địa chỉ'];
export const TEMPLATE_ROWS = [
  ['Nguyễn Văn An', 'an.nguyen@tms.vn', '0912345678', 'Học viên', '2003-05-15', 'Nam', 'Thái Nguyên'],
  ['Trần Thị Bình', 'binh.tran@tms.vn', '0987654321', 'Học viên', '2002-11-20', 'Nữ', 'Hà Nội'],
  ['Lê Hoàng Cường', 'cuong.le@tms.vn', '0903112233', 'Giảng viên', '1990-08-10', 'Nam', 'Đà Nẵng'],
];

type Raw = { name: string; email: string; phone: string; role: string; birth: string };

/** Tên vai trò trong tệp (có dấu hoặc không, hoặc mã tiếng Anh) -> chỉ số trong ROLES; -1 nếu không nhận ra. Không nhập được Quản trị. */
const ROLE_KEYS: Record<string, number> = {
  HOCVIEN: 0, STUDENT: 0, TROGIANG: 1, TA: 1, GIANGVIEN: 2, INSTRUCTOR: 2,
  TUVAN: 3, TUVANTUYENSINH: 3, ADMISSIONS: 3, KETOAN: 4, ACCOUNTANT: 4, QUANLYDAOTAO: 5, TRAININGMANAGER: 5,
};
export function roleIndex(raw: string): number {
  if (!raw.trim()) return 0;
  const key = raw.normalize('NFD').replace(/\p{M}/gu, '').replace(/[đĐ]/g, 'D').toUpperCase().replace(/[^A-Z]/g, '');
  return ROLE_KEYS[key] ?? -1;
}

/** Số 9 chữ số do Excel làm mất số 0 đầu được thêm lại; ô ngày dạng số sê-ri Excel đổi sang YYYY-MM-DD. */
export const fixPhone = (p: string) => (/^[35789]\d{8}$/.test(digits(p)) ? '0' + digits(p) : digits(p));
const fixDate = (v: string) => (/^\d{4,5}(\.\d+)?$/.test(v) ? new Date(Date.UTC(1899, 11, 30) + Math.round(Number(v)) * 86400000).toISOString().slice(0, 10) : v);

/** Dòng tiêu đề -> vị trí cột (giống UserImportParser.columnIndexes ở máy chủ). */
export function toRaws(table: string[][]): Raw[] {
  const start = table.findIndex((r) => r.some((c) => c.trim()));
  if (start < 0) return [];
  const idx = { name: -1, email: -1, phone: -1, role: -1, birth: -1 };
  table[start].forEach((title, i) => {
    const h = title.toLowerCase().replace(/[\s_]/g, '');
    const field: keyof typeof idx | null = /e?mail/.test(h)
      ? 'email'
      : /thoại|phone|sđt|sdt/.test(h)
        ? 'phone'
        : /trò|role/.test(h)
          ? 'role'
          : /sinh|dob|birth/.test(h)
            ? 'birth'
            : /tính|gender|chỉ|address/.test(h)
              ? null
              : /họ|tên|name/.test(h)
                ? 'name'
                : null;
    if (field && idx[field] < 0) idx[field] = i;
  });
  if (idx.name < 0 || idx.email < 0) throw new Error('Dòng đầu của tệp phải có cột "Họ và tên" và "Email". Hãy dùng tệp mẫu.');
  const cell = (r: string[], i: number) => (i >= 0 ? (r[i] ?? '').trim() : '');
  return table
    .slice(start + 1)
    .filter((r) => r.some((c) => c.trim()))
    .map((r) => ({ name: cell(r, idx.name), email: cell(r, idx.email).toLowerCase(), phone: fixPhone(cell(r, idx.phone)), role: cell(r, idx.role), birth: fixDate(cell(r, idx.birth)) }));
}

export function evaluate(raws: Raw[]): ImportRow[] {
  const emails = new Set(USERS.map((u) => u.email.toLowerCase()));
  const phones = new Map(USERS.map((u) => [digits(u.phone), u.name]));
  const seenEmails = new Set<string>();
  const seenPhones = new Set<string>();
  return raws.map((r, i) => {
    const invalid: string[] = [];
    const dup: string[] = [];
    if (!r.name) invalid.push('Thiếu họ và tên');
    if (!r.email) invalid.push('Thiếu địa chỉ email');
    else if (!EMAIL_RE.test(r.email)) invalid.push('Định dạng email không hợp lệ');
    else if (seenEmails.has(r.email)) dup.push('Email bị trùng với một dòng khác trong tệp');
    else if (emails.has(r.email)) dup.push('Email đã được dùng cho tài khoản khác');
    seenEmails.add(r.email);
    if (r.phone) {
      if (!PHONE_RE.test(r.phone)) invalid.push('Số điện thoại không đúng chuẩn di động VN');
      else if (seenPhones.has(r.phone)) dup.push('Số điện thoại bị trùng với một dòng khác trong tệp');
      else if (phones.has(r.phone)) dup.push('Số điện thoại đã được dùng cho tài khoản ' + phones.get(r.phone));
      seenPhones.add(r.phone);
    }
    const role = roleIndex(r.role);
    if (role < 0) invalid.push(`Vai trò "${r.role}" không có trong hệ thống`);
    if (r.birth && !/^\d{4}-\d{2}-\d{2}$/.test(r.birth)) invalid.push('Ngày sinh phải có dạng YYYY-MM-DD');
    const status: RowStatus = invalid.length ? 'error' : dup.length ? 'duplicate' : 'ok';
    return { line: i + 1, name: r.name, email: r.email, phone: r.phone, role: role >= 0 ? ROLES[role] : r.role, birth: r.birth, status, issues: [...invalid, ...dup] };
  });
}

/** Tạo tài khoản cho các dòng hợp lệ (dữ liệu mẫu). Trả về số tài khoản đã tạo. */
export function importOk(rows: ImportRow[]): number {
  let id = Math.max(0, ...USERS.map((u) => u.id));
  const ok = rows.filter((r) => r.status === 'ok');
  for (const r of ok) {
    USERS.unshift({ id: ++id, name: r.name, email: r.email, phone: r.phone, roles: [ROLES.indexOf(r.role as (typeof ROLES)[number])], status: 'Chờ kích hoạt' });
  }
  saveAll();
  return ok.length;
}
