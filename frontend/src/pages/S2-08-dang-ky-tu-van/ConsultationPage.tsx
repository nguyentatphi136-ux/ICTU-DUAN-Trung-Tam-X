import { useTitle } from '../../components/ui';
import { useState, type FormEvent } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../../data/auth';
import { Badge } from '../../components/CardPage';
import { Icon, type IconName } from '../../components/Icon';
import { ThemeToggle } from '../../components/ThemeToggle';
import { Box } from '../../components/ui';
import { apiEnabled, useLoad } from '../../data/api';
import { LEADS } from '../../data/leads';
import { loadPublicPrograms, submitConsultation } from '../../data/s2api';
import { PROGRAMS } from '../../data/training';
import { EMAIL_RE, isPhone, PHONE_MSG } from '../../data/validate';
import '../S1-01-dang-nhap/login.css';
import './consult.css';

const PROMISES: [IconName, string][] = [
  ['phone', 'Gọi lại trong 24 giờ làm việc'],
  ['check', 'Tư vấn miễn phí, không ràng buộc'],
  ['user', 'Không cần tạo tài khoản'],
];
type Form = { name: string; phone: string; email: string; program: string; message: string };

// S2-08. Biểu mẫu công khai, không cần đăng nhập. Gửi thành công tạo một lead trạng thái Mới, chưa phân công.
// Ô "Tôi không phải người máy" là chỗ đặt CAPTCHA thật. Có backend: lấy chương trình từ /api/public/programs,
// gửi POST /api/public/consultations (máy chủ giới hạn 5 lần mỗi 15 phút cho một địa chỉ IP).
export function ConsultationPage() {
  useTitle('Đăng ký tư vấn');
  const { user } = useAuth();
  if (user) {
    return <Navigate to="/" replace />;
  }
  const [f, setF] = useState<Form>({ name: '', phone: '', email: '', program: '', message: '' });
  const [human, setHuman] = useState(false);
  const [errors, setErrors] = useState<Partial<Record<keyof Form | 'human', string>>>({});
  const [sent, setSent] = useState(false);
  const [sending, setSending] = useState(false);
  const [serverError, setServerError] = useState('');
  const [programs, setPrograms] = useState(() => PROGRAMS.filter((p) => p.active).map((p) => ({ code: p.code, name: p.name })));
  const load = useLoad(async () => setPrograms(await loadPublicPrograms()));
  const set = (k: keyof Form) => (e: { target: { value: string } }) => {
    setF((x) => ({ ...x, [k]: e.target.value }));
    setErrors((x) => ({ ...x, [k]: '' }));
  };

  async function submit(e: FormEvent) {
    e.preventDefault();
    const err: typeof errors = {};
    if (!f.name.trim()) err.name = 'Vui lòng nhập họ và tên';
    if (!isPhone(f.phone)) err.phone = PHONE_MSG;
    if (f.email.trim() && !EMAIL_RE.test(f.email.trim())) err.email = 'Email không đúng định dạng';
    if (!f.program) err.program = 'Chọn chương trình bạn quan tâm';
    if (!human) err.human = 'Vui lòng xác nhận bạn không phải người máy';
    setErrors(err);
    if (Object.keys(err).length) return;
    if (apiEnabled) {
      setSending(true);
      setServerError('');
      try {
        const programCode = programs.find((p) => p.name === f.program)?.code ?? '';
        await submitConsultation({ name: f.name.trim().toUpperCase(), phone: f.phone.trim(), email: f.email.trim(), programCode, message: f.message.trim() });
        setSent(true);
      } catch (ex) {
        setServerError((ex as Error).message);
      } finally {
        setSending(false);
      }
      return;
    }
    LEADS.unshift({
      id: Math.max(...LEADS.map((l) => l.id)) + 1,
      name: f.name.trim().toUpperCase(),
      phone: f.phone.trim(),
      email: f.email.trim() || undefined,
      source: 'Biểu mẫu web',
      program: f.program,
      status: 'Mới',
      owner: '',
      createdAt: new Date().toISOString().slice(0, 10),
      note: f.message.trim() || undefined,
    });
    setSent(true);
  }

  if (sent) {
    const first = f.name.trim().split(/\s+/).slice(-2).join(' ');
    return (
      <div className="page">
        <div className="card ct-thanks">
          <ThemeToggle className="theme-in" />
          <Badge icon="check" />
          <div className="eyebrow">Đã nhận đăng ký</div>
          <h1>Cảm ơn bạn, {first}!</h1>
          <p className="lead-t">Chúng tôi đã ghi nhận yêu cầu tư vấn về chương trình {f.program}.</p>
          <Box icon="clock" title="Tư vấn viên sẽ gọi lại trong 24 giờ làm việc">
            Qua số {f.phone}. Nếu số này chưa đúng, bạn có thể gửi lại biểu mẫu.
          </Box>
          <Link className="btn block ct-back" to="/dang-ky-tu-van" onClick={() => setSent(false)}>
            Xem các chương trình đào tạo
          </Link>
        </div>
      </div>
    );
  }

  const field = (k: keyof Form, label: string, icon: IconName | null, ph: string, type = 'text') => (
    <div className="field">
      <label htmlFor={'ct-' + k}>{label}</label>
      <div className={'inp' + (icon ? ' lead' : '') + (errors[k] ? ' bad' : '')}>
        {icon && <Icon name={icon} />}
        <input id={'ct-' + k} type={type} placeholder={ph} value={f[k]} onChange={set(k)} />
      </div>
      {errors[k] && <p className="err">{errors[k]}</p>}
    </div>
  );

  return (
    <div className="lg-page">
      <main className="lg-card ct-card">
        <ThemeToggle className="theme-in" />
        <section className="lg-left">
          <div className="lg-logo">
            <span>
              <Icon name="cap" size={24} />
            </span>
            TMS.
          </div>
          <div className="lg-hero">
            <h1 className="ct-h">Để lại thông tin, chúng tôi sẽ gọi lại cho bạn.</h1>
            <p>Tư vấn viên sẽ giúp bạn chọn khoá học phù hợp với mục tiêu và thời gian của bạn.</p>
          </div>
          <div className="lg-features">
            {PROMISES.map(([icon, text]) => (
              <div className="lg-feat ct-feat" key={text}>
                <div className="lg-ic">
                  <Icon name={icon} />
                </div>
                <h3>{text}</h3>
              </div>
            ))}
          </div>
        </section>

        <section className="ct-right">
          <div className="lg-eyebrow">Đăng ký tư vấn</div>
          <h2>Bạn quan tâm khoá học nào?</h2>
          <form onSubmit={submit} noValidate>
            {field('name', 'Họ và tên', 'user', 'Nguyễn Văn A')}
            <div className="grid2">
              {field('phone', 'Số điện thoại', 'phone', '09xx xxx xxx', 'tel')}
              {field('email', 'Email (không bắt buộc)', 'mail', 'ban@tms.vn', 'email')}
            </div>
            <div className="field">
              <label htmlFor="ct-program">Chương trình quan tâm</label>
              <div className={'inp' + (errors.program ? ' bad' : '')}>
                <select id="ct-program" value={f.program} onChange={set('program')} className={f.program ? undefined : 'ph'}>
                  <option value="">Chọn chương trình</option>
                  {programs.map((p) => (
                    <option key={p.code}>{p.name}</option>
                  ))}
                </select>
              </div>
              {(errors.program || load.error) && <p className="err">{errors.program || 'Chưa tải được danh sách chương trình: ' + load.error}</p>}
            </div>
            <div className="field">
              <label htmlFor="ct-message">Lời nhắn (không bắt buộc)</label>
              <div className="inp">
                <textarea id="ct-message" placeholder="Ví dụ: mình muốn học buổi tối, bắt đầu từ tháng 11" value={f.message} onChange={set('message')} />
              </div>
            </div>
            <label className={'ct-captcha' + (errors.human ? ' bad' : '')}>
              <input
                type="checkbox"
                checked={human}
                onChange={(e) => {
                  setHuman(e.target.checked);
                  setErrors((x) => ({ ...x, human: '' }));
                }}
              />
              Tôi không phải người máy
              <Icon name="shield" size={20} />
            </label>
            {errors.human && <p className="err">{errors.human}</p>}
            {serverError && <p className="err">{serverError}</p>}
            <button type="submit" className="btn primary block ct-submit" disabled={sending}>
              {sending ? 'Đang gửi…' : 'Gửi đăng ký'}
            </button>
            <p className="hint ct-note">Thông tin của bạn chỉ dùng để trung tâm liên hệ tư vấn.</p>
            <p className="hint ct-note">
              Đã có tài khoản? <Link to="/dang-nhap">Đăng nhập</Link>
            </p>
          </form>
        </section>
      </main>
    </div>
  );
}
