import { useState } from 'react';
import { Link } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Box } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { TODAY } from '../../data/funnel';
import { ALWAYS, KINDS, PREFS, prefOf, REMINDER, runReminders, type PrefKind } from '../../data/prefs';
import { findRoom } from '../../data/rooms';
import { longDate, SESSIONS } from '../../data/schedule';
import { saveAll } from '../../data/store';
import { STUDENTS } from '../../data/students';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import { MODULE } from '../S6-05-hoc-lieu/MaterialsPage';
import '../S6-07-thong-bao-lop/notice.css';
import './reminder.css';

const HINT: Record<PrefKind, string> = {
  'Nhắc lịch học': 'Gửi trước mỗi buổi học',
  'Bài tập mới và hạn nộp': 'Khi giảng viên giao bài',
  'Điểm được công bố': 'Khi có điểm và nhận xét',
  'Thay đổi lịch học': 'Buổi học bị dời hoặc huỷ · luôn bật',
  'Kết quả đơn xin phép': 'Khi đơn được duyệt hoặc từ chối',
  'Học phí sắp đến hạn': 'Trước hạn đóng 7 ngày',
};

// S6-08. Quản lý đào tạo đặt mốc nhắc trước buổi học (mặc định 12 giờ) và xem mẫu email nhắc lịch.
// Không gửi nhắc cho buổi đã huỷ; buổi bị dời thì nhắc theo giờ mới; bỏ qua học viên đã tắt nhắc lịch.
export function ReminderSettingsPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [hours, setHours] = useState(String(REMINDER.hours));
  const [error, setError] = useState('');
  const role = user?.active ?? -1;
  if (role !== 5 && role !== 6) return <ErrorPage code="403" />;
  const h = Number(hours);
  const sample = SESSIONS.filter((s) => s.cls === 'WEB-K15' && !s.cancelled && s.date >= TODAY).sort((a, b) => (a.date + a.start).localeCompare(b.date + b.start))[0];
  const st = STUDENTS.find((s) => s.code === 'HV260101');
  const remindAt = sample && Number.isFinite(h) ? new Date(Date.parse(`${sample.date}T${sample.start}:00`) - h * 3600000) : null;

  return (
    <AppLayout crumb={[MODULE, 'Nhắc lịch học']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Nhắc lịch học</h2>
          <p>Hệ thống tự gửi nhắc trước mỗi buổi học</p>
        </div>
      </div>
      <div className="rm-grid">
        <section className="panel sp-card">
          <h3>Mốc gửi nhắc</h3>
          <div className="field">
            <label htmlFor="rm-h">Gửi trước buổi học</label>
            <div className="rm-hours">
              <div className="inp">
                <input id="rm-h" type="number" min={1} max={72} value={hours} onChange={(e) => setHours(e.target.value)} />
              </div>
              <span>giờ</span>
            </div>
            {remindAt && sample && (
              <p className="hint">
                Mặc định 12 giờ. Buổi {sample.start} {longDate(sample.date).toLowerCase()} sẽ nhắc lúc {String(remindAt.getHours()).padStart(2, '0')}:{String(remindAt.getMinutes()).padStart(2, '0')}
                {remindAt.toDateString() === new Date(sample.date + 'T00:00:00').toDateString() ? ' cùng ngày' : ' hôm trước'}.
              </p>
            )}
          </div>
          <h3 style={{ marginTop: 8 }}>Quy tắc</h3>
          <ul className="rm-rules">
            {['Gửi qua thông báo trong ứng dụng và email', 'Bỏ qua học viên đã tắt nhắc lịch', 'Không gửi cho buổi đã bị huỷ', 'Buổi bị dời thì nhắc theo giờ mới'].map((r) => (
              <li key={r}>
                <Icon name="check" size={16} />
                {r}
              </li>
            ))}
          </ul>
          {error && <p className="err">{error}</p>}
          <div className="acts" style={{ justifyContent: 'flex-start' }}>
            <button
              type="button"
              className="btn primary"
              onClick={() => {
                if (!(Number.isInteger(h) && h >= 1 && h <= 72)) return setError('Mốc nhắc từ 1 đến 72 giờ');
                REMINDER.hours = h;
                saveAll();
                setError('');
                toast('Đã lưu mốc nhắc', `Gửi trước ${h} giờ`);
              }}
            >
              Lưu mốc nhắc
            </button>
            <button
              type="button"
              className="btn"
              onClick={() => {
                const n = runReminders();
                saveAll();
                toast(n ? `Đã gửi nhắc tới ${n} học viên` : 'Không có buổi nào cần nhắc trong khoảng này');
              }}
            >
              Chạy nhắc lịch ngay
            </button>
          </div>
        </section>
        {sample && st && (
          <section className="panel sp-card">
            <h3>Mẫu email nhắc lịch</h3>
            <p className="hint">
              Từ: TMS &lt;thongbao@tms.vn&gt; · Tiêu đề: Nhắc lịch: {sample.cls} học lúc {sample.start} {longDate(sample.date).toLowerCase()}
            </p>
            <div className="cn-mail rm-mail">
              <div className="cn-mail-head">TMS.</div>
              <div className="cn-mail-body">
                <p>Chào {st.name},</p>
                <b>Bạn có buổi học {longDate(sample.date).toLowerCase()}</b>
                <dl className="sp-facts">
                  <div>
                    <dt>Thời gian</dt>
                    <dd>
                      {longDate(sample.date)} · {sample.start} – {sample.end}
                    </dd>
                  </div>
                  <div>
                    <dt>Phòng</dt>
                    <dd>{sample.room}</dd>
                  </div>
                  <div className="wide">
                    <dt>Lớp và bài học</dt>
                    <dd>
                      {sample.cls} · {sample.subject} Bài {sample.lesson}: {sample.topic}
                    </dd>
                  </div>
                </dl>
                <span className="hint">
                  Giảng viên {sample.teacher} · {findRoom(sample.room)?.where}
                </span>
                <div className="rm-mail-acts">
                  <span className="btn primary">Xem lịch học</span>
                  <span className="link">Xin phép vắng buổi này</span>
                </div>
              </div>
              <small>Bạn nhận email này vì đang học lớp {sample.cls}. Không muốn nhận nhắc lịch nữa? Tắt trong Cài đặt cá nhân của TMS.</small>
            </div>
          </section>
        )}
      </div>
    </AppLayout>
  );
}

// S6-08. Cài đặt cá nhân: mỗi loại thông báo bật, tắt riêng cho trong ứng dụng và email. Học viên tắt được nhắc lịch học;
// thông báo buổi học bị dời hoặc huỷ luôn được gửi.
export function PersonalSettingsPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [, setTick] = useState(0);
  if (!user) return null;
  const set = (k: PrefKind, field: 'app' | 'email', v: boolean) => {
    const cur = prefOf(user.email, k);
    PREFS[user.email] = { ...PREFS[user.email], [k]: { ...cur, [field]: v } };
    saveAll();
    setTick((t) => t + 1);
    toast(v ? 'Đã bật' : 'Đã tắt', `${k} · ${field === 'app' ? 'trong ứng dụng' : 'email'}`);
  };
  const remindOff = !prefOf(user.email, 'Nhắc lịch học').app && !prefOf(user.email, 'Nhắc lịch học').email;

  return (
    <AppLayout crumb={['Cài đặt cá nhân']}>
      <div className="h">
        <div>
          <h2>Cài đặt cá nhân</h2>
          <p>Chọn những thông báo bạn muốn nhận</p>
        </div>
      </div>
      <nav className="tabs">
        <Link to="/ho-so">Hồ sơ</Link>
        <Link to="/cai-dat" className="on">
          Thông báo
        </Link>
        <Link to="/doi-mat-khau">Mật khẩu</Link>
      </nav>
      <div className="rm-grid">
        <div className="panel">
          <table>
            <thead>
              <tr>
                <th>Loại thông báo</th>
                <th>Trong ứng dụng</th>
                <th>Email</th>
              </tr>
            </thead>
            <tbody>
              {KINDS.map((k) => {
                const p = prefOf(user.email, k);
                const locked = ALWAYS.includes(k);
                return (
                  <tr key={k}>
                    <td>
                      <b className="strong">{k}</b>
                      <small className="lv-sub">{k === 'Nhắc lịch học' ? `Gửi ${REMINDER.hours} giờ trước mỗi buổi học` : HINT[k]}</small>
                    </td>
                    {(['app', 'email'] as const).map((f) => (
                      <td key={f}>
                        <label className="switch">
                          <input type="checkbox" role="switch" checked={p[f]} disabled={locked} aria-label={`${k} ${f === 'app' ? 'trong ứng dụng' : 'email'}`} onChange={(e) => set(k, f, e.target.checked)} />
                          <span />
                        </label>
                      </td>
                    ))}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        {remindOff && (
          <Box icon="bell" tone="info" title="Bạn đã tắt nhắc lịch học">
            Bạn sẽ không nhận nhắc trước buổi học nữa. Thông báo khi buổi học bị dời hoặc huỷ vẫn được gửi.
          </Box>
        )}
      </div>
    </AppLayout>
  );
}
