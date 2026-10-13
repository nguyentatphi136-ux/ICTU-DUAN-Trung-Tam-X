// Nhật ký thao tác trên dữ liệu nhạy cảm (điểm, học phí, vai trò người dùng). S6-01 bắt đầu ghi điểm; S6-11 hiển thị.
// Khi tích hợp: máy chủ ghi nhật ký trong cùng giao dịch với thao tác gốc, GET /audit-logs?user=&entity=&from=&to=.
import { addDays } from './schedule';
import { TODAY } from './funnel';
import { persisted } from './store';

export type AuditEntity = 'Điểm' | 'Điểm danh' | 'Học phí' | 'Vai trò';
export type Change = { field: string; before: string; after: string };
export type AuditEntry = { id: number; at: string; by: string; role: string; entity: AuditEntity; action: string; target: string; changes: Change[]; note?: string };

const d = (n: number, t: string) => addDays(TODAY, n) + 'T' + t;

// Bản ghi mẫu để trang Nhật ký thao tác (S6-11) có dữ liệu ngay.
export const AUDIT: AuditEntry[] = persisted('audit', [
  { id: 7, at: d(-1, '10:12'), by: 'VU DUC LONG', role: 'Giảng viên', entity: 'Điểm', action: 'Sửa điểm', target: 'HV260101 · WEB-K15 · Bài 11', changes: [{ field: 'Điểm', before: '8,0', after: '8,5' }], note: 'Chấm lại phần kiểm thử.' },
  { id: 6, at: d(-2, '16:40'), by: 'DO THI MAI', role: 'Kế toán', entity: 'Học phí', action: 'Ghi nhận thanh toán', target: 'HV260142 · Đợt 1', changes: [{ field: 'Trạng thái', before: 'Chưa đóng', after: 'Đã đóng 9.000.000 ₫' }] },
  { id: 5, at: d(-2, '09:05'), by: 'TRAN QUOC BAO', role: 'Quản trị hệ thống', entity: 'Vai trò', action: 'Thêm vai trò', target: 'IST25200212045 · VU DUC LONG', changes: [{ field: 'Vai trò', before: 'Giảng viên', after: 'Giảng viên, Trợ giảng' }] },
  { id: 4, at: d(-3, '14:22'), by: 'LE HOANG NAM', role: 'Quản lý đào tạo', entity: 'Điểm danh', action: 'Sửa điểm danh', target: 'HV260017 · WEB-K15 buổi 20', changes: [{ field: 'Trạng thái', before: 'Có mặt', after: 'Vắng không phép' }], note: 'Giảng viên báo đánh nhầm.' },
  { id: 3, at: d(-4, '11:30'), by: 'DO THI MAI', role: 'Kế toán', entity: 'Học phí', action: 'Miễn giảm', target: 'HV260088 · Đợt 2', changes: [{ field: 'Mức giảm', before: '0%', after: '10%' }], note: 'Học bổng khuyến học.' },
  { id: 2, at: d(-5, '08:48'), by: 'TRAN QUOC BAO', role: 'Quản trị hệ thống', entity: 'Vai trò', action: 'Gỡ vai trò', target: 'TA25200212011 · NGO BAO TU', changes: [{ field: 'Vai trò', before: 'Trợ giảng', after: '(không còn vai trò)' }] },
  { id: 1, at: d(-6, '20:15'), by: 'VU DUC LONG', role: 'Giảng viên', entity: 'Điểm', action: 'Công bố điểm', target: 'WEB-K15 · Bài 10 · 24 bài', changes: [{ field: 'Trạng thái', before: 'Chưa công bố', after: 'Đã công bố' }] },
]);

export function audit(e: Omit<AuditEntry, 'id'>) {
  AUDIT.unshift({ ...e, id: Math.max(0, ...AUDIT.map((x) => x.id)) + 1 });
}
