import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { ConfirmDialog, Filter, initial, Pager, paginate, Pill, RowMenu, Search, usePaging } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { moveToTrash, restore } from '../../data/trash';
import { ROLES, USERS_MODULE } from '../../data/permissions';
import { statusTone, USERS, type Status, type UserRow } from '../../data/users';
import { LockDialog } from './LockDialog';
import { UserDrawer } from './UserDrawer';

const STATUSES: Status[] = ['Hoạt động', 'Chờ kích hoạt', 'Đã khoá'];

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
  const { user: me } = useAuth();

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

  const update = (u: UserRow) => setUsers((list) => list.map((x) => (x.id === u.id ? u : x)));
  const trash = (u: UserRow) => {
    const id = moveToTrash({ kind: 'user', item: u }, `${u.name} · ${u.email}`, me?.name ?? '');
    setUsers([...USERS]);
    setDeleting(null);
    toast('Đã chuyển vào thùng rác', u.name, {
      label: 'Hoàn tác',
      onClick: () => {
        restore(id);
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
      </div>
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
                    <span className="person">
                      <span className="initial">{initial(u.name)}</span>
                      {u.name}
                    </span>
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
                        { label: 'Sửa', onClick: () => setEditing(u) },
                        u.status === 'Đã khoá' ? { label: 'Mở khoá', onClick: () => setLocking(u) } : { label: 'Khoá', danger: true, onClick: () => setLocking(u) },
                        // Không tự chuyển tài khoản của mình vào thùng rác (máy chủ cũng chặn).
                        ...(u.email === me?.email ? [] : [{ label: 'Chuyển vào thùng rác', danger: true, onClick: () => setDeleting(u) }]),
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
          onSave={(u) => {
            if (editing === 'new') {
              setUsers((list) => [{ ...u, id: Math.max(...list.map((x) => x.id)) + 1 }, ...list]);
              toast('Đã tạo tài khoản', `Email kích hoạt đã gửi tới ${u.email}.`);
            } else {
              update(u);
              toast('Đã lưu thay đổi', u.name);
            }
            setEditing(null);
          }}
        />
      )}
      {locking && (
        <LockDialog
          user={locking}
          onClose={() => setLocking(null)}
          onConfirm={(reason) => {
            const locked = locking.status !== 'Đã khoá';
            update({ ...locking, status: locked ? 'Đã khoá' : 'Hoạt động', lockReason: locked ? reason : undefined });
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
