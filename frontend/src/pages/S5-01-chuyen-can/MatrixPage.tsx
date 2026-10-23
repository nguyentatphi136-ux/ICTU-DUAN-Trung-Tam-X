import { useState, type ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { heldSessions, MARKS, markTone, sheetOf, statOf, thresholdOf, type AttStat, type Mark } from '../../data/attendance';
import { useAuth } from '../../data/auth';
import { classesOf, findClass, totalSessions } from '../../data/classes';
import { longDate } from '../../data/schedule';
import { STUDENTS, type Student } from '../../data/students';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import { MODULE } from '../S4-07-diem-danh/AttendancePage';
import './matrix.css';

export type Row = { s: Student; stat: AttStat };

/** Học viên từng điểm danh ở lớp (kể cả người đã chuyển đi) kèm chuyên cần. */
export function classRows(cls: string): Row[] {
  const k = findClass(cls)!;
  const seen = new Set(heldSessions(cls).flatMap((x) => Object.keys(sheetOf(x)?.marks ?? {})));
  const total = totalSessions(k);
  return STUDENTS.filter((s) => seen.has(s.code) || s.cls === cls).map((s) => ({ s, stat: statOf(s.code, cls, k.program, total) }));
}

type Props = {
  /** Nút thêm ở đầu trang (S5-04 Xuất Excel). */
  head?: (rows: Row[], filtered: Row[]) => ReactNode;
};

// S5-01. Bảng chuyên cần của lớp: ma trận học viên theo buổi, ô màu theo trạng thái; hai cột tổng là số buổi vắng và
// tỷ lệ chuyên cần; mặc định sắp theo tỷ lệ tăng dần để thấy ngay ai có nguy cơ bỏ học.
export function MatrixPage({ head }: Props) {
  const { cls = '' } = useParams();
  const { user } = useAuth();
  const [asc, setAsc] = useState(true);
  const [onlyOver, setOnlyOver] = useState(false);
  const [all, setAll] = useState(false);
  const k = findClass(cls);
  if (!k) return <ErrorPage code="404" />;
  const role = user?.active ?? -1;
  if (role !== 5 && role !== 6 && !classesOf(user!.name).some((c) => c.code === k.code)) return <ErrorPage code="403" />;

  const sessions = heldSessions(cls).filter((s) => sheetOf(s));
  const rows = classRows(cls).sort((a, b) => (asc ? a.stat.rate - b.stat.rate : a.s.name.localeCompare(b.s.name)));
  const over = rows.filter((r) => r.stat.over);
  const filtered = onlyOver ? over : rows;
  const shown = all || filtered.length <= 12 ? filtered : filtered.slice(0, 9);
  const avg = rows.length ? Math.round(rows.reduce((n, r) => n + r.stat.rate, 0) / rows.length) : 100;
  const max = Math.floor((totalSessions(k) * thresholdOf(k.program)) / 100);

  return (
    <AppLayout crumb={[MODULE, 'Chuyên cần ' + k.code]} module={MODULE}>
      <Link to="/diem-danh" className="back">
        <Icon name="back" size={16} />
        Điểm danh
      </Link>
      <div className="h">
        <div>
          <h2>Bảng chuyên cần lớp {k.code}</h2>
          <p>
            Đã học {sessions.length}/{totalSessions(k)} buổi · {rows.filter((r) => r.s.cls === cls).length} học viên · chuyên cần trung bình {avg}%
          </p>
        </div>
        {head?.(rows, filtered)}
      </div>
      <div className="bar mx-bar">
        <button type="button" className={'check-box' + (asc ? ' on' : '')} aria-pressed={asc} onClick={() => setAsc(!asc)}>
          <Icon name="down" size={16} />
          {asc ? 'Tỷ lệ chuyên cần tăng dần' : 'Theo tên học viên'}
        </button>
        <label className="check-box">
          <input type="checkbox" checked={onlyOver} onChange={(e) => setOnlyOver(e.target.checked)} />
          Chỉ học viên vượt ngưỡng ({over.length})
        </label>
        <span className="mx-legend">
          {MARKS.map((m) => (
            <span key={m} className={markTone(m)}>
              {m}
            </span>
          ))}
        </span>
      </div>
      <div className="panel mx-wrap">
        <table className="mx">
          <thead>
            <tr>
              <th className="mx-name">Học viên</th>
              {sessions.map((s, i) => (
                <th key={s.id} title={longDate(s.date)}>
                  {i + 1}
                </th>
              ))}
              <th className="mx-sum">Vắng</th>
              <th className="mx-sum on">Chuyên cần</th>
            </tr>
          </thead>
          <tbody>
            {shown.map(({ s, stat }) => (
              <tr key={s.code}>
                <td className="mx-name">
                  <b>{s.name}</b>
                  <small>{s.code}</small>
                </td>
                {sessions.map((x, i) => {
                  const m: Mark | undefined = sheetOf(x)?.marks[s.code];
                  return (
                    <td key={x.id}>
                      <i className={'mx-cell ' + (markTone(m) ?? 'none')} title={`Buổi ${i + 1}, ${longDate(x.date)}: ${m ?? 'không có trong lớp'}`} />
                    </td>
                  );
                })}
                <td className={'mx-sum' + (stat.over ? ' bad' : '')}>{stat.absent} buổi</td>
                <td className={'mx-sum rate' + (stat.over ? ' bad' : stat.left <= 3 ? ' warn' : '')}>{stat.rate}%</td>
              </tr>
            ))}
          </tbody>
        </table>
        {shown.length < filtered.length && (
          <button type="button" className="link mx-more" onClick={() => setAll(true)}>
            và {filtered.length - shown.length} học viên khác
          </button>
        )}
      </div>
      <p className="hint" style={{ marginTop: 12 }}>
        Ngưỡng vắng của chương trình {k.program}: {thresholdOf(k.program)}% số buổi, tức tối đa {max} trên {totalSessions(k)} buổi. Đi muộn vẫn tính là có mặt.
      </p>
    </AppLayout>
  );
}
