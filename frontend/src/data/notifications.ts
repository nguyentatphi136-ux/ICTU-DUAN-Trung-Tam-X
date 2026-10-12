// Thông báo trong ứng dụng. S4-03 trở đi gửi vào đây; S6-09, S6-10 hiển thị ở chuông và trang Thông báo.
// Khi tích hợp: máy chủ sinh thông báo theo sự kiện, GET /notifications, PATCH /notifications/:id/read.
import { persisted } from './store';

export type Audience = {
  /** Cả lớp: học viên đang học lớp này, giảng viên và trợ giảng của lớp. */
  cls?: string;
  /** Người nhận cụ thể, theo họ tên. */
  users?: string[];
};

export type Notice = {
  id: number;
  at: string;
  kind: 'Lịch học' | 'Bài tập' | 'Điểm' | 'Điểm danh' | 'Học phí' | 'Thông báo lớp' | 'Khảo sát';
  title: string;
  body: string;
  /** Màn hình liên quan, bấm vào thông báo thì mở. */
  link?: string;
  to: Audience;
  /** Họ tên những người đã đọc. */
  readBy: string[];
  /** Mã đối tượng gốc, ví dụ "cn3" là thông báo lớp số 3 (S6-07 đếm số đã đọc). */
  ref?: string;
};

export const NOTICES: Notice[] = persisted('notices', []);

export function notify(n: Omit<Notice, 'id' | 'readBy'>) {
  NOTICES.unshift({ ...n, id: Math.max(0, ...NOTICES.map((x) => x.id)) + 1, readBy: [] });
}
