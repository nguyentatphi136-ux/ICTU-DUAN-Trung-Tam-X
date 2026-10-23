// Đơn xin phép vắng (S4-08). Khi tích hợp: POST /leave-requests (kèm tệp), GET /leave-requests?status=&class=,
// PATCH /leave-requests/:id { decision, note }. Duyệt thì máy chủ tự đặt điểm danh buổi đó thành Vắng có phép.
import { apiEnabled } from './api';
import { TODAY } from './funnel';
import { sessionsOf } from './schedule';
import { persisted } from './store';

export type LeaveStatus = 'Chờ duyệt' | 'Đã duyệt' | 'Từ chối';
export type Leave = {
  id: number;
  student: string;
  session: string;
  reason: string;
  file?: { name: string; size: number };
  sentAt: string;
  status: LeaveStatus;
  decidedBy?: string;
  decidedAt?: string;
  note?: string;
};

export const leaveTone = (s: LeaveStatus) => (s === 'Đã duyệt' ? 'ok' : s === 'Từ chối' ? 'off' : 'wait');

/** Buổi sắp tới thứ n (n ≥ 0) hoặc buổi đã qua thứ n tính ngược (n < 0) của lớp, để đơn mẫu gắn vào buổi có thật. */
const nth = (cls: string, n: number) => {
  const all = sessionsOf(cls).filter((s) => !s.cancelled);
  const list = n >= 0 ? all.filter((s) => s.date >= TODAY) : all.filter((s) => s.date < TODAY).reverse();
  return list[n >= 0 ? n : -n - 1]?.id ?? `${cls}-0`;
};

// Có backend: đơn do máy chủ quản lý, s4api nạp theo trang.
export const LEAVES: Leave[] = persisted('leaves', apiEnabled ? [] : [
  { id: 1, student: 'HV260101', session: nth('WEB-K15', 0), reason: 'Em có lịch thi cuối kỳ ở trường vào buổi tối nên xin nghỉ buổi này. Em sẽ xem lại bài giảng và nộp bài tập trước buổi sau.', file: { name: 'lich-thi-hoc-ky-1.pdf', size: 319488 }, sentAt: '2026-10-05T14:05', status: 'Chờ duyệt' },
  { id: 2, student: 'HV260002', session: nth('WEB-K15', 0), reason: 'Đi công tác theo lịch công ty.', sentAt: '2026-10-05T09:40', status: 'Chờ duyệt' },
  { id: 3, student: 'HV260004', session: nth('WEB-K15', 1), reason: 'Khám sức khoẻ định kỳ.', sentAt: '2026-10-04T21:12', status: 'Chờ duyệt' },
  { id: 4, student: 'HV260101', session: nth('WEB-K15', -3), reason: 'Em bị sốt, có giấy khám bệnh đính kèm.', file: { name: 'giay-kham-benh.jpg', size: 228000 }, sentAt: '2026-09-24T20:30', status: 'Đã duyệt', decidedBy: 'VU DUC LONG', decidedAt: '2026-09-25T08:15', note: 'Em nghỉ ngơi cho khoẻ.' },
  { id: 5, student: 'HV260101', session: nth('WEB-K15', -8), reason: 'Em bận việc riêng.', sentAt: '2026-09-14T17:40', status: 'Từ chối', decidedBy: 'VU DUC LONG', decidedAt: '2026-09-14T17:50', note: 'Đơn gửi sát giờ học và không nêu rõ lý do.' },
], true);

/** Học viên có đơn đã duyệt cho buổi này. */
export const approvedFor = (session: string) => LEAVES.filter((l) => l.session === session && l.status === 'Đã duyệt').map((l) => l.student);
