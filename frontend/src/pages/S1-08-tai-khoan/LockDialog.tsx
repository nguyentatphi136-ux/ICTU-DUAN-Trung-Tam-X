import { useState } from 'react';
import { Icon } from '../../components/Icon';
import { Modal } from '../../components/ui';
import { ROLES } from '../../data/permissions';
import type { UserRow } from '../../data/users';

// S1-10. Khoá tài khoản bắt buộc ghi lý do; lớp người đó phụ trách được cảnh báo cần bàn giao.
// Mở khoá dùng cùng hộp thoại, không có phần cảnh báo. Khi tích hợp: POST /admin/users/:id/lock | /unlock { reason }.
export function LockDialog({ user, onClose, onConfirm }: { user: UserRow; onClose: () => void; onConfirm: (reason: string) => void }) {
  const [reason, setReason] = useState('');
  const [touched, setTouched] = useState(false);
  const unlock = user.status === 'Đã khoá';
  const missing = !reason.trim();

  return (
    <Modal onClose={onClose} label={unlock ? 'Mở khoá tài khoản' : 'Khoá tài khoản'}>
      <div className="modal-head">
        <span className="danger-ic" style={unlock ? { background: 'var(--soft)', color: 'var(--blue)' } : undefined}>
          <Icon name="lock" size={20} />
        </span>
        <div>
          <h3>
            {unlock ? 'Mở khoá' : 'Khoá'} tài khoản {user.name}?
          </h3>
          <p>
            {user.roles.map((r) => ROLES[r]).join(', ')} · {user.email}
          </p>
        </div>
      </div>
      <p style={{ color: 'var(--muted)', fontSize: 15, lineHeight: 1.6, marginBottom: 16 }}>
        {unlock ? 'Người dùng sẽ đăng nhập lại được bằng mật khẩu hiện tại.' : 'Tài khoản sẽ không đăng nhập được và mọi phiên đang mở bị thu hồi ngay.'}
      </p>
      <div className="field">
        <label htmlFor="lock-reason">Lý do {unlock ? 'mở khoá' : 'khoá'} (bắt buộc)</label>
        <div className={'inp' + (touched && missing ? ' bad' : '')}>
          <textarea id="lock-reason" value={reason} placeholder={unlock ? 'Ví dụ: đã quay lại làm việc' : 'Ví dụ: Nghỉ việc từ 05/10/2026'} onChange={(e) => setReason(e.target.value)} />
        </div>
        {touched && missing && <p className="err">Vui lòng nhập lý do</p>}
      </div>
      {!unlock && !!user.classes?.length && (
        <div className="box warn">
          <Icon name="alert" size={20} />
          <div>
            <b>{user.classes.length} lớp đang do người này phụ trách cần bàn giao</b>
            {user.classes.map((c) => (
              <div key={c}>{c}</div>
            ))}
            <small>Quản lý đào tạo sẽ nhận cảnh báo để phân công lại.</small>
          </div>
        </div>
      )}
      <div className="acts">
        <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
          Huỷ
        </button>
        <button
          type="button"
          className={'btn ' + (unlock ? 'primary' : 'danger')}
          style={{ width: 170 }}
          onClick={() => {
            setTouched(true);
            if (!missing) onConfirm(reason.trim());
          }}
        >
          <Icon name="lock" size={16} />
          {unlock ? 'Mở khoá tài khoản' : 'Khoá tài khoản'}
        </button>
      </div>
    </Modal>
  );
}
