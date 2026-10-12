import { useState } from 'react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Box, Pill } from '../../components/ui';
import { ATT_LOGS, countMarks, isToday, MARKS, markTone, SHEETS, sheetOf, SHORT, summary, type Mark } from '../../data/attendance';
import { useAuth } from '../../data/auth';
import { classesOf } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { notify } from '../../data/notifications';
import { ROLES } from '../../data/permissions';
import { addDays, dm, findSession, longDate, SESSIONS, type ClassSession } from '../../data/schedule';
import { saveAll } from '../../data/store';
import { rosterOn } from '../../data/students';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import '../S3-04-ho-so-hoc-vien/student.css';
import './attendance.css';

export const MODULE = 'Điểm danh';
const stamp = () => TODAY + 'T' + new Date().toTimeString().slice(0, 5);
const vdt = (at: string) => at.slice(8, 10) + '/' + at.slice(5, 7) + '/' + at.slice(0, 4) + ' ' + at.slice(11, 16);

/** Buổi người này được xem điểm danh: lớp mình dạy (giảng viên, trợ giảng) hoặc tất cả (Quản lý đào tạo). */
function visible(role: number, name: string) {
  if (role === 5 || role === 6) return SESSIONS;
  const mine = new Set(classesOf(name).map((k) => k.code));
  return SESSIONS.filter((s) => mine.has(s.cls));
}

// S4-07. Danh sách buổi cần điểm danh: buổi hôm nay ở trên, các buổi gần đây ở dưới.
export function AttendanceListPage() {
  const { user } = useAuth();
  const role = user?.active ?? -1;
  if (role === 0) return <Navigate to="/lich-hoc" replace />;
  const list = visible(role, user!.name).filter((s) => !s.cancelled && s.date <= TODAY && s.date >= addDays(TODAY, -14));
  const today = list.filter(isToday).sort((a, b) => a.start.localeCompare(b.start));
  const recent = list.filter((s) => !isToday(s)).sort((a, b) => (b.date + b.start).localeCompare(a.date + a.start));
  return (
    <AppLayout crumb={[MODULE]} module={MODULE}>
      <div className="h">
        <div>
          <h2>Điểm danh</h2>
          <p>
            {today.length} buổi hôm nay · {longDate(TODAY)}
          </p>
        </div>
      </div>
      <h3 className="section-t">Hôm nay</h3>
      <div className="at-today">
        {today.map((s) => (
          <SessionCard key={s.id} s={s} />
        ))}
        {!today.length && <p className="hint">Hôm nay bạn không có buổi dạy nào.</p>}
      </div>
      <h3 className="section-t" style={{ marginTop: 24 }}>
        Hai tuần gần đây
      </h3>
      <div className="panel">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Ngày</th>
                <th>Lớp · buổi</th>
                <th>Bài học</th>
                <th>Điểm danh</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {recent.map((s) => (
                <tr key={s.id}>
                  <td className="strong">{longDate(s.date)}</td>
                  <td className="strong">
                    {s.cls} · Buổi {s.no}
                  </td>
                  <td>
                    {s.subject} Bài {s.lesson}: {s.topic}
                  </td>
                  <td>{summary(sheetOf(s)) || 'Chưa điểm danh'}</td>
                  <td>
                    <Link to={'/diem-danh/' + s.id}>Xem</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </AppLayout>
  );
}

function SessionCard({ s }: { s: ClassSession }) {
  const sh = sheetOf(s);
  return (
    <Link to={'/diem-danh/' + s.id} className="panel at-card">
      <small>
        {s.start} – {s.end} · {s.room ?? 'Chưa xếp phòng'}
      </small>
      <b>
        {s.cls} · Buổi {s.no}
      </b>
      <span>
        {s.subject} Bài {s.lesson}: {s.topic}
      </span>
      {sh ? <Pill tone="ok">Đã lưu {sh.savedAt.slice(11, 16)}</Pill> : <Pill tone="info">Mở điểm danh</Pill>}
    </Link>
  );
}

// S4-07. Điểm danh một buổi: bốn trạng thái, nút đánh dấu tất cả có mặt rồi chỉnh ngoại lệ, dùng tốt ở 360px.
// Giảng viên chỉ điểm danh được buổi của hôm nay; buổi khác cần quyền Quản lý đào tạo. Sửa điểm danh đã lưu được ghi nhật ký.
export function TakeAttendancePage() {
  const { id = '' } = useParams();
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const s = findSession(id);
  const saved = s && sheetOf(s);
  const [marks, setMarks] = useState<Record<string, Mark>>(() => ({ ...(saved?.marks ?? {}) }));
  const [notes, setNotes] = useState<Record<string, string>>(() => ({ ...(saved?.notes ?? {}) }));
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const [all, setAll] = useState(false);
  if (!s) return <ErrorPage code="404" />;
  const role = user?.active ?? -1;
  const manager = role === 5 || role === 6;
  if (!manager && !visible(role, user!.name).includes(s)) return <ErrorPage code="403" />;

  const roster = rosterOn(s.cls, s.date);
  const future = s.date > TODAY;
  const canEdit = !s.cancelled && !future && (manager || isToday(s));
  const c = countMarks({ savedAt: '', savedBy: '', marks, notes });
  const unmarked = roster.filter((st) => !marks[st.code]).length;
  const changes = saved ? roster.filter((st) => saved.marks[st.code] !== marks[st.code]) : [];
  const logs = ATT_LOGS.filter((l) => l.session === s.id);
  const shown = all || roster.length <= 8 || canEdit ? roster : roster.slice(0, 5);

  function save() {
    if (unmarked) return setError(`Còn ${unmarked} học viên chưa điểm danh`);
    if (saved && !changes.length) return setError('Chưa có thay đổi nào');
    if (saved && !isToday(s!) && !reason.trim()) return setError('Nhập lý do sửa điểm danh của buổi đã qua');
    const at = stamp();
    const by = user!.name;
    const roleName = ROLES[role];
    if (saved) {
      for (const st of changes) ATT_LOGS.unshift({ session: s!.id, at, by, role: roleName, text: `${st.name}: ${saved.marks[st.code] ?? 'chưa có'} → ${marks[st.code]}${reason.trim() ? '. Lý do: ' + reason.trim() : ''}` });
    } else {
      ATT_LOGS.unshift({ session: s!.id, at, by, role: roleName, text: `Lưu điểm danh lần đầu: ${summary({ savedAt: at, savedBy: by, marks, notes })}.` });
    }
    SHEETS[s!.id] = { savedAt: at, savedBy: by, marks, notes };
    saveAll();
    toast(saved ? 'Đã lưu sửa điểm danh' : 'Đã lưu điểm danh', `${s!.cls} · Buổi ${s!.no}`);
    navigate('/diem-danh');
  }

  function requestEdit() {
    notify({ at: stamp(), kind: 'Điểm danh', title: `Yêu cầu sửa điểm danh ${s!.cls} buổi ${s!.no}`, body: `${user!.name} đề nghị mở sửa điểm danh buổi ${longDate(s!.date).toLowerCase()}.`, link: '/diem-danh/' + s!.id, to: { users: ['LE HOANG NAM'] } });
    saveAll();
    toast('Đã gửi yêu cầu tới Quản lý đào tạo');
  }

  return (
    <AppLayout crumb={[MODULE, `${s.cls} · Buổi ${s.no}`]} module={MODULE}>
      <Link to="/diem-danh" className="back">
        <Icon name="back" size={16} />
        Điểm danh
      </Link>
      <div className="h">
        <div>
          <h2>
            Điểm danh {s.cls} · Buổi {s.no}
          </h2>
          <p>
            {isToday(s) ? 'Hôm nay ' + dm(s.date) : longDate(s.date)} · {s.start} – {s.end} · {s.room ?? 'Chưa xếp phòng'} · {roster.length} học viên · {s.subject} Bài {s.lesson}
          </p>
        </div>
        {saved && <Pill tone="ok">Đã lưu {vdt(saved.savedAt).slice(0, 5) + ' ' + saved.savedAt.slice(11, 16)}</Pill>}
      </div>
      {s.cancelled && (
        <Box icon="x" tone="bad" title="Buổi đã huỷ">
          Không mở điểm danh. Lý do huỷ: {s.cancelled}
        </Box>
      )}
      {future && <Box icon="clock" title="Chưa tới ngày học">Điểm danh mở vào ngày diễn ra buổi học.</Box>}
      {!canEdit && !future && !s.cancelled && (
        <div className="box warn at-locked">
          <Icon name="lock" size={20} />
          <div>
            <b>Buổi này đã qua, bạn chỉ xem được</b>
            Giảng viên chỉ điểm danh được buổi của ngày hôm nay. Sửa buổi quá khứ cần quyền Quản lý đào tạo.
          </div>
          <button type="button" className="btn" onClick={requestEdit}>
            Gửi yêu cầu sửa điểm danh
          </button>
        </div>
      )}
      {canEdit && (
        <button
          type="button"
          className="btn at-all"
          onClick={() => {
            const next = { ...marks };
            for (const st of roster) next[st.code] ??= 'Có mặt';
            setMarks(next);
            setError('');
          }}
        >
          <Icon name="check" size={16} />
          Đánh dấu tất cả có mặt
        </button>
      )}
      <div className="panel at-list">
        <div className="at-row at-head">
          <span>Mã học viên</span>
          <span>Họ tên</span>
          <span>Trạng thái</span>
          <span>Ghi chú</span>
        </div>
        {shown.map((st) => (
          <div key={st.code} className="at-row">
            <span className="at-code">{st.code}</span>
            <b>{st.name}</b>
            <div className="at-marks" role="radiogroup" aria-label={'Trạng thái ' + st.name}>
              {MARKS.map((m) => (
                <button
                  key={m}
                  type="button"
                  role="radio"
                  aria-checked={marks[st.code] === m}
                  disabled={!canEdit}
                  className={marks[st.code] === m ? 'on ' + markTone(m) : undefined}
                  onClick={() => {
                    setMarks({ ...marks, [st.code]: m });
                    setError('');
                  }}
                >
                  <span className="full">{m}</span>
                  <span className="short">{SHORT[m]}</span>
                </button>
              ))}
            </div>
            {canEdit ? (
              <input className="at-note" aria-label={'Ghi chú ' + st.name} value={notes[st.code] ?? ''} placeholder="Ghi chú" onChange={(e) => setNotes({ ...notes, [st.code]: e.target.value })} />
            ) : (
              <span className="at-note-t">{notes[st.code] ?? ''}</span>
            )}
          </div>
        ))}
        {shown.length < roster.length && (
          <button type="button" className="link at-more" onClick={() => setAll(true)}>
            và {roster.length - shown.length} học viên khác
          </button>
        )}
      </div>
      {saved && canEdit && changes.length > 0 && !isToday(s) && (
        <div className="field" style={{ marginTop: 16 }}>
          <label htmlFor="at-reason">Lý do sửa ({changes.length} thay đổi)</label>
          <div className="inp">
            <input id="at-reason" value={reason} placeholder="Ví dụ: giảng viên báo đánh nhầm." onChange={(e) => setReason(e.target.value)} />
          </div>
        </div>
      )}
      {logs.length > 0 || saved ? (
        <section className="at-log">
          <h3 className="section-t">Nhật ký sửa điểm danh</h3>
          <ul>
            {logs.map((l, i) => (
              <li key={i}>
                <b>
                  {vdt(l.at)} · {l.by} ({l.role})
                </b>
                <span>{l.text}</span>
              </li>
            ))}
            {saved && !logs.length && (
              <li>
                <b>
                  {vdt(saved.savedAt)} · {saved.savedBy} (Giảng viên)
                </b>
                <span>Lưu điểm danh lần đầu: {summary(saved)}.</span>
              </li>
            )}
          </ul>
        </section>
      ) : null}
      {canEdit && (
        <div className="at-foot">
          <div>
            <b>
              {c['Có mặt']} có mặt · {c['Đi muộn']} muộn
            </b>
            <span>
              {c['Vắng có phép']} có phép · {c['Vắng không phép']} vắng{unmarked ? ` · ${unmarked} chưa điểm danh` : ''}
            </span>
            {error && <em>{error}</em>}
          </div>
          <button type="button" className="btn primary" onClick={save}>
            Lưu điểm danh
          </button>
        </div>
      )}
    </AppLayout>
  );
}
