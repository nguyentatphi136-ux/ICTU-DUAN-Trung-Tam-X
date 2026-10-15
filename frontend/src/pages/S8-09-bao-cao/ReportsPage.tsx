import { useState, type CSSProperties, type ReactNode } from 'react';
import { Navigate, NavLink, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { ASSIGNMENTS, SUBMISSIONS } from '../../data/assignments';
import { statOf } from '../../data/attendance';
import { useAuth } from '../../data/auth';
import { CLASSES, classesOf, mainTeacher, STAFF, totalSessions, type ClassStatus, type Klass } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { finalOf, gkey, LOCKS } from '../../data/grades';
import { LEADS, SOURCES, viDate, type Lead } from '../../data/leads';
import { STUDENTS } from '../../data/students';
import { RUNS } from '../../data/surveys';
import { daysBetween } from '../../data/tuition';
import { download, toXlsx, type Cell } from '../../data/xlsx';
import './reports.css';

export const MODULE = 'Báo cáo & dashboard';

type Report = 'lop-hoc' | 'giang-vien' | 'tuyen-sinh';
const NAMES: Record<Report, string> = { 'lop-hoc': 'Lớp học', 'giang-vien': 'Giảng viên', 'tuyen-sinh': 'Tuyển sinh' };

/** Báo cáo mỗi vai trò được xem. Giảng viên chỉ thấy lớp mình dạy, Tư vấn tuyển sinh chỉ thấy lead mình phụ trách (quyền R*). */
const ALLOWED: Record<number, Report[]> = { 2: ['lop-hoc'], 3: ['tuyen-sinh'], 5: ['lop-hoc', 'giang-vien', 'tuyen-sinh'], 6: ['lop-hoc', 'giang-vien', 'tuyen-sinh'] };

const avg = (xs: number[]) => (xs.length ? xs.reduce((a, b) => a + b, 0) / xs.length : null);
const pct = (a: number, b: number) => (b ? Math.round((100 * a) / b) : null);
const r1 = (n: number | null) => (n == null ? null : Math.round(n * 10) / 10);

// ---------- Số liệu theo lớp ----------
/** Chuyên cần, nộp bài đúng hạn, tỷ lệ đạt các môn đã chốt điểm và điểm khảo sát gần nhất của một lớp. */
function classReport(k: Klass) {
  const roster = STUDENTS.filter((s) => s.cls === k.code && s.status !== 'Thôi học');
  const att = roster.map((s) => statOf(s.code, k.code, k.program, totalSessions(k))).filter((a) => a.held);
  const due = ASSIGNMENTS.filter((a) => a.cls === k.code && a.status === 'Đã giao' && a.due.slice(0, 10) < TODAY);
  let onTime = 0;
  for (const a of due)
    for (const s of roster) {
      const sub = SUBMISSIONS.find((x) => x.assignment === a.id && x.student === s.code);
      if (sub?.versions.some((v) => v.at <= a.due)) onTime++;
    }
  const results = k.subjects.filter((sub) => LOCKS[gkey(k.code, sub.code)]).flatMap((sub) => roster.map((s) => finalOf(k.code, sub.code, s.code).result));
  const run = RUNS.filter((r) => r.cls === k.code).sort((a, b) => b.id - a.id)[0];
  const scores = run ? run.answers.flatMap((a) => Object.values(a.scores)) : [];
  return {
    k,
    size: roster.length,
    teacher: mainTeacher(k) ?? '–',
    attendance: att.length ? Math.round(avg(att.map((a) => a.rate))!) : null,
    onTime: pct(onTime, due.length * roster.length),
    pass: pct(results.filter((r) => r === 'Đạt').length, results.length),
    csat: r1(avg(scores)),
    replies: run ? `${run.responded.length}/${run.size}` : '',
  };
}

// ---------- Số liệu theo giảng viên ----------
function teacherReport(name: string) {
  const ks = CLASSES.filter((k) => k.subjects.some((s) => s.teacher === name));
  const running = ks.filter((k) => k.status === 'Đang học');
  const subjects = new Set(ks.flatMap((k) => k.subjects.filter((s) => s.teacher === name).map((s) => gkey(k.code, s.code))));
  const mine = ASSIGNMENTS.filter((a) => subjects.has(gkey(a.cls, a.subject))).map((a) => a.id);
  const subs = SUBMISSIONS.filter((s) => mine.includes(s.assignment));
  const waits = subs.filter((s) => s.grade && s.state === 'Đã chấm').map((s) => daysBetween(s.versions[0].at.slice(0, 10), s.grade!.at.slice(0, 10)));
  // Câu q1 "Giảng viên giải thích bài dễ hiểu", tính cho giảng viên chính của lớp như S8-06.
  const q1 = RUNS.filter((r) => ks.some((k) => k.code === r.cls && mainTeacher(k) === name)).flatMap((r) => r.answers.map((a) => a.scores.q1).filter((v) => v != null));
  return {
    name,
    running: running.length,
    ended: ks.filter((k) => k.status === 'Kết thúc').length,
    attendance: avg(running.map((k) => classReport(k).attendance).filter((v): v is number => v != null)),
    pending: subs.filter((s) => s.state === 'Đã nộp').length,
    gradeDays: r1(avg(waits)),
    q1: r1(avg(q1)),
  };
}

// ---------- Số liệu tuyển sinh ----------
const FOLLOWING = (l: Lead) => l.status === 'Đang chăm sóc' || l.status === 'Hẹn gọi lại';
function leadGroup(name: string, ls: Lead[]) {
  const won = ls.filter((l) => l.status === 'Đã nhập học').length;
  return { name, total: ls.length, fresh: ls.filter((l) => l.status === 'Mới').length, following: ls.filter(FOLLOWING).length, won, lost: ls.filter((l) => l.status === 'Không quan tâm').length, rate: pct(won, ls.length) };
}

/** Ba tháng gần nhất, tính tới tháng hiện tại. */
const lastMonths = () =>
  [2, 1, 0].map((back) => {
    const d = new Date(TODAY + 'T00:00:00');
    d.setDate(1);
    d.setMonth(d.getMonth() - back);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  });

// ---------- Thành phần hiển thị ----------
type Tone = 'ok' | 'warn' | 'bad';
/** Ô chỉ số: con số kèm thanh đo mảnh, tô theo ngưỡng. lower = số càng nhỏ càng tốt (bài chờ chấm, số ngày chấm). */
function Meter({ v, max = 100, unit = '%', good, fair, lower, digits = 0 }: { v: number | null; max?: number; unit?: string; good: number; fair: number; lower?: boolean; digits?: number }) {
  if (v == null) return <span className="hint">–</span>;
  const tone: Tone = lower ? (v <= good ? 'ok' : v <= fair ? 'warn' : 'bad') : v >= good ? 'ok' : v >= fair ? 'warn' : 'bad';
  return (
    <span className={'bc-m ' + tone} style={{ '--p': `${Math.min(100, (100 * v) / max)}%` } as CSSProperties}>
      <b>
        {v.toLocaleString('vi-VN', { minimumFractionDigits: digits, maximumFractionDigits: digits })}
        {unit}
      </b>
      <i />
    </span>
  );
}

function Summary({ items }: { items: [ReactNode, string, string?][] }) {
  return (
    <div className="panel bc-sum">
      {items.map(([v, label, note]) => (
        <div key={label}>
          <b>{v}</b>
          <span>{label}</span>
          {note && <small>{note}</small>}
        </div>
      ))}
    </div>
  );
}

function Seg<T extends string>({ value, options, onChange, label }: { value: T; options: [T, string][]; onChange: (v: T) => void; label: string }) {
  return (
    <div className="seg" role="radiogroup" aria-label={label}>
      {options.map(([k, l]) => (
        <button key={k} type="button" role="radio" aria-checked={value === k} className={value === k ? 'on' : undefined} onClick={() => onChange(k)}>
          {l}
        </button>
      ))}
    </div>
  );
}

const fmt = (n: number | null, d = 0) => (n == null ? '–' : n.toLocaleString('vi-VN', { minimumFractionDigits: d, maximumFractionDigits: d }));

// S8-09. Báo cáo theo vai trò: Lớp học (Giảng viên: lớp mình dạy), Giảng viên, Tuyển sinh (Tư vấn tuyển sinh: lead mình phụ trách).
// Quản lý đào tạo và Quản trị xem cả ba trên toàn trung tâm. Kế toán dùng Báo cáo doanh thu (S7-08).
// Khi tích hợp: GET /reports/classes, /reports/teachers, /reports/admissions?month=, máy chủ lọc theo vai trò.
export function ReportsPage() {
  const { user } = useAuth();
  const report = useParams().report as Report | undefined;
  if (user?.active === 4) return <Navigate to="/hoc-phi/bao-cao" replace />;
  const allowed = user ? (ALLOWED[user.active] ?? []) : [];
  if (allowed.length && (!report || !allowed.includes(report))) return <Navigate to={`/bao-cao/${allowed[0]}`} replace />;
  const scoped = user?.active === 2 || user?.active === 3;

  return (
    <AppLayout crumb={[MODULE, report ? `Báo cáo ${NAMES[report].toLowerCase()}` : 'Báo cáo']} module={MODULE}>
      {user && report && (
        <>
          {allowed.length > 1 && (
            <nav className="tabs">
              {allowed.map((r) => (
                <NavLink key={r} to={`/bao-cao/${r}`} className={({ isActive }) => (isActive ? 'on' : undefined)}>
                  {NAMES[r]}
                </NavLink>
              ))}
            </nav>
          )}
          {report === 'lop-hoc' && <ClassReport classes={scoped ? classesOf(user.name) : CLASSES} scope={scoped ? 'Các lớp bạn dạy hoặc trợ giảng' : 'Toàn trung tâm'} />}
          {report === 'giang-vien' && <TeacherReport />}
          {report === 'tuyen-sinh' && <AdmissionReport leads={scoped ? LEADS.filter((l) => l.owner === user.name) : LEADS} mine={scoped} />}
        </>
      )}
    </AppLayout>
  );
}

function Head({ title, sub, onExport }: { title: string; sub: string; onExport?: () => void }) {
  return (
    <div className="h">
      <div>
        <h2>{title}</h2>
        <p>{sub}</p>
      </div>
      {onExport && (
        <button type="button" className="btn" onClick={onExport}>
          <Icon name="dl" size={16} />
          Xuất Excel
        </button>
      )}
    </div>
  );
}

const STATUS_FILTER: [string, string][] = [
  ['Đang học', 'Đang học'],
  ['Kết thúc', 'Đã kết thúc'],
  ['all', 'Tất cả'],
];

function ClassReport({ classes, scope }: { classes: Klass[]; scope: string }) {
  const [status, setStatus] = useState('Đang học');
  const rows = classes.filter((k) => k.status !== 'Huỷ' && (status === 'all' ? k.status !== 'Chuẩn bị' : k.status === (status as ClassStatus))).map(classReport);
  const mean = (key: 'attendance' | 'onTime' | 'pass' | 'csat') => avg(rows.map((r) => r[key]).filter((v): v is number => v != null));

  function exportXlsx() {
    const head: Cell[] = ['Lớp', 'Chương trình', 'Giảng viên chính', 'Sĩ số', 'Tối đa', 'Chuyên cần TB (%)', 'Nộp bài đúng hạn (%)', 'Tỷ lệ đạt (%)', 'Hài lòng (/5)'];
    const body = rows.map((r): Cell[] => [r.k.code, r.k.program, r.teacher, r.size, r.k.max, r.attendance, r.onTime, r.pass, r.csat]);
    download(toXlsx([[`Báo cáo lớp học đến ${viDate(TODAY)}`], [], head, ...body], 'Lớp học'), `bao-cao-lop-hoc-${TODAY}.xlsx`);
  }

  return (
    <>
      <Head title="Báo cáo lớp học" sub={`${scope}, số liệu đến ${viDate(TODAY)}`} onExport={rows.length ? exportXlsx : undefined} />
      <div className="bar">
        <Seg label="Trạng thái lớp" value={status} options={STATUS_FILTER} onChange={setStatus} />
      </div>
      <Summary
        items={[
          [rows.length, 'Lớp', `${rows.reduce((n, r) => n + r.size, 0)} học viên`],
          [`${fmt(mean('attendance'))}%`, 'Chuyên cần trung bình'],
          [`${fmt(mean('onTime'))}%`, 'Nộp bài đúng hạn'],
          [`${fmt(mean('pass'))}%`, 'Đạt ở các môn đã chốt điểm'],
          [fmt(mean('csat'), 1), 'Điểm hài lòng', 'trên thang 5'],
        ]}
      />
      <section className="panel">
        <div className="scroll">
          <table className="bc-table">
            <thead>
              <tr>
                <th>Lớp</th>
                <th>Giảng viên chính</th>
                <th>Sĩ số</th>
                <th>Chuyên cần</th>
                <th>Nộp đúng hạn</th>
                <th>Tỷ lệ đạt</th>
                <th>Hài lòng</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.k.code}>
                  <td>
                    <b className="bc-name">{r.k.code}</b>
                    <small className="hint">{r.k.program}</small>
                  </td>
                  <td className="hint">{r.teacher}</td>
                  <td>
                    <span className="bc-fill" style={{ '--p': `${Math.min(100, (100 * r.size) / r.k.max)}%` } as CSSProperties}>
                      <b>
                        {r.size}/{r.k.max}
                      </b>
                      <i />
                    </span>
                  </td>
                  <td>
                    <Meter v={r.attendance} good={85} fair={75} />
                  </td>
                  <td>
                    <Meter v={r.onTime} good={80} fair={60} />
                  </td>
                  <td>
                    <Meter v={r.pass} good={80} fair={60} />
                  </td>
                  <td>
                    <Meter v={r.csat} max={5} unit="" good={4} fair={3.5} digits={1} />
                    {r.replies && <small className="hint bc-sub">{r.replies} phiếu</small>}
                  </td>
                </tr>
              ))}
              {!rows.length && (
                <tr>
                  <td colSpan={7} className="empty">
                    Không có lớp nào ở trạng thái này.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
      <p className="hint bc-legend">
        <span className="bc-dot ok" /> Đạt mục tiêu <span className="bc-dot warn" /> Cần theo dõi <span className="bc-dot bad" /> Dưới mức chấp nhận. Mục tiêu: chuyên cần từ 85%, nộp đúng hạn và tỷ lệ đạt từ 80%, hài lòng từ 4/5. Ô trống là lớp chưa có số liệu.
      </p>
    </>
  );
}

function TeacherReport() {
  const rows = STAFF.filter((s) => s.role === 2).map((s) => teacherReport(s.name));

  function exportXlsx() {
    const head: Cell[] = ['Giảng viên', 'Lớp đang dạy', 'Lớp đã dạy xong', 'Chuyên cần TB lớp đang dạy (%)', 'Bài chờ chấm', 'Số ngày chấm TB', 'Giảng dễ hiểu (/5)'];
    const body = rows.map((r): Cell[] => [r.name, r.running, r.ended, r.attendance == null ? null : Math.round(r.attendance), r.pending, r.gradeDays, r.q1]);
    download(toXlsx([[`Báo cáo giảng viên đến ${viDate(TODAY)}`], [], head, ...body], 'Giảng viên'), `bao-cao-giang-vien-${TODAY}.xlsx`);
  }

  return (
    <>
      <Head title="Báo cáo giảng viên" sub={`Toàn trung tâm, số liệu đến ${viDate(TODAY)}`} onExport={exportXlsx} />
      <Summary
        items={[
          [rows.length, 'Giảng viên', `${rows.filter((r) => r.running).length} người đang có lớp`],
          [rows.reduce((n, r) => n + r.pending, 0), 'Bài đang chờ chấm'],
          [`${fmt(avg(rows.map((r) => r.gradeDays).filter((v): v is number => v != null)), 1)} ngày`, 'Thời gian chấm trung bình', 'từ lúc nộp tới lúc có điểm'],
          [fmt(avg(rows.map((r) => r.q1).filter((v): v is number => v != null)), 1), 'Giảng dễ hiểu', 'điểm khảo sát trên thang 5'],
        ]}
      />
      <section className="panel">
        <div className="scroll">
          <table className="bc-table">
            <thead>
              <tr>
                <th>Giảng viên</th>
                <th>Lớp đang dạy</th>
                <th>Chuyên cần lớp</th>
                <th>Bài chờ chấm</th>
                <th>Số ngày chấm</th>
                <th>Giảng dễ hiểu</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.name}>
                  <td>
                    <b className="bc-name">{r.name}</b>
                    <small className="hint">{r.ended ? `${r.ended} lớp đã dạy xong` : 'Chưa có lớp kết thúc'}</small>
                  </td>
                  <td className="strong">{r.running || <span className="hint">–</span>}</td>
                  <td>
                    <Meter v={r.attendance == null ? null : Math.round(r.attendance)} good={85} fair={75} />
                  </td>
                  <td>{r.pending ? <Meter v={r.pending} max={40} unit=" bài" good={5} fair={15} lower /> : <span className="hint">Không còn</span>}</td>
                  <td>
                    <Meter v={r.gradeDays} max={10} unit=" ngày" good={3} fair={7} lower digits={1} />
                  </td>
                  <td>
                    <Meter v={r.q1} max={5} unit="" good={4} fair={3.5} digits={1} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <p className="hint bc-legend">
        <span className="bc-dot ok" /> Đạt mục tiêu <span className="bc-dot warn" /> Cần theo dõi <span className="bc-dot bad" /> Dưới mức chấp nhận. Mục tiêu: chấm xong trong 3 ngày, không để quá 5 bài chờ chấm, điểm giảng dễ hiểu từ 4/5.
      </p>
    </>
  );
}

function AdmissionReport({ leads, mine }: { leads: Lead[]; mine: boolean }) {
  const months = lastMonths();
  const [month, setMonth] = useState('all');
  const [by, setBy] = useState<'owner' | 'program'>(mine ? 'program' : 'owner');
  const ls = month === 'all' ? leads.filter((l) => l.createdAt >= months[0]) : leads.filter((l) => l.createdAt.startsWith(month));
  const total = leadGroup('Tổng', ls);
  const keyOf = (l: Lead) => (by === 'owner' ? l.owner || 'Chưa phân công' : l.program);
  const groups = [...new Set(ls.map(keyOf))].map((k) => leadGroup(k, ls.filter((l) => keyOf(l) === k))).sort((a, b) => b.total - a.total);
  const sources = SOURCES.map((s) => leadGroup(s, ls.filter((l) => l.source === s)));
  const top = Math.max(1, ...sources.map((s) => s.total));
  const period = month === 'all' ? `từ tháng ${+months[0].slice(5)} đến tháng ${+months[2].slice(5)}/${months[2].slice(0, 4)}` : `tháng ${+month.slice(5)}/${month.slice(0, 4)}`;
  const label = by === 'owner' ? 'Tư vấn viên' : 'Chương trình';

  function exportXlsx() {
    const head: Cell[] = [label, 'Lead', 'Mới', 'Đang theo', 'Đã nhập học', 'Không quan tâm', 'Tỷ lệ chuyển đổi (%)'];
    const row = (g: ReturnType<typeof leadGroup>): Cell[] => [g.name, g.total, g.fresh, g.following, g.won, g.lost, g.rate];
    download(toXlsx([[`Báo cáo tuyển sinh ${period}`], [], head, ...groups.map(row), row(total)], 'Tuyển sinh'), `bao-cao-tuyen-sinh-${month}.xlsx`);
  }

  return (
    <>
      <Head title="Báo cáo tuyển sinh" sub={`${mine ? 'Lead bạn phụ trách' : 'Toàn trung tâm'}, ${period}`} onExport={ls.length ? exportXlsx : undefined} />
      <div className="bar bc-bar">
        <Seg label="Thời gian" value={month} options={[...months.map((m): [string, string] => [m, `Tháng ${+m.slice(5)}`]), ['all', '3 tháng']]} onChange={setMonth} />
        {!mine && (
          <>
            <span className="hint">Nhóm theo</span>
            <Seg
              label="Nhóm theo"
              value={by}
              options={[
                ['owner', 'Tư vấn viên'],
                ['program', 'Chương trình'],
              ]}
              onChange={setBy}
            />
          </>
        )}
      </div>
      <Summary
        items={[
          [total.total, 'Lead trong kỳ', mine ? undefined : `${ls.filter((l) => !l.owner).length} chưa phân công`],
          [total.following, 'Đang theo', 'đang chăm sóc hoặc hẹn gọi lại'],
          [total.won, 'Đã nhập học'],
          [`${fmt(total.rate)}%`, 'Tỷ lệ chuyển đổi', 'lead đã nhập học trên tổng lead'],
        ]}
      />
      <div className="bc-grid">
        <section className="panel fe-pad">
          <h3 className="bc-h">Lead theo nguồn</h3>
          {sources.map((s) => (
            <div key={s.name} className="bc-src">
              <span>{s.name}</span>
              <div className="bc-stack" style={{ '--w': `${(100 * s.total) / top}%` } as CSSProperties} aria-label={`${s.total} lead, ${s.won} đã nhập học`}>
                {(
                  [
                    ['won', s.won],
                    ['following', s.following],
                    ['fresh', s.fresh],
                    ['lost', s.lost],
                  ] as const
                ).map(([k, n]) => n > 0 && <i key={k} className={k} style={{ flexGrow: n }} />)}
              </div>
              <b>{s.total}</b>
              <small className="hint">{s.total ? `${s.rate}% nhập học` : ''}</small>
            </div>
          ))}
          <p className="hint bc-legend">
            <span className="bc-dot won" /> Đã nhập học <span className="bc-dot following" /> Đang theo <span className="bc-dot fresh" /> Mới <span className="bc-dot lost" /> Không quan tâm
          </p>
        </section>
        <section className="panel">
          <div className="scroll">
            <table className="bc-table">
              <thead>
                <tr>
                  <th>{label}</th>
                  <th className="num">Lead</th>
                  <th className="num">Đang theo</th>
                  <th className="num">Nhập học</th>
                  <th>Chuyển đổi</th>
                </tr>
              </thead>
              <tbody>
                {groups.map((g) => (
                  <tr key={g.name}>
                    <td className={g.name === 'Chưa phân công' ? 'warn-t' : 'strong'}>{g.name}</td>
                    <td className="num">{g.total}</td>
                    <td className="num">{g.following}</td>
                    <td className="num strong">{g.won}</td>
                    <td>
                      <Meter v={g.rate} good={20} fair={10} />
                    </td>
                  </tr>
                ))}
                {!groups.length && (
                  <tr>
                    <td colSpan={5} className="empty">
                      Không có lead nào trong khoảng thời gian này.
                    </td>
                  </tr>
                )}
              </tbody>
              {groups.length > 1 && (
                <tfoot>
                  <tr className="fe-total">
                    <td>Tổng cộng</td>
                    <td className="num">{total.total}</td>
                    <td className="num">{total.following}</td>
                    <td className="num">{total.won}</td>
                    <td>{fmt(total.rate)}%</td>
                  </tr>
                </tfoot>
              )}
            </table>
          </div>
        </section>
      </div>
      <p className="hint bc-legend">Mục tiêu tỷ lệ chuyển đổi từ 20%. Lead tính theo ngày tạo, trạng thái lấy tại thời điểm xem báo cáo.</p>
    </>
  );
}
