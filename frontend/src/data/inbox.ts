// Hộp thông báo của người đang đăng nhập (S6-09, S6-10): lọc thông báo gửi tới mình, đánh dấu đã đọc.
import { api, apiEnabled, json } from './api';
import type { User } from './auth';
import { CLASS_NOTICES } from './classNotices';
import { classesOf } from './classes';
import { TODAY } from './funnel';
import { NOTICES, setServerNotices, type Notice } from './notifications';
import { saveAll } from './store';
import { STUDENTS } from './students';

/** Lớp của người dùng: lớp đang học (học viên) hoặc lớp đang dạy (giảng viên, trợ giảng). */
function myClasses(u: User) {
  const st = STUDENTS.find((s) => s.login === u.email);
  return new Set([...(st?.cls ? [st.cls] : []), ...classesOf(u.name).map((k) => k.code)]);
}

export function inboxOf(u: User): Notice[] {
  const cls = myClasses(u);
  return NOTICES.filter((n) => n.to.users?.includes(u.name) || (n.to.cls && cls.has(n.to.cls))).sort((a, b) => b.at.localeCompare(a.at));
}

export const isRead = (n: Notice, u: User) => n.readBy.includes(u.name);

const KIND: Record<string, Notice['kind']> = { SCHEDULE: 'Lịch học', ABSENCE: 'Điểm danh', ASSIGNMENT: 'Bài tập', GRADE: 'Điểm', TUITION: 'Học phí' };
type ApiNotice = { id: number; title: string; body: string; type: string; link?: string; read: boolean; at: string };

/** Có backend: tải thông báo máy chủ gửi cho mình (S4-03 đổi lịch, S4-07, S4-08...). Lỗi mạng thì giữ danh sách cũ. */
export async function syncInbox(u: User) {
  if (!apiEnabled) return;
  const list = await api<ApiNotice[]>('/api/me/notifications').catch(() => null);
  if (!list) return;
  setServerNotices(
    list.map((n) => ({ srv: n.id, at: n.at, kind: KIND[n.type] ?? 'Thông báo lớp', title: n.title, body: n.body, link: n.link ?? undefined, to: { users: [u.name] }, readBy: n.read ? [u.name] : [] })),
  );
}

export function markRead(list: Notice[], u: User) {
  const st = STUDENTS.find((s) => s.login === u.email);
  const srv = list.filter((n) => n.srv !== undefined && !n.readBy.includes(u.name)).map((n) => n.srv!);
  if (srv.length) void api('/api/me/notifications/read', json('POST', { ids: srv })).catch(() => undefined);
  for (const n of list) {
    if (!n.readBy.includes(u.name)) n.readBy.push(u.name);
    // Thông báo lớp (S6-07): tính học viên đã đọc.
    const cn = n.ref?.startsWith('cn') ? CLASS_NOTICES.find((x) => 'cn' + x.id === n.ref) : undefined;
    if (cn && st && !cn.readBy.includes(st.code)) cn.readBy.push(st.code);
  }
  saveAll();
}

/** "5 phút trước", "3 giờ trước", "Hôm qua", "17/10". Giờ hiện tại lấy theo ngày dữ liệu mẫu. */
export function ago(at: string) {
  const now = Date.parse(TODAY + 'T' + new Date().toTimeString().slice(0, 5) + ':00');
  const m = Math.round((now - Date.parse(at + ':00')) / 60000);
  if (m < 1) return 'Vừa xong';
  if (m < 60) return `${m} phút trước`;
  if (m < 24 * 60 && at.slice(0, 10) === TODAY) return `${Math.floor(m / 60)} giờ trước`;
  if (m < 48 * 60) return 'Hôm qua';
  return at.slice(8, 10) + '/' + at.slice(5, 7);
}
