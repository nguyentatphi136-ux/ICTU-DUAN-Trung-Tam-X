// Tự động quét và nhắc nhở thời gian thực (Real-time) khi đến hẹn gọi lại khách hàng (S3-01, S3-02).
// Phân quyền chuẩn: Chỉ người phụ trách lead hoặc người viết nhật ký hẹn gọi lại mới nhận được thông báo!
import { FUNNEL, TODAY, overdueDays, ddmm, nowIso, logsOf, type FunnelLead } from './funnel';
import { notify, NOTICES } from './notifications';
import { persisted, saveAll } from './store';

/** Lưu các đợt nhắc hẹn gọi lại đã sinh trong ngày: "2026-10-18" -> ["lead-cb-1-...", ...]. */
const SENT: Record<string, string[]> = persisted('lead-reminders-sent.v2', {});

/** Chuẩn hoá chuỗi tiếng Việt không dấu, viết thường, loại bỏ khoảng trắng thừa để so khớp tên */
export function normalizeName(str?: string): string {
  return (str || '')
    .replace(/[đĐ]/g, 'd')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '');
}

/** So khớp hai danh tính người dùng (họ tên, email, bí danh) */
export function isUserMatch(target: string, u?: { name: string; email?: string } | null): boolean {
  if (!target || !u) return false;
  const tNorm = normalizeName(target);
  const uNorm = normalizeName(u.name || '');
  const uEmail = (u.email || '').toLowerCase().trim();
  const tEmail = target.toLowerCase().trim();

  // Khớp chính xác hoặc khớp email
  if (target === u.name || tEmail === uEmail) return true;

  // Khớp tên không dấu
  if (tNorm && uNorm) {
    if (tNorm === uNorm || uNorm.includes(tNorm) || tNorm.includes(uNorm)) return true;
  }

  // Khớp tài khoản demo tuvan@tms.vn với chuyên viên TRAN THU HA
  if (uEmail === 'tuvan@tms.vn' && tNorm.includes('tranthuha')) return true;

  return false;
}

/** Kiểm tra xem một lead có thuộc trách nhiệm của người dùng hiện tại không */
export function isLeadAssignedToUser(lead: FunnelLead, user?: { name: string; email?: string } | null): boolean {
  if (!lead || !user) return false;

  // 1. Khớp người phụ trách chính của lead (lead.owner)
  if (lead.owner && isUserMatch(lead.owner, user)) return true;

  // 2. Khớp người đã viết nhật ký tương tác hoặc hẹn gọi lại cho lead (log.by)
  const logs = logsOf(lead);
  for (const log of logs) {
    if (log.by && isUserMatch(log.by, user)) {
      return true;
    }
  }

  return false;
}

/** Phát âm thanh chuông báo nhẹ nhàng khi đến hẹn (Real-time Audio) */
export function playChime() {
  try {
    const ctx = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(587.33, ctx.currentTime);
    osc.frequency.setValueAtTime(880, ctx.currentTime + 0.1);
    gain.gain.setValueAtTime(0.15, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.35);
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + 0.35);
  } catch {
    // AudioContext blocked
  }
}

/** Kiểm tra xem một lịch hẹn gọi lại đã đến giờ/hạn hay chưa (Real-time). */
export function isCallbackDue(cb?: string): boolean {
  if (!cb) return false;
  const currentIso = nowIso();
  const currentDate = TODAY;

  if (cb.includes('T')) {
    return cb <= currentIso;
  }
  return cb <= currentDate;
}

/** Tìm các lead đã đến hẹn gọi lại (chưa chốt/từ chối). */
export function dueCallbackLeads(): FunnelLead[] {
  return FUNNEL.filter((l) => {
    if (!l.callback || l.stage === 'Chốt' || l.stage === 'Từ chối' || l.student) return false;
    return isCallbackDue(l.callback);
  });
}

/**
 * Quét danh sách lead và sinh thông báo khi đến hẹn (chạy định kỳ thời gian thực).
 * currentUser: Người dùng đang đăng nhập hiện tại; chỉ báo chuông & toast nếu lead thuộc người này!
 * onNewDue: callback khi có lead mới vừa đến hạn để hiển thị toast nổi và phát chuông.
 */
export function runLeadReminders(
  onNewDue?: (l: FunnelLead) => void,
  currentUser?: { name: string; email?: string } | null
) {
  const today = TODAY;
  const sent = (SENT[today] ??= []);
  let count = 0;

  for (const l of dueCallbackLeads()) {
    const key = `lead-cb-${l.id}-${l.callback}`;
    const alreadyNotified = sent.includes(key) || NOTICES.some((n) => n.ref === key);

    if (!alreadyNotified) {
      sent.push(key);
      const late = overdueDays(l);
      const title = late > 0 ? `Quá hạn gọi lại (${late} ngày): ${l.name}` : `Đến hẹn gọi lại: ${l.name}`;
      const body = `${l.phone} · ${l.program} · Hẹn: ${ddmm(l.callback)} · Phụ trách: ${l.owner || 'Chưa phân công'}`;

      // Danh sách người nhận: Chỉ gồm người phụ trách lead và người viết nhật ký hẹn
      const logs = logsOf(l);
      const logAuthors = logs.map((x) => x.by).filter(Boolean);
      const recipients = Array.from(new Set([l.owner, ...logAuthors].filter(Boolean) as string[]));

      notify({
        at: nowIso(),
        kind: 'Lead',
        title,
        body,
        link: `/tuyen-sinh/pheu?lead=${l.id}`,
        to: { users: recipients },
        ref: key,
      });

      // CHỈ phát chuông và hiển thị toast nếu lead này thuộc về người dùng đang đăng nhập!
      if (!currentUser || isLeadAssignedToUser(l, currentUser)) {
        onNewDue?.(l);
        playChime();
      }
      count++;
    } else {
      if (!sent.includes(key)) sent.push(key);
    }
  }

  if (count > 0) saveAll();
  return count;
}
