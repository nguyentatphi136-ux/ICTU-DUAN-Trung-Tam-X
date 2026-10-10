import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { Icon } from '../../components/Icon';
import { ThemeToggle } from '../../components/ThemeToggle';
import { useToast } from '../../components/Toast';
import { useAuth } from '../../data/auth';
import { ROLES } from '../../data/permissions';
import './session.css';

// Phiên 30 phút không thao tác. Cảnh báo khi còn 2 phút; mọi thao tác đều tự gia hạn.
// Khi tích hợp: thời hạn lấy từ access token, gia hạn bằng refresh token, máy chủ mới là nơi quyết định hết hạn.
const IDLE_WARN_MS = 28 * 60 * 1000;
const WARN_SECONDS = 2 * 60;
const DRAFT_KEY = 'tms.draft.phien';

type Draft = { name: string; phone: string; email: string; course: string; note: string; savedAt?: number };
const EMPTY: Draft = { name: '', phone: '', email: '', course: '', note: '' };
const FIELDS: [keyof Draft, string, string][] = [
  ['name', 'Họ và tên', 'NGUYEN THUY LINH'],
  ['phone', 'Số điện thoại', '0912 345 678'],
  ['email', 'Email', 'S25200212124@tms.vn'],
  ['course', 'Khóa học quan tâm', 'Lập trình web cơ bản'],
];
const mmss = (s: number) => String(Math.floor(s / 60)).padStart(2, '0') + ':' + String(s % 60).padStart(2, '0');
const hhmm = (t: number) => new Date(t).toTimeString().slice(0, 5);

// S1-02 (và cảnh báo phiên của S1-01). Một form đang nhập dở để minh hoạ: cảnh báo sắp hết hạn, menu đăng xuất,
// hết hạn thì lưu nháp và về trang đăng nhập, đăng nhập lại thì mở đúng form và điền lại nội dung.
// ?canh-bao=1 mở ngay hộp thoại cảnh báo để kiểm thử.
export function SessionDemoPage() {
  const { user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const toast = useToast();
  const [params] = useSearchParams();
  const [form, setForm] = useState<Draft>(EMPTY);
  const [warnLeft, setWarnLeft] = useState(params.get('canh-bao') ? WARN_SECONDS - 1 : 0);
  const [menu, setMenu] = useState(false);
  const idle = useRef<number | undefined>(undefined);
  const formRef = useRef(form);
  formRef.current = form;

  const logout = useCallback(
    (expired: boolean) => {
      const email = user?.email;
      if (expired) sessionStorage.setItem(DRAFT_KEY, JSON.stringify({ ...formRef.current, savedAt: Date.now() }));
      navigate('/dang-nhap', { replace: true, state: expired ? { expired: true, email, from: location.pathname } : { loggedOut: true } });
    },
    [user, navigate, location.pathname],
  );

  // Khôi phục bản nháp sau khi đăng nhập lại.
  useEffect(() => {
    if (!(location.state as { restored?: boolean } | null)?.restored) return;
    const raw = sessionStorage.getItem(DRAFT_KEY);
    if (!raw) return;
    const draft: Draft = JSON.parse(raw);
    sessionStorage.removeItem(DRAFT_KEY);
    setForm({ ...EMPTY, ...draft });
    toast('Đã khôi phục nội dung đang nhập dở', `Bản nháp được lưu lúc ${hhmm(draft.savedAt ?? Date.now())}, trước khi phiên hết hạn.`);
  }, [location.state, toast]);

  // Đếm thời gian không thao tác; đang hiện cảnh báo thì chỉ nút trong hộp thoại mới gia hạn.
  useEffect(() => {
    if (warnLeft) return;
    const reset = () => {
      window.clearTimeout(idle.current);
      idle.current = window.setTimeout(() => setWarnLeft(WARN_SECONDS), IDLE_WARN_MS);
    };
    reset();
    const events = ['mousemove', 'keydown', 'click', 'scroll'];
    events.forEach((e) => window.addEventListener(e, reset));
    return () => {
      window.clearTimeout(idle.current);
      events.forEach((e) => window.removeEventListener(e, reset));
    };
  }, [warnLeft]);

  useEffect(() => {
    if (!warnLeft) return;
    const t = window.setTimeout(() => (warnLeft <= 1 ? logout(true) : setWarnLeft(warnLeft - 1)), 1000);
    return () => window.clearTimeout(t);
  }, [warnLeft, logout]);

  if (!user) return <Navigate to="/dang-nhap" replace state={{ from: location.pathname }} />;

  const set = (k: keyof Draft) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [k]: e.target.value }));

  return (
    <div className="ss-page">
      <header className="ss-top">
        <Link to="/" className="logo">
          <span />
          TMS.
        </Link>
        <nav>
          <a href="#">Lead</a>
          <a href="#" className="on">
            Học viên
          </a>
          <a href="#">Lớp học</a>
          <a href="#">Báo cáo</a>
        </nav>
        <ThemeToggle />
        <div className="um">
          <button type="button" className="um-btn" aria-expanded={menu} onClick={() => setMenu((m) => !m)}>
            <span className="who">
              <b>{user.name}</b>
              <span>{ROLES[user.active]}</span>
            </span>
            <span className="ava" />
          </button>
          {menu && (
            <div className="um-menu ss-menu" role="menu">
              <div className="um-links">
                <Link to="/ho-so">Hồ sơ cá nhân</Link>
                <Link to="/doi-mat-khau">Đổi mật khẩu</Link>
              </div>
              <div className="um-links sep-top">
                <button type="button" className="danger" onClick={() => logout(false)}>
                  Đăng xuất
                  <small>Kết thúc phiên trên máy chủ ngay</small>
                </button>
              </div>
            </div>
          )}
        </div>
      </header>

      <main className="ss-form panel">
        <div className="ss-head">
          <span className="ss-ic">
            <Icon name="users" size={24} />
          </span>
          <h1>Thêm học viên mới</h1>
        </div>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            setForm(EMPTY);
            toast('Đã lưu học viên', form.name || undefined);
          }}
        >
          <div className="grid2">
            {FIELDS.map(([k, label, ph]) => (
              <div className="field" key={k}>
                <label htmlFor={'ss-' + k}>{label}</label>
                <div className="inp">
                  <input id={'ss-' + k} value={form[k] as string} placeholder={ph} onChange={set(k)} />
                </div>
              </div>
            ))}
          </div>
          <div className="field">
            <label htmlFor="ss-note">Ghi chú tư vấn</label>
            <div className="inp">
              <textarea id="ss-note" rows={5} value={form.note} placeholder="Học viên muốn học buổi tối, đã có nền tảng HTML…" onChange={set('note')} />
            </div>
          </div>
          <div className="actions">
            <button type="button" className="btn ss-btn" onClick={() => setForm(EMPTY)}>
              Hủy
            </button>
            <button type="submit" className="btn primary ss-btn wide">
              Lưu học viên
            </button>
          </div>
        </form>
      </main>

      {warnLeft > 0 && (
        <div className="overlay">
          <div className="modal ss-dialog" role="alertdialog" aria-modal="true" aria-labelledby="ss-warn">
            <h3 id="ss-warn">Phiên sắp hết hạn</h3>
            <p>
              Bạn chưa thao tác trong 28 phút. Phiên sẽ tự kết thúc sau {mmss(warnLeft)}. Nội dung đang nhập vẫn được giữ nguyên.
            </p>
            <div className="acts">
              <button type="button" className="btn ss-btn" onClick={() => logout(false)}>
                Đăng xuất
              </button>
              <button type="button" className="btn primary ss-btn grow" onClick={() => setWarnLeft(0)}>
                Tiếp tục làm việc
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
