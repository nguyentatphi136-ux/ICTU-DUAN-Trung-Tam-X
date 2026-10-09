import { useMemo, useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Drawer, Filter, Pager, paginate, Pill, RowMenu, Search } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { ADMIN_ROLE } from '../../data/permissions';
import { money, PROGRAMS, type Program } from '../../data/training';

export const MODULE = 'Chương trình & môn học';
const STATUS = ['Đang áp dụng', 'Ngừng áp dụng'];

/** Chỉ Quản lý đào tạo (toàn quyền module) và quản trị được thêm, sửa, xoá. Vai trò khác chỉ xem. */
export function useCanEditTraining() {
  const { user } = useAuth();
  return !!user && (user.active === 5 || user.active === ADMIN_ROLE);
}

export function TrainingTabs() {
  return (
    <nav className="tabs">
      <NavLink to="/dao-tao/chuong-trinh" className={({ isActive }) => (isActive ? 'on' : undefined)} end>
        Chương trình
      </NavLink>
      <NavLink to="/dao-tao/mon-hoc" className={({ isActive }) => (isActive ? 'on' : undefined)} end>
        Môn học
      </NavLink>
    </nav>
  );
}

// S2-04. Danh sách chương trình đào tạo. Chương trình đang có lớp chạy không được xoá, chỉ được ngừng áp dụng.
export function ProgramListPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const canEdit = useCanEditTraining();
  const [items, setItems] = useState(PROGRAMS);
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(1);
  const [editing, setEditing] = useState<Program | 'new' | null>(null);

  const filtered = useMemo(() => {
    const k = q.trim().toLowerCase();
    return items.filter((p) => (!k || p.code.toLowerCase().includes(k) || p.name.toLowerCase().includes(k)) && (!status || (status === STATUS[0]) === p.active));
  }, [items, q, status]);
  const view = paginate(filtered, page);

  const commit = (list: Program[]) => {
    PROGRAMS.splice(0, PROGRAMS.length, ...list);
    setItems(list);
  };

  return (
    <AppLayout crumb={[MODULE, 'Chương trình']} module={MODULE}>
      <div className="h">
        <div>
          <h2>{MODULE}</h2>
        </div>
        {canEdit && (
          <button type="button" className="btn primary" style={{ width: 200 }} onClick={() => setEditing('new')}>
            <Icon name="plus" size={16} />
            Thêm chương trình
          </button>
        )}
      </div>
      <TrainingTabs />
      <div className="bar">
        <Search placeholder="Tìm theo mã hoặc tên chương trình" value={q} onChange={(v) => (setQ(v), setPage(1))} />
        <Filter label="Trạng thái" value={status} options={STATUS} onChange={(v) => (setStatus(v), setPage(1))} width={220} />
      </div>
      <div className="panel">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Mã</th>
                <th>Tên chương trình</th>
                <th>Tổng thời lượng</th>
                <th>Học phí chuẩn</th>
                <th>Lớp đang chạy</th>
                <th>Trạng thái</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {view.items.map((p) => (
                <tr key={p.code}>
                  <td className="strong">{p.code}</td>
                  <td>{p.name}</td>
                  <td>{p.hours} giờ</td>
                  <td>{money(p.fee)}</td>
                  <td>{p.runningClasses}</td>
                  <td>{p.active ? <Pill tone="ok">Đang áp dụng</Pill> : <Pill>Ngừng áp dụng</Pill>}</td>
                  <td style={{ width: 56 }}>
                    <RowMenu
                      actions={[
                        { label: 'Xem lộ trình môn học', onClick: () => navigate('/dao-tao/chuong-trinh/' + p.code) },
                        ...(canEdit
                          ? [
                              { label: 'Sửa chương trình', onClick: () => setEditing(p) },
                              {
                                label: p.active ? 'Ngừng áp dụng' : 'Áp dụng lại',
                                onClick: () => commit(items.map((x) => (x.code === p.code ? { ...x, active: !x.active } : x))),
                              },
                              {
                                label: 'Xoá',
                                danger: true,
                                disabled: p.runningClasses ? `Đang có ${p.runningClasses} lớp chạy nên không xoá được` : false,
                                onClick: () => {
                                  if (!window.confirm(`Xoá chương trình ${p.name}?`)) return;
                                  commit(items.filter((x) => x.code !== p.code));
                                  toast('Đã xoá chương trình', p.name);
                                },
                              },
                            ]
                          : []),
                      ]}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Pager page={view.page} pages={view.pages} onPage={setPage}>
          Hiển thị {view.from}–{view.to} trong {filtered.length} chương trình
        </Pager>
      </div>

      {editing && (
        <ProgramDrawer
          program={editing === 'new' ? null : editing}
          items={items}
          onClose={() => setEditing(null)}
          onSave={(p) => {
            commit(editing === 'new' ? [...items, p] : items.map((x) => (x.code === editing.code ? p : x)));
            toast('Đã lưu chương trình', p.name);
            setEditing(null);
          }}
        />
      )}
    </AppLayout>
  );
}

const num = (s: string) => Number(s.replace(/\D/g, '')) || 0;

function ProgramDrawer({ program, items, onClose, onSave }: { program: Program | null; items: Program[]; onClose: () => void; onSave: (p: Program) => void }) {
  const [f, setF] = useState({
    code: program?.code ?? '',
    name: program?.name ?? '',
    description: program?.description ?? '',
    hours: String(program?.hours ?? ''),
    fee: program ? program.fee.toLocaleString('vi-VN') : '',
    active: program?.active ?? true,
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const set = (k: keyof typeof f) => (e: { target: { value: string } }) => {
    setF((x) => ({ ...x, [k]: e.target.value }));
    setErrors((x) => ({ ...x, [k]: '' }));
  };

  function save() {
    const code = f.code.trim().toUpperCase();
    const dup = items.find((p) => p.code === code && p.code !== program?.code);
    const e: Record<string, string> = {};
    if (!code) e.code = 'Vui lòng nhập mã chương trình';
    else if (dup) e.code = `Mã ${code} đã dùng cho chương trình ${dup.name}`;
    if (!f.name.trim()) e.name = 'Vui lòng nhập tên chương trình';
    if (!num(f.hours)) e.hours = 'Nhập số giờ lớn hơn 0';
    setErrors(e);
    if (Object.keys(e).length) return;
    onSave({ code, name: f.name.trim(), description: f.description.trim(), hours: num(f.hours), fee: num(f.fee), active: f.active, runningClasses: program?.runningClasses ?? 0 });
  }

  const input = (k: 'code' | 'name' | 'hours' | 'fee', label: string, mode?: 'numeric') => (
    <div className="field">
      <label htmlFor={'pg-' + k}>{label}</label>
      <div className={'inp' + (errors[k] ? ' bad' : '')}>
        <input id={'pg-' + k} inputMode={mode} value={f[k]} onChange={set(k)} onBlur={k === 'fee' ? () => setF((x) => ({ ...x, fee: num(x.fee) ? num(x.fee).toLocaleString('vi-VN') : '' })) : undefined} />
      </div>
      {errors[k] && <p className="err">{errors[k]}</p>}
    </div>
  );

  return (
    <Drawer
      title={program ? 'Sửa chương trình' : 'Thêm chương trình'}
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
            Huỷ
          </button>
          <button type="button" className="btn primary" style={{ width: 180 }} onClick={save}>
            Lưu chương trình
          </button>
        </>
      }
    >
      {input('code', 'Mã chương trình')}
      {input('name', 'Tên chương trình')}
      <div className="field">
        <label htmlFor="pg-desc">Mô tả</label>
        <div className="inp">
          <textarea id="pg-desc" value={f.description} onChange={set('description')} placeholder="Đối tượng, mục tiêu của chương trình" />
        </div>
      </div>
      <div className="grid2">
        {input('hours', 'Tổng thời lượng (giờ)', 'numeric')}
        {input('fee', 'Học phí chuẩn (₫)', 'numeric')}
      </div>
      <div className="field">
        <label htmlFor="pg-status">Trạng thái</label>
        <div className="inp">
          <select id="pg-status" value={f.active ? STATUS[0] : STATUS[1]} onChange={(e) => setF((x) => ({ ...x, active: e.target.value === STATUS[0] }))}>
            {STATUS.map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
        </div>
      </div>
    </Drawer>
  );
}
