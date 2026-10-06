import { useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { Badge, CardPage } from '../../components/CardPage';
import { PasswordField } from '../../components/PasswordField';
import { isStrongPassword, PasswordRules } from '../../components/PasswordRules';
import { Box } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { DEMO_PASSWORD } from '../S1-01-dang-nhap/mockAuth';

// S1-04. Đổi mật khẩu khi đã đăng nhập. Nút chỉ bật khi đủ bốn yêu cầu và hai ô mật khẩu mới khớp nhau.
// Khi tích hợp: POST /auth/change-password { current, password }; máy chủ thu hồi mọi phiên khác, giữ phiên hiện tại.
export function ChangePasswordPage() {
  const { user } = useAuth();
  const location = useLocation();
  const [current, setCurrent] = useState('');
  const [pw, setPw] = useState('');
  const [confirm, setConfirm] = useState('');
  const [wrong, setWrong] = useState(false);
  const [done, setDone] = useState(false);

  if (!user) return <Navigate to="/dang-nhap" replace state={{ from: location.pathname }} />;

  const mismatch = confirm.length > 0 && confirm !== pw;
  const valid = current.length > 0 && isStrongPassword(pw) && confirm === pw;

  if (done)
    return (
      <CardPage closeTo="/">
        <Badge icon="check" />
        <div className="eyebrow">Hoàn tất</div>
        <h1>Đã đổi mật khẩu</h1>
        <p className="lead-t">Mật khẩu mới có hiệu lực ngay từ bây giờ.</p>
        <Box icon="devices" tone="ok" title="Các phiên đăng nhập khác đã bị thu hồi">
          Phiên trên thiết bị này vẫn giữ nguyên.
        </Box>
        <Link className="btn primary block" to="/">
          Về trang chủ
        </Link>
      </CardPage>
    );

  return (
    <CardPage closeTo="/">
      <Badge icon="key" />
      <h1>Đổi mật khẩu</h1>
      <p className="lead-t">Nhập mật khẩu hiện tại rồi chọn một mật khẩu mới.</p>
      <form
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          if (!valid) return;
          if (current !== DEMO_PASSWORD) return setWrong(true);
          setCurrent('');
          setPw('');
          setConfirm('');
          setDone(true);
        }}
      >
        <PasswordField
          id="cp-current"
          label="Mật khẩu hiện tại"
          value={current}
          autoComplete="current-password"
          placeholder="Nhập mật khẩu hiện tại"
          onChange={(v) => {
            setCurrent(v);
            setWrong(false);
          }}
          error={wrong ? 'Mật khẩu hiện tại không đúng' : undefined}
        />
        <PasswordField id="cp-new" label="Mật khẩu mới" value={pw} onChange={setPw} placeholder="Nhập mật khẩu mới" />
        <PasswordField id="cp-confirm" label="Xác nhận mật khẩu" value={confirm} onChange={setConfirm} placeholder="Nhập lại mật khẩu" error={mismatch ? 'Mật khẩu xác nhận không khớp' : undefined} />
        <PasswordRules value={pw} />
        <Box icon="devices" title="Các thiết bị khác sẽ bị đăng xuất">
          Phiên trên thiết bị này vẫn giữ nguyên.
        </Box>
        <button className="btn primary block" type="submit" disabled={!valid}>
          Cập nhật mật khẩu
        </button>
      </form>
    </CardPage>
  );
}
