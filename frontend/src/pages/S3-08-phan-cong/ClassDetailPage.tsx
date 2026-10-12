import { useState, type CSSProperties, type ReactNode } from 'react';
import { Link, NavLink, useNavigate, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Box, Pill } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { classesOf, classSize, classTone, ENROLLMENTS, findClass, isOpen, totalSessions, type Klass } from '../../data/classes';
import { viDate } from '../../data/leads';
import { STUDENTS, studentTone } from '../../data/students';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import '../S3-04-ho-so-hoc-vien/student.css';
import { canManageClasses, MODULE } from '../S3-07-danh-sach-lop/ClassListPage';
import '../S3-07-danh-sach-lop/classes.css';
import { AssignTab } from './AssignTab';
import './assign.css';

type ClassTab = { path: string; label: string; render: (k: Klass, manage: boolean, refresh: () => void) => ReactNode };

// S3-08. Trang một lớp với các tab Tổng quan, Học viên, Phân công giảng dạy. Giảng viên và trợ giảng chỉ mở được lớp mình dạy.
export function ClassDetailPage() {
  const { code = '', tab = '' } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [, setTick] = useState(0);
  const k = findClass(code);
  const role = user?.active ?? -1;
  if (!k) return <ErrorPage code="404" />;
  if ((role === 1 || role === 2) && !classesOf(user!.name).includes(k)) return <ErrorPage code="403" />;
  const manage = canManageClasses(role);
  const refresh = () => setTick((t) => t + 1);
  const tabs: ClassTab[] = [
    { path: '', label: 'Tổng quan', render: (c) => <Overview k={c} /> },
    { path: 'hoc-vien', label: 'Học viên', render: (c, m) => <Roster k={c} manage={m} onEnroll={() => navigate(`/lop-hoc/${c.code}/ghi-danh`)} /> },
    { path: 'phan-cong', label: 'Phân công giảng dạy', render: (c, m, r) => <AssignTab k={c} manage={m} onChange={r} /> },
  ];
  const current = tabs.find((t) => t.path === tab);
  if (!current) return <ErrorPage code="404" />;

  return (
    <AppLayout crumb={[MODULE, k.code]} module={MODULE}>
      <Link to="/lop-hoc" className="back">
        <Icon name="back" size={16} />
        Danh sách lớp
      </Link>
      <div className="h">
        <div>
          <h2 className="cd-title">
            {k.code} <Pill tone={classTone(k.status)}>{k.status}</Pill>
          </h2>
          <p>
            {k.program} · Khai giảng {viDate(k.start)} · {classSize(k.code)}/{k.max} học viên
          </p>
        </div>
      </div>
      <nav className="tabs">
        {tabs.map((t) => (
          <NavLink key={t.path} to={`/lop-hoc/${k.code}${t.path ? '/' + t.path : ''}`} end className={({ isActive }) => (isActive ? 'on' : undefined)}>
            {t.label}
          </NavLink>
        ))}
      </nav>
      {current.render(k, manage, refresh)}
    </AppLayout>
  );
}

function Overview({ k }: { k: Klass }) {
  const total = totalSessions(k);
  const facts: [string, ReactNode][] = [
    ['Chương trình', k.program],
    ['Khai giảng', viDate(k.start)],
    ['Hình thức học', k.mode],
    ['Lịch học', k.slot],
    ['Phòng học', k.room ?? 'Chưa xếp phòng'],
    ['Sĩ số', `${classSize(k.code)} / ${k.max}`],
    ['Số môn', `${k.subjects.length} môn · ${total} buổi`],
  ];
  return (
    <div className="panel sp-card">
      <dl className="sp-facts cd-facts">
        {facts.map(([a, b]) => (
          <div key={a}>
            <dt>{a}</dt>
            <dd>{b}</dd>
          </div>
        ))}
        <div className="wide">
          <dt>Tiến độ buổi học</dt>
          <dd>
            <span className="cl-bar" style={{ '--p': `${(100 * k.done) / Math.max(1, total)}%` } as CSSProperties}>
              <i />
              {k.done}/{total} buổi
            </span>
          </dd>
        </div>
      </dl>
    </div>
  );
}

function Roster({ k, manage, onEnroll }: { k: Klass; manage: boolean; onEnroll: () => void }) {
  const list = STUDENTS.filter((s) => s.cls === k.code);
  return (
    <>
      {manage && isOpen(k) && (
        <div className="bar" style={{ justifyContent: 'flex-end' }}>
          <button type="button" className="btn primary" onClick={onEnroll}>
            <Icon name="plus" size={16} />
            Ghi danh học viên
          </button>
        </div>
      )}
      <div className="panel">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Mã học viên</th>
                <th>Họ tên</th>
                <th>Số điện thoại</th>
                <th>Ngày ghi danh</th>
                <th>Người ghi danh</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {list.map((s) => {
                const e = ENROLLMENTS.find((x) => x.student === s.code && x.cls === k.code);
                return (
                  <tr key={s.code}>
                    <td className="code">
                      <Link to={'/hoc-vien/' + s.code}>{s.code}</Link>
                    </td>
                    <td className="strong">{s.name}</td>
                    <td>{s.phone}</td>
                    <td>{e ? viDate(e.at) : viDate(s.enrolledAt)}</td>
                    <td>{e?.by ?? '–'}</td>
                    <td>
                      <Pill tone={studentTone(s.status)}>{s.status}</Pill>
                    </td>
                  </tr>
                );
              })}
              {!list.length && (
                <tr>
                  <td colSpan={6} className="empty">
                    Lớp chưa có học viên.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
      {!isOpen(k) && (
        <Box icon="lock" title={'Lớp đã ' + k.status.toLowerCase()}>
          Không ghi danh thêm được.
        </Box>
      )}
    </>
  );
}
