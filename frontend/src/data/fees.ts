// Học phí cho S7-01 trở đi. Khi tích hợp: GET/POST /fee-schedules?program=, PUT /fee-schedules/:id (chỉ khi chưa áp dụng).
// Biểu phí theo phiên bản và thời gian hiệu lực; học viên nhập học trong khoảng nào thì áp dụng biểu phí của khoảng đó.
import { TODAY } from './funnel';
import { persisted } from './store';
import { STUDENTS } from './students';

export type FeeVersion = { v: number; amount: number; from: string; to?: string };

// Mức học phí: mỗi chương trình tối đa 10 triệu, tính ra mỗi môn khoảng 3 triệu trở xuống.
// Khoá lưu đổi thành fees.v2 để trình duyệt đã lưu số cũ nạp lại số mới.
export const FEES: Record<string, FeeVersion[]> = persisted('fees.v2', {
  'Lập trình web Full-stack': [
    { v: 1, amount: 8500000, from: '2025-06-01', to: '2025-12-31' },
    { v: 2, amount: 9000000, from: '2026-01-01', to: '2026-06-30' },
    { v: 3, amount: 9500000, from: '2026-07-01', to: '2026-12-31' },
    { v: 4, amount: 10000000, from: '2027-01-01' },
  ],
  'Phân tích dữ liệu': [
    { v: 1, amount: 5000000, from: '2025-07-01', to: '2026-06-30' },
    { v: 2, amount: 5500000, from: '2026-07-01' },
  ],
  'Thiết kế UI/UX': [{ v: 1, amount: 5000000, from: '2025-06-01' }],
  'Lập trình Java Backend': [
    { v: 1, amount: 5500000, from: '2025-09-01', to: '2026-06-30' },
    { v: 2, amount: 6000000, from: '2026-07-01' },
  ],
  'Kiểm thử phần mềm': [{ v: 1, amount: 3000000, from: '2025-06-01' }],
});

/** Biểu phí có hiệu lực vào ngày cho trước. */
export const feeOn = (program: string, date: string) => FEES[program]?.find((f) => f.from <= date && (!f.to || date <= f.to));
export const currentFee = (program: string) => feeOn(program, TODAY);

export type FeeState = 'Sắp áp dụng' | 'Đang áp dụng' | 'Hết hiệu lực';
export const feeState = (f: FeeVersion): FeeState => (f.from > TODAY ? 'Sắp áp dụng' : f.to && f.to < TODAY ? 'Hết hiệu lực' : 'Đang áp dụng');

/** Số học viên đã áp dụng phiên bản này (nhập học trong khoảng hiệu lực). Đã có học viên thì không sửa được. */
export const appliedCount = (program: string, f: FeeVersion) => STUDENTS.filter((s) => s.program === program && s.enrolledAt >= f.from && (!f.to || s.enrolledAt <= f.to)).length;
