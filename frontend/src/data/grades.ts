// Cách tính điểm tổng kết (S8-01) và điểm tổng kết môn (S8-02 trở đi).
// Khi tích hợp: GET/PUT /programs/:code/grade-config, GET/PUT/DELETE /classes/:code/grade-config.
import { findClass } from './classes';
import { persisted } from './store';

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
