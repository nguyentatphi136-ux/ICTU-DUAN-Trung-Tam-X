import { useMemo, useState } from 'react';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { MODULE as TRAINING_MODULE, TrainingTabs } from '../../components/TrainingTabs';
import { ConfirmDialog, Pager, paginate, Pill, Search, usePaging } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { ADMIN_ROLE, USERS_MODULE } from '../../data/permissions';
import { KIND_LABEL, purge, restore, TRASH, type TrashEntry, type TrashKind } from '../../data/trash';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';

type Area = 'training' | 'users' | 'leads';

const AREAS: Record<Area, { module: string; kinds: TrashKind[]; crumb: string[]; editors: number[] }> = {
  training: { module: TRAINING_MODULE, kinds: ['program', 'subject', 'lesson'], crumb: [TRAINING_MODULE, 'Thùng rác'], editors: [5] },
  users: { module: USERS_MODULE, kinds: ['user'], crumb: [USERS_MODULE, 'Thùng rác'], editors: [] },
  leads: { module: 'Tuyển sinh & lead', kinds: ['lead'], crumb: ['Tuyển sinh & lead', 'Thùng rác'], editors: [5] },
};

const when = (iso: string) => new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });

// Thùng rác (nhận xét mentor): bản ghi bị xoá nằm đây, khôi phục về chỗ cũ hoặc xoá vĩnh viễn.
// Khôi phục: người có quyền sửa module. Xoá vĩnh viễn: chỉ Quản trị hệ thống, có hộp xác nhận đỏ.
export function TrashPage({ area }: { area: Area }) {
  const conf = AREAS[area];
  const { user } = useAuth();
  const toast = useToast();
  const [items, setItems] = useState<TrashEntry[]>(() => TRASH.filter((t) => conf.kinds.includes(t.kind)));
  const [q, setQ] = useState('');
  const [purging, setPurging] = useState<TrashEntry | null>(null);
  const { page, setPage, size, setSize } = usePaging();

  const filtered = useMemo(() => {
    const k = q.trim().toLowerCase();
    return items.filter((t) => !k || t.label.toLowerCase().includes(k));
  }, [items, q]);
  const view = paginate(filtered, page, size);

  if (!user) return null;
  const isAdmin = user.active === ADMIN_ROLE;
  if (!isAdmin && !conf.editors.includes(user.active)) return <ErrorPage code="403" />;

  const reload = () => setItems(TRASH.filter((t) => conf.kinds.includes(t.kind)));

  return (
    <AppLayout crumb={conf.crumb} module={conf.module}>
      <div className="h">
        <div>
          <h2>Thùng rác</h2>
          <p>Bản ghi đã xoá vẫn khôi phục được. Chỉ Quản trị hệ thống xoá vĩnh viễn được.</p>
        </div>
      </div>
      {area === 'training' && <TrainingTabs />}
      <div className="bar">
        <Search placeholder="Tìm theo tên" value={q} onChange={(v) => (setQ(v), setPage(1))} />
      </div>
      <div className="panel">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Loại</th>
                <th>Tên</th>
                <th>Người xoá</th>
                <th>Thời điểm xoá</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {view.items.map((t) => (
                <tr key={t.id}>
                  <td>
                    <Pill>{KIND_LABEL[t.kind]}</Pill>
                  </td>
                  <td className="strong">{t.label}</td>
                  <td>{t.deletedBy || '—'}</td>
                  <td style={{ whiteSpace: 'nowrap' }}>{when(t.deletedAt)}</td>
                  <td style={{ whiteSpace: 'nowrap', textAlign: 'right' }}>
                    <button
                      type="button"
                      className="btn"
                      style={{ height: 34 }}
                      onClick={() => {
                        const why = restore(t.id);
                        if (why) return toast('Chưa khôi phục được', why);
                        reload();
                        toast('Đã khôi phục', t.label);
                      }}
                    >
                      <Icon name="refresh" size={16} />
                      Khôi phục
                    </button>
                    {isAdmin && (
                      <button type="button" className="btn outline-danger" style={{ height: 34, marginLeft: 8 }} onClick={() => setPurging(t)}>
                        Xoá vĩnh viễn
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              {!view.items.length && (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: 40 }}>
                    {q ? 'Không có bản ghi nào khớp.' : 'Thùng rác trống.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pager page={view.page} pages={view.pages} onPage={setPage} size={size} onSize={setSize}>
          Hiển thị {view.from}–{view.to} trong {filtered.length} bản ghi
        </Pager>
      </div>

      {purging && (
        <ConfirmDialog
          permanent
          title="Xoá vĩnh viễn?"
          onClose={() => setPurging(null)}
          onConfirm={() => {
            purge(purging.id);
            reload();
            setPurging(null);
            toast('Đã xoá vĩnh viễn', purging.label);
          }}
        >
          {KIND_LABEL[purging.kind]} <b>{purging.label}</b> sẽ bị xoá hẳn khỏi hệ thống và không khôi phục được.
        </ConfirmDialog>
      )}
    </AppLayout>
  );
}
