// Cài đặt nhận thông báo của từng người (S6-08) và mốc nhắc lịch học. Khi tích hợp: GET/PUT /me/notification-settings,
// GET/PUT /settings/reminder; tác vụ chạy định kỳ ở máy chủ gửi nhắc trước buổi học theo mốc này.
import { TODAY } from './funnel';
import { notify } from './notifications';
import { findRoom } from './rooms';
import { longDate, SESSIONS } from './schedule';
import { persisted } from './store';
import { rosterOn } from './students';

export const KINDS = ['Nhắc lịch học', 'Bài tập mới và hạn nộp', 'Điểm được công bố', 'Thay đổi lịch học', 'Kết quả đơn xin phép', 'Học phí sắp đến hạn'] as const;
export type PrefKind = (typeof KINDS)[number];
/** Loại luôn bật, người dùng không tắt được. */
export const ALWAYS: PrefKind[] = ['Thay đổi lịch học'];

export type Pref = { app: boolean; email: boolean };
/** Cài đặt theo email đăng nhập; chưa có là bật hết. */
export const PREFS: Record<string, Partial<Record<PrefKind, Pref>>> = persisted('prefs', { 'hocvien@tms.vn': { 'Nhắc lịch học': { app: false, email: false } } });
export const prefOf = (login: string, k: PrefKind): Pref => (ALWAYS.includes(k) ? { app: true, email: true } : (PREFS[login]?.[k] ?? { app: true, email: true }));

export const REMINDER: { hours: number } = persisted('reminder', { hours: 12 });
/** Buổi đã gửi nhắc, tránh gửi hai lần. */
const SENT: Record<string, true> = persisted('reminders-sent', {});

/** Gửi nhắc cho các buổi bắt đầu trong khoảng mốc nhắc tính từ bây giờ. Bỏ buổi đã huỷ và học viên đã tắt nhắc lịch. */
export function runReminders(now = TODAY + 'T' + new Date().toTimeString().slice(0, 5)) {
  const until = new Date(Date.parse(now + ':00') + REMINDER.hours * 3600000);
  const end = `${until.getFullYear()}-${String(until.getMonth() + 1).padStart(2, '0')}-${String(until.getDate()).padStart(2, '0')}T${String(until.getHours()).padStart(2, '0')}:${String(until.getMinutes()).padStart(2, '0')}`;
  let n = 0;
  for (const s of SESSIONS) {
    const at = s.date + 'T' + s.start;
    if (s.cancelled || SENT[s.id] || at < now || at > end) continue;
    SENT[s.id] = true;
    const to = rosterOn(s.cls, s.date).filter((st) => prefOf(st.login, 'Nhắc lịch học').app);
    if (!to.length) continue;
    n += to.length;
    notify({ at: now, kind: 'Lịch học', title: `Nhắc lịch: ${s.cls} học lúc ${s.start} ${s.date === TODAY ? 'hôm nay' : longDate(s.date).toLowerCase()}`, body: `${s.subject} Bài ${s.lesson}: ${s.topic} · phòng ${s.room ?? 'chưa xếp'}${findRoom(s.room)?.where ? ', ' + findRoom(s.room)!.where : ''}.`, link: '/lich-hoc', to: { users: to.map((x) => x.name) } });
  }
  return n;
}
