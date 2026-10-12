// Nhật ký thao tác trên dữ liệu nhạy cảm (điểm, học phí, vai trò người dùng). S6-01 bắt đầu ghi điểm; S6-11 hiển thị.
// Khi tích hợp: máy chủ ghi nhật ký trong cùng giao dịch với thao tác gốc, GET /audit-logs?user=&entity=&from=&to=.
import { persisted } from './store';

export type AuditEntity = 'Điểm' | 'Học phí' | 'Vai trò';
export type Change = { field: string; before: string; after: string };
export type AuditEntry = { id: number; at: string; by: string; role: string; entity: AuditEntity; action: string; target: string; changes: Change[]; note?: string };

export const AUDIT: AuditEntry[] = persisted('audit', []);

export function audit(e: Omit<AuditEntry, 'id'>) {
  AUDIT.unshift({ ...e, id: Math.max(0, ...AUDIT.map((x) => x.id)) + 1 });
}
