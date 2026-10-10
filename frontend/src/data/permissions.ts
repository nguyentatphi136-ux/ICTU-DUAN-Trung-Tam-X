import type { IconName } from '../components/Icon';

// Bảy vai trò có đăng nhập. Khách truy cập (Guest) không đăng nhập nên không nằm ở đây.
export const ROLES = [
  'Học viên',
  'Trợ giảng',
  'Giảng viên',
  'Tư vấn tuyển sinh',
  'Kế toán',
  'Quản lý đào tạo',
  'Quản trị hệ thống',
] as const;

export const ADMIN_ROLE = 6;

/** Module quản lý tài khoản. Tên dùng cả trên menu lẫn làm khoá phân quyền ở các trang S1-05, S1-08, S2-01. */
export const USERS_MODULE = 'Danh sách người dùng';

export type Permission = 'F' | 'W' | 'W*' | 'R' | 'R*' | '–';

export type AppModule = {
  name: string;
  icon: IconName;
  /** Quyền của 6 vai trò đầu trong ROLES. Quản trị hệ thống luôn toàn quyền. */
  perms: Permission[];
  /** Đường dẫn nếu màn hình đã được làm. */
  to?: string;
};

// Giá trị ban đầu lấy từ sheet "2. User Roles". Dấu * nghĩa là chỉ trên dữ liệu của mình hoặc lớp mình phụ trách.
export const MODULES: AppModule[] = [
  { name: 'Chương trình & môn học', icon: 'book', perms: ['R', 'R', 'R', 'R', '–', 'F'], to: '/dao-tao/chuong-trinh' },
  { name: 'Tuyển sinh & lead', icon: 'funnel', perms: ['–', '–', '–', 'F', 'R', 'R'], to: '/tuyen-sinh/lead' },
  { name: 'Hồ sơ học viên', icon: 'users', perms: ['W*', 'R', 'R', 'W', 'R', 'F'] },
  { name: 'Lớp học & thời khoá biểu', icon: 'cal', perms: ['R*', 'R', 'R', 'R', '–', 'F'] },
  { name: 'Điểm danh', icon: 'check', perms: ['R*', 'W', 'W', '–', '–', 'F'] },
  { name: 'Học liệu & thông báo lớp', icon: 'folder', perms: ['R*', 'W*', 'W*', '–', '–', 'F'] },
  { name: 'Bài tập & chấm điểm', icon: 'edit', perms: ['W*', 'W', 'F', '–', '–', 'R'] },
  { name: 'Điểm tổng kết & tốt nghiệp', icon: 'award', perms: ['R*', 'R', 'W', '–', '–', 'F'] },
  { name: 'Học phí & công nợ', icon: 'wallet', perms: ['R*', '–', '–', 'R', 'F', 'R'] },
  { name: 'Khảo sát chất lượng', icon: 'chat', perms: ['W*', '–', 'R*', '–', '–', 'F'] },
  { name: 'Báo cáo & dashboard', icon: 'chart', perms: ['–', '–', 'R*', 'R*', 'R*', 'F'] },
  { name: USERS_MODULE, icon: 'shield', perms: ['–', '–', '–', '–', '–', 'R'], to: '/quan-tri/tai-khoan' },
];

export type MenuItem = { name: string; icon: IconName; to?: string };

/** Menu của một vai trò: chỉ gồm module có quyền khác "–". */
export function menuFor(roleIndex: number): MenuItem[] {
  const home: MenuItem = { name: 'Trang chủ', icon: 'home', to: '/' };
  const mods = MODULES.filter((m) => roleIndex === ADMIN_ROLE || m.perms[roleIndex] !== '–');
  return [home, ...mods.map((m) => ({ name: m.name, icon: m.icon, to: m.to }))];
}

/** Vai trò có được dùng module không (dùng để chặn trang, trả về 403). Quyền thật phải kiểm ở máy chủ. */
export function canAccess(roleIndex: number, moduleName: string): boolean {
  const m = MODULES.find((x) => x.name === moduleName);
  return !!m && (roleIndex === ADMIN_ROLE || m.perms[roleIndex] !== '–');
}
