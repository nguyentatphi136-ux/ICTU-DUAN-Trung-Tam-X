import { useState } from 'react';
import { Box, Modal } from '../../components/ui';
import { vdt, type Assignment, type Submission } from '../../data/assignments';
import { notify } from '../../data/notifications';
import { saveAll } from '../../data/store';
import { nowIso } from '../S5-06-bai-tap-cua-toi/MyAssignmentsPage';

type Props = { a: Assignment; sub: Submission; name: string; me: string; onClose: () => void; onDone: () => void };

// S6-03. Yêu cầu học viên làm lại: chuyển bài nộp sang Cần làm lại kèm nhận xét bắt buộc và hạn nộp mới.
// Học viên nhận thông báo và nộp lại được tới hạn mới; số lần yêu cầu làm lại được lưu lại.
export function RedoDialog({ a, sub, name, me, onClose, onDone }: Props) {
  const [note, setNote] = useState('');
  const [due, setDue] = useState('');
  const [error, setError] = useState('');
  const past = sub.redos ?? [];

  function send() {
    if (!note.trim()) return setError('Bắt buộc ghi rõ cần sửa gì để học viên làm lại');
    if (!due || due <= nowIso()) return setError('Hạn nộp mới phải sau thời điểm hiện tại');
    const redo = { note: note.trim(), due, by: me, at: nowIso() };
    sub.redo = redo;
    sub.redos = [...past, redo];
    sub.state = 'Cần làm lại';
    notify({ at: redo.at, kind: 'Bài tập', title: 'Cần làm lại: ' + a.title.split(':')[0], body: `${redo.note} Hạn nộp lại ${vdt(due)}.`, link: '/bai-tap-cua-toi/' + a.id, to: { users: [name] } });
    saveAll();
    onDone();
  }

  return (
    <Modal onClose={onClose} label="Yêu cầu làm lại">
      <h3 style={{ marginBottom: 4 }}>Yêu cầu làm lại</h3>
      <p className="hint" style={{ fontSize: 14, marginBottom: 16 }}>
        {name} · {a.title.split(':')[0]} · bản {sub.versions[0].no}
      </p>
      {past.length > 0 && (
        <Box icon="refresh" tone="warn" title={`Đã yêu cầu làm lại ${past.length} lần trước đó (${vdt(past.at(-1)!.at).slice(0, 10)})`} />
      )}
      <div className="field">
        <label htmlFor="rd-note">
          Nhận xét cho học viên <span className="pill off">bắt buộc</span>
        </label>
        <div className={'inp' + (error && !note.trim() ? ' bad' : '')}>
          <textarea id="rd-note" rows={4} value={note} placeholder="Ví dụ: phần kiểm tra số điện thoại chưa đúng với số bắt đầu bằng 03." onChange={(e) => (setNote(e.target.value), setError(''))} />
        </div>
      </div>
      <div className="field">
        <label htmlFor="rd-due">Hạn nộp mới</label>
        <div className={'inp' + (error && note.trim() ? ' bad' : '')}>
          <input id="rd-due" type="datetime-local" min={nowIso()} value={due} onChange={(e) => (setDue(e.target.value), setError(''))} />
        </div>
        {error ? <p className="err">{error}</p> : <p className="hint">Phải sau thời điểm hiện tại</p>}
      </div>
      <Box icon="bell" tone="info" title="Học viên nhận thông báo và nộp lại được tới hạn mới" />
      <div className="acts">
        <button type="button" className="btn" style={{ width: 90 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className="btn primary" style={{ width: 140 }} onClick={send}>
          Gửi yêu cầu
        </button>
      </div>
    </Modal>
  );
}
