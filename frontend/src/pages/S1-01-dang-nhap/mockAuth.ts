// S1-01. Dữ liệu và hàm đăng nhập giả để chạy giao diện khi chưa có backend.
// Khi tích hợp: thay login() bằng POST {VITE_API_URL}/auth/login, nhận access token và refresh token (JWT),
// và để máy chủ đếm số lần sai, khoá tạm 15 phút sau 5 lần sai liên tiếp.

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

/** Trả về tài khoản nếu đúng, null nếu sai. Không cho biết email có tồn tại hay không. */
export function login(email: string, password: string): Promise<Account | null> {
  const found = DEMO_ACCOUNTS.find((a) => a.email === email.trim().toLowerCase());
  const ok = found && password === DEMO_PASSWORD ? found : null;
  return new Promise((resolve) => window.setTimeout(() => resolve(ok), 500));
}
