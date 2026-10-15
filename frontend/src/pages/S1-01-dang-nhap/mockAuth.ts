// S1-01. Dữ liệu và hàm đăng nhập giả để chạy giao diện khi chưa có backend.
// Hỗ trợ cả tài khoản @tms.vn (Figma TMS) và @edumanager.vn (Java Backend), chấp nhận mật khẩu Tms@2026, 123456, Admin@123.

import { api, isOffline, roleIndexes, type ApiError } from '../../data/api';

/** roles là chỉ số trong ROLES (src/data/permissions.ts), xếp theo quyền giảm dần: phần tử đầu là vai trò khi vừa đăng nhập. */
export type Account = { email: string; name: string; roles: number[] };

export const DEMO_PASSWORD = 'Tms@2026';
export const MAX_ATTEMPTS = 5;
export const LOCK_SECONDS = 15 * 60;

export const DEMO_ACCOUNTS: Account[] = [
  { email: 'hocvien@tms.vn', name: 'NGUYEN MINH ANH', roles: [0] },
  { email: 'giangvien@tms.vn', name: 'PHAM THI HA', roles: [2] },
  { email: 'trogiang@tms.vn', name: 'PHAM GIA HUY', roles: [1] },
  { email: 'daotao@tms.vn', name: 'LE HOANG NAM', roles: [5, 2] },
  { email: 'tuvan@tms.vn', name: 'TRAN THU HA', roles: [3] },
  { email: 'ketoan@tms.vn', name: 'DO THI MAI', roles: [4] },
  { email: 'admin@tms.vn', name: 'HOANG GIA BAO', roles: [6] },
];

const ALIAS_MAP: Record<string, string> = {
  admin: 'admin@tms.vn',
  'admin@edumanager.vn': 'admin@tms.vn',
  'admin@example.com': 'admin@tms.vn',
  daotao: 'daotao@tms.vn',
  'daotao@edumanager.vn': 'daotao@tms.vn',
  'quanlydaotao@edumanager.vn': 'daotao@tms.vn',
  'training@example.com': 'daotao@tms.vn',
  giangvien: 'giangvien@tms.vn',
  'giangvien@edumanager.vn': 'giangvien@tms.vn',
  'instructor@example.com': 'giangvien@tms.vn',
  trogiang: 'trogiang@tms.vn',
  'trogiang@edumanager.vn': 'trogiang@tms.vn',
  'ta@example.com': 'trogiang@tms.vn',
  tuvan: 'tuvan@tms.vn',
  'tuvan@edumanager.vn': 'tuvan@tms.vn',
  'tuyensinh@edumanager.vn': 'tuvan@tms.vn',
  'admissions@example.com': 'tuvan@tms.vn',
  ketoan: 'ketoan@tms.vn',
  'ketoan@edumanager.vn': 'ketoan@tms.vn',
  'accountant@example.com': 'ketoan@tms.vn',
  hocvien: 'hocvien@tms.vn',
  'hocvien@edumanager.vn': 'hocvien@tms.vn',
  'student@example.com': 'hocvien@tms.vn',
};

const VALID_PASSWORDS = new Set([
  'tms@2026',
  '123456',
  'admin@123',
  'admin123',
  'admin',
  'daotao@123',
  'giangvien@123',
  'trogiang@123',
  'tuyensinh@123',
  'ketoan@123',
  'hocvien@123',
  '12345678',
  'demo@2026',
]);

export function isPasswordAccepted(password: string): boolean {
  if (!password) return false;
  const p = password.trim().toLowerCase();
  return VALID_PASSWORDS.has(p) || password === DEMO_PASSWORD;
}

export function findAccount(input: string): Account | undefined {
  const norm = input.trim().toLowerCase();
  const targetEmail = ALIAS_MAP[norm] || norm;
  return DEMO_ACCOUNTS.find((a) => a.email.toLowerCase() === targetEmail);
}

export type ServerUser = { email: string; fullName: string; roles: string[] };
export const toAccount = (u: ServerUser): Account => ({ email: u.email, name: u.fullName.toUpperCase(), roles: roleIndexes(u.roles) });

/**
 * POST /api/auth/login. Trả tài khoản nếu đúng, null nếu sai, { lockedSeconds } nếu máy chủ đang khoá tạm.
 * Máy chủ chưa bật thì dùng tài khoản mẫu: email TMS, Edumanager, alias và các mật khẩu chuẩn.
 */
export async function login(email: string, password: string): Promise<Account | null | { lockedSeconds: number }> {
  try {
    const data = await api<{ user: ServerUser }>('POST', '/auth/login', { email: email.trim(), password });
    return toAccount(data.user);
  } catch (e) {
    if (!isOffline(e)) {
      const err = e as ApiError;
      return err.status === 423 ? { lockedSeconds: Number(err.detail?.retryAfterSeconds) || LOCK_SECONDS } : null;
    }
  }
  const found = findAccount(email);
  return found && isPasswordAccepted(password) ? found : null;
}
