import { useMemo, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Filter, LoadState, Pager, paginate, Pill, Search, usePaging } from '../../components/ui';
import { apiEnabled, useLoad } from '../../data/api';
import { useAuth } from '../../data/auth';
import { loadStudents } from '../../data/s3api';
import { viDate } from '../../data/leads';
import { STUDENT_STATUSES, STUDENTS, studentTone } from '../../data/students';
import { PROGRAMS } from '../../data/training';
import './student.css';

export const MODULE = 'Hồ sơ học viên';

// S3-04. Danh sách học viên: tìm theo mã, tên hoặc số điện thoại; bấm một dòng để mở hồ sơ.
// Học viên vừa chuyển từ lead (S3-03) nằm đầu danh sách với trạng thái Mới nhập học.
export function StudentListPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [program, setProgram] = useState('');
  const { page, setPage, size, setSize } = usePaging();
  const load = useLoad(loadStudents);

  const filtered = useMemo(() => {
    const k = q.trim().toLowerCase();
    const d = k.replace(/\D/g, '');
    return STUDENTS.filter(
      (s) =>
        (!k || s.code.toLowerCase().includes(k) || s.name.toLowerCase().includes(k) || (d.length > 2 && s.phone.replace(/\D/g, '').includes(d))) &&
        (!status || s.status === status) &&
        (!program || s.program === program),
    );
  }, [q, status, program, load.loading]);
  // Có backend: chương trình lấy từ chính danh sách học viên, khỏi cần quyền quản lý chương trình.
  const programs = apiEnabled ? [...new Set(STUDENTS.map((s) => s.program).filter(Boolean))] : PROGRAMS.map((p) => p.name);
  const view = paginate(filtered, page, size);

  // Học viên chỉ được xem hồ sơ của chính mình.
  if (user?.active === 0) {
    const me = STUDENTS.find((s) => s.login === user.email);
    return <Navigate to={me ? '/hoc-vien/' + me.code : '/'} replace />;
  }

  return (
    <AppLayout crumb={[MODULE]} module={MODULE}>
      <div className="h">
        <div>
          <h2>Hồ sơ học viên</h2>
          <p>
            {STUDENTS.length} học viên · {STUDENTS.filter((s) => s.status === 'Đang học').length} đang học
          </p>
        </div>
      </div>
      <LoadState {...load} />
      <div className="bar">
        <Search placeholder="Tìm theo mã học viên, tên, số điện thoại" value={q} onChange={(v) => (setQ(v), setPage(1))} />
        <Filter label="Trạng thái" value={status} options={STUDENT_STATUSES} onChange={(v) => (setStatus(v), setPage(1))} width={220} />
        <Filter label="Chương trình" value={program} options={programs} onChange={(v) => (setProgram(v), setPage(1))} width={300} />
      </div>
      <div className="panel">
        <div className="scroll">
          <table className="rows">
            <thead>
              <tr>
                <th>Mã học viên</th>
                <th>Họ tên</th>
                <th>Số điện thoại</th>
                <th>Chương trình</th>
                <th>Lớp hiện tại</th>
                <th>Trạng thái</th>
                <th>Ngày nhập học</th>
              </tr>
            </thead>
            <tbody>
              {view.items.map((s) => (
                <tr key={s.code} tabIndex={0} onClick={() => navigate('/hoc-vien/' + s.code)} onKeyDown={(e) => e.key === 'Enter' && navigate('/hoc-vien/' + s.code)}>
                  <td className="code">{s.code}</td>
                  <td className="strong">{s.name}</td>
                  <td className="strong">{s.phone}</td>
                  <td>{s.program}</td>
                  <td className={s.cls ? 'strong' : undefined}>{s.cls ?? 'Chưa xếp lớp'}</td>
                  <td>
                    <Pill tone={studentTone(s.status)}>{s.status}</Pill>
                  </td>
                  <td>{viDate(s.enrolledAt)}</td>
                </tr>
              ))}
              {!view.items.length && (
                <tr>
                  <td colSpan={7} className="empty">
                    Không có học viên khớp. Thử tìm bằng mã học viên hoặc bỏ bớt bộ lọc.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pager page={view.page} pages={view.pages} onPage={setPage} size={size} onSize={setSize}>
          {view.from} đến {view.to} trong {filtered.length} học viên
        </Pager>
      </div>
    </AppLayout>
  );
}
