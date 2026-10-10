import { useMemo, useState, type DragEvent } from 'react';
import { Link } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Pill, Search } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { addLog, ddmm, FUNNEL, overdueDays, STAGES, stageTone, TODAY, type FunnelLead, type Stage } from '../../data/funnel';
import { LEADS } from '../../data/leads';
import { LeadDrawer } from '../S2-09-lead/LeadDrawer';
import { RejectDialog } from './RejectDialog';
import './funnel.css';

const MODULE = 'Tuyển sinh & lead';
/** Số thẻ hiện sẵn mỗi cột, phần còn lại gộp vào "và n lead khác". */
const SHOWN = 3;

// S3-01. Phễu lead dạng bảng Kanban sáu cột. Kéo thẻ sang cột khác để đổi giai đoạn; thả vào Từ chối thì phải chọn lý do.
// Mỗi lần đổi giai đoạn đều được ghi vào nhật ký chăm sóc (S3-02 hiển thị nhật ký khi bấm vào thẻ).
export function FunnelPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [leads, setLeads] = useState(FUNNEL);
  const [q, setQ] = useState('');
  const [onlyLate, setOnlyLate] = useState(false);
  const [open, setOpen] = useState<Stage[]>([]);
  const [dragging, setDragging] = useState<number | null>(null);
  const [over, setOver] = useState<Stage | null>(null);
  const [rejecting, setRejecting] = useState<FunnelLead | null>(null);
  const [adding, setAdding] = useState(false);

  const role = user?.active ?? -1;
  const canWrite = role === 3 || role === 5 || role === 6;

  const commit = (list: FunnelLead[]) => {
    FUNNEL.splice(0, FUNNEL.length, ...list);
    setLeads(list);
  };

  const shown = useMemo(() => {
    const k = q.trim().toLowerCase();
    const digits = k.replace(/\D/g, '');
    return leads.filter(
      (l) =>
        (!k || l.name.toLowerCase().includes(k) || (digits.length > 2 && l.phone.replace(/\D/g, '').includes(digits))) &&
        (!onlyLate || overdueDays(l) > 0),
    );
  }, [leads, q, onlyLate]);
  const late = leads.filter((l) => overdueDays(l) > 0).length;

  const now = () => TODAY + 'T' + new Date().toTimeString().slice(0, 5);
  const logStage = (l: FunnelLead, to: Stage, why?: string) =>
    addLog(l, { kind: 'Đổi trạng thái', at: now(), by: user!.name, text: `${l.stage} → ${to}` + (why ? ` · ${why}` : '') });

  function move(l: FunnelLead, to: Stage) {
    if (l.stage === to) return;
    if (to === 'Từ chối') return setRejecting(l);
    logStage(l, to);
    commit(leads.map((x) => (x.id === l.id ? { ...x, stage: to, reason: undefined } : x)));
    toast('Đã chuyển giai đoạn', `${l.name}: ${l.stage} → ${to}`);
  }

  const drop = (to: Stage) => (e: DragEvent) => {
    e.preventDefault();
    setOver(null);
    const l = leads.find((x) => x.id === dragging);
    setDragging(null);
    if (l) move(l, to);
  };

  return (
    <AppLayout crumb={[MODULE, 'Phễu lead']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Phễu lead</h2>
          <p>
            {leads.length} lead đang theo dõi · {late} lead quá hạn gọi lại
          </p>
        </div>
        {canWrite && (
          <button type="button" className="btn primary" style={{ width: 140 }} onClick={() => setAdding(true)}>
            <Icon name="plus" size={16} />
            Thêm lead
          </button>
        )}
      </div>
      <div className="bar">
        <Search placeholder="Tìm theo tên hoặc số điện thoại" value={q} onChange={setQ} width={320} />
        <div className="seg" role="tablist" aria-label="Chế độ xem">
          <span className="on" role="tab" aria-selected="true">
            Bảng phễu
          </span>
          <Link to="/tuyen-sinh/lead" role="tab" aria-selected="false">
            Danh sách
          </Link>
        </div>
        <label className="check-box">
          <input type="checkbox" checked={onlyLate} onChange={(e) => setOnlyLate(e.target.checked)} />
          Chỉ hiện lead quá hạn
        </label>
      </div>

      <div className="kb">
        {STAGES.map((s) => {
          const col = shown.filter((l) => l.stage === s);
          const all = open.includes(s) || q || onlyLate;
          const cards = all ? col : col.slice(0, SHOWN);
          return (
            <section
              key={s}
              className={'kb-col' + (over === s ? ' over' : '')}
              aria-label={s}
              onDragOver={(e) => {
                if (dragging === null) return;
                e.preventDefault();
                setOver(s);
              }}
              onDragLeave={() => setOver((o) => (o === s ? null : o))}
              onDrop={drop(s)}
            >
              <header>
                <b>{s}</b>
                <Pill tone={stageTone(s)}>{col.length}</Pill>
              </header>
              {cards.map((l) => (
                <LeadCard key={l.id} lead={l} draggable={canWrite} onDragStart={() => setDragging(l.id)} onDragEnd={() => setDragging(null)} />
              ))}
              {cards.length < col.length && (
                <button type="button" className="kb-more" onClick={() => setOpen((o) => [...o, s])}>
                  và {col.length - cards.length} lead khác
                </button>
              )}
            </section>
          );
        })}
      </div>

      {rejecting && (
        <RejectDialog
          lead={rejecting}
          onClose={() => setRejecting(null)}
          onConfirm={(reason) => {
            logStage(rejecting, 'Từ chối', reason);
            commit(leads.map((x) => (x.id === rejecting.id ? { ...x, stage: 'Từ chối', reason } : x)));
            toast('Đã chuyển sang Từ chối', `${rejecting.name}: ${reason}`);
            setRejecting(null);
          }}
        />
      )}
      {adding && (
        <LeadDrawer
          lead={null}
          leads={LEADS}
          readOnly={false}
          canDelete={false}
          onOpen={() => {}}
          onClose={() => setAdding(false)}
          onDelete={() => {}}
          onSave={(l) => {
            const owner = role === 3 ? user!.name : '';
            LEADS.unshift({ ...l, id: Math.max(...LEADS.map((x) => x.id)) + 1, owner });
            commit([...leads, { id: Math.max(...leads.map((x) => x.id)) + 1, name: l.name, phone: l.phone, email: l.email, program: l.program, source: l.source, owner, stage: 'Mới' }]);
            toast('Đã thêm lead', l.name);
            setAdding(false);
          }}
        />
      )}
    </AppLayout>
  );
}

type CardProps = { lead: FunnelLead; draggable: boolean; onDragStart: () => void; onDragEnd: () => void };

function LeadCard({ lead: l, draggable, onDragStart, onDragEnd }: CardProps) {
  const late = overdueDays(l);
  return (
    <button
      type="button"
      className={'kb-card' + (late ? ' late' : '')}
      draggable={draggable}
      onDragStart={(e) => {
        e.dataTransfer.effectAllowed = 'move';
        onDragStart();
      }}
      onDragEnd={onDragEnd}
    >
      <b>{l.name}</b>
      <span>{l.phone}</span>
      <small>{l.program}</small>
      <CardTag lead={l} late={late} />
    </button>
  );
}

/** Nhãn cuối thẻ: lý do từ chối, chờ chuyển học viên, quá hạn, hẹn gọi lại hoặc học thử. */
function CardTag({ lead: l, late }: { lead: FunnelLead; late: number }) {
  if (l.stage === 'Từ chối') return l.reason ? <Pill>{l.reason}</Pill> : null;
  if (l.stage === 'Chốt') return <Pill tone="ok">Chờ chuyển học viên</Pill>;
  if (late) return <Pill tone="off">Quá hạn {late} ngày</Pill>;
  if (l.callback === TODAY) return <Pill tone="wait">Gọi lại hôm nay</Pill>;
  if (l.trial) return <Pill tone="info">Học thử {ddmm(l.trial)}</Pill>;
  if (l.callback) return <Pill>Gọi lại {ddmm(l.callback)}</Pill>;
  return null;
}
