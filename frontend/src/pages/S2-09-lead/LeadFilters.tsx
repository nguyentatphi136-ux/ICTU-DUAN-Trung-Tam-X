import { Icon } from '../../components/Icon';
import { toSystemName } from '../../data/identity';
import { CONSULTANTS, LEAD_SOURCES, LEAD_STATUSES, type Lead } from '../../data/leads';

export type LeadFilter = { query: string; status: string; source: string; owner: string; from: string; to: string };
export const EMPTY_LEAD_FILTER: LeadFilter = { query: '', status: '', source: '', owner: '', from: '', to: '' };
/** Giá trị của ô "Người phụ trách" khi muốn xem lead chưa giao cho ai. */
export const UNASSIGNED = '__none__';

/** Lọc danh sách lead. Tìm theo tên không phân biệt dấu, hoa thường; tìm theo số điện thoại bỏ qua khoảng trắng. */
export function applyLeadFilter(leads: Lead[], f: LeadFilter): Lead[] {
  const text = toSystemName(f.query);
  const digits = f.query.replace(/\D/g, '');
  return leads.filter(
    (l) =>
      (!text || l.name.includes(text) || (digits !== '' && l.phone.replace(/\D/g, '').includes(digits))) &&
      (!f.status || l.status === f.status) &&
      (!f.source || l.source === f.source) &&
      (!f.owner || (f.owner === UNASSIGNED ? l.owner === '' : l.owner === f.owner)) &&
      (!f.from || l.createdAt >= f.from) &&
      (!f.to || l.createdAt <= f.to),
  );
}

type Props = { value: LeadFilter; onChange: (next: LeadFilter) => void; showOwner: boolean };

// S2-11. Tìm nhanh theo tên hoặc số điện thoại, lọc theo trạng thái, nguồn, người phụ trách, khoảng thời gian.
// Khi tích hợp: các điều kiện này thành tham số của GET /leads, máy chủ lọc và phân trang.
export function LeadFilters({ value, onChange, showOwner }: Props) {
  const set = (patch: Partial<LeadFilter>) => onChange({ ...value, ...patch });
  const active = Object.values(value).some((v) => v !== '');
  const badRange = value.from !== '' && value.to !== '' && value.from > value.to;

  return (
    <>
      <div className="toolbar">
        <div className="inp lead grow">
          <Icon name="search" />
          <input placeholder="Tìm theo tên hoặc số điện thoại" aria-label="Tìm kiếm lead" value={value.query} onChange={(e) => set({ query: e.target.value })} />
        </div>
        <div className="inp">
          <select aria-label="Lọc theo trạng thái" value={value.status} onChange={(e) => set({ status: e.target.value })}>
            <option value="">Trạng thái: Tất cả</option>
            {LEAD_STATUSES.map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
        </div>
        <div className="inp">
          <select aria-label="Lọc theo nguồn" value={value.source} onChange={(e) => set({ source: e.target.value })}>
            <option value="">Nguồn: Tất cả</option>
            {LEAD_SOURCES.map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
        </div>
        {showOwner && (
          <div className="inp">
            <select aria-label="Lọc theo người phụ trách" value={value.owner} onChange={(e) => set({ owner: e.target.value })}>
              <option value="">Phụ trách: Tất cả</option>
              <option value={UNASSIGNED}>Chưa phân công</option>
              {CONSULTANTS.map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
          </div>
        )}
      </div>
      <div className="toolbar">
        <span className="muted">Ngày tạo từ</span>
        <div className={'inp' + (badRange ? ' bad' : '')}>
          <input type="date" aria-label="Từ ngày" value={value.from} onChange={(e) => set({ from: e.target.value })} />
        </div>
        <span className="muted">đến</span>
        <div className={'inp' + (badRange ? ' bad' : '')}>
          <input type="date" aria-label="Đến ngày" value={value.to} onChange={(e) => set({ to: e.target.value })} />
        </div>
        {badRange && <span className="err">Ngày bắt đầu đang sau ngày kết thúc</span>}
        {active && (
          <button className="btn" onClick={() => onChange(EMPTY_LEAD_FILTER)}>
            Xoá bộ lọc
          </button>
        )}
      </div>
    </>
  );
}
