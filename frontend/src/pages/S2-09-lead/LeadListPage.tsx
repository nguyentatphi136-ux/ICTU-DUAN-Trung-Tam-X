import { useMemo, useState } from 'react';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Filter, Pager, paginate, Pill, Search } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { ADMIN_ROLE } from '../../data/permissions';
import { COUNSELORS, LEAD_STATUSES, leadTone, LEADS, SOURCES, viDate, type Lead } from '../../data/leads';
import { AssignDialog } from '../S2-10-phan-cong-lead/AssignDialog';
import { LeadDrawer } from './LeadDrawer';
import './lead.css';

const MODULE = 'Tuyển sinh & lead';
const RANGES: Record<string, string> = { '2026-09': '01/09 – 30/09/2026', '2026-08': '01/08 – 31/08/2026' };
const UNASSIGNED = 'Chưa phân công';

// S2-09 danh sách lead, thêm và sửa; S2-10 phân công một hoặc nhiều lead (thư mục S2-10-phan-cong-lead); S2-11 tìm theo tên, số điện thoại và bộ lọc.
// Tư vấn viên chỉ thấy lead được giao cho mình, không có ô chọn và nút Phân công. Chỉ Quản lý đào tạo xoá được lead.
export function LeadListPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [leads, setLeads] = useState(LEADS);
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [source, setSource] = useState('');
  const [owner, setOwner] = useState('');
  const [range, setRange] = useState('');
  const [page, setPage] = useState(1);
  const [picked, setPicked] = useState<number[]>([]);
  const [editing, setEditing] = useState<Lead | 'new' | null>(null);
  const [assigning, setAssigning] = useState(false);

  const role = user?.active ?? -1;
  const counselor = role === 3;
  const manager = role === 5 || role === ADMIN_ROLE;
  const canWrite = counselor || manager;

  const filtered = useMemo(() => {
    const k = q.trim().toLowerCase();
    const digits = k.replace(/\D/g, '');
    return leads.filter(
      (l) =>
        (!counselor || l.owner === user?.name) &&
        (!k || l.name.toLowerCase().includes(k) || (digits.length > 2 && l.phone.replace(/\D/g, '').includes(digits))) &&
        (!status || l.status === status) &&
        (!source || l.source === source) &&
        (!owner || (owner === UNASSIGNED ? !l.owner : l.owner === owner)) &&
        (!range || l.createdAt.startsWith(range)),
    );
  }, [leads, q, status, source, owner, range, counselor, user]);
  const view = paginate(filtered, page);

  const commit = (list: Lead[]) => {
    LEADS.splice(0, LEADS.length, ...list);
    setLeads(list);
  };
  const filter = (set: (v: string) => void) => (v: string) => {
    set(v);
    setPage(1);
  };
  const toggle = (id: number) => setPicked((p) => (p.includes(id) ? p.filter((x) => x !== id) : [...p, id]));
  const allOnPage = view.items.length > 0 && view.items.every((l) => picked.includes(l.id));

  return (
    <AppLayout crumb={[MODULE, 'Lead']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Lead</h2>
          <p>{filtered.length} khách hàng tiềm năng</p>
        </div>
        {canWrite && (
          <button type="button" className="btn primary" style={{ width: 140 }} onClick={() => setEditing('new')}>
            <Icon name="plus" size={16} />
            Thêm lead
          </button>
        )}
      </div>
      <div className="bar">
        <Search placeholder="Tìm theo tên hoặc số điện thoại" value={q} onChange={filter(setQ)} width={280} />
        <Filter label="Trạng thái" value={status} options={LEAD_STATUSES} onChange={filter(setStatus)} width={198} />
        <Filter label="Nguồn" value={source} options={SOURCES} onChange={filter(setSource)} width={198} />
        {!counselor && <Filter label="Phụ trách" value={owner} options={[UNASSIGNED, ...COUNSELORS]} onChange={filter(setOwner)} width={198} />}
        <label className="filter" style={{ width: 198 }}>
          <span>{RANGES[range] ?? 'Mọi thời gian'}</span>
          <Icon name="down" />
          <select value={range} onChange={(e) => filter(setRange)(e.target.value)} aria-label="Khoảng thời gian">
            <option value="">Mọi thời gian</option>
            {Object.entries(RANGES).map(([v, label]) => (
              <option key={v} value={v}>
                {label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {manager && picked.length > 0 && (
        <div className="ld-bulk">
          <b>Đã chọn {picked.length} lead</b>
          <button type="button" className="btn" onClick={() => setPicked([])}>
            Bỏ chọn
          </button>
          <button type="button" className="btn primary" onClick={() => setAssigning(true)}>
            <Icon name="users" size={16} />
            Phân công
          </button>
        </div>
      )}

      <div className="panel">
        <div className="scroll">
          <table className="ld-table">
            <thead>
              <tr>
                {manager && (
                  <th style={{ width: 44 }}>
                    <input
                      type="checkbox"
                      aria-label="Chọn cả trang"
                      checked={allOnPage}
                      onChange={() => setPicked((p) => (allOnPage ? p.filter((id) => !view.items.some((l) => l.id === id)) : [...new Set([...p, ...view.items.map((l) => l.id)])]))}
                    />
                  </th>
                )}
                <th>Họ tên</th>
                <th>Số điện thoại</th>
                <th>Nguồn</th>
                <th>Chương trình quan tâm</th>
                <th>Trạng thái</th>
                <th>Người phụ trách</th>
                <th>Ngày tạo</th>
              </tr>
            </thead>
            <tbody>
              {view.items.map((l) => (
                <tr key={l.id} onClick={() => setEditing(l)}>
                  {manager && (
                    <td onClick={(e) => e.stopPropagation()}>
                      <input type="checkbox" aria-label={'Chọn ' + l.name} checked={picked.includes(l.id)} onChange={() => toggle(l.id)} />
                    </td>
                  )}
                  <td className="strong">{l.name}</td>
                  <td style={{ whiteSpace: 'nowrap' }}>{l.phone}</td>
                  <td>{l.source}</td>
                  <td>{l.program}</td>
                  <td>
                    <Pill tone={leadTone(l.status)}>{l.status}</Pill>
                  </td>
                  <td>{l.owner || <span className="ld-none">{UNASSIGNED}</span>}</td>
                  <td>{viDate(l.createdAt)}</td>
                </tr>
              ))}
              {!view.items.length && (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: 40 }}>
                    Không có lead nào khớp bộ lọc.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pager page={view.page} pages={view.pages} onPage={setPage}>
          Hiển thị {view.from}–{view.to} trong {filtered.length} lead
        </Pager>
      </div>

      {editing && (
        <LeadDrawer
          key={editing === 'new' ? 'new' : editing.id}
          lead={editing === 'new' ? null : editing}
          leads={leads}
          readOnly={!canWrite}
          canDelete={manager}
          onOpen={(l) => setEditing(l)}
          onClose={() => setEditing(null)}
          onDelete={(l) => {
            commit(leads.filter((x) => x.id !== l.id));
            toast('Đã xoá lead', l.name);
            setEditing(null);
          }}
          onSave={(l) => {
            if (editing === 'new') commit([{ ...l, id: Math.max(...leads.map((x) => x.id)) + 1, owner: counselor ? user!.name : '' }, ...leads]);
            else commit(leads.map((x) => (x.id === l.id ? l : x)));
            toast('Đã lưu lead', l.name);
            setEditing(null);
          }}
        />
      )}
      {assigning && (
        <AssignDialog
          leads={leads.filter((l) => picked.includes(l.id))}
          load={(c) => leads.filter((l) => l.owner === c && l.status !== 'Đã nhập học' && l.status !== 'Không quan tâm').length}
          onClose={() => setAssigning(false)}
          onAssign={(c) => {
            commit(leads.map((l) => (picked.includes(l.id) ? { ...l, owner: c, status: l.status === 'Mới' ? 'Đang chăm sóc' : l.status } : l)));
            toast(`Đã phân công ${picked.length} lead`, `Giao cho ${c}.`);
            setPicked([]);
            setAssigning(false);
          }}
        />
      )}
    </AppLayout>
  );
}
