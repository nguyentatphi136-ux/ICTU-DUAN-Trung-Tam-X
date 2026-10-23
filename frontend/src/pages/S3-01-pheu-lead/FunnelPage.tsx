import { useEffect, useMemo, useState, type DragEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { LoadState, Pill, Search } from '../../components/ui';
import { apiEnabled, useLoad } from '../../data/api';
import { useAuth } from '../../data/auth';
import { loadPublicPrograms, saveLead, type PublicProgram } from '../../data/s2api';
import { addLeadLog, loadFunnel, loadLeadLogs, moveLead } from '../../data/s3api';
import { addLog, ddmm, FUNNEL, overdueDays, STAGES, stageTone, TODAY, type FunnelLead, type Stage } from '../../data/funnel';
import { addLead, syncFunnelStage, syncAllLeadsAndFunnel, LEADS } from '../../data/leads';
import { notify } from '../../data/notifications';
import { isCallbackDue, runLeadReminders } from '../../data/leadReminders';
import { LeadDrawer } from '../S2-09-lead/LeadDrawer';
import { LeadPanel } from '../S3-02-nhat-ky-cham-soc/LeadPanel';
import { ConvertBlock, ConvertDialog } from '../S3-03-chuyen-hoc-vien/ConvertDialog';
import { RejectDialog } from './RejectDialog';
import './funnel.css';

const MODULE = 'Tuyển sinh & lead';
/** Số thẻ hiện sẵn mỗi cột, phần còn lại gộp vào "và n lead khác". */
const SHOWN = 3;

// S3-01. Phễu lead dạng bảng Kanban sáu cột. Kéo thẻ sang cột khác để đổi giai đoạn; thả vào Từ chối thì phải chọn lý do.
// S3-02. Bấm vào thẻ để mở ngăn nhật ký chăm sóc. Mỗi lần đổi giai đoạn đều được ghi vào nhật ký.
// S3-03. Lead ở cột Chốt có nút chuyển thành học viên; lead đã chuyển không kéo, không sửa được nữa.
export function FunnelPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [leads, setLeads] = useState(() => {
    syncAllLeadsAndFunnel();
    return [...FUNNEL];
  });
  const [q, setQ] = useState('');
  const [onlyLate, setOnlyLate] = useState(false);
  const [open, setOpen] = useState<Stage[]>([]);
  const [dragging, setDragging] = useState<number | null>(null);
  const [over, setOver] = useState<Stage | null>(null);
  const [rejecting, setRejecting] = useState<FunnelLead | null>(null);
  const [adding, setAdding] = useState(false);
  const [converting, setConverting] = useState<FunnelLead | null>(null);
  const [params] = useSearchParams();
  // ?lead=<id> mở sẵn ngăn của lead đó (dùng cho liên kết từ trang khác và mục lục màn hình).
  const [openId, setOpenId] = useState<number | null>(Number(params.get('lead')) || null);

  useEffect(() => {
    const p = Number(params.get('lead'));
    if (p) setOpenId(p);
  }, [params]);

  const role = user?.active ?? -1;
  // Sheet 2: chỉ Tư vấn tuyển sinh sửa lead; Quản lý đào tạo, Kế toán chỉ xem.
  const canWrite = role === 3;

  useEffect(() => {
    syncAllLeadsAndFunnel();
    setLeads([...FUNNEL]);
    const handleUpdate = () => {
      setLeads([...FUNNEL]);
    };
    window.addEventListener('tms:lead-updated', handleUpdate);
    window.addEventListener('storage', handleUpdate);
    return () => {
      window.removeEventListener('tms:lead-updated', handleUpdate);
      window.removeEventListener('storage', handleUpdate);
    };
  }, []);

  const [programs, setPrograms] = useState<PublicProgram[]>([]);
  const load = useLoad(async () => {
    const [, list] = await Promise.all([loadFunnel(), loadPublicPrograms()]);
    setPrograms(list);
    setLeads([...FUNNEL]);
  });

  const commit = (list: FunnelLead[]) => {
    FUNNEL.splice(0, FUNNEL.length, ...list);
    setLeads([...FUNNEL]);
  };
  /** Có backend: gọi API, tải lại phễu và nhật ký của lead đang mở rồi báo; lỗi thì báo lý do máy chủ trả về. */
  const sync = (call: Promise<unknown>, title: string, sub?: string, after?: () => void) =>
    call
      .then(() => Promise.all([loadFunnel(), openId ? loadLeadLogs(openId) : null]))
      .then(() => {
        setLeads([...FUNNEL]);
        after?.();
        toast(title, sub);
      })
      .catch((e: Error) => toast('Chưa lưu được', e.message));

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
    if (l.stage === to || l.student) return;
    if (to === 'Từ chối') return setRejecting(l);
    if (apiEnabled) return void sync(moveLead(l.id, to), 'Đã chuyển giai đoạn', `${l.name}: ${l.stage} → ${to}`);
    logStage(l, to);
    syncFunnelStage(l.id, to);
    setLeads([...FUNNEL]);
    toast('Đã chuyển giai đoạn', `${l.name}: ${l.stage} → ${to}`);
  }
  const panel = leads.find((l) => l.id === openId);

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
        <div className="seg" role="tablist" aria-label="Chế độ xem">
          <span className="on" role="tab" aria-selected="true">
            Bảng phễu
          </span>
          <Link to="/tuyen-sinh/lead" role="tab" aria-selected="false">
            Danh sách
          </Link>
        </div>
        {canWrite && (
          <button type="button" className="btn primary" style={{ width: 140 }} onClick={() => setAdding(true)}>
            <Icon name="plus" size={16} />
            Thêm lead
          </button>
        )}
      </div>
      <LoadState {...load} />
      <div className="bar">
        <Search placeholder="Tìm theo tên hoặc số điện thoại" value={q} onChange={setQ} width={320} />
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
                <LeadCard key={l.id} lead={l} draggable={canWrite && !l.student} onOpen={() => setOpenId(l.id)} onDragStart={() => setDragging(l.id)} onDragEnd={() => setDragging(null)} />
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
            if (apiEnabled) return void sync(moveLead(rejecting.id, 'Từ chối', reason), 'Đã chuyển sang Từ chối', `${rejecting.name}: ${reason}`, () => setRejecting(null));
            logStage(rejecting, 'Từ chối', reason);
            syncFunnelStage(rejecting.id, 'Từ chối', reason);
            setLeads([...FUNNEL]);
            toast('Đã chuyển sang Từ chối', `${rejecting.name}: ${reason}`);
            setRejecting(null);
          }}
        />
      )}
      {panel && (
        <LeadPanel
          key={panel.id}
          lead={panel}
          canWrite={canWrite && !panel.student}
          me={user!.name}
          extra={<ConvertBlock lead={panel} canWrite={canWrite} onConvert={() => setConverting(panel)} />}
          onClose={() => setOpenId(null)}
          onMove={(to) => move(panel, to)}
          onLog={(log, callback) => {
            if (apiEnabled) return void sync(addLeadLog(panel.id, log, callback), 'Đã lưu nhật ký', callback ? `Nhắc gọi lại ${ddmm(callback)}.` : panel.name);
            addLog(panel, log);
            commit(leads.map((x) => (x.id === panel.id ? { ...x, callback } : x)));
            if (callback && isCallbackDue(callback)) {
              runLeadReminders((lead) => {
                toast('🔔 ĐẾN HẸN GỌI LẠI KHÁCH HÀNG', `${lead.name} (${lead.phone}) · Hẹn: ${ddmm(lead.callback || '')}`);
              }, user);
            }
            toast('Đã lưu nhật ký', callback ? `Nhắc gọi lại: ${ddmm(callback)}.` : panel.name);
          }}
        />
      )}
      {converting && (
        <ConvertDialog
          lead={converting}
          onClose={() => setConverting(null)}
          onDone={(code, sentTo, emailSent, tempPass, loginEmail) => {
            const passText = tempPass ? ` · Mật khẩu: ${tempPass}` : '';
            const accText = loginEmail ? `Tài khoản: ${loginEmail}${passText}. ` : '';
            const sub = emailSent
              ? `${accText}Email kích hoạt đã gửi tới ${sentTo}. Vui lòng kiểm tra hộp thư (cả thư mục Thư rác/Spam).`
              : `${accText}Chưa gửi được email tới ${sentTo}, hãy thông báo mật khẩu tạm trực tiếp cho học viên.`;
            setConverting(null);
            if (apiEnabled) return void sync(Promise.resolve(), 'Đã tạo học viên ' + code, sub);
            addLog(converting, { kind: 'Chuyển thành học viên', at: now(), by: user!.name, text: `Tạo hồ sơ ${code} và tài khoản đăng nhập.` });
            syncFunnelStage(converting.id, 'Chốt', undefined, code);
            setLeads([...FUNNEL]);
            notify({
              at: now(),
              kind: 'Lead',
              title: `Đã cấp tài khoản học viên ${code} (${converting.name})`,
              body: `Tài khoản: ${loginEmail || code} · Mật khẩu tạm: ${tempPass || 'Tms@2026'}. Email nhận: ${sentTo} (${emailSent ? 'Đã gửi thành công' : 'Chưa gửi được email'}).`,
              link: `/hoc-vien/${code}`,
              to: { users: [converting.owner, user!.name, converting.name, loginEmail || '', code].filter(Boolean) },
              ref: `lead-conv-${converting.id}-${code}`,
            });
            toast('Đã tạo học viên ' + code, sub);
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
          programs={apiEnabled ? programs.map((p) => p.name) : undefined}
          onSave={(l) => {
            if (apiEnabled) return void sync(saveLead(l, programs), 'Đã thêm lead', l.name, () => setAdding(false));
            const owner = l.owner || (role === 3 ? user!.name : '') || 'TRAN THU HA';
            addLead({ ...l, owner, createdAt: TODAY });
            setLeads([...FUNNEL]);
            toast('Đã thêm lead', l.name);
            setAdding(false);
          }}
        />
      )}
    </AppLayout>
  );
}

type CardProps = { lead: FunnelLead; draggable: boolean; onOpen: () => void; onDragStart: () => void; onDragEnd: () => void };

function LeadCard({ lead: l, draggable, onOpen, onDragStart, onDragEnd }: CardProps) {
  const late = overdueDays(l);
  return (
    <button
      type="button"
      className={'kb-card' + (late ? ' late' : '')}
      onClick={onOpen}
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
  if (l.student) return <Pill tone="ok">Đã nhập học</Pill>;
  if (l.stage === 'Chốt') return <Pill tone="ok">Chờ chuyển học viên</Pill>;
  if (late) return <Pill tone="off">Quá hạn {late} ngày</Pill>;
  if (l.callback === TODAY) return <Pill tone="wait">Gọi lại hôm nay</Pill>;
  if (l.trial) return <Pill tone="info">Học thử {ddmm(l.trial)}</Pill>;
  if (l.callback) return <Pill>Gọi lại {ddmm(l.callback)}</Pill>;
  return null;
}
