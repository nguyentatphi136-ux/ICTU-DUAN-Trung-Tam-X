import { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Filter, LoadState, Pager, paginate, Pill, Search, usePaging } from '../../components/ui';
import { apiEnabled, useLoad } from '../../data/api';
import { useAuth } from '../../data/auth';
import { COUNSELORS, LEAD_STATUSES, leadTone, LEADS, SOURCES, viDate, addLead, updateLead, removeLead, type Lead } from '../../data/leads';
import { TODAY } from '../../data/funnel';
import { assignLeads, loadCounselors, loadPublicPrograms, restoreItem, saveLead, searchLeads, trashLead, type Counselor, type PublicProgram } from '../../data/s2api';
import { moveToTrash, restore } from '../../data/trash';
import { AssignDialog } from '../S2-10-phan-cong-lead/AssignDialog';
import { LeadDrawer } from './LeadDrawer';
import './lead.css';

const MODULE = 'Tuyển sinh & lead';
const UNASSIGNED = 'Chưa phân công';

const nowYear = Number(TODAY.slice(0, 4)) || 2026;
const nowMonth = Number(TODAY.slice(5, 7)) || 10;
const lastDayThisMonth = new Date(nowYear, nowMonth, 0).getDate();
const pad = (n: number) => String(n).padStart(2, '0');
const thisMonthStart = `${nowYear}-${pad(nowMonth)}-01`;
const thisMonthEnd = `${nowYear}-${pad(nowMonth)}-${pad(lastDayThisMonth)}`;
const thisMonthLabel = `Tháng ${pad(nowMonth)}/${nowYear}`;

// S2-09 danh sách lead, thêm và sửa; S2-10 phân công một hoặc nhiều lead (thư mục S2-10-phan-cong-lead); S2-11 tìm theo tên, số điện thoại và bộ lọc.
// Tư vấn viên chỉ thấy lead được giao cho mình, không có ô chọn và nút Phân công. Chỉ Quản lý đào tạo xoá được lead.
// Có backend: tìm, lọc và phân trang trên máy chủ (GET /api/leads), máy chủ tự giới hạn lead theo người đăng nhập.
export function LeadListPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [leads, setLeads] = useState(() => [...LEADS]);
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [source, setSource] = useState('');
  const [owner, setOwner] = useState('');
  const [rangePreset, setRangePreset] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const { page, setPage, size, setSize } = usePaging();
  const [picked, setPicked] = useState<number[]>([]);
  const [editing, setEditing] = useState<Lead | 'new' | null>(null);
  const [assigning, setAssigning] = useState(false);

  const applyPreset = (presetKey: string) => {
    setRangePreset(presetKey);
    setPage(1);
    if (!presetKey) {
      setFrom('');
      setTo('');
    } else if (presetKey === 'today') {
      setFrom(TODAY);
      setTo(TODAY);
    } else if (presetKey === 'this-month') {
      setFrom(thisMonthStart);
      setTo(thisMonthEnd);
    } else if (presetKey === '2026-09') {
      setFrom('2026-09-01');
      setTo('2026-09-30');
    } else if (presetKey === '2026-08') {
      setFrom('2026-08-01');
      setTo('2026-08-31');
    }
  };

  const rangeLabel = useMemo(() => {
    if (rangePreset === 'custom') {
      if (from && to) return `${viDate(from)} – ${viDate(to)}`;
      if (from) return `Từ ${viDate(from)}`;
      if (to) return `Đến ${viDate(to)}`;
      return 'Tuỳ chọn khoảng ngày...';
    }
    if (rangePreset === 'today') return 'Hôm nay';
    if (rangePreset === '2026-09') return '01/09 – 30/09/2026';
    if (rangePreset === '2026-08') return '01/08 – 31/08/2026';
    if (rangePreset === 'this-month') return `Tháng này (${thisMonthLabel})`;
    return 'Mọi thời gian';
  }, [rangePreset, from, to]);

  const role = user?.active ?? -1;
  const counselor = role === 3;
  // Sheet 2: Tư vấn toàn quyền lead; Quản lý đào tạo chỉ xem, phân công, xoá (S2-10); Kế toán chỉ xem.
  const manager = role === 5;
  const canWrite = counselor;

  // Có backend: dữ liệu của trang hiện tại, tổng số lead khớp bộ lọc, tư vấn viên và chương trình.
  const [total, setTotal] = useState(0);
  const [counselors, setCounselors] = useState<Counselor[]>([]);
  const [programs, setPrograms] = useState<PublicProgram[]>([]);
  const known = useRef<Record<number, Lead>>({});
  const [qSent, setQSent] = useState('');
  useEffect(() => {
    setLeads([...LEADS]);
    const onUpdated = () => {
      setLeads([...LEADS]);
    };
    window.addEventListener('tms:lead-updated', onUpdated);
    window.addEventListener('storage', onUpdated);
    return () => {
      window.removeEventListener('tms:lead-updated', onUpdated);
      window.removeEventListener('storage', onUpdated);
    };
  }, []);
  useEffect(() => {
    const t = setTimeout(() => setQSent(q), 300);
    return () => clearTimeout(t);
  }, [q]);
  const side = useLoad(async () => {
    setPrograms(await loadPublicPrograms());
    if (manager) setCounselors(await loadCounselors());
  }, [manager]);
  const list = useLoad(async () => {
    const ownerParam = owner === UNASSIGNED ? 'none' : owner ? String(counselors.find((c) => c.name === owner)?.id ?? '') : '';
    const res = await searchLeads({ q: qSent, status, source, owner: ownerParam, from, to, page, size });
    res.items.forEach((l) => (known.current[l.id] = l));
    setLeads(res.items);
    setTotal(res.total);
  }, [qSent, status, source, owner, from, to, page, size]);

  const filtered = useMemo(() => {
    if (apiEnabled) return leads;
    const k = q.trim().toLowerCase();
    const digits = k.replace(/\D/g, '');
    return leads.filter((l) => {
      // Khi chọn "Phụ trách", nếu owner rỗng (Tất cả), cho phép xem tất cả lead
      const matchOwner = !owner
        ? true
        : (owner === UNASSIGNED ? !l.owner : l.owner === owner);
      return (
        matchOwner &&
        (!k || l.name.toLowerCase().includes(k) || (digits.length > 2 && l.phone.replace(/\D/g, '').includes(digits))) &&
        (!status || l.status === status) &&
        (!source || l.source === source) &&
        (!from || l.createdAt >= from) &&
        (!to || l.createdAt <= to)
      );
    });
  }, [leads, q, status, source, owner, from, to]);
  const view = apiEnabled
    ? { items: leads, page, pages: Math.max(1, Math.ceil(total / size)), from: total ? (page - 1) * size + 1 : 0, to: Math.min(page * size, total) }
    : paginate(filtered, page, size);
  const count = apiEnabled ? total : filtered.length;
  const counselorNames = useMemo(() => {
    const base = apiEnabled ? counselors.map((c) => c.name) : COUNSELORS;
    if (user?.name && !base.includes(user.name)) {
      return [user.name, ...base];
    }
    return base;
  }, [apiEnabled, counselors, user]);

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
          <p>{count} khách hàng tiềm năng</p>
        </div>
        <div className="seg" role="tablist" aria-label="Chế độ xem">
          <Link to="/tuyen-sinh/pheu" role="tab" aria-selected="false">
            Bảng phễu
          </Link>
          <span className="on" role="tab" aria-selected="true">
            Danh sách
          </span>
        </div>
        {manager && (
          <Link to="/tuyen-sinh/thung-rac" className="btn">
            <Icon name="trash" size={16} />
            Thùng rác
          </Link>
        )}
        {canWrite && (
          <button type="button" className="btn primary" style={{ width: 140 }} onClick={() => setEditing('new')}>
            <Icon name="plus" size={16} />
            Thêm lead
          </button>
        )}
      </div>
      <LoadState loading={list.loading} error={list.error || side.error} reload={() => (list.reload(), side.reload())} />
      <div className="bar">
        <Search placeholder="Tìm theo tên hoặc số điện thoại" value={q} onChange={filter(setQ)} width={260} />
        <Filter label="Trạng thái" value={status} options={LEAD_STATUSES} onChange={filter(setStatus)} width={170} />
        <Filter label="Nguồn" value={source} options={SOURCES} onChange={filter(setSource)} width={160} />
        <Filter label="Phụ trách" value={owner} options={[UNASSIGNED, ...counselorNames]} onChange={filter(setOwner)} width={170} />
        <label className="filter" style={{ width: 180 }} title="Chọn nhanh khoảng thời gian">
          <span>{rangeLabel}</span>
          <Icon name="down" />
          <select value={rangePreset} onChange={(e) => applyPreset(e.target.value)} aria-label="Khoảng thời gian nhanh">
            <option value="">Mọi thời gian</option>
            <option value="today">Hôm nay ({viDate(TODAY)})</option>
            <option value="this-month">{thisMonthLabel} (Hiện tại)</option>
            <option value="2026-09">01/09 – 30/09/2026</option>
            <option value="2026-08">01/08 – 31/08/2026</option>
            <option value="custom">Tuỳ chọn khoảng ngày...</option>
          </select>
        </label>
        {rangePreset === 'custom' && (
          <div className="cl-range ld-range" role="group" aria-label="Khung thời gian bắt đầu và kết thúc" title="Chọn thời điểm bắt đầu và kết thúc">
            <input
              type="date"
              value={from}
              title="Thời điểm bắt đầu (Từ ngày)"
              aria-label="Thời điểm bắt đầu"
              onChange={(e) => {
                setFrom(e.target.value);
                setPage(1);
              }}
            />
            <span>–</span>
            <input
              type="date"
              value={to}
              title="Thời điểm kết thúc (Đến ngày)"
              aria-label="Thời điểm kết thúc"
              onChange={(e) => {
                setTo(e.target.value);
                setPage(1);
              }}
            />
            <button
              type="button"
              className="ld-clear-date"
              onClick={() => {
                setFrom('');
                setTo('');
                setRangePreset('');
                setPage(1);
              }}
              title="Đóng tuỳ chọn thời gian"
              aria-label="Đóng tuỳ chọn thời gian"
            >
              ✕
            </button>
          </div>
        )}
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
        <Pager page={view.page} pages={view.pages} onPage={setPage} size={size} onSize={setSize}>
          Hiển thị {view.from}–{view.to} trong {count} lead
        </Pager>
      </div>

      {editing && (
        <LeadDrawer
          key={editing === 'new' ? 'new' : editing.id}
          lead={editing === 'new' ? null : editing}
          leads={leads}
          readOnly={!canWrite}
          canDelete={manager}
          programs={apiEnabled ? programs.map((p) => p.name) : undefined}
          onOpen={(l) => setEditing(l)}
          onClose={() => setEditing(null)}
          onDelete={async (l) => {
            if (apiEnabled) {
              try {
                await trashLead(l.id);
              } catch (e) {
                return toast('Chưa xoá được', (e as Error).message);
              }
              setEditing(null);
              list.reload();
              toast('Đã chuyển vào thùng rác', l.name, {
                label: 'Hoàn tác',
                onClick: () => void restoreItem('lead', l.id).then(list.reload, (e: Error) => toast('Chưa khôi phục được', e.message)),
              });
              return;
            }
            const id = moveToTrash({ kind: 'lead', item: l }, l.name, user?.name ?? '');
            removeLead(l.id);
            setLeads([...LEADS]);
            setEditing(null);
            toast('Đã chuyển vào thùng rác', l.name, {
              label: 'Hoàn tác',
              onClick: () => {
                restore(id);
                addLead(l);
                setLeads([...LEADS]);
              },
            });
          }}
          onSave={async (l) => {
            if (apiEnabled) {
              try {
                await saveLead(l, programs);
              } catch (e) {
                return toast('Chưa lưu được', (e as Error).message);
              }
              list.reload();
            } else if (editing === 'new') {
              const owner = l.owner || (counselor ? user!.name : '') || 'TRAN THU HA';
              addLead({ ...l, owner, createdAt: TODAY });
              setLeads([...LEADS]);
            } else {
              updateLead(l);
              setLeads([...LEADS]);
            }
            toast('Đã lưu lead', l.name);
            setEditing(null);
          }}
        />
      )}
      {assigning && (
        <AssignDialog
          leads={apiEnabled ? picked.map((id) => known.current[id]).filter(Boolean) : leads.filter((l) => picked.includes(l.id))}
          counselors={counselorNames}
          load={(c) =>
            apiEnabled
              ? (counselors.find((x) => x.name === c)?.openLeads ?? 0)
              : leads.filter((l) => l.owner === c && l.status !== 'Chốt' && l.status !== 'Đã nhập học' && l.status !== 'Từ chối').length
          }
          onClose={() => setAssigning(false)}
          onAssign={async (c, note) => {
            if (apiEnabled) {
              try {
                await assignLeads(picked, counselors.find((x) => x.name === c)!.id, note);
              } catch (e) {
                return toast('Chưa phân công được', (e as Error).message);
              }
              list.reload();
              side.reload();
            } else {
              picked.forEach((id) => {
                const lead = LEADS.find((x) => x.id === id);
                if (lead) {
                  lead.owner = c;
                  if (lead.status === 'Mới') lead.status = 'Đang tư vấn';
                  updateLead(lead);
                }
              });
              setLeads([...LEADS]);
            }
            toast(`Đã phân công ${picked.length} lead`, `Giao cho ${c}.`);
            setPicked([]);
            setAssigning(false);
          }}
        />
      )}
    </AppLayout>
  );
}
