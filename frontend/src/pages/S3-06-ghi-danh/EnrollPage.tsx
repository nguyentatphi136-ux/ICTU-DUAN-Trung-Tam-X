import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Box, Filter, LoadState, Pill, Search } from '../../components/ui';
import { apiEnabled, useLoad } from '../../data/api';
import { useAuth } from '../../data/auth';
import { enrollStudents, loadClass, loadStudents } from '../../data/s3api';
import { classSize, ENROLLMENTS, findClass, isOpen } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { viDate } from '../../data/leads';
import { notify } from '../../data/notifications';
import { saveAll } from '../../data/store';
import { STUDENTS, type Student } from '../../data/students';
import { PROGRAMS } from '../../data/training';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import { canManageClasses, MODULE } from '../S3-07-danh-sach-lop/ClassListPage';
import '../S3-04-ho-so-hoc-vien/student.css';
import './enroll.css';

// S3-06. Ghi danh nhiều học viên vào lớp trong một thao tác (đơn lẻ hoặc hàng loạt).
// Chặn ghi danh khi lớp đã đủ sĩ số hoặc học viên đã ở trong lớp.
// Ghi nhận ngày ghi danh và người thực hiện.
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
  const selectAllRef = useRef<HTMLInputElement>(null);

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

  // Học viên đủ điều kiện ghi danh (chưa có lớp nào)
  const eligibleRows = rows.filter((s) => !s.cls);
  const allEligibleSelected = eligibleRows.length > 0 && eligibleRows.every((s) => picked.includes(s.code));
  const someEligibleSelected = eligibleRows.some((s) => picked.includes(s.code)) && !allEligibleSelected;

  useEffect(() => {
    if (selectAllRef.current) {
      selectAllRef.current.indeterminate = someEligibleSelected;
    }
  }, [someEligibleSelected]);

  // Khóa checkbox:
  // - Học viên đã có lớp (trong lớp này hoặc lớp khác) -> Luôn khóa
  // - Lớp đã đủ sĩ số từ trước (left <= 0) và học viên chưa được chọn -> Khóa
  // - Đã chọn đủ số chỗ còn lại (picked.length >= left) và học viên này chưa được chọn -> Khóa để chặn chọn vượt sĩ số
  const lock = (s: Student) => {
    if (s.cls) return true;
    if (left <= 0 && !picked.includes(s.code)) return true;
    if (picked.length >= left && !picked.includes(s.code)) return true;
    return false;
  };

  const toggle = (c: string) => {
    const s = STUDENTS.find((x) => x.code === c);
    if (!s || s.cls) return;
    if (picked.includes(c)) {
      setPicked((p) => p.filter((x) => x !== c));
    } else {
      if (left <= 0) {
        toast('Lớp đã đủ sĩ số', `Lớp ${k.code} đã đạt sĩ số tối đa ${k.max} học viên.`);
        return;
      }
      if (picked.length >= left) {
        toast('Lớp không còn chỗ trống', `Lớp chỉ còn ${left} chỗ trống. Vui lòng bỏ bớt học viên đã chọn trước khi chọn thêm.`);
        return;
      }
      setPicked((p) => [...p, c]);
    }
  };

  const toggleSelectAll = () => {
    if (left <= 0) {
      toast('Lớp đã đủ sĩ số', `Lớp ${k.code} đã đạt sĩ số tối đa ${k.max} học viên.`);
      return;
    }
    if (allEligibleSelected) {
      const eligibleCodes = new Set(eligibleRows.map((s) => s.code));
      setPicked((p) => p.filter((c) => !eligibleCodes.has(c)));
    } else {
      const currentPicked = new Set(picked);
      const toAdd = eligibleRows.filter((s) => !currentPicked.has(s.code));
      const slotsRemaining = Math.max(0, left - picked.length);
      if (slotsRemaining <= 0) {
        toast('Lớp không còn chỗ trống', `Bạn đã chọn đủ ${picked.length}/${left} học viên theo sĩ số còn lại.`);
        return;
      }
      const canTake = toAdd.slice(0, slotsRemaining).map((s) => s.code);
      if (canTake.length < toAdd.length) {
        toast('Đã chọn tối đa số chỗ còn lại', `Lớp chỉ còn ${slotsRemaining} chỗ trống, hệ thống đã chọn ${canTake.length} học viên.`);
      }
      setPicked((p) => [...p, ...canTake]);
    }
  };

  const selectEntireGroup = () => {
    if (left <= 0) {
      toast('Lớp đã đủ sĩ số', `Lớp ${k.code} đã đạt sĩ số tối đa ${k.max} học viên.`);
      return;
    }
    const candidates = eligibleRows.slice(0, left).map((s) => s.code);
    setPicked(candidates);
    if (eligibleRows.length > left) {
      toast('Đã chọn nhóm học viên', `Đã chọn ${left}/${eligibleRows.length} học viên chưa xếp lớp (vừa đủ số chỗ còn lại của lớp).`);
    } else {
      toast('Đã chọn cả nhóm', `Đã chọn toàn bộ ${candidates.length} học viên chưa xếp lớp.`);
    }
  };

  function enroll() {
    if (!picked.length) return;
    if (left <= 0 || over > 0) {
      toast('Không thể ghi danh', `Lớp không đủ chỗ (sĩ số tối đa: ${k!.max}, đang có: ${now}, chọn thêm: ${picked.length}).`);
      return;
    }

    const enrollDate = TODAY;
    const enrolledBy = user?.name || 'Quản lý đào tạo';
    const enrollTime = new Date().toTimeString().slice(0, 5);

    if (apiEnabled) {
      setBusy(true);
      return void enrollStudents(k!.code, picked).then(
        () => {
          toast(`Đã ghi danh ${picked.length} học viên vào ${k!.code}`, `Sĩ số mới ${after}/${k!.max}. Ngày ghi danh: ${viDate(enrollDate)}, Người thực hiện: ${enrolledBy}.`);
          navigate('/lop-hoc');
        },
        (e: Error) => (setBusy(false), toast('Chưa ghi danh được', e.message), load.reload()),
      );
    }

    for (const c of picked) {
      const s = STUDENTS.find((x) => x.code === c);
      if (!s) continue;
      s.cls = k!.code;
      s.status = 'Đang học';
      ENROLLMENTS.unshift({ student: c, cls: k!.code, at: enrollDate, by: enrolledBy });
      s.notes.unshift({
        at: enrollDate,
        by: enrolledBy,
        text: `Quản lý đào tạo (${enrolledBy}) ghi danh vào lớp ${k!.code} (${k!.program}). Sĩ số lớp: ${classSize(k!.code)}/${k!.max}.`,
      });
      notify({
        at: enrollDate + 'T' + enrollTime,
        kind: 'Lịch học',
        title: `Bạn đã được ghi danh vào lớp ${k!.code}`,
        body: `Lớp ${k!.code} (${k!.program}) · Khai giảng ${viDate(k!.start)} · Người thực hiện: ${enrolledBy}.`,
        link: '/lich-hoc',
        to: { users: [s.name, s.code, s.login] },
        ref: `enr-${k!.code}-${c}`,
      });
    }

    notify({
      at: enrollDate + 'T' + enrollTime,
      kind: 'Lịch học',
      title: `Ghi danh thành công ${picked.length} học viên vào lớp ${k!.code}`,
      body: `Sĩ số mới: ${after}/${k!.max} · Ngày: ${viDate(enrollDate)} · Người thực hiện: ${enrolledBy}. Danh sách: ${picked.join(', ')}.`,
      link: '/lop-hoc/' + k!.code,
      to: { users: [enrolledBy] },
      ref: `enr-batch-${k!.code}-${Date.now()}`,
    });

    saveAll();
    toast(`Đã ghi danh ${picked.length} học viên vào lớp ${k!.code}`, `Sĩ số mới ${after}/${k!.max}. Ngày ghi danh: ${viDate(enrollDate)}, Người thực hiện: ${enrolledBy}.`);
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
        <div className={'en-size' + (over > 0 || left <= 0 ? ' bad' : '')}>
          <span>Sĩ số sau khi ghi danh</span>
          <b>
            {after} / {k.max}
          </b>
          <small>
            Đang có {now} · đang chọn {picked.length} · {over > 0 ? `vượt ${over} chỗ` : left <= 0 ? 'lớp đã đủ sĩ số' : `còn ${k.max - after} chỗ`}
          </small>
        </div>
      </div>

      {left <= 0 && (
        <Box icon="lock" tone="warn" title="Lớp đã đủ sĩ số">
          Lớp {k.code} hiện đã có đủ sĩ số ({now}/{k.max} học viên), không thể ghi danh thêm. Vui lòng tăng sĩ số tối đa nếu muốn nhận thêm học viên.
        </Box>
      )}

      <div className="bar">
        <Search placeholder="Tìm theo mã học viên, tên, số điện thoại" value={q} onChange={setQ} />
        <Filter label="Chương trình" value={program} options={apiEnabled ? [...new Set(STUDENTS.map((s) => s.program).filter(Boolean))] : PROGRAMS.map((p) => p.name)} onChange={setProgram} width={320} />
        <label className="check-box">
          <input type="checkbox" checked={onlyFree} onChange={(e) => setOnlyFree(e.target.checked)} />
          Chỉ học viên chưa xếp lớp
        </label>
        <div style={{ marginLeft: 'auto', display: 'flex', gap: 8, alignItems: 'center' }}>
          {eligibleRows.length > 0 && left > 0 && (
            <button type="button" className="btn small primary" onClick={selectEntireGroup}>
              <Icon name="check" size={14} /> Chọn cả nhóm chưa xếp lớp ({Math.min(eligibleRows.length, left)})
            </button>
          )}
          {picked.length > 0 && (
            <button type="button" className="btn small" onClick={() => setPicked([])}>
              Bỏ chọn ({picked.length})
            </button>
          )}
        </div>
      </div>
      <div className="panel en-table">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th style={{ width: 48, textAlign: 'center' }}>
                  <input
                    ref={selectAllRef}
                    type="checkbox"
                    checked={allEligibleSelected}
                    disabled={left <= 0 || eligibleRows.length === 0}
                    onChange={toggleSelectAll}
                    title={allEligibleSelected ? 'Bỏ chọn tất cả' : 'Chọn tất cả học viên hợp lệ'}
                    aria-label="Chọn tất cả học viên hợp lệ"
                  />
                </th>
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
                const isAlreadyInThisClass = s.cls === k.code;
                const isAlreadyInOtherClass = !!s.cls && s.cls !== k.code;
                return (
                  <tr
                    key={s.code}
                    className={(s.cls ? 'muted' : '') + (on ? ' on' : '')}
                    onClick={() => !locked && toggle(s.code)}
                    style={{ cursor: locked ? 'not-allowed' : 'pointer' }}
                  >
                    <td style={{ textAlign: 'center' }}>
                      <input
                        type="checkbox"
                        checked={on || isAlreadyInThisClass}
                        disabled={locked}
                        aria-label={'Chọn ' + s.name}
                        onChange={() => toggle(s.code)}
                        onClick={(e) => e.stopPropagation()}
                      />
                    </td>
                    <td className="strong">{s.code}</td>
                    <td className="strong">{s.name}</td>
                    <td>{s.phone}</td>
                    <td>{viDate(s.enrolledAt)}</td>
                    <td>
                      {isAlreadyInThisClass ? (
                        <Pill tone="wait">Đã ở trong lớp này</Pill>
                      ) : isAlreadyInOtherClass ? (
                        <Pill tone="off">Đang học {s.cls}</Pill>
                      ) : s.status === 'Mới nhập học' ? (
                        <Pill tone="info">Mới nhập học · Chưa xếp lớp</Pill>
                      ) : (
                        <Pill tone="info">Chưa xếp lớp</Pill>
                      )}
                    </td>
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
      <div className={'en-foot' + (over > 0 || left <= 0 ? ' bad' : '')}>
        {over > 0 ? (
          <div>
            <b>
              <Icon name="alert" size={16} /> Không ghi danh được: lớp chỉ còn {left} chỗ, bạn đang chọn {picked.length} học viên
            </b>
            <span>Bỏ chọn {over} học viên hoặc tăng sĩ số tối đa của lớp.</span>
          </div>
        ) : left <= 0 ? (
          <div>
            <b>Lớp đã đủ sĩ số ({now} / {k.max})</b>
            <span>Tăng sĩ số tối đa của lớp nếu muốn ghi danh thêm.</span>
          </div>
        ) : (
          <div>
            <b>Đã chọn {picked.length} học viên</b>
            <span>
              Ngày ghi danh: {viDate(TODAY)} · Người thực hiện: {user!.name}
            </span>
          </div>
        )}
        <button type="button" className="btn" style={{ width: 80 }} onClick={() => navigate('/lop-hoc')}>
          Huỷ
        </button>
        <button type="button" className="btn primary" disabled={!picked.length || over > 0 || left <= 0 || busy} onClick={enroll}>
          {busy ? 'Đang ghi danh…' : `Ghi danh ${picked.length || ''} học viên`}
        </button>
      </div>
    </AppLayout>
  );
}
