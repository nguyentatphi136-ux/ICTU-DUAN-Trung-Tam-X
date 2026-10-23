import { useState } from 'react';
import { Icon } from '../../components/Icon';
import { Modal, Pill, Box } from '../../components/ui';
import { CLASSES, classSize, ENROLLMENTS, isOpen, type Klass } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { viDate } from '../../data/leads';
import { notify } from '../../data/notifications';
import { saveAll } from '../../data/store';
import { type Student } from '../../data/students';

type Props = {
  student: Student;
  userName: string;
  onClose: () => void;
  onEnrolled: (classCode: string) => void;
};

// S3-06. Ghi danh đơn lẻ học viên vào lớp trực tiếp từ hồ sơ học viên.
// Chặn ghi danh khi lớp đã đủ sĩ số hoặc lớp đã kết thúc/huỷ. Ghi nhận ngày và người thực hiện.
export function EnrollStudentModal({ student, userName, onClose, onEnrolled }: Props) {
  const [filterSameProgram, setFilterSameProgram] = useState(true);
  const [selectedCode, setSelectedCode] = useState<string>('');
  const [busy, setBusy] = useState(false);

  // Danh sách các lớp đang mở (Chuẩn bị hoặc Đang học)
  const openClasses = CLASSES.filter(isOpen);
  const availableClasses = openClasses.filter(
    (c) => !filterSameProgram || !student.program || c.program === student.program
  );

  const selectedClass = openClasses.find((c) => c.code === selectedCode);
  const size = selectedClass ? classSize(selectedClass.code) : 0;
  const isFull = selectedClass ? size >= selectedClass.max : false;

  function handleEnroll() {
    if (!selectedClass || isFull || busy) return;
    setBusy(true);

    const enrollDate = TODAY;
    const enrolledBy = userName || 'Quản lý đào tạo';
    const enrollTime = new Date().toTimeString().slice(0, 5);

    student.cls = selectedClass.code;
    student.status = 'Đang học';

    ENROLLMENTS.unshift({
      student: student.code,
      cls: selectedClass.code,
      at: enrollDate,
      by: enrolledBy,
    });

    student.notes.unshift({
      at: enrollDate,
      by: enrolledBy,
      text: `Quản lý đào tạo (${enrolledBy}) ghi danh vào lớp ${selectedClass.code} (${selectedClass.program}). Sĩ số mới: ${size + 1}/${selectedClass.max}.`,
    });

    notify({
      at: enrollDate + 'T' + enrollTime,
      kind: 'Lịch học',
      title: `Bạn đã được ghi danh vào lớp ${selectedClass.code}`,
      body: `Chương trình: ${selectedClass.program} · Khai giảng: ${viDate(selectedClass.start)} · Người thực hiện: ${enrolledBy}.`,
      link: '/lich-hoc',
      to: { users: [student.name, student.code, student.login] },
      ref: `enr-single-${selectedClass.code}-${student.code}`,
    });

    notify({
      at: enrollDate + 'T' + enrollTime,
      kind: 'Lịch học',
      title: `Đã ghi danh học viên ${student.name} vào lớp ${selectedClass.code}`,
      body: `Mã HV: ${student.code} · Sĩ số mới: ${size + 1}/${selectedClass.max} · Người thực hiện: ${enrolledBy}.`,
      link: '/lop-hoc/' + selectedClass.code,
      to: { users: [enrolledBy] },
      ref: `enr-single-mgr-${selectedClass.code}-${student.code}`,
    });

    saveAll();
    setBusy(false);
    onEnrolled(selectedClass.code);
  }

  return (
    <Modal onClose={onClose} label="Ghi danh học viên vào lớp" wide>
      <h3 style={{ marginBottom: 4 }}>Ghi danh học viên vào lớp</h3>
      <p className="hint" style={{ fontSize: 14, marginBottom: 16 }}>
        Xếp học viên <b>{student.name}</b> ({student.code}) vào lớp học phù hợp.
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 12, padding: '12px 16px', background: 'var(--surface-soft, #f8fafc)', borderRadius: 8, marginBottom: 16, border: '1px solid var(--line, #e2e8f0)' }}>
        <div>
          <span style={{ fontSize: 12, color: 'var(--muted, #64748b)', display: 'block' }}>Học viên</span>
          <b style={{ fontSize: 15 }}>{student.name}</b>
        </div>
        <div>
          <span style={{ fontSize: 12, color: 'var(--muted, #64748b)', display: 'block' }}>Mã học viên</span>
          <b>{student.code}</b>
        </div>
        <div>
          <span style={{ fontSize: 12, color: 'var(--muted, #64748b)', display: 'block' }}>Chương trình đăng ký</span>
          <b style={{ color: 'var(--primary, #2563eb)' }}>{student.program || 'Chưa chọn'}</b>
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <h4 style={{ margin: 0, fontSize: 14 }}>Chọn lớp học mở:</h4>
        {student.program && (
          <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={filterSameProgram}
              onChange={(e) => setFilterSameProgram(e.target.checked)}
            />
            Chỉ hiện lớp cùng chương trình ({student.program})
          </label>
        )}
      </div>

      <div style={{ maxHeight: 320, overflowY: 'auto', border: '1px solid var(--line, #e2e8f0)', borderRadius: 8, marginBottom: 16 }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
          <thead>
            <tr style={{ background: 'var(--surface-soft, #f8fafc)', borderBottom: '1px solid var(--line, #e2e8f0)', textAlign: 'left' }}>
              <th style={{ width: 44, padding: '10px 12px', textAlign: 'center' }} />
              <th style={{ padding: '10px 12px' }}>Mã lớp</th>
              <th style={{ padding: '10px 12px' }}>Chương trình</th>
              <th style={{ padding: '10px 12px' }}>Khai giảng</th>
              <th style={{ padding: '10px 12px' }}>Sĩ số</th>
              <th style={{ padding: '10px 12px' }}>Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            {availableClasses.map((c) => {
              const currentSize = classSize(c.code);
              const classIsFull = currentSize >= c.max;
              const isSelected = selectedCode === c.code;
              return (
                <tr
                  key={c.code}
                  onClick={() => !classIsFull && setSelectedCode(c.code)}
                  style={{
                    borderBottom: '1px solid var(--line, #e2e8f0)',
                    cursor: classIsFull ? 'not-allowed' : 'pointer',
                    background: isSelected ? 'var(--soft, #eff6ff)' : classIsFull ? 'var(--surface-soft, #f8fafc)' : undefined,
                    opacity: classIsFull ? 0.65 : 1,
                  }}
                >
                  <td style={{ padding: '10px 12px', textAlign: 'center' }}>
                    <input
                      type="radio"
                      name="selectedClass"
                      checked={isSelected}
                      disabled={classIsFull}
                      onChange={() => setSelectedCode(c.code)}
                      onClick={(e) => e.stopPropagation()}
                    />
                  </td>
                  <td style={{ padding: '10px 12px', fontWeight: 600 }}>{c.code}</td>
                  <td style={{ padding: '10px 12px' }}>{c.program}</td>
                  <td style={{ padding: '10px 12px' }}>{viDate(c.start)}</td>
                  <td style={{ padding: '10px 12px' }}>
                    <span style={{ fontWeight: 600, color: classIsFull ? 'var(--bad, #dc2626)' : 'inherit' }}>
                      {currentSize} / {c.max}
                    </span>
                  </td>
                  <td style={{ padding: '10px 12px' }}>
                    {classIsFull ? (
                      <Pill tone="off">Đã đủ sĩ số</Pill>
                    ) : (
                      <Pill tone="ok">Còn {c.max - currentSize} chỗ</Pill>
                    )}
                  </td>
                </tr>
              );
            })}
            {!availableClasses.length && (
              <tr>
                <td colSpan={6} style={{ padding: 24, textAlign: 'center', color: 'var(--muted, #64748b)' }}>
                  Không có lớp học nào đang mở phù hợp với bộ lọc.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {selectedClass && isFull && (
        <Box icon="alert" tone="bad" title="Lớp đã đủ sĩ số">
          Lớp {selectedClass.code} đã đạt sĩ số tối đa ({size}/{selectedClass.max}). Vui lòng chọn lớp khác hoặc tăng sĩ số của lớp.
        </Box>
      )}

      {selectedClass && !isFull && (
        <div style={{ display: 'flex', gap: 16, alignItems: 'center', padding: '10px 14px', background: 'var(--soft, #eff6ff)', borderRadius: 6, fontSize: 13, marginBottom: 16 }}>
          <Icon name="check" size={16} />
          <div>
            <b>Đã chọn lớp {selectedClass.code}:</b> Sĩ số sau khi ghi danh sẽ là{' '}
            <b>{size + 1}/{selectedClass.max}</b> · Ngày ghi danh: <b>{viDate(TODAY)}</b> · Người thực hiện: <b>{userName}</b>.
          </div>
        </div>
      )}

      <div className="acts" style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 16 }}>
        <button type="button" className="btn" onClick={onClose} disabled={busy}>
          Huỷ
        </button>
        <button
          type="button"
          className="btn primary"
          disabled={!selectedCode || isFull || busy}
          onClick={handleEnroll}
        >
          {busy ? 'Đang ghi danh…' : 'Xác nhận ghi danh'}
        </button>
      </div>
    </Modal>
  );
}
