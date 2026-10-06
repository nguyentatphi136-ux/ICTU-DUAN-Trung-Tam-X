// S1-01. Dữ liệu và hàm đăng nhập giả để chạy giao diện khi chưa có backend.
// Khi tích hợp: thay login() bằng POST {VITE_API_URL}/auth/login, nhận access token và refresh token (JWT),
// và để máy chủ đếm số lần sai, khoá tạm 15 phút sau 5 lần sai liên tiếp.

import type { Account } from '../../data/auth';

export type { Account };

export const DEMO_PASSWORD = 'Tms@2026';
export const MAX_ATTEMPTS = 5;
export const LOCK_SECONDS = 15 * 60;

export const DEMO_ACCOUNTS: Account[] = [
  { email: 'S25200212101@tms.vn', name: 'NGUYEN MINH ANH', roles: [0] },
  { email: 'IST25200212045@tms.vn', name: 'VU DUC LONG', roles: [2] },
  { email: 'TA25200212007@tms.vn', name: 'PHAM GIA HUY', roles: [1] },
  { email: 'TM25200212002@tms.vn', name: 'LE HOANG NAM', roles: [5, 2] },
  { email: 'AMS25200212012@tms.vn', name: 'TRAN THU HA', roles: [3] },
  { email: 'ACT25200212003@tms.vn', name: 'DO THI MAI', roles: [4] },
  { email: 'A25200212001@tms.vn', name: 'TRAN QUOC BAO', roles: [6] },
];

/** Trả về tài khoản nếu đúng, null nếu sai. Không cho biết email có tồn tại hay không. */
export function login(email: string, password: string): Promise<Account | null> {
  const normalizedEmail = email.trim().toLowerCase();
  const found = DEMO_ACCOUNTS.find((a) => a.email.toLowerCase() === normalizedEmail);
  const ok = found && password === DEMO_PASSWORD ? found : null;
  return new Promise((resolve) => window.setTimeout(() => resolve(ok), 500));
}
