import { Link } from 'react-router-dom';
import { ASSIGNMENTS, SUBMISSIONS } from '../../data/assignments';
import { statOf } from '../../data/attendance';
import { classesOf, isOpen, isTeacherMatch, totalSessions } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { approvedFor } from '../../data/leaves';
import { addDays, longDate, mondayOf, SESSIONS, shortDate } from '../../data/schedule';
import { rosterOn } from '../../data/students';
import { daysBetween } from '../../data/tuition';
import '../S8-07-dashboard-qldt/dashboard.css';
import './teacher.css';

// S8-08. Dashboard Giảng viên: buổi dạy kế tiếp, bài chưa chấm, học viên vắng nhiều nhất, lịch tuần này.
export function TeacherDashboard({ name }: { name: string }) {
  const classes = classesOf(name).filter(isOpen);
  const codes = new Set(classes.map((k) => k.code));
  const mine = SESSIONS.filter((s) => codes.has(s.cls) && (!s.teacher || isTeacherMatch(s.teacher, name))).sort((a, b) => (a.date + a.start).localeCompare(b.date + b.start));
  const nowHm = new Date().toTimeString().slice(0, 5);
  const next = mine.find((s) => !s.cancelled && (s.date > TODAY || (s.date === TODAY && s.end >= nowHm)));
  const todo = ASSIGNMENTS.filter((a) => codes.has(a.cls) && a.status === 'Đã giao')
    .map((a) => {
      const subs = SUBMISSIONS.filter((s) => s.assignment === a.id);
      const open = subs.filter((s) => s.state === 'Đã nộp');
      return { a, subs: subs.length, open: open.length, oldest: open.map((s) => s.versions.at(-1)?.at ?? '').sort()[0] };
    })
    .filter((x) => x.open)
    .sort((x, y) => y.open - x.open);
  const total = todo.reduce((n, x) => n + x.open, 0);
  const oldest = todo.map((x) => x.oldest).filter(Boolean).sort()[0];
  const absent = classes
    .flatMap((k) => rosterOn(k.code, TODAY).map((s) => ({ s, k, st: statOf(s.code, k.code, k.program, totalSessions(k)) })))
    .filter((r) => r.st.absent)
    .sort((a, b) => b.st.absent - a.st.absent)
    .slice(0, 3);
  const mon = mondayOf(TODAY);
  const week = mine.filter((s) => s.date >= mon && s.date <= addDays(mon, 6));

  return (
    <>
      <div className="h">
        <div>
          <h2>Chào {name}</h2>
          <p>
            {longDate(TODAY)} · bạn đang dạy {classes.length} lớp
          </p>
        </div>
      </div>
      <div className="tdb-top">
        <section className="tdb-next">
          {next ? (
            <>
              <small>BUỔI DẠY KẾ TIẾP · {next.date === TODAY ? 'HÔM NAY' : longDate(next.date).toUpperCase()}</small>
              <span>
                {next.start} – {next.end} · {next.room ? `Phòng ${next.room}` : 'chưa xếp phòng'}
              </span>
              <b>
                {next.cls} · {next.subject} Bài {next.lesson}: {next.topic}
              </b>
              <span>
                {rosterOn(next.cls, next.date).length} học viên{approvedFor(next.id).length ? ` · ${approvedFor(next.id).length} đơn xin phép vắng đã duyệt` : ''}
              </span>
              <div className="tdb-acts">
                <Link to={`/diem-danh/${next.id}`} className="btn">
                  Mở điểm danh
                </Link>
                <Link to={`/hoc-lieu/${next.cls}`} className="btn ghost">
                  Tài liệu buổi học
                </Link>
              </div>
            </>
          ) : (
            <b>Không có buổi dạy sắp tới.</b>
          )}
        </section>
        <section className="panel fe-pad tdb-grade">
          <h3>Bài chưa chấm</h3>
          <p>
            <b className="warn-t">{total}</b> <span className="hint">bài ở {todo.length} bài tập</span>
          </p>
          <span className="hint">{oldest ? `Cũ nhất nộp cách đây ${daysBetween(oldest.slice(0, 10), TODAY)} ngày` : 'Đã chấm hết'}</span>
          {todo[0] && (
            <Link to={`/bai-tap/${todo[0].a.id}`} className="btn primary">
              Chấm ngay
            </Link>
          )}
        </section>
      </div>
      <div className="db-grid" style={{ marginTop: 16 }}>
        <section className="panel">
          <div className="fe-head">
            <h3>Bài tập cần chấm</h3>
          </div>
          <table>
            <thead>
              <tr>
                <th>Bài tập</th>
                <th>Lớp</th>
                <th>Chưa chấm</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {todo.slice(0, 5).map((x) => (
                <tr key={x.a.id}>
                  <td className="strong">{x.a.title}</td>
                  <td className="hint">{x.a.cls}</td>
                  <td className="warn-t">
                    {x.open} / {x.subs}
                  </td>
                  <td className="fe-used">
                    <Link className="link" to={`/bai-tap/${x.a.id}`}>
                      Mở màn chấm bài
                    </Link>
                  </td>
                </tr>
              ))}
              {!todo.length && (
                <tr>
                  <td colSpan={4} className="empty">
                    Không còn bài nào chờ chấm.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </section>
        <section className="panel fe-pad">
          <h3>Học viên vắng nhiều nhất</h3>
          {absent.map((r) => (
            <div key={r.s.code + r.k.code} className="tdb-abs">
              <span>
                <b>{r.s.name}</b>
                <small className="hint">{r.k.code}</small>
              </span>
              <span className={r.st.over ? 'bad-t' : r.st.left <= 1 ? 'warn-t' : undefined}>
                {r.st.absent} buổi · {r.st.rate}%
              </span>
            </div>
          ))}
          {!absent.length && <p className="hint">Chưa có học viên vắng.</p>}
          <Link to="/diem-danh" className="link">
            Xem bảng chuyên cần
          </Link>
        </section>
      </div>
      <section style={{ marginTop: 16 }}>
        <h3 className="tdb-h">Tuần này</h3>
        <div className="tdb-week">
          {week.map((s) => (
            <Link key={s.id} to={`/diem-danh/${s.id}`} className={'panel tdb-day' + (s.date === TODAY ? ' today' : '') + (s.cancelled ? ' off' : '')}>
              <small className="hint">
                {shortDate(s.date).slice(0, -5)} · {s.start}
              </small>
              <b>
                {s.cls} · B{s.lesson}
                {s.makeup ? ' học bù' : ''}
              </b>
              {s.date === TODAY && <em className="today-t">Hôm nay</em>}
              {s.cancelled && <em className="bad-t">Đã huỷ</em>}
              {s.movedFrom && !s.cancelled && <em className="warn-t">Đổi lịch</em>}
            </Link>
          ))}
          {!week.length && <p className="hint">Tuần này bạn không có buổi dạy.</p>}
        </div>
      </section>
    </>
  );
}
