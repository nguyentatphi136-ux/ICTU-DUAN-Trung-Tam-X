import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Badge, CardPage } from '../../components/CardPage';
import { Icon } from '../../components/Icon';
import { PasswordField } from '../../components/PasswordField';
import { isStrongPassword, PasswordRules } from '../../components/PasswordRules';
import { Box } from '../../components/ui';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const LINK_SECONDS = 30 * 60;
const mmss = (s: number) => String(Math.floor(s / 60)).padStart(2, '0') + ':' + String(s % 60).padStart(2, '0');

const BackToLogin = () => (
  <Link className="center" to="/dang-nhap">
    <Icon name="back" />
    Quay lại đăng nhập
  </Link>
);

// S1-03 bước 1 và 2: nhập email, báo đã gửi. Cùng một thông báo cho mọi email để không dò được ai có tài khoản.
// Khi tích hợp: POST /auth/forgot-password { email }.
export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [sentAt, setSentAt] = useState(0);
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    if (!sentAt) return;
    const t = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(t);
  }, [sentAt]);

  function submit(e: FormEvent) {
    e.preventDefault();
    const v = email.trim();
    if (!v) return setError('Vui lòng nhập email');
    if (!EMAIL_RE.test(v)) return setError('Email không đúng định dạng');
    setError('');
    setSentAt(Date.now());
    setNow(Date.now());
  }

  if (sentAt) {
    const left = Math.max(0, LINK_SECONDS - Math.floor((now - sentAt) / 1000));
    return (
      <CardPage closeTo="/dang-nhap">
        <Badge icon="check" />
        <div className="eyebrow">Đã gửi yêu cầu</div>
        <h1>Kiểm tra hộp thư của bạn</h1>
        <p className="lead-t">Nếu {email.trim()} có tài khoản, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu đến địa chỉ này.</p>
        <Box icon="clock" title={left ? `Liên kết hết hạn sau ${mmss(left)}` : 'Liên kết đã hết hạn'}>
          Chỉ sử dụng được một lần.
        </Box>
        <button type="button" className="btn block" onClick={() => setSentAt(Date.now())}>
          Gửi lại liên kết
        </button>
        <a
          className="center"
          href="#"
          onClick={(e) => {
            e.preventDefault();
            setSentAt(0);
          }}
        >
          Nhập email khác
        </a>
      </CardPage>
    );
  }

  return (
    <CardPage closeTo="/dang-nhap">
      <Badge icon="mail" />
      <h1>Quên mật khẩu?</h1>
      <p className="lead-t">Nhập email bạn dùng để đăng nhập.</p>
      <form onSubmit={submit} noValidate>
        <div className="field">
          <label htmlFor="fp-email">Email đăng nhập</label>
          <div className={'inp lead' + (error ? ' bad' : '')}>
            <Icon name="mail" />
            <input
              id="fp-email"
              type="email"
              placeholder="S25200212123@tms.vn"
              autoComplete="username"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setError('');
              }}
            />
          </div>
          {error ? <p className="err">{error}</p> : <p className="hint">Chúng tôi sẽ gửi một liên kết bảo mật đến email này.</p>}
        </div>
        <button className="btn primary block" type="submit">
          Gửi liên kết đặt lại
          <Icon name="go" />
        </button>
      </form>
      <BackToLogin />
    </CardPage>
  );
}

// S1-03 bước 3 đến 5: mở từ liên kết trong email. token "het-han" mô phỏng liên kết quá 30 phút hoặc đã dùng.
// Khi tích hợp: POST /auth/reset-password { token, password }; máy chủ trả 410 nếu liên kết hết hiệu lực.
export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [pw, setPw] = useState('');
  const [confirm, setConfirm] = useState('');
  const [done, setDone] = useState(false);
  const expired = params.get('token') === 'het-han' || !params.get('token');
  const mismatch = confirm.length > 0 && confirm !== pw;
  const valid = isStrongPassword(pw) && confirm === pw;

  if (expired)
    return (
      <CardPage closeTo="/dang-nhap">
        <Badge icon="alert" amber />
        <h1>Liên kết không còn hiệu lực</h1>
        <p className="lead-t">Bạn cần yêu cầu một liên kết mới để đặt lại mật khẩu.</p>
        <Box icon="alert" tone="warn" title="Liên kết đã hết hạn hoặc đã được dùng">
          Mỗi liên kết chỉ dùng được một lần trong vòng 30 phút.
        </Box>
        <Link className="btn primary block" to="/quen-mat-khau">
          Yêu cầu liên kết mới
        </Link>
        <BackToLogin />
      </CardPage>
    );

  if (done)
    return (
      <CardPage closeTo="/dang-nhap">
        <Badge icon="check" />
        <div className="eyebrow">Hoàn tất</div>
        <h1>Mật khẩu đã được đặt lại</h1>
        <p className="lead-t">Bạn có thể đăng nhập bằng mật khẩu mới.</p>
        <Box icon="devices" tone="ok" title="Các phiên cũ đã bị đăng xuất">
          Mọi thiết bị khác cần đăng nhập lại bằng mật khẩu mới.
        </Box>
        <button type="button" className="btn primary block" onClick={() => navigate('/dang-nhap', { replace: true })}>
          Đăng nhập
        </button>
      </CardPage>
    );

  return (
    <CardPage closeTo="/dang-nhap">
      <Badge icon="lock" />
      <h1>Tạo mật khẩu mới</h1>
      <p className="lead-t">Chọn một mật khẩu mạnh để bảo vệ tài khoản của bạn.</p>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (valid) setDone(true);
        }}
        noValidate
      >
        <PasswordField id="rp-new" label="Mật khẩu mới" value={pw} onChange={setPw} placeholder="Nhập mật khẩu mới" />
        <PasswordField id="rp-confirm" label="Xác nhận mật khẩu" value={confirm} onChange={setConfirm} placeholder="Nhập lại mật khẩu" error={mismatch ? 'Mật khẩu xác nhận không khớp' : undefined} />
        <PasswordRules value={pw} />
        <button className="btn primary block" type="submit" disabled={!valid}>
          Cập nhật mật khẩu
        </button>
      </form>
    </CardPage>
  );
}
