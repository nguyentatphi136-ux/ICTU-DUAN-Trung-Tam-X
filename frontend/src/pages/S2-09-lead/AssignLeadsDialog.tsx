import { useState } from 'react';
import { Icon } from '../../components/Icon';
import { CONSULTANTS, type Lead, type Transfer } from '../../data/leads';

/** Giao các lead cho một tư vấn viên và ghi một dòng lịch sử chuyển giao cho mỗi lead đổi người phụ trách. */
export function assignLeads(leads: Lead[], ids: number[], to: string, by: string, at: string, note?: string): Lead[] {
  const picked = new Set(ids);
  return leads.map((l) => {
    if (!picked.has(l.id) || l.owner === to) return l;
    const entry: Transfer = { at, from: l.owner, to, by, note: note || undefined };
    return { ...l, owner: to, history: [...l.history, entry] };
  });
}

type Props = { leads: Lead[]; onAssign: (to: string, note: string) => void; onCancel: () => void };

// S2-10. Phân công một hoặc nhiều lead cùng lúc. Khi tích hợp: POST /leads/assign { ids, ownerId, note }.
export function AssignLeadsDialog({ leads, onAssign, onCancel }: Props) {
  const [to, setTo] = useState('');
  const [note, setNote] = useState('');
  const reassigned = leads.filter((l) => l.owner !== '' && l.owner !== to);
  const unchanged = to ? leads.filter((l) => l.owner === to).length : 0;

  return (
    <div className="overlay">
      <div className="modal" role="dialog" aria-labelledby="assign-title">
        <h3 id="assign-title">Phân công {leads.length} lead</h3>
        <p className="muted" style={{ margin: '8px 0 16px' }}>
          {leads
            .slice(0, 3)
            .map((l) => l.name)
            .join(', ')}
          {leads.length > 3 ? ` và ${leads.length - 3} lead khác` : ''}
        </p>
        <div className="field">
          <label htmlFor="assign-to">Tư vấn viên phụ trách</label>
          <div className="inp">
            <select id="assign-to" value={to} onChange={(e) => setTo(e.target.value)}>
              <option value="">Chọn tư vấn viên</option>
              {CONSULTANTS.map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
          </div>
        </div>
        <div className="field">
          <label htmlFor="assign-note">Ghi chú bàn giao (không bắt buộc)</label>
          <div className="inp">
            <textarea id="assign-note" style={{ height: 72 }} value={note} onChange={(e) => setNote(e.target.value)} />
          </div>
        </div>
        {to && reassigned.length > 0 && (
          <div className="box warn">
            <Icon name="alert" />
            <div>
              <b>{reassigned.length} lead đang có người phụ trách</b>
              Các lead này sẽ chuyển sang {to}. Người phụ trách cũ không còn thấy chúng trong danh sách của mình.
            </div>
          </div>
        )}
        {unchanged > 0 && <p className="hint">{unchanged} lead đã thuộc về {to} nên giữ nguyên.</p>}
        <div className="acts">
          <button className="btn" onClick={onCancel}>
            Huỷ
          </button>
          <button className="btn primary" disabled={!to} onClick={() => onAssign(to, note.trim())}>
            Phân công
          </button>
        </div>
      </div>
    </div>
  );
}
