import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Icon } from '../../components/Icon';
import { DEMO_ACCOUNTS, DEMO_PASSWORD, LOCK_SECONDS, MAX_ATTEMPTS, login, type Account } from './mockAuth';
import './login.css';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const mmss = (s: number) => String(Math.floor(s / 60)).padStart(2, '0') + ':' + String(s % 60).padStart(2, '0');

const FEATURES = [
  ['funnel', 'Từ lead đến tốt nghiệp', 'Cả hành trình của học viên nằm trên một nguồn dữ liệu duy nhất.'],
  ['users', 'Mọi vai trò, cùng một sự thật', 'Số liệu cập nhật theo thời gian thực, thay cho Sheets, Zalo và sổ tay.'],
  ['bell', 'Chủ động cảnh báo rủi ro', 'Hệ thống báo sớm khi có dấu hiệu bất thường, không chờ người phát hiện.'],
] as const;

// S1-01. Đăng nhập bằng email và mật khẩu: validate, báo sai thông tin, khoá tạm, chuyển trang theo vai trò.
export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});
  const [fails, setFails] = useState(0);
  const [lockedUntil, setLockedUntil] = useState(0);
  const [now, setNow] = useState(Date.now());
  const [wrong, setWrong] = useState(false);
  const [loading, setLoading] = useState(false);
  const [account, setAccount] = useState<Account | null>(null);

  const lockLeft = Math.max(0, Math.ceil((lockedUntil - now) / 1000));
  const locked = lockLeft > 0;

  // Đếm ngược khi đang bị khoá tạm; hết thời gian thì mở lại form và xoá bộ đếm.
  useEffect(() => {
    if (!lockedUntil) return;
    const t = window.setInterval(() => {
      const current = Date.now();
      setNow(current);
      if (current >= lockedUntil) {
        setLockedUntil(0);
        setFails(0);
      }
    }, 1000);
    return () => window.clearInterval(t);
  }, [lockedUntil]);

  function validate() {
    const next: typeof errors = {};
    const v = email.trim();
    if (!v) next.email = 'Vui lòng nhập email';
    else if (!EMAIL_RE.test(v)) next.email = 'Email không đúng định dạng';
    if (!password) next.password = 'Vui lòng nhập mật khẩu';
    setErrors(next);
    return !next.email && !next.password;
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (locked || loading) return;
    if (!validate()) {
      setWrong(false);
      return;
    }
    setLoading(true);
    const result = await login(email, password);
    setLoading(false);
    if (result) {
      setFails(0);
      setWrong(false);
      setAccount(result); // khi có các trang thật: navigate(result.home)
      return;
    }
    const count = fails + 1;
    setFails(count);
    setPassword('');
    if (count >= MAX_ATTEMPTS) {
      setWrong(false);
      setNow(Date.now());
      setLockedUntil(Date.now() + LOCK_SECONDS * 1000);
    } else {
      setWrong(true);
    }
  }

  function logout() {
    setAccount(null);
    setEmail('');
    setPassword('');
  }

  return (
    <div className="lg-page">
      <main className="lg-card">
        <section className="lg-left">
          <div className="lg-logo">
            <span>
              <Icon name="cap" size={24} />
            </span>
            TMS.
          </div>
          <div className="lg-hero">
            <h1>Một nguồn dữ liệu cho trọn vòng đời học viên.</h1>
            <p>TMS là hệ thống quản lý đào tạo nội bộ trên nền web, theo dõi học viên từ lúc còn là lead đến ngày tốt nghiệp.</p>
          </div>
          <div className="lg-features">
            {FEATURES.map(([icon, title, text]) => (
              <div className="lg-feat" key={title}>
                <div className="lg-ic">
                  <Icon name={icon} size={22} />
                </div>
                <div>
                  <h3>{title}</h3>
                  <p>{text}</p>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="lg-right">
          {account ? (
            <div className="lg-success">
              <div className="lg-eyebrow">Đăng nhập thành công</div>
              <h2>Xin chào, {account.role}</h2>
              <div className="lg-dest">
                Vai trò: {account.role}
                <br />
                Chuyển đến: {account.homeName} <code>{account.home}</code>
              </div>
              <a
                href="#"
                onClick={(e) => {
                  e.preventDefault();
                  logout();
                }}
              >
                Đăng xuất và quay lại màn hình đăng nhập
              </a>
            </div>
          ) : (
            <div>
              <div className="lg-eyebrow">Chào mừng quay trở lại</div>
              <h2>Đăng nhập</h2>
              <p className="lg-lead">Nhập thông tin tài khoản của bạn để truy cập hệ thống quản trị đào tạo.</p>

              {locked && (
                <div className="lg-alert lg-lock" role="alert">
                  <strong>Tài khoản tạm khóa</strong>
                  Bạn đã nhập sai {MAX_ATTEMPTS} lần. Vui lòng thử lại sau {mmss(lockLeft)}.
                </div>
              )}
              {!locked && wrong && (
                <div className="lg-alert lg-error" role="alert">
                  <strong>Email hoặc mật khẩu không đúng</strong>
                  Bạn còn {MAX_ATTEMPTS - fails} lần thử trước khi tài khoản bị khóa tạm.
                </div>
              )}

              <form onSubmit={onSubmit} noValidate>
                <label className="lg-f" htmlFor="email">
                  Email
                </label>
                <div className={'lg-field' + (errors.email ? ' lg-invalid' : '')}>
                  <input
                    id="email"
                    type="email"
                    placeholder="name@company.com"
                    autoComplete="username"
                    value={email}
                    disabled={locked}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      setErrors((x) => ({ ...x, email: undefined }));
                    }}
                  />
                  {errors.email && <p className="lg-err">{errors.email}</p>}
                </div>

                <label className="lg-f" htmlFor="pw">
                  Mật khẩu
                </label>
                <div className={'lg-field' + (errors.password ? ' lg-invalid' : '')}>
                  <input
                    id="pw"
                    type={showPassword ? 'text' : 'password'}
                    placeholder="Nhập mật khẩu"
                    autoComplete="current-password"
                    value={password}
                    disabled={locked}
                    onChange={(e) => {
                      setPassword(e.target.value);
                      setErrors((x) => ({ ...x, password: undefined }));
                    }}
                  />
                  <button
                    type="button"
                    className="lg-eye"
                    aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                    onClick={() => setShowPassword((s) => !s)}
                  >
                    <Icon name={showPassword ? 'eyeoff' : 'eye'} size={20} />
                  </button>
                  {errors.password && <p className="lg-err">{errors.password}</p>}
                </div>

                <div className="lg-row">
                  <label>
                    <input type="checkbox" defaultChecked /> Ghi nhớ đăng nhập
                  </label>
                  <Link to="/quen-mat-khau">Quên mật khẩu?</Link>
                </div>

                <button className="lg-btn" type="submit" disabled={locked || loading}>
                  {loading ? 'Đang đăng nhập…' : 'Đăng nhập'}
                </button>
              </form>

              <div className="lg-or">Hoặc tiếp tục với</div>
              <div className="lg-social">
                <button type="button">
                  <span className="lg-dot lg-g">G</span>Google
                </button>
                <button type="button">
                  <span className="lg-dot lg-fb">f</span>Facebook
                </button>
              </div>

              <div className="lg-secure">
                <Icon name="lock" size={16} />
                Dữ liệu được bảo mật tối đa theo tiêu chuẩn ISO 27001
              </div>

              <div className="lg-foot">
                <span>© 2026 TMS Platform.</span>
                <a href="#">Trợ giúp &amp; hỗ trợ</a>
              </div>

              <details className="lg-demo">
                <summary>Tài khoản mẫu để thử</summary>
                <table>
                  <tbody>
                    {DEMO_ACCOUNTS.map((a) => (
                      <tr key={a.email}>
                        <td>
                          <code>{a.email}</code>
                        </td>
                        <td>{a.role}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <p>
                  Mật khẩu chung: <code>{DEMO_PASSWORD}</code>. Nhập sai {MAX_ATTEMPTS} lần liên tiếp sẽ bị khóa tạm 15 phút.
                </p>
              </details>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
