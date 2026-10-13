// Thông báo gửi tới lớp (S6-07). Khi tích hợp: POST /class-notices (máy chủ đẩy vào ứng dụng và gửi email bất đồng bộ),
// GET /class-notices/:id/reads. Mỗi lần gửi sinh một thông báo trong ứng dụng cho từng lớp; học viên mở là tính đã đọc.
import { addDays } from './schedule';
import { TODAY } from './funnel';
import { persisted } from './store';
import { STUDENTS } from './students';

export type ClassNotice = {
  id: number;
  title: string;
  body: string;
  classes: string[];
  inApp: boolean;
  email: boolean;
  by: string;
  at: string;
  status: 'Nháp' | 'Đã gửi';
  /** Mã học viên đã đọc. */
  readBy: string[];
};

const hash = (s: string) => [...s].reduce((h, c) => (h * 31 + c.charCodeAt(0)) >>> 0, 3);
/** Một phần học viên của các lớp đã đọc, cố định giữa các lần tải trang. */
const readers = (classes: string[], pct: number, salt: string) => STUDENTS.filter((s) => s.cls && classes.includes(s.cls) && hash(s.code + salt) % 100 < pct).map((s) => s.code);

export const CLASS_NOTICES: ClassNotice[] = persisted('class-notices', [
  {
    id: 1,
    title: 'Nghỉ học ngày 20/11 nhân Ngày Nhà giáo Việt Nam',
    body: 'Chào các bạn,\n\nTrung tâm nghỉ học thứ Sáu 20/11/2026. Buổi học của ngày này được dời sang thứ Bảy 21/11 cùng giờ, cùng phòng.\n\nChúc các bạn học tốt.',
    classes: ['WEB-K15', 'WEB-K16', 'DATA-K07'],
    inApp: true,
    email: true,
    by: 'LE HOANG NAM',
    at: TODAY + 'T09:20',
    status: 'Đã gửi',
    readBy: readers(['WEB-K15', 'WEB-K16', 'DATA-K07'], 62, 'a'),
  },
  { id: 2, title: 'Lịch thi thử cuối môn WEB-02', body: 'Thi thử cuối môn JavaScript vào buổi học tuần sau, làm bài trên máy cá nhân.', classes: ['WEB-K15'], inApp: true, email: true, by: 'LE HOANG NAM', at: addDays(TODAY, -3) + 'T09:00', status: 'Đã gửi', readBy: readers(['WEB-K15'], 92, 'b') },
  { id: 3, title: 'Đổi phòng học từ P.301 sang P.401', body: 'Từ tuần này lớp WEB-K15 học ở phòng P.401, tầng 4.', classes: ['WEB-K15'], inApp: true, email: false, by: 'LE HOANG NAM', at: addDays(TODAY, -7) + 'T17:45', status: 'Đã gửi', readBy: readers(['WEB-K15'], 100, 'c') },
  { id: 4, title: 'Khảo sát giữa khoá', body: 'Các bạn dành 5 phút trả lời khảo sát giữa khoá để trung tâm cải thiện lớp học.', classes: ['WEB-K15', 'DATA-K07', 'UIUX-K09', 'JAVA-K12'], inApp: true, email: true, by: 'LE HOANG NAM', at: addDays(TODAY, -11) + 'T10:10', status: 'Đã gửi', readBy: readers(['WEB-K15', 'DATA-K07', 'UIUX-K09', 'JAVA-K12'], 71, 'd') },
]);

/** Học viên đang học các lớp nhận thông báo. */
export const audienceOf = (classes: string[]) => STUDENTS.filter((s) => s.cls && classes.includes(s.cls) && s.status !== 'Bảo lưu' && s.status !== 'Thôi học');
