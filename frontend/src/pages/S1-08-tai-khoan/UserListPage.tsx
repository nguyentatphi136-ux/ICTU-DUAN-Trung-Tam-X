import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Filter, initial, Pager, paginate, Pill, RowMenu, Search } from '../../components/ui';
import { api, ApiError, isOffline, ROLE_CODES, roleIndexes } from '../../data/api';
import { ROLES } from '../../data/permissions';
import { statusTone, loadUsers, saveUsers, type Status, type UserRow } from '../../data/users';
import { LockDialog } from './LockDialog';
import { UserDrawer } from './UserDrawer';

const STATUSES: Status[] = ['Hoạt động', 'Chờ kích hoạt', 'Đã khoá'];
const STATUS_OF: Record<string, Status> = { ACTIVE: 'Hoạt động', PENDING: 'Chờ kích hoạt', LOCKED: 'Đã khoá' };

type ServerRow = { id: number; fullName: string; email: string; phone: string; roles: string[]; status: string; lockedReason?: string };
const fromServer = (u: ServerRow): UserRow => ({
  id: u.id,
  name: u.fullName.toUpperCase(),
  email: u.email,
  phone: u.phone ?? '',
  roles: roleIndexes(u.roles),
  status: STATUS_OF[u.status] ?? 'Hoạt động',
  lockReason: u.lockedReason ?? undefined,
});
const codes = (roles: number[]) => roles.map((r) => ROLE_CODES[r]);

// S1-08. Danh sách tài khoản: tìm theo tên, email, số điện thoại; lọc vai trò và trạng thái; 20 dòng mỗi trang.
// S1-09 (gán vai trò) nằm trong ngăn Sửa, S1-10 (khoá, mở khoá) là hộp thoại mở từ menu ba chấm.
// Có backend thì đọc GET /api/admin/users và ghi qua POST /users, PUT /users/:id, /roles, /status; chưa bật thì dùng dữ liệu mẫu.
export function UserListPage() {
  const toast = useToast();
  const [users, setUsers] = useState<UserRow[]>(loadUsers);
  const [q, setQ] = useState('');
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(1);
  const [editing, setEditing] = useState<UserRow | 'new' | null>(null);
  const [locking, setLocking] = useState<UserRow | null>(null);
  const [remote, setRemote] = useState(false);

  // ponytail: tải hết một lần rồi lọc ở trình duyệt, đủ cho vài nghìn tài khoản; nhiều hơn thì chuyển sang lọc và phân trang ở máy chủ.
  useEffect(() => {
    api<{ items: ServerRow[] }>('GET', '/admin/users?pageSize=5000')
      .then((d) => {
        setUsers(d.items.map(fromServer));
        setRemote(true);
      })
      .catch((e) => {
        if (!isOffline(e)) toast('Không tải được danh sách từ máy chủ', (e as ApiError).message);
      });
  }, [toast]);

  /** Chạy thao tác trên máy chủ (nếu có); lỗi thì báo và trả false để giữ nguyên hộp thoại. */
  const remoteDo = async (f: () => Promise<unknown>) => {
    if (!remote) return true;
    try {
      await f();
      return true;
    } catch (e) {
      toast('Chưa lưu được', (e as ApiError).message);
      return false;
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
  const view = paginate(filtered, page);

  const update = (u: UserRow) => {
    setUsers((list) => {
      const next = list.map((x) => (x.id === u.id ? u : x));
      saveUsers(next);
      return next;
    });
  };
  const resetPage = <T,>(f: (v: T) => void) => (v: T) => {
    f(v);
    setPage(1);
  };

  return (
    <AppLayout crumb={['Người dùng & nhật ký', 'Tài khoản']} module="Người dùng & nhật ký">
      <div className="h">
        <div>
          <h2>Tài khoản người dùng</h2>
          <p>{users.length} tài khoản</p>
        </div>
        <Link to="/quan-tri/phan-quyen" className="btn">
          <Icon name="shield" size={16} />
          Ma trận phân quyền
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
        <Pager page={view.page} pages={view.pages} onPage={setPage}>
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
            const body = { fullName: u.name, phone: u.phone.replace(/\s/g, '') };
            if (editing === 'new') {
              let nextId = Math.max(0, ...users.map((x) => x.id)) + 1;
              const ok = await remoteDo(async () => {
                const created = await api<ServerRow>('POST', '/admin/users', { ...body, email: u.email, roles: codes(u.roles) });
                nextId = created.id;
                u = { ...u, status: 'Hoạt động' };
              });
              if (!ok) return;
              const next = [{ ...u, id: nextId }, ...users];
              setUsers(next);
              saveUsers(next);
              toast('Đã tạo tài khoản', `Email kích hoạt đã gửi tới ${u.email}.`);
            } else {
              const before = editing;
              const ok = await remoteDo(async () => {
                await api('PUT', `/admin/users/${u.id}`, body);
                if (codes(u.roles).join() !== codes(before.roles).join()) await api('PUT', `/admin/users/${u.id}/roles`, { roleCodes: codes(u.roles) });
              });
              if (!ok) return;
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
          onConfirm={async (reason) => {
            const locked = locking.status !== 'Đã khoá';
            const ok = await remoteDo(() => api('PUT', `/admin/users/${locking.id}/status`, { status: locked ? 'LOCKED' : 'ACTIVE', reason }));
            if (!ok) return;
            update({ ...locking, status: locked ? 'Đã khoá' : 'Hoạt động', lockReason: locked ? reason : undefined });
            toast(locked ? 'Đã khoá tài khoản' : 'Đã mở khoá tài khoản', locking.name);
            setLocking(null);
          }}
        />
      )}
    </AppLayout>
  );
}
