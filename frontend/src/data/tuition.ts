// Kế hoạch thanh toán, giảm giá, khoản thu và bút toán điều chỉnh (S7-02 trở đi).
// Khi tích hợp: GET/PUT /students/:code/payment-plan, POST /payment-plans/bulk, POST /payments, POST /payments/:id/adjustments.
import { feeOn, currentFee } from './fees';
import { TODAY } from './funnel';
import { addDays } from './schedule';
import { persisted } from './store';
import { STUDENTS, type Student } from './students';

export type Installment = { no: number; label: string; amount: number; due: string };
export type Discount = { kind: '%' | 'đ'; value: number; reason: string };
export type Plan = { student: string; items: Installment[]; discount?: Discount; pending?: Discount & { by: string; at: string } };
export type Method = 'Tiền mặt' | 'Chuyển khoản';
/** Khoản thu đã ghi nhận. Không xoá; sai thì lập bút toán điều chỉnh. */
export type Payment = { id: string; student: string; no: number; amount: number; at: string; time: string; method: Method; by: string; note?: string; ref?: string };
export type AdjustKind = 'Giảm khoản thu' | 'Tăng khoản thu' | 'Huỷ toàn bộ';
export type Adjustment = { id: string; payment: string; kind: AdjustKind; delta: number; reason: string; by: string; at: string };

/** Mẫu chia đợt. gaps là số ngày tính từ ngày nhập học. */
export const TEMPLATES = [
  { name: 'Đóng một lần', hint: '100% khi nhập học', pct: [100], labels: ['Khi nhập học'], gaps: [0] },
  { name: '3 đợt 40% · 30% · 30%', hint: 'Nhập học, trước buổi 20, trước buổi 40', pct: [40, 30, 30], labels: ['Khi nhập học', 'Trước buổi 20', 'Trước buổi 40'], gaps: [0, 45, 100] },
  { name: '4 đợt đều nhau', hint: '25% mỗi đợt, cách nhau 30 ngày', pct: [25, 25, 25, 25], labels: ['Khi nhập học', 'Sau 30 ngày', 'Sau 60 ngày', 'Sau 90 ngày'], gaps: [0, 30, 60, 90] },
];
export type Template = (typeof TEMPLATES)[number];
/** Giảm giá vượt mức này (phần trăm học phí) cần Quản lý đào tạo phê duyệt (S7-04). */
export const APPROVAL_PCT = 30;
export const APPROVER = 'LE HOANG NAM';

const k = (n: number) => Math.round(n / 1000) * 1000;
/** Chia tổng theo tỷ lệ, làm tròn nghìn đồng, đợt cuối nhận phần lẻ để tổng luôn khớp. */
export function split(total: number, weights: number[]) {
  const sum = weights.reduce((a, b) => a + b, 0);
  const out = weights.map((w) => k((total * w) / sum));
  out[out.length - 1] = total - out.slice(0, -1).reduce((a, b) => a + b, 0);
  return out;
}
export const fromTemplate = (t: Template, total: number, dues: string[]): Installment[] =>
  split(total, t.pct).map((amount, i) => ({ no: i + 1, label: t.labels[i], amount, due: dues[i] }));

export const daysBetween = (a: string, b: string) => Math.round((Date.parse(b) - Date.parse(a)) / 864e5);

/** Biểu phí áp dụng cho học viên: theo ngày nhập học. */
export const feeOf = (s: Student) => feeOn(s.program, s.enrolledAt) ?? currentFee(s.program);
export const discountAmount = (base: number, d?: Discount) => (!d ? 0 : d.kind === '%' ? k((base * d.value) / 100) : Math.min(base, d.value));
export const discountLabel = (d?: Discount) => (!d ? 'Chưa có giảm giá' : (d.kind === '%' ? `Giảm giá ${d.value}%` : `Giảm ${d.value.toLocaleString('vi-VN')} ₫`) + ` · ${d.reason.charAt(0).toLowerCase()}${d.reason.slice(1)}`);
export const needsApproval = (base: number, d: Discount) => discountAmount(base, d) > (base * APPROVAL_PCT) / 100;

// ---------- Dữ liệu mẫu ----------
const pad = (n: number, w: number) => String(n).padStart(w, '0');
const seq = (code: string) => Number(code.slice(4));
const RAW: Omit<Payment, 'id'>[] = [];
const PLAN_SEED: Record<string, Plan> = {};

function seed(s: Student, d: Discount | undefined, t: Template, pay: (i: Installment, idx: number) => number) {
  const fee = feeOf(s);
  if (!fee) return;
  const plan: Plan = { student: s.code, items: fromTemplate(t, fee.amount - discountAmount(fee.amount, d), t.gaps.map((g) => addDays(s.enrolledAt, g))), discount: d };
  PLAN_SEED[s.code] = plan;
  plan.items.forEach((it, idx) => {
    const amount = Math.min(it.amount, pay(it, idx));
    const n = seq(s.code);
    if (amount > 0) RAW.push({ student: s.code, no: it.no, amount, at: it.due < TODAY ? it.due : TODAY, time: `${pad(8 + (n % 9), 2)}:${pad((n * 7) % 60, 2)}`, method: n % 3 ? 'Chuyển khoản' : 'Tiền mặt', by: 'DO THI MAI', ref: n % 3 ? `VCB · FT26${pad((n * 7919) % 1e7, 7)}` : undefined });
  });
}

const kiet = STUDENTS.find((s) => s.code === 'HV260142');
if (kiet) {
  PLAN_SEED[kiet.code] = {
    student: kiet.code,
    discount: { kind: '%', value: 10, reason: 'Học bổng đầu vào' },
    items: [
      { no: 1, label: 'Khi nhập học', amount: 10080000, due: '2026-10-05' },
      { no: 2, label: 'Trước buổi 20', amount: 7560000, due: '2026-11-25' },
      { no: 3, label: 'Trước buổi 40', amount: 7560000, due: '2027-01-15' },
    ],
  };
  RAW.push({ student: kiet.code, no: 1, amount: 10080000, at: '2026-10-05', time: '10:15', method: 'Chuyển khoản', by: 'DO THI MAI', ref: 'VCB · FT26278104452' });
  RAW.push({ student: kiet.code, no: 2, amount: 3000000, at: TODAY, time: '09:42', method: 'Tiền mặt', by: 'DO THI MAI', note: 'Đóng trước một phần' });
}

for (const s of STUDENTS) {
  if (!s.cls || PLAN_SEED[s.code]) continue;
  // Lớp WEB-K16 mới có hai học viên đã lập kế hoạch, để minh hoạ sinh hàng loạt (S7-02).
  if (s.cls === 'WEB-K16' && s.code !== 'HV260131' && s.code !== 'HV260128') continue;
  const n = seq(s.code);
  const d: Discount | undefined =
    n % 5 === 0 ? { kind: '%', value: 10, reason: 'Học bổng đầu vào' } : n % 7 === 0 ? { kind: '%', value: 20, reason: 'Ưu đãi đăng ký nhóm 3 người' } : n % 11 === 0 ? { kind: 'đ', value: 2000000, reason: 'Giảm cho học viên cũ' } : undefined;
  const t = TEMPLATES[n % 4 === 0 ? 0 : 1];
  seed(s, d, t, (it, idx) => {
    if (s.status === 'Đã tốt nghiệp') return it.amount;
    if (s.status === 'Thôi học') return idx === 0 ? k(it.amount * 0.5) : 0;
    if (it.due > TODAY) return s.code === 'HV260128' || n % 9 === 0 ? it.amount : 0;
    if (idx > 0 && n % 6 === 1) return 0;
    if (idx > 0 && n % 6 === 3) return k(it.amount * 0.6);
    return it.amount;
  });
}

RAW.sort((a, b) => (a.at + a.time).localeCompare(b.at + b.time));
export const PLANS: Record<string, Plan> = persisted('plans', PLAN_SEED);
export const PAYMENTS: Payment[] = persisted(
  'payments',
  RAW.map((p, i) => ({ ...p, id: `BL-${p.at.slice(0, 4)}-${pad(i + 1, 6)}` })),
);
export const ADJUSTMENTS: Adjustment[] = persisted('adjustments', []);

// ---------- Tính toán ----------
/** Số tiền thực của khoản thu sau các bút toán điều chỉnh. */
export const netOf = (p: Payment) => p.amount + ADJUSTMENTS.filter((a) => a.payment === p.id).reduce((n, a) => n + a.delta, 0);
export const paymentsOf = (code: string) => PAYMENTS.filter((p) => p.student === code);
export const paidOn = (code: string, no: number) => paymentsOf(code).filter((p) => p.no === no).reduce((n, p) => n + netOf(p), 0);

export type ItemState = 'Chưa thanh toán' | 'Thanh toán một phần' | 'Đã thanh toán';
export const itemState = (paid: number, amount: number): ItemState => (paid >= amount ? 'Đã thanh toán' : paid > 0 ? 'Thanh toán một phần' : 'Chưa thanh toán');
export const itemTone = (s: ItemState) => (s === 'Đã thanh toán' ? 'ok' : s === 'Thanh toán một phần' ? 'wait' : undefined);

/** Tổng hợp học phí của một học viên: phải thu, đã thu, còn nợ, số ngày quá hạn (tính từ đợt chưa đóng đủ có hạn sớm nhất). */
export function debtOf(code: string, today = TODAY) {
  const plan = PLANS[code];
  const s = STUDENTS.find((x) => x.code === code);
  const base = s ? (feeOf(s)?.amount ?? 0) : 0;
  const discount = discountAmount(base, plan?.discount);
  const items = (plan?.items ?? []).map((it) => {
    const paid = paidOn(code, it.no);
    return { ...it, paid, owed: Math.max(0, it.amount - paid), state: itemState(paid, it.amount) };
  });
  const due = items.reduce((n, it) => n + it.amount, 0);
  const paid = paymentsOf(code).reduce((n, p) => n + netOf(p), 0);
  const late = items.find((it) => it.owed > 0 && it.due < today);
  const next = items.find((it) => it.owed > 0);
  return { plan, base, discount, items, due, paid, owed: Math.max(0, due - paid), overdue: late ? daysBetween(late.due, today) : 0, next };
}
