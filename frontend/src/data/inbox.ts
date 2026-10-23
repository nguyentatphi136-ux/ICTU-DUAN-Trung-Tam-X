// Hộp thông báo của người đang đăng nhập (S6-09, S6-10): lọc thông báo gửi tới mình, đánh dấu đã đọc.
import type { User } from './auth';
import { CLASS_NOTICES } from './classNotices';
import { classesOf } from './classes';
import { FUNNEL, TODAY } from './funnel';
import { isLeadAssignedToUser, isUserMatch } from './leadReminders';
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
  return NOTICES.filter((n) => {
    // 1. Thông báo lớp học (học viên / giảng viên trong lớp)
    if (n.to.cls && cls.has(n.to.cls)) return true;

    // 2. Thông báo Lead: nhật ký của ai thì người đó nhận (chỉ người phụ trách hoặc người ghi nhật ký)
    if (n.kind === 'Lead') {
      if (n.ref?.startsWith('lead-cb-')) {
        const leadId = Number(n.ref.split('-')[2]);
        const lead = FUNNEL.find((x) => x.id === leadId);
        if (lead) {
          return isLeadAssignedToUser(lead, u);
        }
      }
      if (n.to.users && n.to.users.length > 0) {
        return n.to.users.some((target) => isUserMatch(target, u));
      }
      return false;
    }

    // 3. Các loại thông báo khác gửi đích danh
    if (n.to.users && n.to.users.length > 0) {
      return n.to.users.some((target) => isUserMatch(target, u));
    }

    return false;
  }).sort((a, b) => b.at.localeCompare(a.at));
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
