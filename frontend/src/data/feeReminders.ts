// S7-06. Nhắc học phí: trước hạn 3 ngày và khi quá hạn (gửi lại mỗi ngày tới khi đóng đủ), qua email và thông báo trong ứng dụng.
// Khi tích hợp: tác vụ định kỳ ở máy chủ chạy 07:00 mỗi ngày, gửi email qua hàng đợi; ở đây chạy khi mở ứng dụng và chống gửi trùng theo ngày.
import { TODAY } from './funnel';
import { notify } from './notifications';
import { prefOf } from './prefs';
import { persisted, saveAll } from './store';
import { STUDENTS } from './students';
import { daysBetween, debtOf, PLANS } from './tuition';
import { vnd } from './vnd';

export const BEFORE_DAYS = 3;
export type FeeMail = { at: string; student: string; name: string; email: string; no: number; program: string; kind: 'Trước hạn' | 'Quá hạn'; days: number; owed: number; amount: number; paid: number; due: string; app: boolean; mail: boolean };

/** Email đã gửi, mới nhất ở đầu. */
export const FEE_MAILS: FeeMail[] = persisted('fee-mails.v2', []);
/** Đợt đã nhắc theo ngày: "2026-10-06" → ["HV260142#2", ...]. Mỗi đợt tối đa một lần mỗi ngày. */
const SENT: Record<string, string[]> = persisted('fee-reminders-sent', {});

export const subjectOf = (m: Pick<FeeMail, 'kind' | 'no' | 'days' | 'due'>) =>
  m.kind === 'Trước hạn' ? `Học phí đợt ${m.no} đến hạn ngày ${m.due.split('-').reverse().join('/')}` : `Học phí đợt ${m.no} đã quá hạn ${m.days} ngày`;

/** Các đợt cần nhắc trong ngày (chưa tính chống trùng). */
export function dueReminders(today = TODAY) {
  const out: Omit<FeeMail, 'at' | 'app' | 'mail'>[] = [];
  for (const s of STUDENTS) {
    if (!PLANS[s.code] || s.status === 'Thôi học') continue;
    for (const it of debtOf(s.code, today).items) {
      if (!it.owed) continue;
      const left = daysBetween(today, it.due);
      if (left !== BEFORE_DAYS && left >= 0) continue;
      out.push({ student: s.code, name: s.name, email: s.email, no: it.no, program: s.program, kind: left > 0 ? 'Trước hạn' : 'Quá hạn', days: Math.abs(left), owed: it.owed, amount: it.amount, paid: it.paid, due: it.due });
    }
  }
  return out;
}

/** Chạy tác vụ nhắc của một ngày. Trả về số đợt được nhắc lần này (0 nếu hôm nay đã chạy). */
export function runFeeReminders(today = TODAY) {
  const sent = (SENT[today] ??= []);
  let n = 0;
  for (const r of dueReminders(today)) {
    const key = `${r.student}#${r.no}`;
    if (sent.includes(key)) continue;
    sent.push(key);
    const st = STUDENTS.find((s) => s.code === r.student)!;
    const pref = prefOf(st.login, 'Học phí sắp đến hạn');
    const at = today + 'T07:00';
    if (pref.app)
      notify({ at, kind: 'Học phí', title: r.kind === 'Trước hạn' ? `Học phí đợt ${r.no} sắp đến hạn` : `Học phí đợt ${r.no} đã quá hạn`, body: `${r.kind === 'Trước hạn' ? `Còn ${r.days} ngày` : `Quá hạn ${r.days} ngày`} · còn phải đóng ${vnd(r.owed)}.`, link: '/hoc-phi-cua-toi', to: { users: [r.name] } });
    if (pref.app || pref.email) FEE_MAILS.unshift({ ...r, at, app: pref.app, mail: pref.email });
    n++;
  }
  saveAll();
  return n;
}
