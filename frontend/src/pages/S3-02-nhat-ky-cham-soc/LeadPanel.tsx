import { useState, type ReactNode } from 'react';
import { Icon, type IconName } from '../../components/Icon';
import { Drawer, Pill } from '../../components/ui';
import { logsOf, overdueDays, STAGES, stageTone, TODAY, viDateTime, type CareLog, type FunnelLead, type LogKind, type Stage } from '../../data/funnel';
import './care.css';

type Props = {
  lead: FunnelLead;
  canWrite: boolean;
  /** Người đang đăng nhập, ghi vào nhật ký. */
  me: string;
  onClose: () => void;
  onLog: (log: CareLog, callback?: string) => void;
  onMove: (to: Stage) => void;
  /** Phần thêm ở đầu ngăn, ví dụ nút chuyển thành học viên (S3-03). */
  extra?: ReactNode;
};

const KINDS: [LogKind, IconName][] = [
  ['Gọi', 'phone'],
  ['Nhắn', 'chat'],
  ['Gặp', 'users'],
];
const ICONS: Record<LogKind, IconName> = { Gọi: 'phone', Nhắn: 'chat', Gặp: 'users', 'Đổi trạng thái': 'swap', 'Phân công': 'user' };

// S3-02. Bấm vào thẻ lead để mở ngăn bên: ghi lần tương tác mới (gọi, nhắn, gặp), đặt ngày nhắc gọi lại, xem nhật ký mới nhất ở trên.
export function LeadPanel({ lead, canWrite, me, onClose, onLog, onMove, extra }: Props) {
  const [kind, setKind] = useState<LogKind>('Gọi');
  const [at, setAt] = useState(TODAY + 'T10:15');
  const [text, setText] = useState('');
  const [callback, setCallback] = useState('');
  const [error, setError] = useState('');
  const late = overdueDays(lead);
  const logs = logsOf(lead);
  const lastTouch = logs.find((l) => l.kind === 'Gọi' || l.kind === 'Nhắn' || l.kind === 'Gặp');

  function save() {
    if (!text.trim()) return setError('Ghi lại nội dung trao đổi với khách');
    if (callback && callback < TODAY) return setError('Ngày nhắc gọi lại phải từ hôm nay trở đi');
    onLog({ kind, at, by: me, text: text.trim() }, callback || undefined);
    setText('');
    setCallback('');
    setError('');
  }

  return (
    <Drawer title={lead.name} onClose={onClose}>
      <div className="cr-tags">
        <Pill tone={stageTone(lead.stage)}>{lead.stage}</Pill>
        {late > 0 && <Pill tone="off">Quá hạn gọi lại {late} ngày</Pill>}
        {canWrite && (
          <label className="cr-stage">
            Chuyển sang
            <select value={lead.stage} onChange={(e) => onMove(e.target.value as Stage)} aria-label="Chuyển giai đoạn">
              {STAGES.map((s) => (
                <option key={s}>{s}</option>
              ))}
            </select>
          </label>
        )}
      </div>
      <p className="cr-sub">
        {lead.phone} · {lead.program} · {lead.source}
      </p>
      {extra}

      {canWrite && (
        <section className="cr-new">
          <h4>Ghi lần tương tác mới</h4>
          <div className="cr-row">
            <div className="field">
              <span className="lbl">Loại</span>
              <div className="cr-kinds" role="radiogroup" aria-label="Loại tương tác">
                {KINDS.map(([k, ic]) => (
                  <button key={k} type="button" role="radio" aria-checked={kind === k} className={kind === k ? 'on' : undefined} onClick={() => setKind(k)}>
                    <Icon name={ic} size={16} />
                    {k}
                  </button>
                ))}
              </div>
            </div>
            <div className="field">
              <label htmlFor="cr-at">Thời điểm</label>
              <div className="inp">
                <input id="cr-at" type="datetime-local" value={at} onChange={(e) => setAt(e.target.value)} />
              </div>
            </div>
          </div>
          <div className="field">
            <label htmlFor="cr-text">Nội dung</label>
            <div className={'inp' + (error && !text.trim() ? ' bad' : '')}>
              <textarea id="cr-text" value={text} placeholder="Khách đã xem lịch khai giảng, hẹn trả lời sau khi bàn với gia đình." onChange={(e) => setText(e.target.value)} />
            </div>
          </div>
          <div className="cr-row end">
            <div className="field">
              <label htmlFor="cr-cb">Nhắc gọi lại vào ngày</label>
              <div className="inp lead" style={{ width: 240 }}>
                <Icon name="cal" />
                <input id="cr-cb" type="date" min={TODAY} value={callback} onChange={(e) => setCallback(e.target.value)} />
              </div>
            </div>
            <button type="button" className="btn primary" style={{ width: 140, marginBottom: 16 }} onClick={save}>
              Lưu nhật ký
            </button>
          </div>
          {error && <p className="err" style={{ marginTop: -8 }}>{error}</p>}
        </section>
      )}

      <section>
        <h4>Nhật ký</h4>
        <ol className="cr-log">
          {late > 0 && (
            <li className="bad">
              <span className="dot">
                <Icon name="alert" size={14} />
              </span>
              <div>
                <b>Nhắc gọi lại {viDateTime(lead.callback!)} đã quá hạn</b>
                <p>Chưa có lần tương tác nào sau {lastTouch ? viDateTime(lastTouch.at) : 'khi nhận lead'}.</p>
              </div>
            </li>
          )}
          {logs.map((l, i) => (
            <li key={i}>
              <span className="dot">
                <Icon name={ICONS[l.kind]} size={14} />
              </span>
              <div>
                <b>
                  {l.kind} · {viDateTime(l.at)} · {l.by}
                </b>
                <p>{l.text}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>
    </Drawer>
  );
}
