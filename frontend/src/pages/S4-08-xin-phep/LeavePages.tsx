import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Box, Filter, LoadState, Pill } from '../../components/ui';
import { apiEnabled, useLoad } from '../../data/api';
import { MARKS, SHEETS, sheetOf, type Mark } from '../../data/attendance';
import { useAuth } from '../../data/auth';
import { classesOf } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { LEAVES, leaveTone, type Leave, type LeaveStatus } from '../../data/leaves';
import { notify } from '../../data/notifications';
import { dm, findSession, longDate, sessionsOf, WEEKDAY_NAMES, dow } from '../../data/schedule';
import { decideLeave, LEAVE_COUNTS, loadLeaves, loadMySchedule, MY, sendLeave } from '../../data/s4api';
import { saveAll } from '../../data/store';
import { findStudent } from '../../data/students';
import { useMe } from '../S4-05-lich-cua-toi/MySchedulePage';
import { MODULE } from '../S4-07-diem-danh/AttendancePage';
import './leave.css';

const stamp = () => TODAY + 'T' + new Date().toTimeString().slice(0, 5);
const vdt = (at: string) => `${at.slice(8, 10)}/${at.slice(5, 7)}/${at.slice(0, 4)} ${at.slice(11, 16)}`;
const kb = (n: number) => (n > 1048576 ? (n / 1048576).toFixed(1) + ' MB' : Math.round(n / 1024) + ' KB');
const sessionLabel = (id: string) => {
  const s = findSession(id);
  return s ? `${WEEKDAY_NAMES[dow(s.date)]} ${dm(s.date)} · ${s.subject} Bài ${s.lesson}` : id;
};

// S4-08. Học viên gửi đơn xin phép vắng: chọn buổi sắp tới của lớp mình, nhập lý do, đính kèm tệp minh chứng nếu có,
// rồi theo dõi kết quả duyệt ở cột bên phải.
export function MyLeavesPage() {
  // Có backend: lịch lớp mình và đơn của mình tải trước khi chọn sẵn buổi sắp tới.
  const load = useLoad(() => Promise.all([loadMySchedule(), loadLeaves(true)]));
  if (apiEnabled && (load.loading || load.error))
    return (
      <AppLayout crumb={[MODULE, 'Đơn xin phép vắng']} module={MODULE}>
        <LoadState {...load} />
      </AppLayout>
    );
  return <MyLeaves />;
}

function MyLeaves() {
  const { user } = useAuth();
  const local = useMe();
  // Có backend: học viên không xem được danh sách học viên, mã và lớp lấy từ lịch của tôi và đơn đã tải.
  const me = apiEnabled ? { code: LEAVES[0]?.student ?? '', name: user?.name ?? '', cls: MY.cls } : local;
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  const upcoming = me?.cls ? sessionsOf(me.cls).filter((s) => !s.cancelled && s.date >= TODAY) : [];
  const [session, setSession] = useState(upcoming[0]?.id ?? '');
  const [reason, setReason] = useState('');
  const [file, setFile] = useState<{ name: string; size: number; raw: File } | null>(null);
  const [error, setError] = useState('');
  const [, setTick] = useState(0);
  if (user && user.active !== 0) return <Navigate to="/duyet-phep" replace />;
  const mine = (apiEnabled ? LEAVES : LEAVES.filter((l) => l.student === me?.code)).sort((a, b) => b.sentAt.localeCompare(a.sentAt));
  const taken = new Set(mine.filter((l) => l.status !== 'Từ chối').map((l) => l.session));

  function send() {
    if (!session) return setError('Chọn buổi học muốn xin vắng');
    if (taken.has(session)) return setError('Bạn đã gửi đơn cho buổi này');
    if (reason.trim().length < 10) return setError('Ghi rõ lý do, ít nhất 10 ký tự');
    if (apiEnabled) {
      // Máy chủ kiểm buổi thuộc lớp mình, chưa qua, chưa có đơn; giảng viên của lớp thấy đơn ở trang duyệt.
      setBusy(true);
      sendLeave(session, reason.trim(), file?.raw).then(
        () => (setReason(''), setFile(null), setError(''), setBusy(false), toast('Đã gửi đơn xin phép', 'Bạn sẽ nhận thông báo khi giảng viên duyệt.')),
        (e: Error) => (setBusy(false), setError(e.message)),
      );
      return;
    }
    LEAVES.unshift({ id: Math.max(0, ...LEAVES.map((l) => l.id)) + 1, student: me!.code, session, reason: reason.trim(), file: file ? { name: file.name, size: file.size } : undefined, sentAt: stamp(), status: 'Chờ duyệt' });
    const s = findSession(session);
    notify({ at: stamp(), kind: 'Điểm danh', title: `${me!.name} xin phép vắng`, body: `${sessionLabel(session)}. Lý do: ${reason.trim()}`, link: '/duyet-phep', to: { users: s?.teacher ? [s.teacher] : [] } });
    saveAll();
    setReason('');
    setFile(null);
    setError('');
    setTick((t) => t + 1);
    toast('Đã gửi đơn xin phép', 'Bạn sẽ nhận thông báo khi giảng viên duyệt.');
  }

  return (
    <AppLayout crumb={[MODULE, 'Đơn xin phép vắng']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Đơn xin phép vắng</h2>
          <p>Gửi trước giờ học để giảng viên kịp duyệt</p>
        </div>
        {/* S5-03 */}
        <Link to="/diem-danh-cua-toi" className="btn">
          Điểm danh của tôi
        </Link>
      </div>
      <div className="lv-grid">
        <section className="panel lv-form">
          <h3 className="section-t">Gửi đơn mới</h3>
          <div className="field">
            <label htmlFor="lv-s">Buổi học</label>
            <div className="inp">
              <select id="lv-s" value={session} onChange={(e) => setSession(e.target.value)}>
                {!upcoming.length && <option value="">Không có buổi sắp tới</option>}
                {upcoming.slice(0, 12).map((s) => (
                  <option key={s.id} value={s.id} disabled={taken.has(s.id)}>
                    {WEEKDAY_NAMES[dow(s.date)]} {dm(s.date)} · {s.start} · {s.subject} Bài {s.lesson} · {s.room ?? ''}
                    {taken.has(s.id) ? ' (đã gửi đơn)' : ''}
                  </option>
                ))}
              </select>
            </div>
            <p className="hint">Chỉ chọn được buổi sắp tới của lớp bạn tham gia</p>
          </div>
          <div className="field">
            <label htmlFor="lv-r">Lý do</label>
            <div className={'inp' + (error && reason.trim().length < 10 ? ' bad' : '')}>
              <textarea id="lv-r" rows={4} value={reason} placeholder="Ví dụ: em có lịch thi ở trường vào tối hôm đó." onChange={(e) => setReason(e.target.value)} />
            </div>
          </div>
          <span className="lbl">Tệp minh chứng (không bắt buộc)</span>
          <label className="lv-drop">
            <Icon name="upload" />
            <b>Chọn tệp hoặc kéo vào đây</b>
            <small>JPG, PNG, PDF · tối đa 5MB</small>
            <input
              type="file"
              accept=".jpg,.jpeg,.png,.pdf"
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (!f) return;
                if (f.size > 5 * 1048576) return setError('Tệp vượt quá 5MB');
                if (!/\.(jpe?g|png|pdf)$/i.test(f.name)) return setError('Chỉ nhận JPG, PNG hoặc PDF');
                setFile({ name: f.name, size: f.size, raw: f });
                setError('');
              }}
            />
          </label>
          {file && (
            <div className="lv-file">
              <Icon name="file" size={16} />
              <span>{file.name}</span>
              <small>{kb(file.size)}</small>
              <button type="button" className="icon-btn" aria-label="Bỏ tệp" onClick={() => setFile(null)}>
                <Icon name="x" size={14} />
              </button>
            </div>
          )}
          <Box icon="check" tone="info" title="Đơn được duyệt thì buổi này tự ghi Vắng có phép">
            Bạn sẽ nhận thông báo khi giảng viên duyệt hoặc từ chối.
          </Box>
          {error && <p className="err" style={{ marginBottom: 12 }}>{error}</p>}
          <div className="acts" style={{ marginTop: 0 }}>
            <button type="button" className="btn primary" style={{ width: 140 }} disabled={busy} onClick={send}>
              Gửi đơn
            </button>
          </div>
        </section>
        <section>
          <h3 className="section-t">Đơn đã gửi</h3>
          <div className="lv-list">
            {mine.map((l) => (
              <div key={l.id} className="panel lv-item">
                <div className="lv-top">
                  <b>{sessionLabel(l.session)}</b>
                  <Pill tone={leaveTone(l.status)}>{l.status}</Pill>
                </div>
                <small>
                  Gửi {vdt(l.sentAt)}
                  {l.file?.url ? (
                    <>
                      {' · '}
                      <a href={l.file.url} target="_blank" rel="noreferrer">
                        {l.file.name}
                      </a>
                    </>
                  ) : l.file ? (
                    ' · có 1 tệp đính kèm'
                  ) : (
                    ''
                  )}
                </small>
                <p>{l.reason}</p>
                {l.status !== 'Chờ duyệt' && (
                  <p className={'lv-dec ' + leaveTone(l.status)}>
                    {l.decidedBy} {l.status === 'Đã duyệt' ? 'duyệt' : 'từ chối'} ngày {vdt(l.decidedAt!).slice(0, 5)} {l.decidedAt!.slice(11, 16)}.
                    {l.status === 'Đã duyệt' ? ' Điểm danh buổi này: Vắng có phép.' : ` Lý do: ${l.note}`}
                  </p>
                )}
              </div>
            ))}
            {!mine.length && <p className="hint">Bạn chưa gửi đơn nào.</p>}
          </div>
        </section>
      </div>
    </AppLayout>
  );
}

const TABS: LeaveStatus[] = ['Chờ duyệt', 'Đã duyệt', 'Từ chối'];

// S4-08. Giảng viên duyệt hoặc từ chối đơn của lớp mình dạy, có ghi lý do (bắt buộc khi từ chối).
// Duyệt thì điểm danh buổi đó của học viên tự đặt thành Vắng có phép; học viên nhận thông báo kết quả.
export function ApproveLeavesPage() {
  const { user } = useAuth();
  const toast = useToast();
  const role = user?.active ?? -1;
  const [tab, setTab] = useState<LeaveStatus>('Chờ duyệt');
  const [cls, setCls] = useState('');
  const [openId, setOpenId] = useState<number | null>(null);
  const [note, setNote] = useState('');
  const [error, setError] = useState('');
  const [, setTick] = useState(0);
  // Có backend: máy chủ chỉ trả đơn của các lớp người này dạy (quản lý: tất cả).
  const load = useLoad(() => loadLeaves(false));
  const [busy, setBusy] = useState(false);
  if (role === 0) return <Navigate to="/xin-phep" replace />;
  const myClasses = apiEnabled || role === 5 || role === 6 ? null : new Set(classesOf(user!.name).map((k) => k.code));
  const scope = LEAVES.filter((l) => {
    const st = findStudent(l.student);
    const s = findSession(l.session);
    return (!myClasses || (s && myClasses.has(s.cls))) && (!cls || s?.cls === cls) && !!st;
  });
  const list = scope.filter((l) => l.status === tab).sort((a, b) => b.sentAt.localeCompare(a.sentAt));
  const open = list.find((l) => l.id === openId) ?? list[0];
  const classes = [...new Set(LEAVES.map((l) => findSession(l.session)?.cls).filter((x): x is string => !!x && (!myClasses || myClasses.has(x))))];

  function decide(l: Leave, ok: boolean) {
    if (!ok && !note.trim()) return setError('Nhập lý do từ chối để học viên biết');
    if (apiEnabled) {
      // Máy chủ ghi kết quả, duyệt thì đặt Vắng có phép cho buổi đã điểm danh.
      setBusy(true);
      const name = findStudent(l.student)?.name ?? l.student;
      decideLeave(l.id, ok, note.trim()).then(
        () => (setNote(''), setError(''), setOpenId(null), setBusy(false), toast(ok ? 'Đã duyệt đơn' : 'Đã từ chối đơn', name)),
        (e: Error) => (setBusy(false), setError(e.message)),
      );
      return;
    }
    l.status = ok ? 'Đã duyệt' : 'Từ chối';
    l.decidedBy = user!.name;
    l.decidedAt = stamp();
    l.note = note.trim() || undefined;
    const s = findSession(l.session);
    if (ok && s) {
      const sh = SHEETS[s.id] ?? (sheetOf(s) ? { ...sheetOf(s)!, marks: { ...sheetOf(s)!.marks }, notes: { ...sheetOf(s)!.notes } } : undefined);
      if (sh) {
        sh.marks[l.student] = 'Vắng có phép' satisfies Mark;
        sh.notes[l.student] = 'Đơn xin phép đã duyệt';
        SHEETS[s.id] = sh;
      }
    }
    const st = findStudent(l.student)!;
    notify({ at: stamp(), kind: 'Điểm danh', title: ok ? 'Đơn xin phép đã được duyệt' : 'Đơn xin phép bị từ chối', body: `${sessionLabel(l.session)}.${ok ? ' Buổi này ghi Vắng có phép.' : ' Lý do: ' + l.note}`, link: '/xin-phep', to: { users: [st.name] } });
    saveAll();
    setNote('');
    setError('');
    setOpenId(null);
    setTick((t) => t + 1);
    toast(ok ? 'Đã duyệt đơn' : 'Đã từ chối đơn', st.name);
  }

  const st = open && findStudent(open.student);
  const ses = open && findSession(open.session);
  const counts = (code: string) => {
    if (apiEnabled && open) return LEAVE_COUNTS[open.id] ?? [0, 0];
    let ex = 0;
    let un = 0;
    if (st?.cls)
      for (const s of sessionsOf(st.cls)) {
        const m = sheetOf(s)?.marks[code];
        if (m === MARKS[2]) ex++;
        if (m === MARKS[3]) un++;
      }
    return [ex, un];
  };

  return (
    <AppLayout crumb={[MODULE, 'Duyệt đơn xin phép']} module={MODULE}>
      {apiEnabled && (load.loading || load.error) && <LoadState {...load} />}
      <div className="h">
        <div>
          <h2>Duyệt đơn xin phép vắng</h2>
          <p>
            {scope.filter((l) => l.status === 'Chờ duyệt').length} đơn chờ duyệt {myClasses ? 'ở các lớp bạn dạy' : ''}
          </p>
        </div>
      </div>
      <div className="bar">
        <div className="seg lv-tabs" role="tablist">
          {TABS.map((t) => (
            <button key={t} type="button" role="tab" aria-selected={tab === t} className={tab === t ? 'on' : undefined} onClick={() => (setTab(t), setOpenId(null))}>
              {t}
              {t === 'Chờ duyệt' ? ` (${scope.filter((l) => l.status === t).length})` : ''}
            </button>
          ))}
        </div>
        <span className="cal-gap" style={{ flex: 1 }} />
        <Filter label="Lớp" value={cls} options={classes} onChange={setCls} />
      </div>
      <div className="lv-grid wide">
        <div className="panel">
          <table className="rows">
            <thead>
              <tr>
                <th>Học viên</th>
                <th>Buổi xin vắng</th>
                <th>Lý do</th>
              </tr>
            </thead>
            <tbody>
              {list.map((l) => {
                const x = findStudent(l.student)!;
                const s = findSession(l.session);
                return (
                  <tr key={l.id} className={open?.id === l.id ? 'on' : undefined} tabIndex={0} onClick={() => setOpenId(l.id)} onKeyDown={(e) => e.key === 'Enter' && setOpenId(l.id)}>
                    <td>
                      <b className="strong">{x.name}</b>
                      <small className="lv-sub">
                        {x.code} · {s?.cls}
                      </small>
                    </td>
                    <td>
                      <b className="strong">{s ? `${WEEKDAY_NAMES[dow(s.date)]} ${dm(s.date)} · ${s.start}` : l.session}</b>
                      <small className="lv-sub">Gửi {vdt(l.sentAt).slice(0, 5)} {l.sentAt.slice(11, 16)}</small>
                    </td>
                    <td>{l.reason.length > 40 ? l.reason.slice(0, 40) + '…' : l.reason}</td>
                  </tr>
                );
              })}
              {!list.length && (
                <tr>
                  <td colSpan={3} className="empty">
                    Không có đơn nào ở mục này.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {open && st && (
          <section className="panel lv-form">
            <div className="lv-top">
              <h3>{st.name}</h3>
              <Pill tone={leaveTone(open.status)}>{open.status}</Pill>
            </div>
            <p className="hint">
              {st.code} · {ses?.cls} · đã vắng có phép {counts(st.code)[0]} buổi, không phép {counts(st.code)[1]} buổi
            </p>
            <span className="lbl" style={{ marginTop: 16 }}>
              Buổi xin vắng
            </span>
            <p>{ses ? `${longDate(ses.date)}, ${ses.start} – ${ses.end} · ${ses.subject} Bài ${ses.lesson}` : open.session}</p>
            <span className="lbl" style={{ marginTop: 16 }}>
              Lý do
            </span>
            <p>{open.reason}</p>
            {open.file && (
              <div className="lv-file">
                <Icon name="file" size={16} />
                {open.file.url ? (
                  <a href={open.file.url} target="_blank" rel="noreferrer">
                    {open.file.name}
                  </a>
                ) : (
                  <span>{open.file.name}</span>
                )}
                <small>{kb(open.file.size)}</small>
              </div>
            )}
            {open.status === 'Chờ duyệt' ? (
              <>
                <div className="field" style={{ marginTop: 16 }}>
                  <label htmlFor="lv-note">Lý do duyệt hoặc từ chối</label>
                  <div className={'inp' + (error ? ' bad' : '')}>
                    <textarea id="lv-note" value={note} onChange={(e) => (setNote(e.target.value), setError(''))} />
                  </div>
                  {error ? <p className="err">{error}</p> : <p className="hint">Bắt buộc khi từ chối. Học viên sẽ đọc được nội dung này.</p>}
                </div>
                <Box icon="check" tone="info" title="Nếu duyệt">
                  Điểm danh buổi {ses ? dm(ses.date) : ''} của học viên tự đặt thành Vắng có phép. Học viên nhận thông báo kết quả.
                </Box>
                <div className="acts">
                  <button type="button" className="btn danger" disabled={busy} onClick={() => decide(open, false)}>
                    Từ chối
                  </button>
                  <button type="button" className="btn primary" disabled={busy} onClick={() => decide(open, true)}>
                    Duyệt đơn
                  </button>
                </div>
              </>
            ) : (
              <p className={'lv-dec ' + leaveTone(open.status)}>
                {open.decidedBy} {open.status === 'Đã duyệt' ? 'duyệt' : 'từ chối'} lúc {vdt(open.decidedAt!)}
                {open.note ? '. ' + open.note : ''}
              </p>
            )}
          </section>
        )}
      </div>
    </AppLayout>
  );
}

export const pendingFor = (name: string, role: number) => {
  // Có backend: LEAVES đã chỉ gồm đơn của các lớp người này dạy.
  const mine = apiEnabled || role === 5 || role === 6 ? null : new Set(classesOf(name).map((k) => k.code));
  return LEAVES.filter((l) => l.status === 'Chờ duyệt' && (!mine || mine.has(findSession(l.session)?.cls ?? ''))).length;
};

