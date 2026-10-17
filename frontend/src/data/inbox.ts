// Hộp thông báo của người đang đăng nhập (S6-09, S6-10): lọc thông báo gửi tới mình, đánh dấu đã đọc.
import type { User } from './auth';
import { CLASS_NOTICES } from './classNotices';
import { classesOf } from './classes';
import { TODAY } from './funnel';
import { NOTICES, type Notice } from './notifications';
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

export function markRead(list: Notice[], u: User) {
  const st = STUDENTS.find((s) => s.login === u.email);
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
