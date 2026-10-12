import { useState, type CSSProperties, type ReactNode } from 'react';
import { AppLayout } from '../../components/AppLayout';
import { Filter, Pager, paginate, Pill, RowMenu, Search, usePaging, type MenuAction } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { CLASS_STATUSES, CLASSES, classesOf, classSize, classTone, isOpen, mainTeacher, missingTeachers, STAFF, totalSessions, type Klass } from '../../data/classes';
import { viDate } from '../../data/leads';
import { PROGRAMS } from '../../data/training';
import '../S3-04-ho-so-hoc-vien/student.css';
import './classes.css';

export const MODULE = 'Lớp học & thời khoá biểu';

/** Quản lý đào tạo và quản trị được mở lớp, ghi danh, phân công. Vai trò khác chỉ xem. */
export const canManageClasses = (role: number) => role === 5 || role === 6;

type Props = {
  /** Nút ở đầu trang, ví dụ Mở lớp mới (S3-05). */
  head?: ReactNode;
  /** Thao tác ở cuối mỗi dòng (S3-06 ghi danh, S3-08 phân công). */
  rowActions?: (k: Klass) => MenuAction[];
  /** Bấm vào một dòng (S3-08 mở trang lớp). */
  onOpen?: (k: Klass) => void;
};

// S3-07. Danh sách lớp kèm bốn chỉ số nhanh và bộ lọc theo trạng thái, chương trình, giảng viên, khoảng ngày khai giảng.
// Mỗi dòng hiện sĩ số hiện tại trên sĩ số tối đa và tiến độ buổi học. Giảng viên và trợ giảng chỉ thấy lớp mình dạy.
export function ClassListPage({ head, rowActions, onOpen }: Props) {
  const { user } = useAuth();
  const role = user?.active ?? -1;
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [program, setProgram] = useState('');
  const [teacher, setTeacher] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const { page, setPage, size, setSize } = usePaging();

  const mine = role === 1 || role === 2;
  const base = mine ? classesOf(user!.name) : CLASSES;
  const k = q.trim().toLowerCase();
  const filtered = base.filter(
    (c) =>
      (!k || c.code.toLowerCase().includes(k)) &&
        (!status || c.status === status) &&
        (!program || c.program === program) &&
        (!teacher || c.subjects.some((s) => s.teacher === teacher)) &&
        (!from || c.start >= from) &&
        (!to || c.start <= to),
  );
  const view = paginate(filtered, page, size);

  const open = base.filter(isOpen);
  const seats = open.reduce((n, c) => n + classSize(c.code), 0);
  const max = open.reduce((n, c) => n + c.max, 0);
  const short = open.filter((c) => missingTeachers(c) > 0).length;
  const reset = (f: (v: string) => void) => (v: string) => (f(v), setPage(1));

  return (
    <AppLayout crumb={[MODULE, 'Lớp học']} module={MODULE}>
      <div className="h">
        <div>
          <h2>{mine ? 'Lớp của tôi' : 'Lớp học'}</h2>
          <p>{filtered.length} lớp trong bộ lọc hiện tại</p>
        </div>
        {head}
      </div>
      <div className="stats cl-stats">
        <Stat value={open.filter((c) => c.status === 'Đang học').length} label="Lớp đang học" />
        <Stat value={open.filter((c) => c.status === 'Chuẩn bị').length} label="Lớp chuẩn bị khai giảng" />
        <Stat value={`${seats} / ${max}`} label="Học viên trên tổng sĩ số tối đa" />
        <Stat value={short} label="Lớp chưa đủ giảng viên" bad={short > 0} />
      </div>
      <div className="bar">
        <Search placeholder="Tìm theo mã lớp" value={q} onChange={reset(setQ)} width={260} />
        <Filter label="Trạng thái" value={status} options={CLASS_STATUSES} onChange={reset(setStatus)} width={190} />
        <Filter label="Chương trình" value={program} options={PROGRAMS.filter((p) => p.active).map((p) => p.name)} onChange={reset(setProgram)} width={260} />
        {!mine && <Filter label="Giảng viên" value={teacher} options={STAFF.filter((s) => s.role === 2).map((s) => s.name)} onChange={reset(setTeacher)} width={240} />}
        <div className="cl-range" role="group" aria-label="Khai giảng trong khoảng">
          <input type="date" value={from} aria-label="Khai giảng từ ngày" onChange={(e) => reset(setFrom)(e.target.value)} />
          <span>–</span>
          <input type="date" value={to} aria-label="Khai giảng đến ngày" onChange={(e) => reset(setTo)(e.target.value)} />
        </div>
      </div>
      <div className="panel">
        <div className="scroll">
          <table className={onOpen ? 'rows' : undefined}>
            <thead>
              <tr>
                <th>Mã lớp</th>
                <th>Chương trình</th>
                <th>Giảng viên</th>
                <th>Khai giảng</th>
                <th>Sĩ số</th>
                <th>Tiến độ buổi học</th>
                <th>Trạng thái</th>
                {rowActions && <th />}
              </tr>
            </thead>
            <tbody>
              {view.items.map((c) => {
                const n = classSize(c.code);
                const total = totalSessions(c);
                const t = mainTeacher(c);
                return (
                  <tr key={c.code} tabIndex={onOpen ? 0 : undefined} onClick={() => onOpen?.(c)} onKeyDown={(e) => e.key === 'Enter' && onOpen?.(c)}>
                    <td className="code">{c.code}</td>
                    <td className="strong">{c.program}</td>
                    <td className={t ? 'strong' : 'bad-t'}>{t ?? 'Chưa phân công'}</td>
                    <td>{viDate(c.start)}</td>
                    <td className={'cl-size' + (n >= c.max ? ' full' : n >= c.max * 0.9 ? ' near' : '')}>
                      {n}/{c.max}
                    </td>
                    <td>
                      <span className="cl-bar" style={{ '--p': `${(100 * c.done) / Math.max(1, total)}%` } as CSSProperties}>
                        <i />
                        {c.done}/{total}
                      </span>
                    </td>
                    <td>
                      <Pill tone={classTone(c.status)}>{c.status}</Pill>
                    </td>
                    {rowActions && (
                      <td style={{ width: 56 }} onClick={(e) => e.stopPropagation()}>
                        <RowMenu actions={rowActions(c)} />
                      </td>
                    )}
                  </tr>
                );
              })}
              {!view.items.length && (
                <tr>
                  <td colSpan={8} className="empty">
                    {mine ? 'Bạn chưa được phân công lớp nào.' : 'Không có lớp khớp bộ lọc. Bỏ bớt bộ lọc hoặc đổi khoảng ngày khai giảng.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pager page={view.page} pages={view.pages} onPage={setPage} size={size} onSize={setSize}>
          {view.from} đến {view.to} trong {filtered.length} lớp
        </Pager>
      </div>
    </AppLayout>
  );
}

function Stat({ value, label, bad }: { value: ReactNode; label: string; bad?: boolean }) {
  return (
    <div className={'stat' + (bad ? ' bad' : '')}>
      <b>{value}</b>
      <span>{label}</span>
    </div>
  );
}
