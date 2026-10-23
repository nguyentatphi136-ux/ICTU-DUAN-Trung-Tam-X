import { useState } from 'react';
import { Box, Modal } from '../../components/ui';
import { apiEnabled, useLoad } from '../../data/api';
import { CLASSES, classSize, ENROLLMENTS, findClass, isOpen } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { viDate } from '../../data/leads';
import { sessionsOf } from '../../data/schedule';
import { loadClasses } from '../../data/s3api';
import { changeStatus } from '../../data/s4api';
import { saveAll } from '../../data/store';
import type { Student, StatusChangeType } from '../../data/students';
import './status.css';

const TYPES: StatusChangeType[] = ['Bảo lưu', 'Chuyển lớp', 'Thôi học'];
const REASONS: Record<StatusChangeType, string[]> = {
  'Bảo lưu': ['Lý do sức khoẻ', 'Bận công việc', 'Lịch thi ở trường', 'Lý do gia đình', 'Lý do khác'],
  'Chuyển lớp': ['Đổi sang lịch học phù hợp hơn', 'Học lại từ đầu', 'Chuyển cơ sở học', 'Lý do khác'],
  'Thôi học': ['Không theo kịp chương trình', 'Chuyển sang trung tâm khác', 'Lý do tài chính', 'Lý do khác'],
};
const month = (iso: string) => iso.slice(5, 7) + '/' + iso.slice(0, 4);

type Props = { s: Student; me: string; onClose: () => void; onDone: (msg: string) => void };

// S4-06. Bảo lưu, chuyển lớp, thôi học. Mỗi loại có lý do và ngày hiệu lực riêng; từ ngày hiệu lực học viên rời danh sách
// điểm danh của lớp cũ. Chuyển lớp giữ nguyên lịch sử điểm và điểm danh ở lớp cũ. Thao tác được ghi vào lịch sử hồ sơ.
export function StatusDialog({ s, me, onClose, onDone }: Props) {
  const k = findClass(s.cls);
  // Có backend: tải các lớp để chọn lớp chuyển đến (máy chủ kiểm lại cùng chương trình, đang mở, còn chỗ).
  useLoad(loadClasses);
  const [busy, setBusy] = useState(false);
  const [type, setType] = useState<StatusChangeType>('Bảo lưu');
  const [at, setAt] = useState(TODAY);
  const [reason, setReason] = useState('');
  const [note, setNote] = useState('');
  const targets = CLASSES.filter((c) => c.code !== s.cls && isOpen(c) && c.program === s.program && classSize(c.code) < c.max);
  const [to, setTo] = useState(targets[0]?.code ?? '');
  const [error, setError] = useState('');
  const done = k ? sessionsOf(k.code).filter((x) => x.date < at && !x.cancelled).length : 0;

  function save() {
    if (!at) return setError('Chọn ngày hiệu lực');
    if (!reason) return setError('Chọn lý do');
    if (reason === 'Lý do khác' && !note.trim()) return setError('Ghi rõ lý do vào ô ghi chú');
    if (type === 'Chuyển lớp' && !to) return setError('Không có lớp phù hợp để chuyển đến');
    const msg = type === 'Chuyển lớp' ? `Đã chuyển ${s.name} sang lớp ${to}` : `Đã ghi nhận ${type.toLowerCase()} cho ${s.name}`;
    if (apiEnabled) {
      setBusy(true);
      changeStatus(s.code, { type, at, reason, note: note.trim(), to: type === 'Chuyển lớp' ? to : undefined }).then(
        () => onDone(msg),
        (e: Error) => (setBusy(false), setError(e.message)),
      );
      return;
    }
    const why = reason === 'Lý do khác' ? note.trim() : reason + (note.trim() ? '. ' + note.trim() : '');
    s.changes = [{ type, at, reason: why, by: me, from: s.cls, to: type === 'Chuyển lớp' ? to : undefined, recorded: TODAY }, ...(s.changes ?? [])];
    if (k) s.history.unshift({ cls: k.code, program: k.program, period: `${month(k.start)} – ${month(at)}`, result: type === 'Chuyển lớp' ? 'Chuyển sang ' + to : type });
    if (type === 'Chuyển lớp') {
      s.cls = to;
      ENROLLMENTS.unshift({ student: s.code, cls: to, at, by: me });
    } else {
      s.status = type === 'Bảo lưu' ? 'Bảo lưu' : 'Thôi học';
      s.cls = undefined;
    }
    saveAll();
    onDone(msg);
  }

  const effects: Record<StatusChangeType, string[]> = {
    'Bảo lưu': [`Rời danh sách điểm danh của ${k?.code ?? 'lớp hiện tại'}.`, `Điểm và điểm danh ${done} buổi đã học được giữ trong lịch sử.`, 'Khi quay lại, Quản lý đào tạo ghi danh học viên vào lớp phù hợp.'],
    'Chuyển lớp': [`Rời danh sách điểm danh của ${k?.code ?? 'lớp hiện tại'}, vào danh sách của ${to || 'lớp mới'}.`, `Điểm và điểm danh ${done} buổi ở ${k?.code ?? 'lớp cũ'} được giữ nguyên trong lịch sử.`],
    'Thôi học': [`Rời danh sách điểm danh của ${k?.code ?? 'lớp hiện tại'}.`, 'Hồ sơ chuyển sang Thôi học, vẫn tra cứu được về sau.'],
  };

  return (
    <Modal onClose={onClose} label="Thay đổi tình trạng học" wide>
      <h3 style={{ marginBottom: 4 }}>Thay đổi tình trạng học</h3>
      <p className="hint" style={{ fontSize: 14, marginBottom: 16 }}>
        {s.name} · {s.code} · {k ? `đang học lớp ${k.code}` : 'chưa xếp lớp'}
      </p>
      <div className="seg st-type" role="radiogroup" aria-label="Loại thao tác">
        {TYPES.map((t) => (
          <button
            key={t}
            type="button"
            role="radio"
            aria-checked={type === t}
            className={type === t ? 'on' : undefined}
            onClick={() => {
              setType(t);
              setReason('');
              setError('');
            }}
          >
            {t}
          </button>
        ))}
      </div>
      {type === 'Chuyển lớp' && (
        <div className="field">
          <label htmlFor="st-to">Lớp chuyển đến</label>
          <div className="inp">
            <select id="st-to" value={to} onChange={(e) => setTo(e.target.value)}>
              {!targets.length && <option value="">Không có lớp phù hợp</option>}
              {targets.map((c) => (
                <option key={c.code} value={c.code}>
                  {c.code} · {c.program} · còn {c.max - classSize(c.code)} chỗ
                </option>
              ))}
            </select>
          </div>
          <p className="hint">Chỉ hiện lớp cùng chương trình, đang Chuẩn bị hoặc Đang học và còn chỗ</p>
        </div>
      )}
      <div className="grid2">
        <div className="field">
          <label htmlFor="st-at">Ngày hiệu lực</label>
          <div className="inp">
            <input id="st-at" type="date" value={at} onChange={(e) => setAt(e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label htmlFor="st-reason">Lý do</label>
          <div className="inp">
            <select id="st-reason" value={reason} onChange={(e) => setReason(e.target.value)}>
              <option value="">Chọn lý do</option>
              {REASONS[type].map((r) => (
                <option key={r}>{r}</option>
              ))}
            </select>
          </div>
        </div>
      </div>
      <div className="field">
        <div className="inp">
          <textarea aria-label="Ghi chú" value={note} placeholder="Ghi chú thêm, ví dụ: học viên đi làm ca tối thứ Hai." onChange={(e) => setNote(e.target.value)} />
        </div>
      </div>
      <div className="st-effects">
        <b>Điều sẽ xảy ra từ ngày {at ? viDate(at) : '…'}</b>
        <ul>
          {[...effects[type], `Thao tác được ghi vào lịch sử hồ sơ kèm người thực hiện ${me}.`].map((x) => (
            <li key={x}>{x}</li>
          ))}
        </ul>
      </div>
      <Box icon="alert" tone="info" title="Bảo lưu và Thôi học cũng có lý do và ngày hiệu lực riêng">
        Học viên bảo lưu không còn trong danh sách điểm danh từ ngày hiệu lực.
      </Box>
      {error && <p className="err" style={{ marginBottom: 12 }}>{error}</p>}
      <div className="acts">
        <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className={'btn ' + (type === 'Thôi học' ? 'danger' : 'primary')} style={{ minWidth: 130 }} disabled={busy} onClick={save}>
          {type}
        </button>
      </div>
    </Modal>
  );
}
