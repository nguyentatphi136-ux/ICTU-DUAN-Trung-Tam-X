import * as XLSX from 'xlsx';
import { loadUsers } from '../../data/users';
import { ROLES } from '../../data/permissions';

export type ImportRow = {
  line: number;
  name: string;
  email: string;
  phone: string;
  role: string;
  roleIndex?: number;
  error?: string;
};

const FIXED: Record<number, Omit<ImportRow, 'line'>> = {
  5: { name: 'LE MINH PHUC', email: 'S25200212105@tms.vn', phone: '0903 441 276', role: 'Học viên', roleIndex: 0 },
  6: { name: 'TRAN THI TRANG', email: 'S25200212106@tms.vn', phone: '0938 120 554', role: 'Học viên', roleIndex: 0 },
  7: { name: '', email: 'S25200212107@tms.vn', phone: '0912 880 142', role: 'Học viên', roleIndex: 0, error: 'Thiếu họ tên' },
  8: { name: 'PHAM QUOC KHAI', email: 'S25200212108@tms.vn', phone: '0965 702 318', role: 'Học viên', roleIndex: 0 },
  11: { name: 'HOANG GIA LINH', email: 'S25200212111@tms.vn', phone: '0919 663 207', role: 'Học viên', roleIndex: 0 },
  12: { name: 'DANG NGOC VY', email: 'S25200212101@tms.vn', phone: '0944 205 771', role: 'Học viên', roleIndex: 0, error: 'Email trùng với tài khoản đã có' },
  22: { name: 'VU DUC TU', email: 'TA25200212022@tms.vn', phone: '0981 254 690', role: 'Trợ giảng', roleIndex: 1 },
  23: { name: 'BUI THANH SON', email: 'S25200212131@tms.vn', phone: '912 55 60', role: 'Học viên', roleIndex: 0, error: 'Số điện thoại sai định dạng' },
  31: { name: 'NGO BAO KHOA', email: 'S25200212132@tms.vn', phone: '0977 310 468', role: 'Sinh viên', error: 'Vai trò không tồn tại' },
};

/** Dữ liệu mẫu ban đầu nếu người dùng chưa tải tệp lên. */
export function previewRows(): ImportRow[] {
  return [...Array(50)].map((_, i) => {
    const line = i + 1;
    const n = 200 + line;
    return {
      line,
      ...(FIXED[line] ?? {
        name: `HOC VIEN K15 ${line}`,
        email: `S25200212${n}@tms.vn`,
        phone: `0905 ${String(100 + line)} ${String(300 + line)}`,
        role: 'Học viên',
        roleIndex: 0,
      }),
    };
  });
}

function resolveRole(rawRole: string): { role: string; roleIndex: number } | null {
  const norm = (rawRole || '').trim().toLowerCase();
  if (!norm || norm === 'học viên' || norm === 'hoc vien' || norm === 'student' || norm === 'hoc_vien' || norm === 'hv') {
    return { role: 'Học viên', roleIndex: 0 };
  }
  if (norm === 'trợ giảng' || norm === 'tro giang' || norm === 'tro_giang' || norm === 'ta' || norm === 'teaching assistant') {
    return { role: 'Trợ giảng', roleIndex: 1 };
  }
  if (norm === 'giảng viên' || norm === 'giang vien' || norm === 'giang_vien' || norm === 'instructor' || norm === 'teacher' || norm === 'gv') {
    return { role: 'Giảng viên', roleIndex: 2 };
  }
  if (norm.includes('tư vấn') || norm.includes('tu van') || norm.includes('tuyển sinh') || norm.includes('tuyen sinh') || norm === 'admissions' || norm === 'sale') {
    return { role: 'Tư vấn tuyển sinh', roleIndex: 3 };
  }
  if (norm.includes('kế toán') || norm.includes('ke toan') || norm === 'accountant' || norm === 'kt') {
    return { role: 'Kế toán', roleIndex: 4 };
  }
  if (norm.includes('đào tạo') || norm.includes('dao tao') || norm.includes('training') || norm === 'qldt') {
    return { role: 'Quản lý đào tạo', roleIndex: 5 };
  }
  if (norm.includes('quản trị') || norm.includes('quan tri') || norm === 'admin' || norm === 'administrator') {
    return { role: 'Quản trị hệ thống', roleIndex: 6 };
  }

  // Check exact name match in ROLES
  const foundIdx = ROLES.findIndex((r) => r.toLowerCase() === norm);
  if (foundIdx !== -1) {
    return { role: ROLES[foundIdx], roleIndex: foundIdx };
  }
  return null;
}

function formatPhone(phone: string): string {
  const clean = phone.replace(/\D/g, '');
  if (clean.length === 10) {
    return `${clean.slice(0, 4)} ${clean.slice(4, 7)} ${clean.slice(7)}`;
  }
  return phone;
}

/**
 * Đọc và kiểm tra tệp Excel (.xlsx, .xls) hoặc CSV do người dùng tải lên.
 * Tự động nhận diện tiêu đề cột, kiểm tra trùng email, định dạng số điện thoại Việt Nam, và vai trò.
 */
export async function parseExcelFile(file: File): Promise<ImportRow[]> {
  const buffer = await file.arrayBuffer();
  const workbook = XLSX.read(buffer, { type: 'array' });
  const sheetName = workbook.SheetNames[0];
  if (!sheetName) return [];
  const worksheet = workbook.Sheets[sheetName];
  if (!worksheet) return [];

  const rawRows: any[][] = XLSX.utils.sheet_to_json(worksheet, { header: 1, defval: '' });
  if (!rawRows.length) return [];

  // 1. Nhận diện dòng tiêu đề (Header row)
  let headerRowIdx = 0;
  let nameCol = 0;
  let emailCol = 1;
  let phoneCol = 2;
  let roleCol = 3;
  let foundHeaders = false;

  for (let r = 0; r < Math.min(5, rawRows.length); r++) {
    const row = rawRows[r].map((c: any) => String(c).trim().toLowerCase());
    const nIdx = row.findIndex((c: string) => c.includes('họ') || c.includes('tên') || c.includes('name'));
    const eIdx = row.findIndex((c: string) => c.includes('email') || c.includes('mail'));
    const pIdx = row.findIndex((c: string) => c.includes('thoại') || c.includes('sđt') || c.includes('sdt') || c.includes('phone'));
    const rIdx = row.findIndex((c: string) => c.includes('vai trò') || c.includes('chức vụ') || c.includes('role') || c.includes('quyền'));

    if (eIdx !== -1 && (nIdx !== -1 || pIdx !== -1)) {
      headerRowIdx = r;
      nameCol = nIdx !== -1 ? nIdx : 0;
      emailCol = eIdx !== -1 ? eIdx : 1;
      phoneCol = pIdx !== -1 ? pIdx : 2;
      roleCol = rIdx !== -1 ? rIdx : 3;
      foundHeaders = true;
      break;
    }
  }

  const startRow = foundHeaders ? headerRowIdx + 1 : 0;
  const existingUsers = loadUsers();
  const existingEmails = new Set(existingUsers.map((u) => u.email.toLowerCase().trim()));
  const seenEmailsInFile = new Set<string>();

  const results: ImportRow[] = [];
  const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const VN_PHONE_RE = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;

  let displayLine = 1;
  for (let r = startRow; r < rawRows.length; r++) {
    const row = rawRows[r];
    // Bỏ qua dòng trống hoàn toàn
    if (!row || row.every((c: any) => !String(c).trim())) continue;

    const rawName = String(row[nameCol] ?? '').trim();
    const rawEmail = String(row[emailCol] ?? '').trim();
    let rawPhone = String(row[phoneCol] ?? '').trim();
    const rawRole = String(row[roleCol] ?? '').trim();

    // Chuẩn hoá số điện thoại (xử lý trường hợp Excel định dạng kiểu số làm mất số 0 đầu)
    let cleanPhone = rawPhone.replace(/[\s\-\.]/g, '');
    if (cleanPhone.startsWith('+84')) {
      cleanPhone = '0' + cleanPhone.slice(3);
    } else if (cleanPhone.length === 9 && /^[35789]/.test(cleanPhone)) {
      cleanPhone = '0' + cleanPhone;
    }

    let error: string | undefined = undefined;

    // 1. Kiểm tra họ tên
    if (!rawName) {
      error = 'Thiếu họ tên';
    }

    // 2. Kiểm tra email
    const emailLower = rawEmail.toLowerCase();
    if (!error) {
      if (!rawEmail) {
        error = 'Thiếu email';
      } else if (!EMAIL_RE.test(emailLower)) {
        error = 'Email không đúng định dạng';
      } else if (seenEmailsInFile.has(emailLower)) {
        error = 'Email bị trùng lặp trong tệp';
      } else if (existingEmails.has(emailLower)) {
        error = 'Email trùng với tài khoản đã có';
      }
    }
    if (rawEmail && EMAIL_RE.test(emailLower)) {
      seenEmailsInFile.add(emailLower);
    }

    // 3. Kiểm tra số điện thoại
    if (!error) {
      if (cleanPhone && !VN_PHONE_RE.test(cleanPhone)) {
        error = 'Số điện thoại sai định dạng';
      }
    }

    // 4. Kiểm tra vai trò
    const resolvedRole = resolveRole(rawRole);
    if (!error && !resolvedRole) {
      error = 'Vai trò không tồn tại';
    }

    results.push({
      line: displayLine++,
      name: rawName,
      email: rawEmail,
      phone: formatPhone(cleanPhone || rawPhone),
      role: resolvedRole ? resolvedRole.role : (rawRole || 'Học viên'),
      roleIndex: resolvedRole ? resolvedRole.roleIndex : 0,
      error,
    });
  }

  return results;
}

/** Tải tệp Excel mẫu thực thụ (.xlsx) */
export function downloadExcelTemplate(): void {
  const wsData = [
    ['Họ và tên', 'Email', 'Số điện thoại', 'Vai trò', 'Ngày sinh', 'Giới tính', 'Địa chỉ'],
    ['Nguyễn Văn An', 'an.nguyen@tms.vn', '0912345678', 'Học viên', '2003-05-15', 'Nam', 'Hà Nội'],
    ['Trần Thị Bình', 'binh.tran@tms.vn', '0987654321', 'Học viên', '2002-11-20', 'Nữ', 'Thái Nguyên'],
    ['Lê Hoàng Cường', 'cuong.le@tms.vn', '0903112233', 'Giảng viên', '1990-08-10', 'Nam', 'Đà Nẵng'],
  ];
  const ws = XLSX.utils.aoa_to_sheet(wsData);
  // Đặt độ rộng các cột cho đẹp mắt
  ws['!cols'] = [
    { wch: 22 },
    { wch: 26 },
    { wch: 16 },
    { wch: 18 },
    { wch: 14 },
    { wch: 12 },
    { wch: 20 },
  ];
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, 'DanhSachNguoiDung');
  XLSX.writeFile(wb, 'mau-nhap-nguoi-dung.xlsx');
}
