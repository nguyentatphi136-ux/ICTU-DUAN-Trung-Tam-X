// Cách tính điểm tổng kết (S8-01) và điểm tổng kết môn (S8-02 trở đi).
// Khi tích hợp: GET/PUT /programs/:code/grade-config, GET/PUT/DELETE /classes/:code/grade-config.
import { ASSIGNMENTS, isPublished, subOf } from './assignments';
import { heldSessions, sheetOf } from './attendance';
import { CLASSES, findClass, type Klass } from './classes';
import { persisted } from './store';
import { STUDENTS } from './students';
import { SUBJECTS } from './training';

export type Source = 'Điểm danh' | 'Bài tập' | 'Nhập tay' | 'Rubric';
export const SOURCES: Record<Source, string> = {
  'Điểm danh': 'Tự tính từ điểm danh',
  'Bài tập': 'Trung bình các bài đã chấm',
  'Nhập tay': 'Nhập tay',
  Rubric: 'Nhập tay hoặc theo rubric',
};
export type Comp = { key: string; name: string; weight: number; source: Source };
/** pass: điểm đạt; fail: điểm liệt; failOn: các thành phần xét điểm liệt. */
export type GradeConfig = { comps: Comp[]; pass: number; fail: number; failOn: string[] };

const base = (): GradeConfig => ({
  comps: [
    { key: 'cc', name: 'Chuyên cần', weight: 10, source: 'Điểm danh' },
    { key: 'bt', name: 'Bài tập', weight: 30, source: 'Bài tập' },
    { key: 'gk', name: 'Kiểm tra giữa môn', weight: 20, source: 'Nhập tay' },
    { key: 'ck', name: 'Kiểm tra cuối môn', weight: 40, source: 'Nhập tay' },
  ],
  pass: 5,
  fail: 3,
  failOn: ['ck', 'gk'],
});

export const PROGRAM_GRADES: Record<string, GradeConfig> = persisted('grade-config', {
  'Lập trình web Full-stack': base(),
  'Phân tích dữ liệu': base(),
  'Thiết kế UI/UX': { ...base(), comps: [base().comps[0], { key: 'bt', name: 'Bài tập', weight: 40, source: 'Bài tập' }, { key: 'da', name: 'Dự án cuối môn', weight: 50, source: 'Rubric' }], failOn: ['da'] },
  'Lập trình Java Backend': base(),
  'Kiểm thử phần mềm': base(),
});
/** Ghi đè ở cấp lớp. Lớp có ghi đè dùng cấu hình của lớp, lớp không ghi đè dùng của chương trình. */
export const CLASS_GRADES: Record<string, GradeConfig> = persisted('grade-config-class', {
  'WEB-K12': { ...base(), pass: 6 },
  'WEB-K16': {
    ...base(),
    comps: [base().comps[0], { key: 'bt', name: 'Bài tập', weight: 40, source: 'Bài tập' }, base().comps[2], { key: 'da', name: 'Dự án cuối môn', weight: 30, source: 'Rubric' }],
    failOn: ['da', 'gk'],
  },
});

export const programConfig = (program: string) => PROGRAM_GRADES[program] ?? (PROGRAM_GRADES[program] = base());
export const configOf = (cls: string) => CLASS_GRADES[cls] ?? programConfig(findClass(cls)?.program ?? '');
export const totalWeight = (c: GradeConfig) => c.comps.reduce((n, x) => n + x.weight, 0);
export const num = (n: number, d = 1) => n.toLocaleString('vi-VN', { minimumFractionDigits: d, maximumFractionDigits: d });
export const configLine = (c: GradeConfig) => c.comps.map((x) => `${x.name.replace('Kiểm tra ', '').replace(/^giữa/, 'Giữa').replace(/^cuối/, 'Cuối')} ${x.weight}%`).join(' · ');

/** Kiểm tra cấu hình trước khi lưu. Trả về lỗi đầu tiên, hoặc rỗng nếu hợp lệ. */
export function validateConfig(c: GradeConfig) {
  if (!c.comps.length) return 'Cần ít nhất một thành phần điểm';
  const names = new Set<string>();
  for (const x of c.comps) {
    if (!x.name.trim()) return 'Thành phần nào cũng cần tên';
    if (names.has(x.name.trim().toLowerCase())) return `Trùng tên thành phần "${x.name.trim()}"`;
    names.add(x.name.trim().toLowerCase());
    if (!(x.weight > 0) || !Number.isInteger(x.weight)) return `Trọng số của ${x.name} phải là số nguyên lớn hơn 0`;
  }
  const t = totalWeight(c);
  if (t !== 100) return `Tổng trọng số đang là ${t}%, cần đúng 100%`;
  if (!(c.pass > 0 && c.pass <= 10)) return 'Điểm đạt từ 0 đến 10';
  if (!(c.fail >= 0 && c.fail < c.pass)) return 'Điểm liệt phải thấp hơn điểm đạt';
  return '';
}

// ---------- S8-02. Điểm thành phần, điểm tổng kết môn, chốt điểm ----------
export const gkey = (cls: string, subject: string) => `${cls}|${subject}`;
/** Điểm nhập tay: "WEB-K15|WEB-02" → mã học viên → mã thành phần → điểm. Cũng là điểm mẫu khi nguồn tự tính chưa có dữ liệu. */
export type ScoreBook = Record<string, Record<string, Record<string, number>>>;
export type Lock = { at: string; by: string; config: GradeConfig; values?: Record<string, Record<string, number | null>> };
export type LockLog = { key: string; at: string; by: string; text: string };

const hash = (s: string) => [...s].reduce((h, c) => (h * 31 + c.charCodeAt(0)) >>> 0, 7);
const r1 = (n: number) => Math.round(Math.min(10, Math.max(0, n)) * 10) / 10;

/** Môn đã học xong và môn đang học của lớp, theo số buổi đã dạy. */
export function subjectProgress(k: Klass) {
  let start = 0;
  return k.subjects.map((s) => {
    const from = start;
    start += s.sessions;
    const state = k.status === 'Kết thúc' || k.done >= start ? 'Đã học xong' : k.done > from ? 'Đang học' : 'Chưa học';
    return { ...s, state };
  });
}

function seed(): [ScoreBook, Record<string, Lock>] {
  const book: ScoreBook = {};
  const locks: Record<string, Lock> = {};
  for (const k of CLASSES) {
    if (k.status !== 'Đang học' && k.status !== 'Kết thúc') continue;
    const roster = STUDENTS.filter((s) => s.cls === k.code);
    const cfg = configOf(k.code);
    for (const sub of subjectProgress(k)) {
      if (sub.state === 'Chưa học') continue;
      const key = gkey(k.code, sub.code);
      book[key] = {};
      roster.forEach((st, i) => {
        const ability = 4 + (hash(st.code) % 60) / 10;
        const row: Record<string, number> = {};
        for (const c of cfg.comps) {
          const h = hash(st.code + c.key + sub.code);
          // Chuyên cần phần lớn từ 8 trở lên; mã kết thúc bằng 7 vắng nhiều như dữ liệu điểm danh mẫu (S5-02).
          row[c.key] = c.source === 'Điểm danh' ? r1(8.2 + (h % 19) / 10 - (st.code.endsWith('7') ? 2.6 : 0)) : r1(ability + ((h % 31) - 15) / 10);
        }
        // Môn đang học: một học viên thiếu điểm giữa môn để minh hoạ "Chưa đủ điều kiện tính".
        if (sub.state === 'Đang học' && i === roster.length - 1) delete row.gk;
        book[key][st.code] = row;
      });
      if (sub.state === 'Đã học xong') locks[key] = { at: '2026-09-30T16:20', by: sub.teacher ?? 'LE HOANG NAM', config: cfg };
    }
    // Học viên mẫu HV260101 có đủ 6 môn chốt điểm để minh hoạ bảng điểm tốt nghiệp toàn khoá (S8-03)
    if (k.code === 'WEB-K15') {
      const gradesTable: Record<string, { cc: number; bt: number; gk: number; ck: number }> = {
        'GIT-01': { cc: 9.5, bt: 8.5, gk: 8.0, ck: 8.5 },
        'WEB-01': { cc: 9.0, bt: 8.0, gk: 8.0, ck: 8.0 },
        'WEB-02': { cc: 9.0, bt: 8.8, gk: 8.5, ck: 9.0 },
        'DB-01': { cc: 8.5, bt: 8.0, gk: 8.0, ck: 8.5 },
        'WEB-03': { cc: 9.0, bt: 8.5, gk: 8.5, ck: 8.5 },
        'BE-01': { cc: 8.5, bt: 8.0, gk: 8.0, ck: 8.0 },
      };
      for (const s of k.subjects) {
        const key = gkey(k.code, s.code);
        if (!book[key]) book[key] = {};
        book[key]['HV260101'] = gradesTable[s.code] ?? { cc: 9.0, bt: 8.5, gk: 8.0, ck: 8.5 };
        if (!locks[key]) {
          locks[key] = { at: '2026-10-01T15:00', by: s.teacher ?? 'VU DUC LONG', config: cfg };
        }
      }
    }
  }
  return [book, locks];
}
const [BOOK0, LOCKS0] = seed();
export const SCORES: ScoreBook = persisted('scores', BOOK0);
export const LOCKS: Record<string, Lock> = persisted('grade-locks', LOCKS0);
export const LOCK_LOGS: LockLog[] = persisted('grade-lock-logs', []);

/** Giá trị một thành phần và ghi chú nguồn. Điểm danh và bài tập tự tính nếu có dữ liệu, không có thì dùng điểm đã nhập. */
export function compValue(cls: string, subject: string, code: string, c: Comp): { v: number | null; note?: string } {
  const typed = SCORES[gkey(cls, subject)]?.[code]?.[c.key];
  if (c.source === 'Điểm danh') {
    let held = 0;
    let absent = 0;
    for (const s of heldSessions(cls)) {
      if (s.subject !== subject) continue;
      const m = sheetOf(s)?.marks[code];
      if (!m) continue;
      held++;
      if (m === 'Vắng có phép' || m === 'Vắng không phép') absent++;
    }
    if (held) {
      const rate = Math.round((100 * (held - absent)) / held);
      return { v: rate / 10, note: `${c.name} ${num(rate / 10)} từ tỷ lệ ${rate}%.` };
    }
  }
  if (c.source === 'Bài tập') {
    const graded = ASSIGNMENTS.filter((a) => a.cls === cls && a.subject === subject && a.status === 'Đã giao')
      .map((a) => ({ a, g: subOf(a.id, code)?.grade }))
      .filter((x) => x.g && isPublished(x.g));
    if (graded.length) {
      const avg = graded.reduce((n, x) => n + (10 * x.g!.score) / x.a.max, 0) / graded.length;
      return { v: r1(avg), note: `${c.name} ${num(r1(avg))} là trung bình ${graded.length} bài đã chấm.` };
    }
  }
  return { v: typed ?? null };
}

export type Result = 'Đạt' | 'Không đạt' | 'Không đạt · điểm liệt' | 'Chưa đủ điều kiện tính';
export const resultTone = (r: Result) => (r === 'Đạt' ? 'ok' : r === 'Chưa đủ điều kiện tính' ? 'wait' : 'off');

/** Điểm tổng kết của một học viên trong một môn. Môn đã chốt dùng cấu hình và điểm tại lúc chốt. */
export function finalOf(cls: string, subject: string, code: string) {
  const lock = LOCKS[gkey(cls, subject)];
  const cfg = lock?.config ?? configOf(cls);
  const parts = cfg.comps.map((c) => {
    const frozen = lock?.values?.[code];
    const { v, note } = frozen && c.key in frozen ? { v: frozen[c.key], note: undefined } : compValue(cls, subject, code, c);
    return { c, v, note, weighted: v == null ? 0 : (v * c.weight) / 100 };
  });
  const missing = parts.filter((p) => p.v == null);
  const raw = parts.reduce((n, p) => n + p.weighted, 0);
  const total = missing.length ? null : Math.round(raw * 10) / 10;
  const failed = parts.filter((p) => cfg.failOn.includes(p.c.key) && p.v != null && p.v < cfg.fail);
  const result: Result = total == null ? 'Chưa đủ điều kiện tính' : failed.length ? 'Không đạt · điểm liệt' : total >= cfg.pass ? 'Đạt' : 'Không đạt';
  return { cfg, parts, missing, raw, total, failed, result, locked: !!lock };
}

// ---------- S8-03. Bảng điểm toàn khoá ----------
/** Bảng điểm của học viên ở lớp hiện tại: chỉ gồm môn đã chốt điểm; điểm trung bình tính theo trọng số môn. */
export function transcriptOf(code: string) {
  const st = STUDENTS.find((s) => s.code === code);
  const k = findClass(st?.cls);
  if (!st || !k) return null;
  const progress = subjectProgress(k);
  const rows = progress
    .filter((s) => LOCKS[gkey(k.code, s.code)])
    .map((s) => ({ s, weight: SUBJECTS.find((x) => x.code === s.code)?.weight ?? 1, f: finalOf(k.code, s.code, code) }));
  const scored = rows.filter((r) => r.f.total != null);
  const wsum = scored.reduce((n, r) => n + r.weight, 0);
  const gpa = wsum ? Math.round((10 * scored.reduce((n, r) => n + r.f.total! * r.weight, 0)) / wsum) / 10 : null;
  const passed = rows.filter((r) => r.f.result === 'Đạt').length;
  const total = k.subjects.reduce((n, s) => n + s.sessions, 0);
  // Tỷ lệ chuyên cần: trung bình điểm chuyên cần các môn đã học, quy về phần trăm.
  const cc = progress
    .filter((s) => s.state !== 'Chưa học')
    .map((s) => finalOf(k.code, s.code, code).parts.find((p) => p.c.source === 'Điểm danh')?.v)
    .filter((v): v is number => v != null);
  const attendance = cc.length ? Math.round((10 * cc.reduce((a, b) => a + b, 0)) / cc.length) : null;
  return { st, k, rows, gpa, passed, subjects: k.subjects.length, done: Math.min(k.done, total), total, attendance, complete: passed === k.subjects.length };
}
export const transcriptNo = (code: string, year: string) => `BD-${year}-${String(hash(code) % 1e6).padStart(6, '0')}`;
