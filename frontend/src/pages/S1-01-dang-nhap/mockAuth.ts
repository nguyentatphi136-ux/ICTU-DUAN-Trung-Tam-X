// S1-01. Dữ liệu và hàm đăng nhập giả để chạy giao diện khi chưa có backend.
// Khi tích hợp: thay login() bằng POST {VITE_API_URL}/auth/login, nhận access token và refresh token (JWT),
// và để máy chủ đếm số lần sai, khoá tạm 15 phút sau 5 lần sai liên tiếp.

export type Account = { email: string; role: string; home: string; homeName: string };

export const DEMO_PASSWORD = 'Tms@2026';
export const MAX_ATTEMPTS = 5;
export const LOCK_SECONDS = 15 * 60;

export const DEMO_ACCOUNTS: Account[] = [
  { email: 'hocvien@tms.vn', role: 'Học viên', home: '/student', homeName: 'Lịch học của tôi' },
  { email: 'giangvien@tms.vn', role: 'Giảng viên', home: '/instructor', homeName: 'Dashboard lớp phụ trách' },
  { email: 'trogiang@tms.vn', role: 'Trợ giảng', home: '/ta', homeName: 'Lớp được phân công' },
  { email: 'daotao@tms.vn', role: 'Quản lý đào tạo', home: '/training', homeName: 'Dashboard vận hành đào tạo' },
  { email: 'tuvan@tms.vn', role: 'Tư vấn tuyển sinh', home: '/admissions/leads', homeName: 'Danh sách lead' },
  { email: 'ketoan@tms.vn', role: 'Kế toán', home: '/accounting', homeName: 'Học phí và công nợ' },
  { email: 'admin@tms.vn', role: 'Quản trị hệ thống', home: '/admin/users', homeName: 'Quản lý tài khoản' },
];

/** Trả về tài khoản nếu đúng, null nếu sai. Không cho biết email có tồn tại hay không. */
export function login(email: string, password: string): Promise<Account | null> {
  const found = DEMO_ACCOUNTS.find((a) => a.email === email.trim().toLowerCase());
  const ok = found && password === DEMO_PASSWORD ? found : null;
  return new Promise((resolve) => window.setTimeout(() => resolve(ok), 500));
}
