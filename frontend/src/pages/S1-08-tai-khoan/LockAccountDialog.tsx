import { useState } from 'react';
import { Icon } from '../../components/Icon';
import { ROLES } from '../../data/permissions';
import type { UserRow as User } from '../../data/users';

type Props = {
  user: User;
  /** Các lớp người này đang phụ trách, để cảnh báo cần bàn giao. */
  classes: string[];
  /** Khoá thì kèm lý do; mở khoá thì không có lý do. */
  onConfirm: (reason?: string) => void;
  onCancel: () => void;
};

// S1-10. Hộp thoại khoá hoặc mở khoá tài khoản. Khoá bắt buộc ghi lý do.
// Khi tích hợp: POST /admin/users/{id}/lock, máy chủ thu hồi mọi phiên đang mở của người đó.
export function LockAccountDialog({ user, classes, onConfirm, onCancel }: Props) {
  const unlocking = user.status === 'Đã khoá';
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');

  function confirm() {
    if (unlocking) return onConfirm();
    if (!reason.trim()) return setError('Vui lòng ghi lý do khoá');
    onConfirm(reason.trim());
  }

  return (
    <div className="overlay">
      <div className="modal" role="alertdialog" aria-labelledby="lock-title">
        <div className="lk-head">
          <span className="t">
            <Icon name="lock" />
          </span>
          <div>
            <h3 id="lock-title">
              {unlocking ? 'Mở khoá' : 'Khoá'} tài khoản {user.name}?
            </h3>
            <p>
              {user.roles.map((r) => ROLES[r]).join(', ')} · {user.email}
            </p>
          </div>
        </div>
        <p style={{ color: 'var(--muted)', marginBottom: 16 }}>
          {unlocking
            ? 'Người này sẽ đăng nhập lại được bằng mật khẩu hiện có.'
            : 'Tài khoản sẽ không đăng nhập được và mọi phiên đang mở bị thu hồi ngay.'}
        </p>
        {!unlocking && (
          <>
            <div className="field">
              <label htmlFor="lock-reason">Lý do khoá (bắt buộc)</label>
              <div className={'inp' + (error ? ' bad' : '')}>
                <textarea
                  id="lock-reason"
                  autoFocus
                  value={reason}
                  onChange={(e) => {
                    setReason(e.target.value);
                    setError('');
                  }}
                />
              </div>
              {error && <p className="err">{error}</p>}
            </div>
            {classes.length > 0 && (
              <div className="box warn">
                <Icon name="alert" />
                <div>
                  <b>{classes.length} lớp đang do người này phụ trách cần bàn giao</b>
                  {classes.map((c) => (
                    <div key={c}>{c}</div>
                  ))}
                  Quản lý đào tạo sẽ nhận cảnh báo để phân công lại.
                </div>
              </div>
            )}
          </>
        )}
        <div className="acts">
          <button className="btn" onClick={onCancel}>
            Huỷ
          </button>
          <button className={'btn ' + (unlocking ? 'primary' : 'danger')} onClick={confirm}>
            {!unlocking && <Icon name="lock" />}
            {unlocking ? 'Mở khoá' : 'Khoá tài khoản'}
          </button>
        </div>
      </div>
    </div>
  );
}
