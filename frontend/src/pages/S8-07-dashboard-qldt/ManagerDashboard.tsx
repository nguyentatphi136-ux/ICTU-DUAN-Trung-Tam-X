import { useState, type CSSProperties } from 'react';
import { Link } from 'react-router-dom';
import { ASSIGNMENTS, stateOf } from '../../data/assignments';
import { statOf, thresholdOf } from '../../data/attendance';
import { CLASSES, findClass, isOpen, totalSessions } from '../../data/classes';
import { FUNNEL, STAGES, TODAY } from '../../data/funnel';
import { viDate } from '../../data/leads';
import { STUDENTS, type Student } from '../../data/students';
import { debtOf, PLANS } from '../../data/tuition';
import { vnd } from '../../data/vnd';
import './dashboard.css';

/** Chuyên cần, bài chưa nộp, nợ quá hạn và mức rủi ro của một học viên đang học (S8-07). */
export function riskOf(s: Student) {
  const k = findClass(s.cls)!;
  const att = statOf(s.code, k.code, k.program, totalSessions(k));
  const missing = ASSIGNMENTS.filter((a) => a.cls === k.code && a.status === 'Đã giao' && a.due.slice(0, 10) < TODAY && stateOf(a, s.code) === 'Chưa nộp').length;
  const d = PLANS[s.code] ? debtOf(s.code) : null;
  const late = d ? d.items.filter((it) => it.owed && it.due < TODAY).reduce((n, it) => n + it.owed, 0) : 0;
  const min = 100 - thresholdOf(k.program);
  const reasons: string[] = [];
  if (att.over) reasons.push('Vượt ngưỡng vắng');
  else if (att.rate < min + 5 && att.held) reasons.push('Gần ngưỡng vắng');
  if (missing >= 2) reasons.push(`${missing} bài chưa nộp`);
  if (late) reasons.push('Nợ học phí');
  const score = (att.over ? 4 : att.rate < min + 5 && att.held ? 2 : 0) + Math.min(3, missing) + (late ? 2 + Math.min(2, (d?.overdue ?? 0) / 30) : 0);
  return { s, k, att, missing, late, overdue: d?.overdue ?? 0, reasons, score, level: score >= 6 ? 'Rất cao' : score >= 3 ? 'Cao' : '' };
}

const months = (n: number) =>
  Array.from({ length: n }, (_, i) => {
    const d = new Date(TODAY + 'T00:00:00');
    d.setMonth(d.getMonth() - (n - 1 - i));
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  });

// S8-07. Dashboard Quản lý đào tạo: bốn chỉ số, tuyển sinh theo tháng, phễu lead, học viên rủi ro cao.
export function ManagerDashboard() {
  const [all, setAll] = useState(false);
  const running = CLASSES.filter((k) => k.status === 'Đang học');
  const active = STUDENTS.filter((s) => {
    const k = findClass(s.cls);
    return s.status === 'Đang học' && !!k && isOpen(k);
  });
  const risks = active.map(riskOf);
  const rated = risks.filter((r) => r.att.held);
  const avgAtt = rated.length ? Math.round(rated.reduce((n, r) => n + r.att.rate, 0) / rated.length) : 100;
  const overdue = STUDENTS.filter((s) => PLANS[s.code] && s.status !== 'Thôi học').map((s) => debtOf(s.code)).filter((d) => d.overdue > 0);
  const lateSum = overdue.reduce((n, d) => n + d.items.filter((it) => it.owed && it.due < TODAY).reduce((m, it) => m + it.owed, 0), 0);
  const ms = months(6);
  const enrol = ms.map((m) => ({ m, n: STUDENTS.filter((s) => s.enrolledAt.startsWith(m)).length }));
  const max = Math.max(1, ...enrol.map((x) => x.n));
  // Phễu tích luỹ: lead ở bước sau đã đi qua các bước trước; lead từ chối chỉ tính ở bước Mới.
  const steps = STAGES.filter((s) => s !== 'Từ chối').map((st, i) => ({ st, n: FUNNEL.filter((l) => l.stage !== 'Từ chối' && STAGES.indexOf(l.stage) >= i).length + (i === 0 ? FUNNEL.filter((l) => l.stage === 'Từ chối').length : 0) }));
  const funnel = [...steps, { st: 'Đã nhập học', n: STUDENTS.filter((s) => s.leadId).length }];
  const top = risks.filter((r) => r.level).sort((a, b) => b.score - a.score);

  return (
    <>
      <div className="h">
        <div>
          <h2>Tổng quan đào tạo</h2>
          <p>Số liệu đến {viDate(TODAY)} {new Date().toTimeString().slice(0, 2)}:00 · cập nhật mỗi giờ</p>
        </div>
      </div>
      <div className="fe-stats db-stats">
        <div className="stat">
          <b>{running.length}</b>
          <span>Lớp đang chạy</span>
        </div>
        <div className="stat">
          <b>{active.length}</b>
          <span>Học viên đang học</span>
        </div>
        <div className={'stat ' + (avgAtt >= 85 ? 'ok' : 'bad')}>
          <b>{avgAtt}%</b>
          <span>Tỷ lệ chuyên cần trung bình</span>
        </div>
        <Link to="/hoc-phi/cong-no" className="stat bad">
          <b>{vnd(lateSum)}</b>
          <span>Công nợ quá hạn · {overdue.length} học viên</span>
        </Link>
      </div>
      <div className="db-grid">
        <section className="panel fe-pad">
          <div className="fe-head" style={{ padding: 0, marginBottom: 12 }}>
            <h3>Tuyển sinh theo tháng</h3>
            <span className="hint">học viên nhập học</span>
          </div>
          <div className="rv-chart db-chart">
            {enrol.map((x) => (
              <div key={x.m} className={'rv-col' + (x.m === TODAY.slice(0, 7) ? ' now' : '')}>
                <span>{x.n}</span>
                <i style={{ '--h': `${(100 * x.n) / max}%` } as CSSProperties} />
                <em>T{+x.m.slice(5)}</em>
              </div>
            ))}
          </div>
        </section>
        <section className="panel fe-pad">
          <h3>Phễu lead</h3>
          {funnel.map((f, i) => (
            <div key={f.st} className="db-step">
              <span>{f.st}</span>
              <i style={{ '--w': `${(100 * f.n) / Math.max(1, funnel[0].n)}%` } as CSSProperties}>
                <b>{f.n}</b>
              </i>
              <small className="hint">{i ? `${Math.round((100 * f.n) / Math.max(1, funnel[i - 1].n))}% từ bước trước` : ''}</small>
            </div>
          ))}
        </section>
      </div>
      <section className="panel" style={{ marginTop: 16 }}>
        <div className="fe-head">
          <h3>Học viên rủi ro cao</h3>
          {top.length > 3 && (
            <button type="button" className="link" onClick={() => setAll(!all)}>
              {all ? 'Thu gọn' : `Xem tất cả ${top.length} học viên`}
            </button>
          )}
        </div>
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Mức độ</th>
                <th>Học viên</th>
                <th>Lớp</th>
                <th>Chuyên cần</th>
                <th>Bài chưa nộp</th>
                <th>Nợ quá hạn</th>
                <th>Lý do chính</th>
              </tr>
            </thead>
            <tbody>
              {(all ? top : top.slice(0, 3)).map((r) => (
                <tr key={r.s.code}>
                  <td>
                    <span className={'fe-late' + (r.level === 'Rất cao' ? ' bad' : '')}>{r.level}</span>
                  </td>
                  <td>
                    <Link to={`/hoc-vien/${r.s.code}`} className="strong">
                      {r.s.name}
                    </Link>
                  </td>
                  <td>{r.k.code}</td>
                  <td className={r.att.over ? 'bad-t' : r.reasons.includes('Gần ngưỡng vắng') ? 'warn-t' : undefined}>{r.att.held ? `${r.att.rate}%` : '–'}</td>
                  <td>{r.missing ? `${r.missing} bài` : '–'}</td>
                  <td>{r.late ? `${vnd(r.late)} · ${r.overdue} ngày` : '–'}</td>
                  <td className="hint">{r.reasons.join(', ')}</td>
                </tr>
              ))}
              {!top.length && (
                <tr>
                  <td colSpan={7} className="empty">
                    Chưa có học viên rủi ro cao.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </>
  );
}
