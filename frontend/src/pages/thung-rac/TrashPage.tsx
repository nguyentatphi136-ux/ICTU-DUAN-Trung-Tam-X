import { useMemo, useState } from 'react';
import { NavLink } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { MODULE as TRAINING_MODULE, TrainingTabs } from '../../components/TrainingTabs';
import { ConfirmDialog, LoadState, Pager, paginate, Pill, Search, usePaging } from '../../components/ui';
import { apiEnabled, useLoad } from '../../data/api';
import { useAuth } from '../../data/auth';
import { ADMIN_ROLE, USERS_MODULE } from '../../data/permissions';
import { loadTrash, purgeFromTrash, restoreItem, type ServerTrashItem, type ServerTrashKind } from '../../data/s2api';
import { KIND_LABEL, purge, restore, TRASH, type TrashEntry, type TrashKind } from '../../data/trash';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';

type Area = 'training' | 'users' | 'leads';

const AREAS: Record<Area, { module: string; kinds: TrashKind[]; crumb: string[]; editors: number[] }> = {
  training: { module: TRAINING_MODULE, kinds: ['program', 'subject', 'lesson'], crumb: [TRAINING_MODULE, 'Thùng rác'], editors: [5] },
  users: { module: USERS_MODULE, kinds: ['user'], crumb: [USERS_MODULE, 'Thùng rác'], editors: [] },
  leads: { module: 'Tuyển sinh & lead', kinds: ['lead'], crumb: ['Tuyển sinh & lead', 'Thùng rác'], editors: [5] },
};

const AREA_PATH: Record<Area, string> = { users: '/quan-tri/thung-rac', training: '/dao-tao/thung-rac', leads: '/tuyen-sinh/thung-rac' };

const when = (iso: string) => new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });

// Thùng rác (nhận xét mentor): bản ghi bị xoá nằm đây, khôi phục về chỗ cũ hoặc xoá vĩnh viễn.
// Khôi phục: người có quyền sửa module. Xoá vĩnh viễn: chỉ Quản trị hệ thống, có hộp xác nhận đỏ.
// Quản trị không vào module nghiệp vụ (sheet 4) nhưng mở được thùng rác của mọi module để xoá vĩnh viễn.
// Có backend: đọc thùng rác trên máy chủ (chương trình, môn học, lead, tài khoản). Buổi học xoá hẳn nên không có ở đây.
type Row = Pick<TrashEntry, 'id' | 'kind' | 'label' | 'deletedAt' | 'deletedBy'> & { server?: ServerTrashItem };
const fromServer = (t: ServerTrashItem): Row => ({ id: t.kind + ':' + t.serverId, kind: t.kind, label: t.label, deletedAt: t.deletedAt, deletedBy: t.deletedBy, server: t });

export function TrashPage({ area }: { area: Area }) {
  const conf = AREAS[area];
  const { user } = useAuth();
  const toast = useToast();
  const [items, setItems] = useState<Row[]>(() => TRASH.filter((t) => conf.kinds.includes(t.kind)));
  const load = useLoad(async () => setItems((await loadTrash(conf.kinds.filter((k) => k !== 'lesson') as ServerTrashKind[])).map(fromServer)), [area]);
  const [q, setQ] = useState('');
  const [purging, setPurging] = useState<Row | null>(null);
  const { page, setPage, size, setSize } = usePaging();

  const filtered = useMemo(() => {
    const k = q.trim().toLowerCase();
    return items.filter((t) => !k || t.label.toLowerCase().includes(k));
  }, [items, q]);
  const view = paginate(filtered, page, size);

  if (!user) return null;
  const isAdmin = user.active === ADMIN_ROLE;
  if (!isAdmin && !conf.editors.includes(user.active)) return <ErrorPage code="403" />;

  const reload = () => (apiEnabled ? load.reload() : setItems(TRASH.filter((t) => conf.kinds.includes(t.kind))));
  /** Có backend: gửi lệnh lên máy chủ; trả lý do nếu bị từ chối. Không có backend: thao tác trên dữ liệu mẫu. */
  const run = async (t: Row, server: (s: ServerTrashItem) => Promise<unknown>, local: (id: string) => string | null | void) => {
    if (!t.server) return local(t.id) || null;
    try {
      await server(t.server);
      return null;
    } catch (e) {
      return (e as Error).message;
    }
  };

  return (
    <AppLayout crumb={conf.crumb} module={isAdmin ? USERS_MODULE : conf.module}>
      <div className="h">
        <div>
          <h2>Thùng rác</h2>
          <p>Bản ghi đã xoá vẫn khôi phục được. Chỉ Quản trị hệ thống xoá vĩnh viễn được.</p>
        </div>
      </div>
      {isAdmin ? (
        <nav className="tabs">
          {(Object.keys(AREAS) as Area[]).map((a) => (
            <NavLink key={a} to={AREA_PATH[a]} className={({ isActive }) => (isActive ? 'on' : undefined)} end>
              {AREAS[a].crumb[0]}
            </NavLink>
          ))}
        </nav>
      ) : (
        area === 'training' && <TrainingTabs />
      )}
      <LoadState {...load} />
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
                    {(!isAdmin || area === 'users') && (
                      <button
                        type="button"
                        className="btn"
                        style={{ height: 34 }}
                        onClick={async () => {
                          const why = await run(t, (s) => restoreItem(s.kind, s.serverId), restore);
                          if (why) return toast('Chưa khôi phục được', why);
                          reload();
                          toast('Đã khôi phục', t.label);
                        }}
                      >
                        <Icon name="refresh" size={16} />
                        Khôi phục
                      </button>
                    )}
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
          onConfirm={async () => {
            const why = await run(purging, purgeFromTrash, purge);
            if (why) {
              setPurging(null);
              return toast('Chưa xoá được', why);
            }
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
