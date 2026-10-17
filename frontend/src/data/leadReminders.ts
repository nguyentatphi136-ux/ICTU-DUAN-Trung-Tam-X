// Tự động quét và nhắc nhở thời gian thực (Real-time) khi đến hẹn gọi lại khách hàng (S3-01, S3-02).
import { FUNNEL, TODAY, overdueDays, ddmm, nowIso, type FunnelLead } from './funnel';
import { notify, NOTICES } from './notifications';
import { persisted, saveAll } from './store';

/** Lưu các đợt nhắc hẹn gọi lại đã sinh trong ngày: "2026-10-18" -> ["lead-cb-1-...", ...]. */
const SENT: Record<string, string[]> = persisted('lead-reminders-sent.v2', {});

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
 * onNewDue: callback khi có lead mới vừa đến hạn để hiển thị toast nổi và phát chuông.
 */
export function runLeadReminders(onNewDue?: (l: FunnelLead) => void) {
  const today = TODAY;
  const sent = (SENT[today] ??= []);
  let count = 0;

  for (const l of dueCallbackLeads()) {
    const key = `lead-cb-${l.id}-${l.callback}`;
    if (sent.includes(key)) continue;
    if (NOTICES.some((n) => n.ref === key)) {
      sent.push(key);
      continue;
    }

    sent.push(key);
    const late = overdueDays(l);
    const title = late > 0 ? `Quá hạn gọi lại (${late} ngày): ${l.name}` : `Đến hẹn gọi lại: ${l.name}`;
    const body = `${l.phone} · ${l.program} · Hẹn: ${ddmm(l.callback)} · Phụ trách: ${l.owner || 'Chưa phân công'}`;

    notify({
      at: nowIso(),
      kind: 'Lead',
      title,
      body,
      link: `/tuyen-sinh/pheu?lead=${l.id}`,
      to: { users: [l.owner, 'Tư vấn tuyển sinh (Admissions)'].filter(Boolean) as string[] },
      ref: key,
    });

    onNewDue?.(l);
    playChime();
    count++;
  }

  if (count > 0) saveAll();
  return count;
}
