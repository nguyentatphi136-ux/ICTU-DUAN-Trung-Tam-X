// Thông báo trong ứng dụng. S4-03 trở đi gửi vào đây; S6-09, S6-10 hiển thị ở chuông và trang Thông báo.
// Khi tích hợp: máy chủ sinh thông báo theo sự kiện, GET /notifications, PATCH /notifications/:id/read.
import { TODAY } from './funnel';
import { persisted, saveAll } from './store';

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

const t = (h: number, m = 0) => {
  const d = new Date(TODAY + 'T00:00:00');
  d.setMinutes(h * 60 + m);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}T${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
};
const ME = { users: ['NGUYEN MINH ANH'] };

// Vài thông báo mẫu của học viên NGUYEN MINH ANH để chuông và trang Thông báo có nội dung ngay (S6-09, S6-10).
export const NOTICES: Notice[] = persisted('notices', [
  { id: 6, at: t(14, 55), kind: 'Bài tập', title: 'Bài tập mới: Bài 15 Xử lý lỗi và trạng thái tải', body: 'WEB-02 · hạn nộp sau 3 ngày.', link: '/bai-tap-cua-toi/8', to: { cls: 'WEB-K15' }, readBy: [] },
  { id: 5, at: t(9, 0), kind: 'Điểm', title: 'Đã có điểm Bài 11: Hàm và phạm vi biến', body: 'Giảng viên VU DUC LONG vừa công bố điểm và nhận xét.', link: '/bai-tap-cua-toi/4', to: ME, readBy: [] },
  { id: 4, at: t(-2, 30), kind: 'Điểm danh', title: 'Đơn xin phép vắng đã được duyệt', body: 'Buổi này được ghi Vắng có phép.', link: '/xin-phep', to: ME, readBy: [] },
  { id: 3, at: t(-14), kind: 'Lịch học', title: 'Lớp WEB-K15 đổi phòng sang P.401', body: 'Từ tuần này lớp học ở phòng P.401, tầng 4.', link: '/lich-hoc', to: { cls: 'WEB-K15' }, readBy: ['NGUYEN MINH ANH'] },
  { id: 2, at: t(-60), kind: 'Học phí', title: 'Học phí đợt 2 sắp đến hạn', body: 'Còn 7 ngày · 9.000.000 ₫.', link: '/', to: ME, readBy: ['NGUYEN MINH ANH'] },
  { id: 1, at: t(-100), kind: 'Thông báo lớp', title: 'Khảo sát giữa khoá', body: 'Thông báo từ Quản lý đào tạo.', link: '/thong-bao', to: { cls: 'WEB-K15' }, readBy: ['NGUYEN MINH ANH'] },
]);

const listeners = new Set<() => void>();
/** Theo dõi thông báo mới (chuông cập nhật số chưa đọc). Trả về hàm huỷ theo dõi. */
export function onNotice(f: () => void) {
  listeners.add(f);
  return () => void listeners.delete(f);
}

/** Sinh thông báo bất đồng bộ (S6-09) để không làm chậm thao tác gốc; khi tích hợp là hàng đợi ở máy chủ. */
export function notify(n: Omit<Notice, 'id' | 'readBy'>) {
  setTimeout(() => {
    NOTICES.unshift({ ...n, id: Math.max(0, ...NOTICES.map((x) => x.id)) + 1, readBy: [] });
    saveAll();
    listeners.forEach((f) => f());
  }, 0);
}
