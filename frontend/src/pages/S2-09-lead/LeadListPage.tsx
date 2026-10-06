import { useMemo, useState } from 'react';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Pager, paginate, Pill } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { ADMIN_ROLE } from '../../data/permissions';
import { leadTone, LEADS, viDate, type Lead } from '../../data/leads';
import { AssignDialog } from './AssignDialog';
import { LeadDrawer } from './LeadDrawer';
import { applyLeadFilter, EMPTY_LEAD_FILTER, LeadFilters, type LeadFilter } from './LeadFilters';
import './lead.css';

const MODULE = 'Tuyển sinh & lead';

// S2-09 danh sách lead, thêm và sửa; S2-10 phân công một hoặc nhiều lead; S2-11 tìm theo tên, số điện thoại và bộ lọc.
// Tư vấn viên chỉ thấy lead được giao cho mình, không có ô chọn và nút Phân công. Chỉ Quản lý đào tạo xoá được lead.
export function LeadListPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [leads, setLeads] = useState(LEADS);
  const [filters, setFilters] = useState<LeadFilter>(EMPTY_LEAD_FILTER);
  const [page, setPage] = useState(1);
  const [picked, setPicked] = useState<number[]>([]);
  const [editing, setEditing] = useState<Lead | 'new' | null>(null);
  const [assigning, setAssigning] = useState(false);

  const role = user?.active ?? -1;
  const counselor = role === 3;
  const manager = role === 5 || role === ADMIN_ROLE;
  const canWrite = counselor || manager;

  // Tư vấn viên chỉ thấy lead của mình, rồi mới áp bộ lọc S2-11.
  const filtered = useMemo(
    () => applyLeadFilter(counselor ? leads.filter((l) => l.owner === user?.name) : leads, filters),
    [leads, filters, counselor, user],
  );
  const view = paginate(filtered, page);

  const commit = (list: Lead[]) => {
    LEADS.splice(0, LEADS.length, ...list);
    setLeads(list);
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
      <LeadFilters
        value={filters}
        showOwner={!counselor}
        onChange={(f) => {
          setFilters(f);
          setPage(1);
        }}
      />

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
                  <td>{l.owner || <span className="ld-none">Chưa phân công</span>}</td>
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
