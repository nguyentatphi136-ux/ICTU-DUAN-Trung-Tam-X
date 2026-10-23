import { Drawer, initial, Pill } from '../../components/ui';
import { viDate } from '../../data/leads';
import { ROLES } from '../../data/permissions';
import { statusTone, type UserRow } from '../../data/users';

const GENDER: Record<string, string> = { MALE: 'Nam', FEMALE: 'Nữ', OTHER: 'Khác' };

// S1-08. Thông tin chi tiết một tài khoản. Danh sách hiện họ tên IN HOA không dấu; ở đây hiện họ tên viết như người nhập.
export function UserDetail({ user, onClose, onEdit }: { user: UserRow; onClose: () => void; onEdit?: () => void }) {
  const rows: [string, string | undefined][] = [
    ['Họ và tên', user.displayName ?? user.name],
    ['Tên trong danh sách', user.name],
    ['Mã tài khoản', user.code],
    ['Email đăng nhập', user.email],
    ['Số điện thoại', user.phone],
    ['Ngày sinh', user.birth && viDate(user.birth)],
    ['Giới tính', user.gender && GENDER[user.gender]],
    ['Quê quán', user.hometown],
    ['Địa chỉ hiện tại', user.address],
    ['Vai trò', user.roles.map((r) => ROLES[r]).join(', ')],
    ['Ngày tạo tài khoản', user.createdAt && viDate(user.createdAt.slice(0, 10))],
  ];
  return (
    <Drawer
      title="Thông tin tài khoản"
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
            Đóng
          </button>
          {onEdit && (
            <button type="button" className="btn primary" style={{ width: 160 }} onClick={onEdit}>
              Sửa tài khoản
            </button>
          )}
        </>
      }
    >
      <div className="person" style={{ marginBottom: 20 }}>
        <span className="initial" style={{ width: 48, height: 48, fontSize: 18 }}>
          {initial(user.name)}
        </span>
        <span>
          <b style={{ display: 'block', fontSize: 18, fontWeight: 600 }}>{user.displayName ?? user.name}</b>
          <Pill tone={statusTone(user.status)}>{user.status}</Pill>
        </span>
      </div>
      {user.status === 'Đã khoá' && user.lockReason && (
        <p className="err" style={{ marginBottom: 16 }}>
          Lý do khoá: {user.lockReason}
        </p>
      )}
      <dl style={{ display: 'grid', gridTemplateColumns: 'minmax(120px, 40%) 1fr', gap: '12px 16px', margin: 0 }}>
        {rows.map(([label, value]) => [
          <dt key={label + ':k'} style={{ color: 'var(--muted)' }}>
            {label}
          </dt>,
          <dd key={label + ':v'} style={{ margin: 0, fontWeight: 500, overflowWrap: 'anywhere' }}>
            {value || '—'}
          </dd>,
        ])}
      </dl>
    </Drawer>
  );
}
