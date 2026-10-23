import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { ConfirmDialog, Filter, initial, LoadState, Pager, paginate, Pill, RowMenu, Search, usePaging } from '../../components/ui';
import { apiEnabled, useLoad } from '../../data/api';
import { auditBy, type Change } from '../../data/audit';
import { useAuth } from '../../data/auth';
import { moveToTrash, restore } from '../../data/trash';
import { ADMIN_ROLE, ROLES, USERS_MODULE } from '../../data/permissions';
import { createUser, loadUserDetail, loadUsers, saveUser, setLocked, trashUser } from '../../data/s1api';
import { restoreItem } from '../../data/s2api';
import { statusTone, USERS, type Status, type UserRow } from '../../data/users';
import { LockDialog } from './LockDialog';
import { UserDetail } from './UserDetail';
import { UserDrawer } from './UserDrawer';

const STATUSES: Status[] = ['Hoạt động', 'Chờ kích hoạt', 'Đã khoá'];
const roleNames = (u: UserRow) => u.roles.map((r) => ROLES[r]).join(', ');
// Thông tin tài khoản ghi vào nhật ký khi sửa (S6-11), cặp [tên trường, cách lấy giá trị].
const FIELDS: [string, (u: UserRow) => string | undefined][] = [
  ['Họ tên', (u) => u.name],
  ['Email', (u) => u.email],
  ['Số điện thoại', (u) => u.phone],
  ['Ngày sinh', (u) => u.birth],
  ['Giới tính', (u) => u.gender],
  ['Quê quán', (u) => u.hometown],
  ['Địa chỉ', (u) => u.address],
];

// S1-08. Danh sách tài khoản: tìm theo tên, email, số điện thoại; lọc vai trò và trạng thái; 20 dòng mỗi trang.
// S1-09 (gán vai trò) nằm trong ngăn Sửa, S1-10 (khoá, mở khoá) là hộp thoại mở từ menu ba chấm.
export function UserListPage() {
  const toast = useToast();
  const [users, setUsers] = useState(USERS);
  const [q, setQ] = useState('');
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const { page, setPage, size, setSize } = usePaging();
  const [editing, setEditing] = useState<UserRow | 'new' | null>(null);
  const [locking, setLocking] = useState<UserRow | null>(null);
  const [deleting, setDeleting] = useState<UserRow | null>(null);
  const [viewing, setViewing] = useState<UserRow | null>(null);
  const { user: me } = useAuth();
  // Sheet 2: Quản lý đào tạo chỉ xem (R) "Người dùng & nhật ký"; tạo, sửa, khoá là việc của Quản trị (S1-08, S1-09, S1-10).
  const canWrite = me?.active === ADMIN_ROLE;
  // Có backend: tải tài khoản từ máy chủ; mọi thay đổi gửi lên máy chủ rồi tải lại.
  const load = useLoad(async () => {
    await loadUsers();
    setUsers([...USERS]);
  });

  // Có backend: danh sách chỉ có thông tin gọn, mở xem hoặc sửa thì tải thông tin chi tiết của tài khoản đó.
  const withDetail = async (u: UserRow, open: (d: UserRow) => void) => {
    if (!apiEnabled) return open(u);
    try {
      open(await loadUserDetail(u.id));
    } catch (e) {
      toast('Chưa tải được thông tin tài khoản', (e as Error).message);
    }
  };

  const filtered = useMemo(() => {
    const k = q.trim().toLowerCase().replace(/\s/g, '');
    return users.filter(
      (u) =>
        (!k || u.name.toLowerCase().replace(/\s/g, '').includes(k) || u.email.toLowerCase().includes(k) || u.phone.replace(/\s/g, '').includes(k)) &&
        (!role || u.roles.includes(ROLES.indexOf(role as (typeof ROLES)[number]))) &&
        (!status || u.status === status),
    );
  }, [users, q, role, status]);
  const view = paginate(filtered, page, size);

  // S6-11: thao tác của Quản trị trên tài khoản cũng ghi nhật ký.
  const log = (entity: 'Tài khoản' | 'Vai trò', action: string, u: UserRow, changes: Change[], note?: string) =>
    auditBy(me, { entity, action, target: `${u.email.split('@')[0]} · ${u.name}`, changes, note });
  const logEdit = (old: UserRow, u: UserRow) => {
    const before = roleNames(old);
    const after = roleNames(u);
    if (before !== after)
      log('Vai trò', u.roles.length > old.roles.length ? 'Thêm vai trò' : u.roles.length < old.roles.length ? 'Gỡ vai trò' : 'Đổi vai trò', u, [{ field: 'Vai trò', before: before || '(không có)', after: after || '(không còn vai trò)' }]);
    const changes = FIELDS.filter(([, get]) => (get(old) ?? '') !== (get(u) ?? '')).map(([field, get]) => ({ field, before: get(old) || '–', after: get(u) || '–' }));
    if (changes.length) log('Tài khoản', 'Sửa thông tin', u, changes);
  };
  const update = (u: UserRow) => setUsers((list) => list.map((x) => (x.id === u.id ? u : x)));
  const trashed = (u: UserRow) => log('Tài khoản', 'Chuyển vào thùng rác', u, [{ field: 'Trạng thái', before: u.status, after: 'Trong thùng rác' }]);
  const restored = (u: UserRow) => log('Tài khoản', 'Khôi phục', u, [{ field: 'Trạng thái', before: 'Trong thùng rác', after: u.status }]);
  const trash = async (u: UserRow) => {
    if (apiEnabled) {
      try {
        await trashUser(u.id);
      } catch (e) {
        return toast('Chưa chuyển được vào thùng rác', (e as Error).message);
      }
      setDeleting(null);
      load.reload();
      trashed(u);
      return toast('Đã chuyển vào thùng rác', u.name, {
        label: 'Hoàn tác',
        onClick: () =>
          void restoreItem('user', u.id).then(() => {
            restored(u);
            load.reload();
          }),
      });
    }
    const id = moveToTrash({ kind: 'user', item: u }, `${u.name} · ${u.email}`, me?.name ?? '');
    setUsers([...USERS]);
    setDeleting(null);
    trashed(u);
    toast('Đã chuyển vào thùng rác', u.name, {
      label: 'Hoàn tác',
      onClick: () => {
        restore(id);
        restored(u);
        setUsers([...USERS]);
      },
    });
  };
  const resetPage = <T,>(f: (v: T) => void) => (v: T) => {
    f(v);
    setPage(1);
  };

  return (
    <AppLayout crumb={[USERS_MODULE, 'Tài khoản']} module={USERS_MODULE}>
      <div className="h">
        <div>
          <h2>Tài khoản người dùng</h2>
          <p>{users.length} tài khoản</p>
        </div>
        <Link to="/quan-tri/phan-quyen" className="btn">
          <Icon name="shield" size={16} />
          Ma trận phân quyền
        </Link>
        {canWrite && (
          <>
            {/* S6-11, chỉ Quản trị xem */}
            <Link to="/quan-tri/nhat-ky" className="btn">
              <Icon name="file" size={16} />
              Nhật ký thao tác
            </Link>
            <Link to="/quan-tri/thung-rac" className="btn">
              <Icon name="trash" size={16} />
              Thùng rác
            </Link>
            <Link to="/quan-tri/nhap-excel" className="btn">
              <Icon name="upload" size={16} />
              Nhập từ Excel
            </Link>
            <button type="button" className="btn primary" onClick={() => setEditing('new')}>
              <Icon name="plus" size={16} />
              Thêm tài khoản
            </button>
          </>
        )}
      </div>
      <LoadState {...load} />
      <div className="bar">
        <Search placeholder="Tìm theo tên, email, số điện thoại" value={q} onChange={resetPage(setQ)} width={460} />
        <Filter label="Vai trò" value={role} options={[...ROLES]} onChange={resetPage(setRole)} />
        <Filter label="Trạng thái" value={status} options={STATUSES} onChange={resetPage(setStatus)} />
      </div>
      <div className="panel">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Họ tên</th>
                <th>Email</th>
                <th>Số điện thoại</th>
                <th>Vai trò</th>
                <th>Trạng thái</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {view.items.map((u) => (
                <tr key={u.id}>
                  <td>
                    <button
                      type="button"
                      className="person"
                      title="Xem thông tin chi tiết"
                      style={{ background: 'none', border: 0, padding: 0, color: 'inherit', font: 'inherit', cursor: 'pointer', textAlign: 'left' }}
                      onClick={() => void withDetail(u, setViewing)}
                    >
                      <span className="initial">{initial(u.name)}</span>
                      {u.name}
                    </button>
                  </td>
                  <td>{u.email}</td>
                  <td style={{ whiteSpace: 'nowrap' }}>{u.phone}</td>
                  <td>
                    <span className="tags">
                      {u.roles.map((r) => (
                        <Pill key={r}>{ROLES[r]}</Pill>
                      ))}
                    </span>
                  </td>
                  <td>
                    <Pill tone={statusTone(u.status)}>{u.status}</Pill>
                  </td>
                  <td style={{ width: 56 }}>
                    <RowMenu
                      actions={[
                        { label: 'Xem chi tiết', onClick: () => void withDetail(u, setViewing) },
                        ...(canWrite
                          ? [
                              { label: 'Sửa', onClick: () => void withDetail(u, setEditing) },
                              u.status === 'Đã khoá' ? { label: 'Mở khoá', onClick: () => setLocking(u) } : { label: 'Khoá', danger: true, onClick: () => setLocking(u) },
                              // Không tự chuyển tài khoản của mình vào thùng rác (máy chủ cũng chặn).
                              ...(u.email === me?.email ? [] : [{ label: 'Chuyển vào thùng rác', danger: true, onClick: () => setDeleting(u) }]),
                            ]
                          : []),
                      ]}
                    />
                  </td>
                </tr>
              ))}
              {!view.items.length && (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: 40 }}>
                    Không có tài khoản nào khớp bộ lọc.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pager page={view.page} pages={view.pages} onPage={setPage} size={size} onSize={setSize}>
          Hiển thị {view.from}–{view.to} trong {filtered.length} tài khoản
        </Pager>
      </div>

      {editing && (
        <UserDrawer
          user={editing === 'new' ? null : editing}
          users={users}
          onClose={() => setEditing(null)}
          onLock={(u) => {
            setEditing(null);
            setLocking(u);
          }}
          onSave={async (u) => {
            if (apiEnabled) {
              try {
                if (editing === 'new') {
                  const created = await createUser(u);
                  log('Tài khoản', 'Tạo tài khoản', u, [{ field: 'Vai trò', before: '–', after: roleNames(u) }]);
                  toast('Đã tạo tài khoản', `Mật khẩu tạm ${created?.tempPassword ?? ''}, đã gửi kèm email kích hoạt tới ${u.email}. Người dùng phải đổi ở lần đăng nhập đầu.`);
                } else {
                  await saveUser(editing, u);
                  logEdit(editing, u);
                  toast('Đã lưu thay đổi', u.name);
                }
              } catch (e) {
                // Ví dụ 409 EMAIL_ALREADY_EXISTS hoặc số điện thoại trùng: giữ ngăn mở để sửa lại.
                return toast('Chưa lưu được', (e as Error).message);
              }
              setEditing(null);
              return load.reload();
            }
            if (editing === 'new') {
              setUsers((list) => [{ ...u, id: Math.max(...list.map((x) => x.id)) + 1 }, ...list]);
              log('Tài khoản', 'Tạo tài khoản', u, [{ field: 'Vai trò', before: '–', after: roleNames(u) }]);
              toast('Đã tạo tài khoản', `Email kích hoạt đã gửi tới ${u.email}.`);
            } else {
              logEdit(editing, u);
              update(u);
              toast('Đã lưu thay đổi', u.name);
            }
            setEditing(null);
          }}
        />
      )}
      {viewing && (
        <UserDetail
          user={viewing}
          onClose={() => setViewing(null)}
          onEdit={
            canWrite
              ? () => {
                  setEditing(viewing);
                  setViewing(null);
                }
              : undefined
          }
        />
      )}
      {locking && (
        <LockDialog
          user={locking}
          onClose={() => setLocking(null)}
          onConfirm={async (reason) => {
            const locked = locking.status !== 'Đã khoá';
            const logLock = () =>
              log('Tài khoản', locked ? 'Khoá tài khoản' : 'Mở khoá tài khoản', locking, [{ field: 'Trạng thái', before: locking.status, after: locked ? 'Đã khoá' : 'Hoạt động' }], reason || undefined);
            if (apiEnabled) {
              let res;
              try {
                res = await setLocked(locking.id, locked, reason);
              } catch (e) {
                return toast(locked ? 'Chưa khoá được' : 'Chưa mở khoá được', (e as Error).message);
              }
              // S1-10: lớp người đó đang phụ trách cần bàn giao (máy chủ trả về sau khi khoá).
              logLock();
              const classes = res?.assignedClasses ?? [];
              toast(locked ? 'Đã khoá tài khoản' : 'Đã mở khoá tài khoản', classes.length ? `${locking.name}. Cần bàn giao ${classes.length} lớp: ${classes.map((c) => c.classCode).join(', ')}.` : locking.name);
              setLocking(null);
              return load.reload();
            }
            update({ ...locking, status: locked ? 'Đã khoá' : 'Hoạt động', lockReason: locked ? reason : undefined });
            logLock();
            toast(locked ? 'Đã khoá tài khoản' : 'Đã mở khoá tài khoản', locking.name);
            setLocking(null);
          }}
        />
      )}
      {deleting && (
        <ConfirmDialog title="Chuyển tài khoản vào thùng rác?" onClose={() => setDeleting(null)} onConfirm={() => trash(deleting)}>
          Tài khoản <b>{deleting.name}</b> ({deleting.email}) sẽ không đăng nhập được và ẩn khỏi danh sách.
        </ConfirmDialog>
      )}
    </AppLayout>
  );
}
