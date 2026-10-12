import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { MODULE, TrainingTabs, useCanEditTraining } from '../../components/TrainingTabs';
import { Box, ConfirmDialog, Drawer, Filter, Pager, paginate, Pill, RowMenu, Search, usePaging } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { money, PROGRAMS, type Program } from '../../data/training';
import { moveToTrash, restore } from '../../data/trash';

const STATUS = ['Đang áp dụng', 'Ngừng áp dụng'];

// S2-04. Danh sách chương trình đào tạo. Chương trình đang có lớp chạy không có nút xoá, chỉ được ngừng áp dụng.
// Xoá là chuyển vào thùng rác (khôi phục được), có nút Hoàn tác ngay trên thông báo.
export function ProgramListPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const canEdit = useCanEditTraining();
  const [items, setItems] = useState(PROGRAMS);
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const { page, setPage, size, setSize } = usePaging();
  const [editing, setEditing] = useState<Program | 'new' | null>(null);
  const [deleting, setDeleting] = useState<Program | null>(null);
  const { user } = useAuth();

  const filtered = useMemo(() => {
    const k = q.trim().toLowerCase();
    return items.filter((p) => (!k || p.code.toLowerCase().includes(k) || p.name.toLowerCase().includes(k)) && (!status || (status === STATUS[0]) === p.active));
  }, [items, q, status]);
  const view = paginate(filtered, page, size);

  const commit = (list: Program[]) => {
    PROGRAMS.splice(0, PROGRAMS.length, ...list);
    setItems(list);
  };
  const trash = (p: Program) => {
    const id = moveToTrash({ kind: 'program', item: p }, p.name, user?.name ?? '');
    setItems([...PROGRAMS]);
    setDeleting(null);
    toast('Đã chuyển vào thùng rác', p.name, {
      label: 'Hoàn tác',
      onClick: () => {
        restore(id);
        setItems([...PROGRAMS]);
      },
    });
  };

  return (
    <AppLayout crumb={[MODULE, 'Chương trình']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Chương trình đào tạo</h2>
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
                              // Đang có lớp chạy thì không có mục xoá (nhận xét mentor); lý do vẫn ghi trong ngăn Sửa.
                              ...(p.runningClasses ? [] : [{ label: 'Chuyển vào thùng rác', danger: true, onClick: () => setDeleting(p) }]),
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
        <Pager page={view.page} pages={view.pages} onPage={setPage} size={size} onSize={setSize}>
          Hiển thị {view.from}–{view.to} trong {filtered.length} chương trình
        </Pager>
      </div>

      {deleting && (
        <ConfirmDialog title="Chuyển chương trình vào thùng rác?" onClose={() => setDeleting(null)} onConfirm={() => trash(deleting)}>
          Chương trình <b>{deleting.name}</b> ({deleting.code}) và lộ trình môn của nó sẽ ẩn khỏi danh sách.
        </ConfirmDialog>
      )}
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
      {!!program?.runningClasses && (
        <Box icon="lock" tone="warn" title="Không xoá được chương trình này">
          Đang có {program.runningClasses} lớp chạy. Bạn có thể chuyển sang Ngừng áp dụng.
        </Box>
      )}
    </Drawer>
  );
}
