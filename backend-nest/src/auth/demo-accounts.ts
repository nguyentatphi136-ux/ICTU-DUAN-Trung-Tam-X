import type { AuthUser } from './auth.contract';

/**
 * Tài khoản thử của khung, trùng email với dữ liệu mẫu trong database/schema.sql.
 * Chỉ dùng khi chưa nối đăng nhập vào bảng users (story S1-01); tắt hẳn khi NODE_ENV=production.
 */
export const DEMO_PASSWORD = process.env.DEMO_PASSWORD ?? '123456';

export const DEMO_ACCOUNTS: AuthUser[] = [
  { id: 1, email: 'admin@edumanager.vn', fullName: 'Quản trị viên Hệ thống', roles: ['admin'] },
  { id: 2, email: 'quanlydaotao@edumanager.vn', fullName: 'Quản lý đào tạo', roles: ['training_manager'] },
  { id: 3, email: 'giangvien@edumanager.vn', fullName: 'Giảng viên', roles: ['instructor'] },
  { id: 4, email: 'trogiang@edumanager.vn', fullName: 'Trợ giảng', roles: ['ta'] },
  { id: 5, email: 'hocvien@edumanager.vn', fullName: 'Học viên', roles: ['student'] },
  { id: 6, email: 'ketoan@edumanager.vn', fullName: 'Kế toán', roles: ['accountant'] },
  { id: 7, email: 'tuvan@edumanager.vn', fullName: 'Tư vấn tuyển sinh', roles: ['admissions'] },
];
