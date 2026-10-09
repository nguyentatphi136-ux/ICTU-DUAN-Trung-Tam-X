import { useState } from 'react';
import { Icon } from '../../components/Icon';
import { Box, Drawer, initial, Pill } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { ADMIN_ROLE, ROLES } from '../../data/permissions';
import { statusTone, type UserRow } from '../../data/users';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const PHONE_RE = /^0\d{9}$/;

type Props = {
  /** null: tạo mới. */
  user: UserRow | null;
  users: UserRow[];
  onClose: () => void;
  onSave: (u: UserRow) => void;
  onLock: (u: UserRow) => void;
};

// S1-08 tạo tài khoản và S1-09 sửa, gán hoặc thu hồi vai trò. Email trùng bị từ chối ngay tại ô.
// Khi tích hợp: POST /admin/users (máy chủ gửi email kích hoạt kèm mật khẩu tạm), PUT /admin/users/:id, PUT /admin/users/:id/roles.
export function UserDrawer({ user, users, onClose, onSave, onLock }: Props) {
  const { user: me } = useAuth();
  const [name, setName] = useState(user?.name ?? '');
  const [email, setEmail] = useState(user?.email ?? '');
  const [phone, setPhone] = useState(user?.phone ?? '');
  const [roles, setRoles] = useState<number[]>(user?.roles ?? []);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const isNew = !user;
  // Không tự thu hồi vai trò quản trị của chính mình.
  const isSelf = !!user && !!me && user.email.toLowerCase() === me.email.toLowerCase();

  const duplicate = isNew && users.find((u) => u.email.toLowerCase() === email.trim().toLowerCase());

  function save() {
    const e: Record<string, string> = {};
    if (!name.trim()) e.name = 'Vui lòng nhập họ và tên';
    if (isNew && !EMAIL_RE.test(email.trim())) e.email = 'Email không đúng định dạng';
    if (duplicate) e.email = `Email này đã được dùng cho tài khoản ${duplicate.name}`;
    if (!PHONE_RE.test(phone.replace(/\s/g, ''))) e.phone = 'Số điện thoại Việt Nam gồm 10 chữ số, bắt đầu bằng 0';
    if (!roles.length) e.roles = 'Chọn ít nhất một vai trò';
    setErrors(e);
    if (Object.keys(e).length) return;
    onSave({ id: user?.id ?? 0, status: 'Chờ kích hoạt', ...user, name: name.trim().toUpperCase(), email: email.trim(), phone: phone.trim(), roles });
  }

  const field = (id: string, label: string, value: string, set: (v: string) => void, opts: { icon?: 'mail'; disabled?: boolean; hint?: string } = {}) => (
    <div className="field">
      <label htmlFor={'ud-' + id}>{label}</label>
      <div className={'inp' + (opts.icon ? ' lead' : '') + (errors[id] ? ' bad' : '')}>
        {opts.icon && <Icon name={opts.icon} />}
        <input
          id={'ud-' + id}
          value={value}
          disabled={opts.disabled}
          onChange={(e) => {
            set(e.target.value);
            setErrors((x) => ({ ...x, [id]: '' }));
          }}
        />
      </div>
      {errors[id] ? <p className="err">{errors[id]}</p> : opts.hint && <p className="hint">{opts.hint}</p>}
    </div>
  );

  return (
    <Drawer
      title={isNew ? 'Thêm tài khoản' : 'Sửa tài khoản'}
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
            Huỷ
          </button>
          <button type="button" className="btn primary" style={{ width: 160 }} onClick={save}>
            {isNew ? 'Tạo tài khoản' : 'Lưu thay đổi'}
          </button>
        </>
      }
    >
      {user && (
        <div className="person" style={{ marginBottom: 20 }}>
          <span className="initial" style={{ width: 48, height: 48, fontSize: 18 }}>
            {initial(user.name)}
          </span>
          <span>
            <b style={{ display: 'block', fontSize: 18, fontWeight: 600 }}>{user.name}</b>
            <Pill tone={statusTone(user.status)}>{user.status}</Pill>
          </span>
        </div>
      )}
      {field('name', 'Họ và tên', name, setName)}
      {field('email', 'Email', email, setEmail, { icon: 'mail', disabled: !isNew, hint: isNew ? undefined : 'Email là tên đăng nhập, không đổi được ở đây.' })}
      {field('phone', 'Số điện thoại', phone, setPhone)}

      {isNew ? (
        <div className="field">
          <label htmlFor="ud-role">Vai trò</label>
          <div className={'inp chipbox' + (errors.roles ? ' bad' : '')}>
            <RoleChips roles={roles} setRoles={setRoles} />
            <select
              id="ud-role"
              value=""
              onChange={(e) => {
                setRoles((r) => [...r, Number(e.target.value)]);
                setErrors((x) => ({ ...x, roles: '' }));
              }}
            >
              <option value="">Thêm vai trò</option>
              {ROLES.map((r, i) => !roles.includes(i) && <option key={r} value={i}>{r}</option>)}
            </select>
          </div>
          {errors.roles ? <p className="err">{errors.roles}</p> : <p className="hint">Một người có thể giữ nhiều vai trò cùng lúc.</p>}
        </div>
      ) : (
        <>
          <hr className="sep" />
          <div className="section-t">Vai trò</div>
          <div className="chips">
            <RoleChips roles={roles} setRoles={setRoles} locked={isSelf ? ADMIN_ROLE : undefined} />
            <AddRole roles={roles} onAdd={(r) => setRoles((x) => [...x, r])} />
          </div>
          {errors.roles ? <p className="err">{errors.roles}</p> : <p className="hint" style={{ marginTop: 10 }}>Thay đổi vai trò có hiệu lực ngay ở thao tác kế tiếp, người dùng không cần đăng nhập lại. Bạn không thể tự thu hồi vai trò quản trị của chính mình.</p>}
          <hr className="sep" />
          <div className="section-t">Trạng thái tài khoản</div>
          {user.status === 'Đã khoá' ? (
            <button type="button" className="btn" onClick={() => onLock(user)}>
              <Icon name="lock" size={16} />
              Mở khoá tài khoản
            </button>
          ) : (
            <button type="button" className="btn outline-danger" style={{ width: 180 }} disabled={isSelf} onClick={() => onLock(user)}>
              <Icon name="lock" size={16} />
              Khoá tài khoản
            </button>
          )}
        </>
      )}

      {isNew && (
        <Box icon="mail" title="Email kích hoạt sẽ được gửi ngay">
          Kèm mật khẩu tạm, người dùng đổi lại ở lần đăng nhập đầu.
        </Box>
      )}
    </Drawer>
  );
}

function RoleChips({ roles, setRoles, locked }: { roles: number[]; setRoles: (f: (r: number[]) => number[]) => void; locked?: number }) {
  return (
    <>
      {roles.map((r) => (
        <span className="chip" key={r}>
          {ROLES[r]}
          {r !== locked && (
            <button type="button" aria-label={'Thu hồi vai trò ' + ROLES[r]} onClick={() => setRoles((x) => x.filter((y) => y !== r))}>
              <Icon name="x" />
            </button>
          )}
        </span>
      ))}
    </>
  );
}

function AddRole({ roles, onAdd }: { roles: number[]; onAdd: (r: number) => void }) {
  const [open, setOpen] = useState(false);
  const rest = ROLES.map((r, i) => [r, i] as const).filter(([, i]) => !roles.includes(i));
  if (!rest.length) return null;
  return (
    <span style={{ position: 'relative' }}>
      <button type="button" className="chip-add" aria-expanded={open} onClick={() => setOpen((o) => !o)}>
        <Icon name="plus" />
        Thêm vai trò
      </button>
      {open && (
        <div className="dropdown" style={{ left: 0, right: 'auto' }}>
          {rest.map(([r, i]) => (
            <button
              key={r}
              type="button"
              onClick={() => {
                onAdd(i);
                setOpen(false);
              }}
            >
              {r}
            </button>
          ))}
        </div>
      )}
    </span>
  );
}
