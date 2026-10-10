import { useState } from 'react';
import { Modal } from '../../components/ui';
import { REJECT_REASONS, type FunnelLead } from '../../data/funnel';

type Props = { lead: FunnelLead; onClose: () => void; onConfirm: (reason: string) => void };

// S3-01. Thả thẻ vào cột Từ chối thì bắt buộc chọn lý do. "Lý do khác" thì phải ghi chú.
export function RejectDialog({ lead, onClose, onConfirm }: Props) {
  const [reason, setReason] = useState('');
  const [note, setNote] = useState('');
  const [error, setError] = useState('');

  function confirm() {
    if (!reason) return setError('Chọn một lý do từ chối');
    if (reason === 'Lý do khác' && !note.trim()) return setError('Ghi rõ lý do vào ô ghi chú');
    onConfirm(reason === 'Lý do khác' ? note.trim() : reason);
  }

  return (
    <Modal onClose={onClose} label="Chuyển lead sang Từ chối">
      <h3 style={{ marginBottom: 4 }}>Chuyển lead sang Từ chối</h3>
      <p className="hint" style={{ fontSize: 14, marginBottom: 18 }}>
        {lead.name} · {lead.phone} · đang ở {lead.stage}
      </p>
      <fieldset className="kb-reasons">
        <legend>
          Lý do từ chối <span className="pill off">bắt buộc</span>
        </legend>
        {REJECT_REASONS.map((r) => (
          <label key={r} className={'ld-option' + (reason === r ? ' on' : '')}>
            <input
              type="radio"
              name="reason"
              checked={reason === r}
              onChange={() => {
                setReason(r);
                setError('');
              }}
            />
            <b>{r}</b>
          </label>
        ))}
      </fieldset>
      <div className="field">
        <label htmlFor="kb-note">Ghi chú thêm {reason === 'Lý do khác' ? '' : '(không bắt buộc)'}</label>
        <div className="inp">
          <input id="kb-note" value={note} placeholder="Ví dụ: khách chỉ học được tối thứ Bảy" onChange={(e) => setNote(e.target.value)} />
        </div>
      </div>
      {error && <p className="err" style={{ marginTop: -8, marginBottom: 12 }}>{error}</p>}
      <div className="acts">
        <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className="btn danger" style={{ width: 116 }} onClick={confirm}>
          Từ chối
        </button>
      </div>
    </Modal>
  );
}
