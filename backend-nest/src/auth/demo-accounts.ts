import type { AuthUser } from './auth.contract';

/**
 * Tài khoản thử của khung, trùng email với dữ liệu mẫu trong database/schema.sql.
 * Chỉ dùng khi chưa nối đăng nhập vào bảng users (story S1-01); tắt hẳn khi NODE_ENV=production.
 */
export const DEMO_PASSWORD = process.env.DEMO_PASSWORD ?? '123456';

export const DEMO_ACCOUNTS: AuthUser[] = [
  { id: 1, email: 'A25200212001@tms.vn', fullName: 'QUAN TRI VIEN HE THONG', roles: ['admin'] },
  { id: 2, email: 'TM25200212002@tms.vn', fullName: 'QUAN LY DAO TAO', roles: ['training_manager'] },
  { id: 3, email: 'IST25200212045@tms.vn', fullName: 'GIANG VIEN', roles: ['instructor'] },
  { id: 4, email: 'TA25200212007@tms.vn', fullName: 'TRO GIANG', roles: ['ta'] },
  { id: 5, email: 'S25200212101@tms.vn', fullName: 'HOC VIEN', roles: ['student'] },
  { id: 6, email: 'ACT25200212003@tms.vn', fullName: 'KE TOAN', roles: ['accountant'] },
  { id: 7, email: 'AMS25200212012@tms.vn', fullName: 'TU VAN TUYEN SINH', roles: ['admissions'] },
];
