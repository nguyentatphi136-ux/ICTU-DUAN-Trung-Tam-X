import { useState } from 'react';
import { Box, Modal, Pill } from '../../components/ui';
import { COUNSELORS, type Lead } from '../../data/leads';

// S2-10. Phân công một hoặc nhiều lead cho một tư vấn viên. Số lead đang giữ giúp chia việc đều.
// Khi tích hợp: POST /admissions/leads/assign { ids, counselorId, note }; máy chủ ghi lịch sử chuyển giao.
export function AssignDialog({ leads, load, onClose, onAssign }: { leads: Lead[]; load: (c: string) => number; onClose: () => void; onAssign: (c: string) => void }) {
  const [pick, setPick] = useState('');
  const [note, setNote] = useState('');
  return (
    <Modal onClose={onClose} label="Phân công lead" wide>
      <h3>Phân công {leads.length} lead</h3>
      <div className="tags" style={{ marginBottom: 20 }}>
        {leads.map((l) => (
          <Pill key={l.id}>{l.name}</Pill>
        ))}
      </div>
      <div className="field">
        <label>Giao cho tư vấn viên</label>
        <div className="ld-options" role="radiogroup">
          {COUNSELORS.map((c) => (
            <label key={c} className={'ld-option' + (pick === c ? ' on' : '')}>
              <input type="radio" name="counselor" checked={pick === c} onChange={() => setPick(c)} />
              <b>{c}</b>
              <span>đang giữ {load(c)} lead</span>
            </label>
          ))}
        </div>
      </div>
      <div className="field">
        <label htmlFor="as-note">Ghi chú chuyển giao (không bắt buộc)</label>
        <div className="inp">
          <input id="as-note" value={note} placeholder="Ví dụ: lead từ biểu mẫu web cuối tuần" onChange={(e) => setNote(e.target.value)} />
        </div>
      </div>
      <Box icon="clock" title="Lịch sử chuyển giao được ghi lại">
        Mỗi lead lưu ai giao, giao cho ai và vào lúc nào.
      </Box>
      <div className="acts">
        <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className="btn primary" style={{ width: 150 }} disabled={!pick} onClick={() => onAssign(pick)}>
          Phân công
        </button>
      </div>
    </Modal>
  );
}
