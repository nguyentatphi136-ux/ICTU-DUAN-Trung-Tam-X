import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Box, LoadState, Pill } from '../../components/ui';
import { apiEnabled, useLoad } from '../../data/api';
import { useAuth } from '../../data/auth';
import { findClass } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { findRoom } from '../../data/rooms';
import { addDays, dm, dow, longDate, mondayOf, sessionsOf, WEEKDAYS, WEEKDAY_NAMES, type ClassSession } from '../../data/schedule';
import { loadMySchedule, MY } from '../../data/s4api';
import { STUDENTS } from '../../data/students';
import { MODULE } from '../S3-07-danh-sach-lop/ClassListPage';
import './my.css';

/** Học viên đang đăng nhập (theo email đăng nhập). */
export const useMe = () => {
  const { user } = useAuth();
  return STUDENTS.find((s) => s.login === user?.email);
};

const minutes = (hhmm: string) => Number(hhmm.slice(0, 2)) * 60 + Number(hhmm.slice(3, 5));

/** "3 giờ 20 phút" tới lúc bắt đầu nếu là hôm nay, "2 ngày" nếu là ngày khác. */
function until(s: ClassSession) {
  if (s.date !== TODAY) return `${Math.round((Date.parse(s.date) - Date.parse(TODAY)) / 86400000)} ngày`;
  const n = new Date();
  const left = minutes(s.start) - (n.getHours() * 60 + n.getMinutes());
  if (left <= 0) return 'Đang diễn ra';
  return (left >= 60 ? `${Math.floor(left / 60)} giờ ` : '') + `${left % 60} phút`;
}

function state(s: ClassSession): [string, 'ok' | 'off' | 'wait' | 'info' | undefined] {
  if (s.cancelled) return ['Đã huỷ', 'off'];
  if (s.movedFrom || s.makeup) return ['Đổi lịch', 'wait'];
  if (s.date === TODAY) return ['Hôm nay', 'info'];
  return s.date < TODAY ? ['Đã học', undefined] : ['Sắp tới', undefined];
}

// S4-05. Lịch học của học viên: mặc định hiện buổi kế tiếp và các buổi trong tuần. Chỉ thấy buổi của lớp mình học.
// Buổi bị huỷ hoặc dời có nhãn riêng và một dòng giải thích lý do.
export function MySchedulePage() {
  const { user } = useAuth();
  const me = useMe();
  const [week, setWeek] = useState(mondayOf(TODAY));
  // Có backend: lớp đang học và các buổi của lớp đó lấy từ /api/me/schedule (học viên không có quyền xem danh sách học viên).
  const load = useLoad(loadMySchedule);
  if (user && user.active !== 0) return <Navigate to="/thoi-khoa-bieu" replace />;
  const cls = apiEnabled ? MY.cls : me?.cls;
  const k = findClass(cls);
  const all = cls ? sessionsOf(cls) : [];
  const next = all.find((s) => !s.cancelled && s.date >= TODAY);
  const days = [...Array(7)].map((_, i) => addDays(week, i));
  const list = all.filter((s) => s.date >= days[0] && s.date <= days[6]);
  const changes = list.filter((s) => s.cancelled || s.movedFrom || s.makeup);
  const room = findRoom(next?.room);

  return (
    <AppLayout crumb={[MODULE, 'Lịch học của tôi']} module={MODULE}>
      {apiEnabled && (load.loading || load.error) && <LoadState {...load} />}
      <div className="h">
        <div>
          <h2>Lịch học của tôi</h2>
          <p>{cls ? `Lớp ${cls} · ${k?.program ?? MY.program ?? ''}` : 'Bạn chưa được xếp lớp'}</p>
        </div>
        {/* S4-08 */}
        {k && (
          <Link to="/xin-phep" className="btn">
            Xin phép vắng một buổi
          </Link>
        )}
      </div>
      {next ? (
        <section className="my-next">
          <div>
            <small>Buổi kế tiếp · {next.date === TODAY ? 'hôm nay' : longDate(next.date).toLowerCase()}</small>
            <span>
              {next.start} – {next.end} · Phòng {next.room ?? 'chưa xếp'}
            </span>
            <b>
              {next.subject} · Bài {next.lesson}: {next.topic}
            </b>
            <span>
              Giảng viên {next.teacher ?? 'chưa phân công'}
              {room ? ' · ' + room.where : ''}
            </span>
          </div>
          <div className="my-left">
            <small>Còn</small>
            <b>{until(next)}</b>
          </div>
        </section>
      ) : (
        <Box icon="cal" title="Chưa có buổi học sắp tới">
          Lịch sẽ hiện ở đây khi lớp của bạn được xếp thời khoá biểu.
        </Box>
      )}

      <div className="my-week-h">
        <h3>
          {week === mondayOf(TODAY) ? 'Tuần này' : 'Tuần'} · {dm(days[0])} – {dm(days[6])}/{days[6].slice(0, 4)}
        </h3>
        <div className="my-nav">
          <button type="button" className="icon-btn" aria-label="Tuần trước" onClick={() => setWeek(addDays(week, -7))}>
            <Icon name="back" size={16} />
          </button>
          <button type="button" className="btn" onClick={() => setWeek(mondayOf(TODAY))}>
            Tuần này
          </button>
          <button type="button" className="icon-btn" aria-label="Tuần sau" onClick={() => setWeek(addDays(week, 7))}>
            <Icon name="go" size={16} />
          </button>
        </div>
      </div>
      <div className="panel my-table">
        <table>
          <thead>
            <tr>
              <th>Ngày</th>
              <th>Giờ</th>
              <th>Môn · bài</th>
              <th>Chủ đề</th>
              <th>Phòng</th>
              <th>Giảng viên</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {list.map((s) => {
              const [label, tone] = state(s);
              return (
                <tr key={s.id} className={s.cancelled ? 'off' : undefined}>
                  <td className="strong">
                    {WEEKDAY_NAMES[dow(s.date)]} {dm(s.date)}
                  </td>
                  <td className="strong">
                    {s.start} – {s.end}
                  </td>
                  <td className="strong">
                    {s.subject} · Bài {s.lesson}
                  </td>
                  <td>
                    {s.topic}
                    {s.makeup ? ' (học bù)' : ''}
                  </td>
                  <td className="strong">{s.room ?? '–'}</td>
                  <td>{s.teacher ?? '–'}</td>
                  <td>
                    <Pill tone={tone}>{label}</Pill>
                  </td>
                </tr>
              );
            })}
            {!list.length && (
              <tr>
                <td colSpan={7} className="empty">
                  Tuần này không có buổi học.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      <div className="my-cards">
        {list.map((s) => {
          const [label, tone] = state(s);
          return (
            <div key={s.id} className={'panel my-card' + (s.cancelled ? ' off' : '')}>
              <small>
                {WEEKDAYS[dow(s.date)]} {dm(s.date)} · {s.start}
              </small>
              <b>
                {s.subject} · Bài {s.lesson}
                {s.makeup ? ' (học bù)' : ''}
              </b>
              <span>{s.cancelled ?? `${s.room ?? 'Chưa xếp phòng'} · ${s.teacher ?? ''}`}</span>
              <Pill tone={tone}>{label}</Pill>
            </div>
          );
        })}
      </div>
      {changes.map((s) => (
        <Box key={s.id} icon="alert" tone="warn" title={s.cancelled ? `Buổi ${longDate(s.date).toLowerCase()} đã huỷ` : s.makeup ? `Học bù ${longDate(s.date).toLowerCase()} lúc ${s.start}${s.room ? ' tại ' + s.room : ''}` : `Buổi ${s.no} dời sang ${longDate(s.date).toLowerCase()} lúc ${s.start}`}>
          {s.cancelled ? 'Lý do: ' + s.cancelled : s.movedFrom ? `Lịch cũ: ${longDate(s.movedFrom.slice(0, 10)).toLowerCase()} lúc ${s.movedFrom.slice(11)}.` : 'Buổi học bù cho buổi đã huỷ.'}
        </Box>
      ))}
    </AppLayout>
  );
}
