import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Filter, LoadState, Pill, Search } from '../../components/ui';
import { apiEnabled, useLoad } from '../../data/api';
import { useAuth } from '../../data/auth';
import { enrollStudents, loadClass, loadStudents } from '../../data/s3api';
import { classSize, ENROLLMENTS, findClass, isOpen } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { viDate } from '../../data/leads';
import { saveAll } from '../../data/store';
import { STUDENTS, type Student } from '../../data/students';
import { PROGRAMS } from '../../data/training';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import { canManageClasses, MODULE } from '../S3-07-danh-sach-lop/ClassListPage';
import '../S3-04-ho-so-hoc-vien/student.css';
import './enroll.css';

// S3-06. Ghi danh nhiều học viên vào lớp trong một thao tác. Học viên đã ở trong lớp bị khoá ô chọn;
// chọn quá số chỗ còn lại thì nút Ghi danh tắt và nói rõ lý do. Mỗi lần ghi danh lưu ngày và người thực hiện.
export function EnrollPage() {
  const { code = '' } = useParams();
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const load = useLoad(() => Promise.all([loadClass(code), loadStudents()]), [code]);
  const k = findClass(code);
  const [q, setQ] = useState('');
  const [program, setProgram] = useState(apiEnabled ? '' : (k?.program ?? ''));
  const [onlyFree, setOnlyFree] = useState(true);
  const [picked, setPicked] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);
  if (apiEnabled && (load.loading || load.error))
    return (
      <AppLayout crumb={[MODULE, code, 'Ghi danh']} module={MODULE}>
        <LoadState {...load} />
      </AppLayout>
    );
  if (!k) return <ErrorPage code="404" />;
  if (!canManageClasses(user?.active ?? -1) || !isOpen(k)) return <ErrorPage code="403" />;

  const now = classSize(k.code);
  const left = k.max - now;
  const after = now + picked.length;
  const over = after - k.max;
  const kw = q.trim().toLowerCase();
  const d = kw.replace(/\D/g, '');
  const rows = STUDENTS.filter(
    (s) =>
      (s.cls === k.code || !onlyFree || !s.cls) &&
      (!program || s.program === program) &&
      (s.status === 'Mới nhập học' || s.status === 'Đang học') &&
      (!kw || s.code.toLowerCase().includes(kw) || s.name.toLowerCase().includes(kw) || (d.length > 2 && s.phone.replace(/\D/g, '').includes(d))),
  );
  const lock = (s: Student) => !!s.cls || (left <= 0 && !picked.includes(s.code));
  const toggle = (c: string) => setPicked((p) => (p.includes(c) ? p.filter((x) => x !== c) : [...p, c]));

  function enroll() {
    if (apiEnabled) {
      setBusy(true);
      return void enrollStudents(k!.code, picked).then(
        () => (toast(`Đã ghi danh ${picked.length} học viên vào ${k!.code}`, `Sĩ số mới ${after}/${k!.max}.`), navigate('/lop-hoc')),
        // Máy chủ chặn khi lớp vừa đủ sĩ số hoặc có người đã ghi danh trước: tải lại để thấy số liệu mới.
        (e: Error) => (setBusy(false), toast('Chưa ghi danh được', e.message), load.reload()),
      );
    }
    for (const c of picked) {
      const s = STUDENTS.find((x) => x.code === c)!;
      s.cls = k!.code;
      s.status = 'Đang học';
      ENROLLMENTS.unshift({ student: c, cls: k!.code, at: TODAY, by: user!.name });
    }
    saveAll();
    toast(`Đã ghi danh ${picked.length} học viên vào ${k!.code}`, `Sĩ số mới ${after}/${k!.max}.`);
    navigate('/lop-hoc');
  }

  return (
    <AppLayout crumb={[MODULE, k.code, 'Ghi danh']} module={MODULE}>
      <Link to="/lop-hoc" className="back">
        <Icon name="back" size={16} />
        Lớp {k.code}
      </Link>
      <div className="h">
        <div>
          <h2>Ghi danh học viên vào lớp {k.code}</h2>
          <p>
            {k.program} · Khai giảng {viDate(k.start)}
          </p>
        </div>
        <div className={'en-size' + (over > 0 ? ' bad' : '')}>
          <span>Sĩ số sau khi ghi danh</span>
          <b>
            {after} / {k.max}
          </b>
          <small>
            Đang có {now} · đang chọn {picked.length} · {over > 0 ? `vượt ${over} chỗ` : `còn ${k.max - after} chỗ`}
          </small>
        </div>
      </div>
      <div className="bar">
        <Search placeholder="Tìm theo mã học viên, tên, số điện thoại" value={q} onChange={setQ} />
        <Filter label="Chương trình" value={program} options={apiEnabled ? [...new Set(STUDENTS.map((s) => s.program).filter(Boolean))] : PROGRAMS.map((p) => p.name)} onChange={setProgram} width={340} />
        <label className="check-box">
          <input type="checkbox" checked={onlyFree} onChange={(e) => setOnlyFree(e.target.checked)} />
          Chỉ học viên chưa xếp lớp
        </label>
      </div>
      <div className="panel en-table">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th style={{ width: 48 }} />
                <th>Mã học viên</th>
                <th>Họ tên</th>
                <th>Số điện thoại</th>
                <th>Ngày nhập học</th>
                <th>Tình trạng</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((s) => {
                const locked = lock(s);
                const on = picked.includes(s.code);
                return (
                  <tr key={s.code} className={(s.cls ? 'muted' : '') + (on ? ' on' : '')} onClick={() => !locked && toggle(s.code)}>
                    <td>
                      <input type="checkbox" checked={on || s.cls === k.code} disabled={locked} aria-label={'Chọn ' + s.name} onChange={() => toggle(s.code)} onClick={(e) => e.stopPropagation()} />
                    </td>
                    <td className="strong">{s.code}</td>
                    <td className="strong">{s.name}</td>
                    <td>{s.phone}</td>
                    <td>{viDate(s.enrolledAt)}</td>
                    <td>{s.cls === k.code ? <Pill>Đã ở trong lớp này</Pill> : s.cls ? <Pill>Đang học {s.cls}</Pill> : <Pill tone="info">Chưa xếp lớp</Pill>}</td>
                  </tr>
                );
              })}
              {!rows.length && (
                <tr>
                  <td colSpan={6} className="empty">
                    Không còn học viên nào để ghi danh theo bộ lọc này.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
      <div className={'en-foot' + (over > 0 ? ' bad' : '')}>
        {over > 0 ? (
          <div>
            <b>
              <Icon name="alert" size={16} /> Không ghi danh được: lớp chỉ còn {left} chỗ, bạn đang chọn {picked.length} học viên
            </b>
            <span>Bỏ chọn {over} học viên hoặc tăng sĩ số tối đa của lớp.</span>
          </div>
        ) : left <= 0 ? (
          <div>
            <b>Lớp đã đủ sĩ số</b>
            <span>Tăng sĩ số tối đa của lớp nếu muốn ghi danh thêm.</span>
          </div>
        ) : (
          <div>
            <b>Đã chọn {picked.length} học viên</b>
            <span>
              Ngày ghi danh {viDate(TODAY)} · Người thực hiện {user!.name}
            </span>
          </div>
        )}
        <button type="button" className="btn" style={{ width: 80 }} onClick={() => navigate('/lop-hoc')}>
          Huỷ
        </button>
        <button type="button" className="btn primary" disabled={!picked.length || over > 0 || busy} onClick={enroll}>
          Ghi danh {picked.length || ''} học viên
        </button>
      </div>
    </AppLayout>
  );
}
