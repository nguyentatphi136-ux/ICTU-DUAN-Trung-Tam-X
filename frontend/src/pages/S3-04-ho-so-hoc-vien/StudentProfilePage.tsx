import { useState, type CSSProperties, type ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Drawer, Pill } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { classSize, findClass, totalSessions } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { viDate } from '../../data/leads';
import { saveAll } from '../../data/store';
import { findStudent, studentTone, type Student } from '../../data/students';
import { isPhone, PHONE_MSG } from '../../data/validate';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import { StatusDialog } from '../S4-06-tinh-trang-hoc/StatusDialog';
import { AbsenceFlag } from '../S5-02-nguong-vang/AbsenceFlag';
import { MODULE } from './StudentListPage';
import './student.css';

/** Hai chữ cái của tên gọi, ví dụ NGUYEN MINH ANH thành MA. */
export const initials = (name: string) =>
  name
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((w) => w[0])
    .join('');

const resultTone = (r: string) => (r.startsWith('Hoàn thành') ? 'ok' : r.startsWith('Bảo lưu') ? 'wait' : r.startsWith('Thôi') || r.startsWith('Không') ? 'off' : undefined);

type Props = {
  /** Nút thêm ở đầu trang, ví dụ Bảo lưu, chuyển lớp, thôi học (S4-06). */
  actions?: (s: Student, refresh: () => void) => ReactNode;
};

// S3-04. Hồ sơ học viên: thông tin cá nhân, trình độ đầu vào, ghi chú, lớp đang học và lịch sử các lớp đã học.
// Nút Sửa hồ sơ mở form sửa ba khối bên trái. Học viên chỉ xem được hồ sơ của mình.
export function StudentProfilePage({ actions }: Props) {
  const { code = '' } = useParams();
  const { user } = useAuth();
  const toast = useToast();
  const [, setTick] = useState(0);
  const [editing, setEditing] = useState(false);
  const [changing, setChanging] = useState(false);
  const s = findStudent(code);
  const role = user?.active ?? -1;
  if (!s) return <ErrorPage code="404" />;
  if (role === 0 && s.login !== user!.email) return <ErrorPage code="403" />;
  const canEdit = role === 3 || role === 5 || role === 6;
  const k = findClass(s.cls);
  const refresh = () => setTick((t) => t + 1);

  return (
    <AppLayout crumb={[MODULE, s.code]} module={MODULE}>
      {role !== 0 && (
        <Link to="/hoc-vien" className="back">
          <Icon name="back" size={16} />
          Danh sách học viên
        </Link>
      )}
      <div className="h sp-head">
        <span className="sp-ava">{initials(s.name)}</span>
        <div>
          <h2>
            {s.name} <Pill tone={studentTone(s.status)}>{s.status}</Pill>
          </h2>
          <p>
            {s.code} · {s.login}
          </p>
        </div>
        {actions?.(s, refresh)}
        {(role === 5 || role === 6) && s.cls && (
          <button type="button" className="btn" onClick={() => setChanging(true)}>
            Bảo lưu, chuyển lớp, thôi học
          </button>
        )}
        {canEdit && (
          <button type="button" className="btn" style={{ width: 140 }} onClick={() => setEditing(true)}>
            Sửa hồ sơ
          </button>
        )}
      </div>

      {/* S5-02 */}
      {k && role !== 0 && <AbsenceFlag code={s.code} cls={k.code} program={k.program} total={totalSessions(k)} canNote={canEdit} onNote={(text) => (s.notes.unshift({ at: TODAY, by: user!.name, text }), saveAll(), refresh())} />}
      <div className="sp-grid">
        <div className="sp-col">
          <section className="panel sp-card">
            <h3>Thông tin cá nhân</h3>
            <dl className="sp-facts">
              <Fact k="Số điện thoại" v={s.phone} />
              <Fact k="Ngày sinh" v={s.birth && viDate(s.birth)} />
              <Fact k="Email liên hệ" v={s.email} />
              <Fact k="Ngày nhập học" v={viDate(s.enrolledAt)} />
              <Fact k="Địa chỉ" v={s.address} wide />
              <Fact k="Tư vấn viên" v={s.counselor} />
              <Fact k="Nguồn" v={s.source} />
            </dl>
          </section>
          <section className="panel sp-card">
            <h3>Trình độ đầu vào</h3>
            <dl className="sp-facts">
              <Fact k="Kết quả kiểm tra đầu vào" v={s.entryScore} />
              <Fact k="Xếp loại" v={s.level} />
              <Fact k="Nền tảng" v={s.background} wide />
            </dl>
            <h3 className="sp-sub">Ghi chú</h3>
            {s.notes.length ? (
              <ul className="sp-notes">
                {s.notes.map((n, i) => (
                  <li key={i}>
                    <small>
                      {viDate(n.at)} · {n.by}
                    </small>
                    <p>{n.text}</p>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="hint">Chưa có ghi chú.</p>
            )}
          </section>
        </div>

        <div className="sp-col">
          <section className="panel sp-card">
            <h3>Lớp đang học</h3>
            {k ? (
              <Link to={'/lop-hoc/' + k.code} className="sp-class">
                <div>
                  <b>{k.code}</b>
                  <Pill tone="ok">{s.status === 'Bảo lưu' ? 'Bảo lưu' : k.status}</Pill>
                </div>
                <span>
                  {k.program} · {k.slot}
                  {k.room ? ' · Phòng ' + k.room : ''} · {classSize(k.code)}/{k.max} học viên
                </span>
                <div className="sp-progress">
                  <span>Tiến độ buổi học</span>
                  <b>
                    {k.done}/{totalSessions(k)} buổi
                  </b>
                  <i style={{ '--p': `${(100 * k.done) / Math.max(1, totalSessions(k))}%` } as CSSProperties} />
                </div>
              </Link>
            ) : (
              <p className="hint">Chưa xếp lớp. Quản lý đào tạo ghi danh học viên vào lớp ở màn hình Lớp học.</p>
            )}
          </section>
          <h3 className="section-t sp-hist-t">Lịch sử các lớp đã học</h3>
          <div className="panel">
            <div className="scroll">
              <table>
                <thead>
                  <tr>
                    <th>Lớp</th>
                    <th>Chương trình</th>
                    <th>Thời gian</th>
                    <th>Kết quả</th>
                  </tr>
                </thead>
                <tbody>
                  {s.history.map((h) => (
                    <tr key={h.cls}>
                      <td className="strong">{h.cls}</td>
                      <td>{h.program}</td>
                      <td>{h.period}</td>
                      <td>
                        <Pill tone={resultTone(h.result)}>{h.result}</Pill>
                      </td>
                    </tr>
                  ))}
                  {!s.history.length && (
                    <tr>
                      <td colSpan={4} className="empty">
                        Chưa có lớp nào đã kết thúc.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      {s.changes?.length ? (
        <section className="panel sp-card" style={{ marginTop: 16 }}>
          <h3>Lịch sử thay đổi tình trạng</h3>
          <ul className="st-log">
            {s.changes.map((c, i) => (
              <li key={i}>
                <b>
                  {c.type === 'Chuyển lớp' ? `Chuyển lớp ${c.from} → ${c.to}` : `${c.type}${c.from ? ' khỏi lớp ' + c.from : ''}`} · hiệu lực {viDate(c.at)}
                </b>
                <small>
                  Lý do: {c.reason} · ghi nhận {viDate(c.recorded)} bởi {c.by}
                </small>
              </li>
            ))}
          </ul>
        </section>
      ) : null}
      {changing && (
        <StatusDialog
          s={s}
          me={user!.name}
          onClose={() => setChanging(false)}
          onDone={(msg) => {
            setChanging(false);
            refresh();
            toast(msg);
          }}
        />
      )}
      {editing && (
        <EditDrawer
          s={s}
          me={user!.name}
          onClose={() => setEditing(false)}
          onSave={() => {
            saveAll();
            setEditing(false);
            refresh();
            toast('Đã lưu hồ sơ', s.code + ' · ' + s.name);
          }}
        />
      )}
    </AppLayout>
  );
}

function Fact({ k, v, wide }: { k: string; v?: string; wide?: boolean }) {
  return (
    <div className={wide ? 'wide' : undefined}>
      <dt>{k}</dt>
      <dd>{v || '–'}</dd>
    </div>
  );
}

type EditProps = { s: Student; me: string; onClose: () => void; onSave: () => void };

function EditDrawer({ s, me, onClose, onSave }: EditProps) {
  const [f, setF] = useState({ phone: s.phone, birth: s.birth ?? '', email: s.email, address: s.address ?? '', entryScore: s.entryScore ?? '', level: s.level ?? '', background: s.background ?? '', note: '' });
  const [error, setError] = useState('');
  const set = (k: keyof typeof f) => (e: { target: { value: string } }) => setF({ ...f, [k]: e.target.value });

  function save() {
    if (!isPhone(f.phone)) return setError(PHONE_MSG);
    Object.assign(s, { phone: f.phone, birth: f.birth || undefined, email: f.email.trim(), address: f.address.trim(), entryScore: f.entryScore.trim(), level: f.level.trim(), background: f.background.trim() });
    if (f.note.trim()) s.notes.unshift({ at: TODAY, by: me, text: f.note.trim() });
    onSave();
  }

  const field = (id: keyof typeof f, label: string, type = 'text') => (
    <div className="field">
      <label htmlFor={'sp-' + id}>{label}</label>
      <div className={'inp' + (id === 'phone' && error ? ' bad' : '')}>
        <input id={'sp-' + id} type={type} value={f[id]} onChange={set(id)} />
      </div>
      {id === 'phone' && error && <p className="err">{error}</p>}
    </div>
  );

  return (
    <Drawer
      title={'Sửa hồ sơ ' + s.code}
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
            Huỷ
          </button>
          <button type="button" className="btn primary" style={{ width: 140 }} onClick={save}>
            Lưu hồ sơ
          </button>
        </>
      }
    >
      <h4 className="section-t">Thông tin cá nhân</h4>
      <div className="grid2">
        {field('phone', 'Số điện thoại', 'tel')}
        {field('birth', 'Ngày sinh', 'date')}
      </div>
      {field('email', 'Email liên hệ', 'email')}
      {field('address', 'Địa chỉ')}
      <hr className="sep" />
      <h4 className="section-t">Trình độ đầu vào</h4>
      <div className="grid2">
        {field('entryScore', 'Kết quả kiểm tra đầu vào')}
        {field('level', 'Xếp loại')}
      </div>
      <div className="field">
        <label htmlFor="sp-bg">Nền tảng</label>
        <div className="inp">
          <textarea id="sp-bg" value={f.background} onChange={set('background')} />
        </div>
      </div>
      <hr className="sep" />
      <h4 className="section-t">Thêm ghi chú</h4>
      <div className="field">
        <div className="inp">
          <textarea aria-label="Ghi chú mới" value={f.note} placeholder="Ví dụ: xin nghỉ hai buổi vì lịch thi ở trường." onChange={set('note')} />
        </div>
        <p className="hint">Ghi chú lưu kèm ngày hôm nay và tên của bạn.</p>
      </div>
    </Drawer>
  );
}
