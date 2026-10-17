// S1-01. Dữ liệu và hàm đăng nhập giả để chạy giao diện khi chưa có backend.
// Khi tích hợp: thay login() bằng POST {VITE_API_URL}/auth/login, nhận access token và refresh token (JWT),
// và để máy chủ đếm số lần sai, khoá tạm 15 phút sau 5 lần sai liên tiếp.

import { api, apiEnabled } from '../../data/api';
import type { Account } from '../../data/auth';
import { USERS } from '../../data/users';
import { STUDENTS } from '../../data/students';

export type { Account };

export const DEMO_PASSWORD = '123456';
export const MAX_ATTEMPTS = 5;
export const LOCK_SECONDS = 15 * 60;

/** 1 Actor 1 Tài khoản chuẩn duy nhất dùng để test hệ thống */
export const DEMO_ACCOUNTS: Account[] = [
  { email: 'admin@tms.vn', name: 'Quản trị viên (Admin)', roles: [6] },
  { email: 'daotao@tms.vn', name: 'Quản lý đào tạo (Training Manager)', roles: [5] },
  { email: 'giangvien@tms.vn', name: 'Giảng viên (Instructor)', roles: [2] },
  { email: 'trogiang@tms.vn', name: 'Trợ giảng (Teaching Assistant)', roles: [1] },
  { email: 'tuvan@tms.vn', name: 'Tư vấn tuyển sinh (Admissions)', roles: [3] },
  { email: 'ketoan@tms.vn', name: 'Kế toán (Accountant)', roles: [4] },
  { email: 'hocvien@tms.vn', name: 'Học viên (Student)', roles: [0] },
];

/** Bảng ánh xạ bí danh (alias) giúp đăng nhập linh hoạt bằng username ngắn hoặc mã cũ */
const ALIASES: Record<string, string> = {
  // Admin
  'admin': 'admin@tms.vn',
  'a25200212001@tms.vn': 'admin@tms.vn',
  'admin@edumanager.vn': 'admin@tms.vn',
  'admin@example.com': 'admin@tms.vn',

  // Quản lý đào tạo
  'daotao': 'daotao@tms.vn',
  'qldt': 'daotao@tms.vn',
  'tm25200212002@tms.vn': 'daotao@tms.vn',
  'quanlydaotao@edumanager.vn': 'daotao@tms.vn',
  'training@example.com': 'daotao@tms.vn',

  // Giảng viên
  'giangvien': 'giangvien@tms.vn',
  'ist25200212045@tms.vn': 'giangvien@tms.vn',
  'giangvien@edumanager.vn': 'giangvien@tms.vn',
  'instructor@example.com': 'giangvien@tms.vn',

  // Trợ giảng
  'trogiang': 'trogiang@tms.vn',
  'ta25200212007@tms.vn': 'trogiang@tms.vn',
  'trogiang@edumanager.vn': 'trogiang@tms.vn',
  'ta@example.com': 'trogiang@tms.vn',

  // Tư vấn tuyển sinh
  'tuvan': 'tuvan@tms.vn',
  'ams25200212012@tms.vn': 'tuvan@tms.vn',
  'tuvan@edumanager.vn': 'tuvan@tms.vn',
  'admissions@example.com': 'tuvan@tms.vn',

  // Kế toán
  'ketoan': 'ketoan@tms.vn',
  'act25200212003@tms.vn': 'ketoan@tms.vn',
  'ketoan@edumanager.vn': 'ketoan@tms.vn',
  'accountant@example.com': 'ketoan@tms.vn',

  // Học viên
  'hocvien': 'hocvien@tms.vn',
  's25200212101@tms.vn': 'hocvien@tms.vn',
  'hocvien@edumanager.vn': 'hocvien@tms.vn',
  'student@example.com': 'hocvien@tms.vn',
};

/** Mã vai trò máy chủ trả về (LoginApiServlet.roleSlug) theo thứ tự ROLES. */
const ROLE_SLUGS = ['student', 'ta', 'instructor', 'admissions', 'accountant', 'training-manager', 'admin'];

type LoginData = { user: { email: string; fullName: string; roles: string[]; mustChangePassword?: boolean } };

/** Trả về tài khoản nếu đúng, null nếu sai. Không cho biết email có tồn tại hay không. */
export async function login(email: string, password: string): Promise<Account | null> {
  if (apiEnabled) {
    try {
      const { user } = await api<LoginData>('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });
      const roles = user.roles.map((r) => ROLE_SLUGS.indexOf(r)).filter((r) => r >= 0).sort((a, b) => b - a);
      return roles.length ? { email: user.email, name: user.fullName, roles, mustChangePassword: !!user.mustChangePassword } : null;
    } catch {
      return null;
    }
  }

  const raw = email.trim().toLowerCase();
  const normalized = ALIASES[raw] || raw;

  // 1. Kiểm tra trong danh sách DEMO_ACCOUNTS chuẩn
  const demoFound = DEMO_ACCOUNTS.find((a) => a.email.toLowerCase() === normalized);
  if (demoFound) {
    const ok = password === DEMO_PASSWORD || password === '123456' || password === 'Tms@2026';
    return new Promise((resolve) => window.setTimeout(() => resolve(ok ? { ...demoFound, mustChangePassword: false } : null), 150));
  }

  // 2. Kiểm tra trong danh sách người dùng USERS (bao gồm cả tài khoản SV vừa tạo)
  const u = USERS.find((x) => {
    const em = x.email.toLowerCase();
    const contact = x.contactEmail?.toLowerCase() || '';
    const code = x.code?.toLowerCase() || '';
    const userPrefix = em.split('@')[0];
    return (
      em === raw ||
      contact === raw ||
      code === raw ||
      userPrefix === raw ||
      em === normalized
    );
  });

  if (u) {
    if (u.status === 'Đã khoá') {
      return new Promise((resolve) => window.setTimeout(() => resolve(null), 150));
    }
    const tempPass = u.tempPassword || 'Tms@2026';
    const birthPass = u.birth
      ? `${u.email.split('@')[0]}@${u.birth.slice(8, 10)}${u.birth.slice(5, 7)}${u.birth.slice(2, 4)}`
      : '';
    const isPassValid =
      password === tempPass ||
      password === 'Tms@2026' ||
      password === '123456' ||
      password === DEMO_PASSWORD ||
      (birthPass && password === birthPass);

    if (isPassValid) {
      return new Promise((resolve) =>
        window.setTimeout(
          () =>
            resolve({
              email: u.email,
              name: u.displayName || u.name,
              roles: u.roles && u.roles.length ? u.roles : [0],
              mustChangePassword: u.status === 'Chờ kích hoạt',
            }),
          150
        )
      );
    }
  }

  // 3. Kiểm tra trong danh sách học viên STUDENTS
  const st = STUDENTS.find((s) => {
    const l = s.login.toLowerCase();
    const e = s.email.toLowerCase();
    const c = s.code.toLowerCase();
    const prefix = l.split('@')[0];
    return l === raw || e === raw || c === raw || prefix === raw;
  });

  if (st) {
    const isPassValid = password === 'Tms@2026' || password === '123456' || password === DEMO_PASSWORD;
    if (isPassValid) {
      return new Promise((resolve) =>
        window.setTimeout(
          () =>
            resolve({
              email: st.login,
              name: st.name,
              roles: [0], // Học viên
              mustChangePassword: st.status === 'Mới nhập học',
            }),
          150
        )
      );
    }
  }

  return new Promise((resolve) => window.setTimeout(() => resolve(null), 150));
}

