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
export const USERS_MODULE = 'Người dùng & nhật ký';

export type Permission = 'F' | 'W' | 'W*' | 'R' | 'R*' | '–';

export type AppModule = {
  name: string;
  icon: IconName;
  /** Quyền của 6 vai trò đầu trong ROLES. Quyền của Quản trị hệ thống cố định, xem adminPerm. */
  perms: Permission[];
  /** Đường dẫn nếu màn hình đã được làm. */
  to?: string;
};

// Giá trị ban đầu lấy từ sheet "2. User Roles". Dấu * nghĩa là chỉ trên dữ liệu của mình hoặc lớp mình phụ trách.
export const MODULES: AppModule[] = [
  { name: 'Chương trình & môn học', icon: 'book', perms: ['R', 'R', 'R', 'R', '–', 'F'], to: '/dao-tao/chuong-trinh' },
  { name: 'Tuyển sinh & lead', icon: 'funnel', perms: ['–', '–', '–', 'F', 'R', 'R'], to: '/tuyen-sinh/lead' },
  { name: 'Hồ sơ học viên', icon: 'users', perms: ['W*', 'R', 'R', 'W', 'R', 'F'], to: '/hoc-vien' },
  { name: 'Lớp học & thời khoá biểu', icon: 'cal', perms: ['R*', 'R', 'R', 'R', '–', 'F'], to: '/lop-hoc' },
  { name: 'Điểm danh', icon: 'check', perms: ['R*', 'W', 'W', '–', '–', 'F'], to: '/diem-danh' },
  { name: 'Học liệu & thông báo lớp', icon: 'folder', perms: ['R*', 'W*', 'W*', '–', '–', 'F'], to: '/hoc-lieu' },
  { name: 'Bài tập & chấm điểm', icon: 'edit', perms: ['W*', 'W', 'F', '–', '–', 'R'], to: '/bai-tap' },
  { name: 'Điểm tổng kết & tốt nghiệp', icon: 'award', perms: ['R*', 'R', 'W', '–', '–', 'F'], to: '/diem-tong-ket' },
  { name: 'Học phí & công nợ', icon: 'wallet', perms: ['R*', '–', '–', 'R', 'F', 'R'], to: '/hoc-phi/bieu-phi' },
  { name: 'Khảo sát chất lượng', icon: 'chat', perms: ['W*', '–', 'R*', '–', '–', 'F'], to: '/khao-sat' },
  { name: 'Báo cáo & dashboard', icon: 'chart', perms: ['–', '–', 'R*', 'R*', 'R*', 'F'], to: '/bao-cao' },
  { name: USERS_MODULE, icon: 'shield', perms: ['–', '–', '–', '–', '–', 'R'], to: '/quan-tri/tai-khoan' },
];

/** Vai trò có màn hình riêng thì menu dẫn thẳng tới đó (học viên xem "của tôi"), khỏi chuyển hướng qua trang chung. */
const ROLE_LINKS: Record<number, Record<string, string>> = {
  0: {
    'Lớp học & thời khoá biểu': '/lich-hoc',
    'Điểm danh': '/diem-danh-cua-toi',
    'Học liệu & thông báo lớp': '/hoc-lieu-cua-toi',
    'Bài tập & chấm điểm': '/bai-tap-cua-toi',
    'Điểm tổng kết & tốt nghiệp': '/bang-diem',
    'Học phí & công nợ': '/hoc-phi-cua-toi',
  },
  2: { 'Khảo sát chất lượng': '/khao-sat/ket-qua' },
  4: { 'Học phí & công nợ': '/hoc-phi/cong-no', 'Báo cáo & dashboard': '/hoc-phi/bao-cao' },
};

/** exact: chỉ sáng khi đang ở đúng trang này (các mục Quản trị cùng nằm dưới /quan-tri). */
export type MenuItem = { name: string; icon: IconName; to?: string; exact?: boolean };

/**
 * Quản trị hệ thống chỉ quản lý tài khoản, vai trò và nhật ký (Product Backlog sheet 4: S1-05, S1-08, S1-09, S1-10,
 * S2-01, S6-11), không vào các module nghiệp vụ. Riêng xoá vĩnh viễn trong thùng rác vẫn là của Quản trị (mentor S2).
 */
export const adminPerm = (moduleName: string): Permission => (moduleName === USERS_MODULE ? 'F' : '–');

const permOf = (roleIndex: number, m: AppModule) => (roleIndex === ADMIN_ROLE ? adminPerm(m.name) : m.perms[roleIndex]);

/**
 * Mã quyền máy chủ của các module đã có API: vai trò có một trong các mã thì module hiện ra, nên đổi quyền ở S1-05
 * thì menu đổi theo. Module chưa có API (bài tập, học phí...) vẫn theo ma trận sheet 2.
 */
const MODULE_CODES: Record<string, string[]> = {
  'Chương trình & môn học': ['PROGRAM_READ', 'PROGRAM_MANAGE'],
  'Tuyển sinh & lead': ['LEAD_READ', 'LEAD_MANAGE', 'LEAD_ASSIGN'],
  'Hồ sơ học viên': ['STUDENT_READ', 'STUDENT_WRITE'],
  'Lớp học & thời khoá biểu': ['CLASS_READ', 'CLASS_MANAGE', 'SCHEDULE_READ'],
  // S4-07 điểm danh, S4-08 học viên gửi đơn xin phép.
  'Điểm danh': ['ATTENDANCE_MANAGE', 'ABSENCE_REQUEST'],
  [USERS_MODULE]: ['USER_READ'],
};

/** codes: mã quyền máy chủ của vai trò đang dùng; không có (chạy không backend) thì theo ma trận sheet 2. */
const visible = (roleIndex: number, m: AppModule, codes?: string[]) =>
  codes && MODULE_CODES[m.name] ? MODULE_CODES[m.name].some((c) => codes.includes(c)) : permOf(roleIndex, m) !== '–';

/** Menu Quản trị: mỗi chức năng (story) một mục riêng thay vì gộp chung module "Người dùng & nhật ký". code: quyền cần có. */
const ADMIN_MENU: (MenuItem & { code?: string })[] = [
  { name: 'Tài khoản', icon: 'users', to: '/quan-tri/tai-khoan', exact: true, code: 'USER_READ' }, // S1-08, S1-09, S1-10
  { name: 'Phân quyền', icon: 'shield', to: '/quan-tri/phan-quyen', exact: true, code: 'ROLE_PERMISSION_READ' }, // S1-05
  { name: 'Nhập người dùng từ Excel', icon: 'upload', to: '/quan-tri/nhap-excel', exact: true, code: 'USER_CREATE' }, // S2-01
  { name: 'Nhật ký thao tác', icon: 'clock', to: '/quan-tri/nhat-ky', exact: true }, // S6-11, chưa có quyền riêng
  { name: 'Thùng rác', icon: 'trash', to: '/quan-tri/thung-rac', exact: true, code: 'TRASH_PURGE' }, // xoá vĩnh viễn (mentor S2)
];

/** Menu của một vai trò: chỉ gồm module vai trò đó được dùng. */
export function menuFor(roleIndex: number, codes?: string[]): MenuItem[] {
  const home: MenuItem = { name: 'Trang chủ', icon: 'home', to: '/' };
  const mods = MODULES.filter((m) => visible(roleIndex, m, codes)).map((m) => ({ name: m.name, icon: m.icon, to: ROLE_LINKS[roleIndex]?.[m.name] ?? m.to }));
  if (roleIndex !== ADMIN_ROLE) return [home, ...mods];
  // Quản trị: các mục tài khoản tách riêng thay cho module "Người dùng & nhật ký", cộng module nghiệp vụ nếu được cấp quyền ở S1-05.
  const admin = ADMIN_MENU.filter((i) => !codes || !i.code || codes.includes(i.code));
  return [home, ...admin, ...mods.filter((m) => m.name !== USERS_MODULE)];
}

/** Vai trò có được dùng module không (dùng để chặn trang, trả về 403). Quyền thật phải kiểm ở máy chủ. */
export function canAccess(roleIndex: number, moduleName: string, codes?: string[]): boolean {
  const m = MODULES.find((x) => x.name === moduleName);
  return !!m && visible(roleIndex, m, codes);
}
