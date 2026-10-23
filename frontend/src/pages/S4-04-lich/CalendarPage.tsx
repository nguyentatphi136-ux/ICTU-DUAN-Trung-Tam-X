import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Filter } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { CLASSES, classesOf, isOpen, isTeacherMatch, STAFF } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { ROOMS } from '../../data/rooms';
import { addDays, clashes, dm, dow, findSession, longDate, mondayOf, SESSIONS, shiftOf, SHIFTS, WEEKDAYS, type ClassSession, type Shift } from '../../data/schedule';
import { STUDENTS } from '../../data/students';
import { canManageClasses, MODULE } from '../S3-07-danh-sach-lop/ClassListPage';
import { SessionDrawer } from './SessionDrawer';
import './calendar.css';

const SHIFT_LIST = Object.keys(SHIFTS) as Shift[];
const MONTH_DOW = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ nhật'];

/** Màu theo nhóm chương trình để nhìn lịch phân biệt nhanh. */
export const tint = (cls: string) => (cls.startsWith('UIUX') ? 'amber' : cls.startsWith('JAVA') ? 'green' : cls.startsWith('TEST') ? 'violet' : 'blue');

// S4-04. Thời khoá biểu dạng lịch tuần (chia ca Sáng, Chiều, Tối) và tháng. Lọc theo lớp, giảng viên, phòng.
// Ô trống là khoảng trống; buổi trùng giảng viên hoặc phòng tô đỏ. Bấm một buổi để mở chi tiết. Trên điện thoại,
// lịch tuần thành dải ngày và danh sách buổi theo ngày.
export function CalendarPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const role = user?.active ?? -1;
  const [mode, setMode] = useState<'week' | 'month'>('week');
  const [anchor, setAnchor] = useState(TODAY);
  const [cls, setCls] = useState('');
  const [teacher, setTeacher] = useState('');
  const [room, setRoom] = useState('');
  const [day, setDay] = useState(TODAY);
  const [, setTick] = useState(0);
  const open = params.get('buoi');
  const openSession = open ? findSession(open) : undefined;

  // Giảng viên, trợ giảng chỉ thấy lớp mình dạy; học viên chỉ thấy lớp mình học.
  const mine = role === 1 || role === 2 ? new Set(classesOf(user!.name).map((k) => k.code)) : role === 0 ? new Set(STUDENTS.filter((s) => s.login === user!.email).map((s) => s.cls)) : null;
  const pool = SESSIONS.filter((s) => (!mine || mine.has(s.cls)) && (!cls || s.cls === cls) && (!teacher || isTeacherMatch(s.teacher, teacher)) && (!room || s.room === room));

  const monday = mondayOf(anchor);
  const week = [...Array(7)].map((_, i) => addDays(monday, i));
  const monthStart = anchor.slice(0, 8) + '01';
  const gridStart = mondayOf(monthStart);
  const month = [...Array(42)].map((_, i) => addDays(gridStart, i));
  const range = mode === 'week' ? week : month.filter((x) => x.slice(0, 7) === anchor.slice(0, 7));
  const inRange = pool.filter((s) => s.date >= range[0] && s.date <= range.at(-1)!);
  const clashOf = (s: ClassSession) => clashes(s);
  const nClash = inRange.filter((s) => clashOf(s).length).length;
  const byDay = (x: string) => pool.filter((s) => s.date === x).sort((a, b) => a.start.localeCompare(b.start));

  const step = (n: number) => {
    if (mode === 'week') setAnchor(addDays(anchor, 7 * n));
    else {
      const x = new Date(anchor + 'T00:00:00');
      x.setMonth(x.getMonth() + n, 1);
      setAnchor(`${x.getFullYear()}-${String(x.getMonth() + 1).padStart(2, '0')}-01`);
    }
  };
  const label = mode === 'week' ? `${dm(week[0])} – ${dm(week[6])}/${week[6].slice(0, 4)}` : `Tháng ${Number(anchor.slice(5, 7))}/${anchor.slice(0, 4)}`;
  const openDetail = (s: ClassSession) => setParams({ buoi: s.id });

  return (
    <AppLayout crumb={[MODULE, 'Thời khoá biểu']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Thời khoá biểu</h2>
          <p>
            {mode === 'week' ? 'Tuần ' : ''}
            {label} · {inRange.filter((s) => !s.cancelled).length} buổi · {nClash} xung đột
          </p>
        </div>
        {canManageClasses(role) && <GenerateButton onPick={(c) => navigate(`/lop-hoc/${c}/sinh-lich`)} />}
      </div>
      <div className="bar cal-bar">
        <div className="seg" role="tablist" aria-label="Chế độ xem">
          <button type="button" role="tab" aria-selected={mode === 'week'} className={mode === 'week' ? 'on' : undefined} onClick={() => setMode('week')}>
            Tuần
          </button>
          <button type="button" role="tab" aria-selected={mode === 'month'} className={mode === 'month' ? 'on' : undefined} onClick={() => setMode('month')}>
            Tháng
          </button>
        </div>
        <div className="cal-nav">
          <button type="button" className="icon-btn" aria-label="Trước" onClick={() => step(-1)}>
            <Icon name="back" size={16} />
          </button>
          <b>{label}</b>
          <button type="button" className="icon-btn" aria-label="Sau" onClick={() => step(1)}>
            <Icon name="go" size={16} />
          </button>
        </div>
        <button type="button" className="btn" onClick={() => (setAnchor(TODAY), setDay(TODAY))}>
          Hôm nay
        </button>
        <span className="cal-gap" />
        {!mine && (
          <>
            <Filter label="Lớp" value={cls} options={CLASSES.filter(isOpen).map((k) => k.code)} onChange={setCls} width={170} />
            <Filter label="Giảng viên" value={teacher} options={STAFF.filter((s) => s.role === 2).map((s) => s.name)} onChange={setTeacher} width={240} />
            <Filter label="Phòng" value={room} options={ROOMS.map((r) => r.name)} onChange={setRoom} width={170} />
          </>
        )}
      </div>

      {mode === 'week' ? (
        <>
          <div className="panel cal-week">
            <div className="cal-head">
              <span />
              {week.map((x) => (
                <span key={x} className={(x === TODAY ? 'today ' : '') + (dow(x) === 0 ? 'sun' : '')}>
                  {WEEKDAYS[dow(x)]} {dm(x)}
                </span>
              ))}
            </div>
            {SHIFT_LIST.map((sh) => (
              <div key={sh} className="cal-row">
                <div className="cal-shift">
                  <b>{sh}</b>
                  <span>{SHIFTS[sh][0]}</span>
                </div>
                {week.map((x) => (
                  <div key={x} className="cal-cell">
                    {byDay(x)
                      .filter((s) => shiftOf(s.start) === sh)
                      .map((s) => (
                        <SessionChip key={s.id} s={s} clash={clashOf(s)[0]?.kind} onOpen={() => openDetail(s)} />
                      ))}
                  </div>
                ))}
              </div>
            ))}
          </div>
          <div className="cal-mobile">
            <div className="cal-strip">
              {week.map((x) => (
                <button key={x} type="button" className={x === day ? 'on' : undefined} onClick={() => setDay(x)}>
                  <small>{WEEKDAYS[dow(x)]}</small>
                  <b>{x.slice(8)}</b>
                </button>
              ))}
            </div>
            {week
              .filter((x) => x >= day)
              .map((x) => {
                const list = byDay(x);
                if (!list.length && x !== day) return null;
                return (
                  <section key={x} className="cal-day">
                    <h3>
                      {longDate(x)} <span>{list.length} buổi</span>
                    </h3>
                    {list.map((s) => (
                      <MobileItem key={s.id} s={s} onOpen={() => openDetail(s)} />
                    ))}
                    {!list.length && <p className="hint">Không có buổi học.</p>}
                  </section>
                );
              })}
          </div>
        </>
      ) : (
        <div className="panel cal-month">
          <div className="cal-mhead">
            {MONTH_DOW.map((x) => (
              <span key={x}>{x}</span>
            ))}
          </div>
          <div className="cal-mgrid">
            {month.map((x) => {
              const list = byDay(x);
              const out = x.slice(0, 7) !== anchor.slice(0, 7);
              return (
                <button
                  key={x}
                  type="button"
                  className={'cal-mday' + (out ? ' out' : '') + (x === TODAY ? ' today' : '')}
                  onClick={() => {
                    setAnchor(x);
                    setDay(x);
                    setMode('week');
                  }}
                >
                  <b>{out ? `${Number(x.slice(8))}/${Number(x.slice(5, 7))}` : Number(x.slice(8))}</b>
                  {list.slice(0, 3).map((s) => {
                    const c = clashOf(s)[0];
                    return (
                      <span key={s.id} className={'ev ' + (s.cancelled ? 'off' : c ? 'red' : tint(s.cls))}>
                        {s.cls} · {c ? c.kind.replace('giảng viên', 'GV') : shiftOf(s.start)}
                      </span>
                    );
                  })}
                  {list.length > 3 && <small>và {list.length - 3} buổi</small>}
                </button>
              );
            })}
          </div>
        </div>
      )}
      <div className="cal-legend">
        <span className="blue">Buổi học</span>
        <span className="red">Đang xung đột</span>
        <span className="off">Đã huỷ</span>
        <em>{mode === 'week' ? 'Bấm vào một buổi để xem chi tiết' : 'Bấm vào một ngày để xem tuần đó'}</em>
      </div>
      {openSession && (
        <SessionDrawer
          s={openSession}
          onClose={() => setParams({})}
          onChange={() => setTick((t) => t + 1)}
        />
      )}
    </AppLayout>
  );
}

function SessionChip({ s, clash, onOpen }: { s: ClassSession; clash?: string; onOpen: () => void }) {
  return (
    <button type="button" className={'ev ' + (s.cancelled ? 'off' : clash ? 'red' : tint(s.cls))} onClick={onOpen}>
      <b>
        {s.cls} · {s.makeup ? 'Học bù' : 'B' + s.no}
      </b>
      <span>{s.cancelled ? 'Đã huỷ' : clash ? `${clash.replace('giảng viên', 'GV')} · ${clash === 'Trùng phòng' ? s.room : s.teacher}` : `${s.room ?? 'Chưa xếp phòng'} · ${s.teacher ?? 'Chưa có GV'}`}</span>
    </button>
  );
}

function MobileItem({ s, onOpen }: { s: ClassSession; onOpen: () => void }) {
  const c = clashes(s);
  return (
    <button type="button" className={'cal-item' + (s.cancelled ? ' off' : '')} onClick={onOpen}>
      <small>
        {s.start} – {s.end}
      </small>
      <b>
        {s.cls} · {s.makeup ? 'Học bù' : 'Buổi ' + s.no}
      </b>
      <span>
        {s.room ?? 'Chưa xếp phòng'} · {s.teacher ?? 'Chưa có giảng viên'}
      </span>
      {c.map((x, i) => (
        <em key={i}>
          {x.kind} với {x.other.cls} buổi {x.other.no}
        </em>
      ))}
      {s.cancelled && <em>Đã huỷ: {s.cancelled}</em>}
    </button>
  );
}

function GenerateButton({ onPick }: { onPick: (code: string) => void }) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return;
    const f = (e: MouseEvent) => !ref.current?.contains(e.target as Node) && setOpen(false);
    document.addEventListener('mousedown', f);
    return () => document.removeEventListener('mousedown', f);
  }, [open]);
  return (
    <div className="rowmenu" ref={ref}>
      <button type="button" className="btn primary" aria-expanded={open} onClick={() => setOpen((o) => !o)}>
        <Icon name="plus" size={16} />
        Sinh thời khoá biểu
      </button>
      {open && (
        <div className="menu" role="menu">
          <small className="cal-pick">Chọn lớp cần sinh lịch</small>
          {CLASSES.filter(isOpen).map((k) => (
            <button key={k.code} type="button" role="menuitem" onClick={() => onPick(k.code)}>
              {k.code}
              <small>
                {k.program} · {SESSIONS.some((s) => s.cls === k.code) ? 'đã có lịch' : 'chưa có lịch'}
              </small>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

