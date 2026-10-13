import { useState } from 'react';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Box, Modal } from '../../components/ui';
import { audit } from '../../data/audit';
import { useAuth } from '../../data/auth';
import { TODAY } from '../../data/funnel';
import { notify } from '../../data/notifications';
import { ROLES } from '../../data/permissions';
import { saveAll } from '../../data/store';
import type { Student } from '../../data/students';
import { applyDiscount, APPROVAL_PCT, APPROVER, discountAmount, discountLabel, feeOf, needsApproval, PLANS, type Discount } from '../../data/tuition';
import { parseVnd, typingVnd, vnd } from '../../data/vnd';
import { MoneyInput } from '../S7-01-bieu-phi/FeeTablePage';

const at = () => TODAY + 'T' + new Date().toTimeString().slice(0, 5);
const short = (d?: Discount) => discountLabel(d).split(' · ')[0];

// S7-04. Học bổng, giảm giá: theo phần trăm hoặc số tiền cố định, bắt buộc lý do. Áp dụng xong kế hoạch được tính lại.
// Giảm vượt 30% thì gửi Quản lý đào tạo phê duyệt; trong lúc chờ học viên vẫn đóng theo mức cũ.
export function DiscountDialog({ s, onClose, onDone }: { s: Student; onClose: () => void; onDone: () => void }) {
  const { user } = useAuth();
  const toast = useToast();
  const plan = PLANS[s.code];
  const base = feeOf(s)?.amount ?? 0;
  const cur = plan?.discount;
  const [kind, setKind] = useState<Discount['kind']>(cur?.kind ?? '%');
  const [value, setValue] = useState(cur ? (cur.kind === '%' ? String(cur.value) : typingVnd(String(cur.value))) : '');
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const n = kind === '%' ? Number(value.replace(',', '.')) || 0 : parseVnd(value) || 0;
  const d: Discount | undefined = n > 0 ? { kind, value: n, reason: reason.trim() } : undefined;
  const cut = discountAmount(base, d);
  const approval = !!d && needsApproval(base, d);

  function save() {
    if (kind === '%' && (n < 0 || n > 100)) return setError('Phần trăm giảm từ 0 đến 100');
    if (kind === 'đ' && n > base) return setError(`Không giảm quá học phí ${vnd(base)}`);
    if (!reason.trim()) return setError('Nhập lý do giảm giá');
    const who = { at: at(), by: user!.name, role: ROLES[user!.active], entity: 'Học phí' as const, target: `${s.code} · ${s.name}` };
    if (approval) {
      plan.pending = { ...d!, by: user!.name, at: who.at };
      audit({ ...who, action: 'Gửi duyệt giảm giá', changes: [{ field: 'Mức giảm', before: short(cur), after: short(d) }], note: reason.trim() });
      notify({ at: who.at, kind: 'Học phí', title: `Cần duyệt giảm giá cho ${s.name}`, body: `${short(d)} (${vnd(cut)}) · ${reason.trim()} · đề xuất bởi ${user!.name}.`, link: `/hoc-phi/ke-hoach/${s.code}`, to: { users: [APPROVER] } });
      saveAll();
      toast('Đã gửi yêu cầu phê duyệt', `Chờ ${APPROVER} duyệt ${short(d)}`);
      return onDone();
    }
    const err = applyDiscount(s.code, d);
    if (err) return setError(err);
    audit({ ...who, action: 'Miễn giảm', changes: [{ field: 'Mức giảm', before: short(cur), after: short(d) }], note: reason.trim() });
    saveAll();
    toast(d ? `Đã áp dụng ${short(d).toLowerCase()}` : 'Đã bỏ giảm giá', `Còn phải đóng ${vnd(base - cut)}`);
    onDone();
  }

  return (
    <Modal onClose={onClose} label="Học bổng, giảm giá">
      <h3 style={{ marginBottom: 4 }}>Học bổng, giảm giá</h3>
      <p className="hint" style={{ fontSize: 14, marginBottom: 16 }}>
        {s.name} · {s.code} · học phí {vnd(base)}
      </p>
      <div className="field">
        <label>Cách giảm</label>
        <div className="seg fe-seg" role="radiogroup" aria-label="Cách giảm">
          {(
            [
              ['%', 'Theo phần trăm'],
              ['đ', 'Số tiền cố định'],
            ] as const
          ).map(([k, label]) => (
            <button
              key={k}
              type="button"
              role="radio"
              aria-checked={kind === k}
              className={kind === k ? 'on' : undefined}
              onClick={() => {
                setKind(k);
                setValue('');
              }}
            >
              {label}
            </button>
          ))}
        </div>
      </div>
      <div className="grid2">
        <div className="field">
          <label htmlFor="dc-value">Mức giảm</label>
          {kind === '%' ? (
            <div className="inp fe-money">
              <input id="dc-value" inputMode="decimal" value={value} onChange={(e) => setValue(e.target.value.replace(/[^\d,.]/g, ''))} />
              <span>%</span>
            </div>
          ) : (
            <MoneyInput id="dc-value" value={value} onChange={setValue} />
          )}
        </div>
        <div className="fe-calc">
          <div>
            <span>Số tiền giảm</span>
            <b>− {vnd(cut)}</b>
          </div>
          <div>
            <span>Còn phải đóng</span>
            <b>{vnd(base - cut)}</b>
          </div>
        </div>
      </div>
      <div className="field">
        <label htmlFor="dc-reason">Lý do</label>
        <textarea id="dc-reason" className="inp" rows={2} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Ví dụ: học bổng đợt tuyển sinh tháng 10, theo quyết định số 12/QĐ-HB" />
        <p className="hint">Bắt buộc</p>
      </div>
      {approval ? (
        <Box icon="alert" tone="warn" title={`Giảm giá vượt ${APPROVAL_PCT}% cần Quản lý đào tạo phê duyệt`}>
          Yêu cầu sẽ gửi tới {APPROVER}. Trong lúc chờ, học viên vẫn đóng theo {cur ? `mức ${short(cur).toLowerCase()}` : 'học phí chưa giảm'} hiện tại.
        </Box>
      ) : (
        <Box icon="refresh" tone="info" title="Sau khi áp dụng, kế hoạch thanh toán được tính lại">
          Các đợt chưa thu được chia lại theo tỷ lệ cũ. Đợt đã thu giữ nguyên.
        </Box>
      )}
      {error && <p className="err" style={{ margin: '12px 0' }}>{error}</p>}
      <div className="acts" style={{ marginTop: 16 }}>
        <button type="button" className="btn" style={{ width: 90 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className="btn primary" onClick={save}>
          {approval ? 'Gửi phê duyệt' : d ? 'Áp dụng' : 'Bỏ giảm giá'}
        </button>
      </div>
    </Modal>
  );
}

/** Yêu cầu giảm giá đang chờ duyệt. Quản lý đào tạo (và quản trị) duyệt hoặc từ chối ngay tại đây. */
export function PendingDiscount({ s, onDone }: { s: Student; onDone: () => void }) {
  const { user } = useAuth();
  const toast = useToast();
  const plan = PLANS[s.code];
  const p = plan?.pending;
  if (!p) return null;
  const can = user?.active === 5 || user?.active === 6;
  const base = feeOf(s)?.amount ?? 0;

  function decide(ok: boolean) {
    const err = ok ? applyDiscount(s.code, { kind: p!.kind, value: p!.value, reason: p!.reason }) : undefined;
    if (err) return toast('Không áp dụng được', err);
    if (!ok) plan.pending = undefined;
    const now = at();
    audit({ at: now, by: user!.name, role: ROLES[user!.active], entity: 'Học phí', action: ok ? 'Duyệt giảm giá' : 'Từ chối giảm giá', target: `${s.code} · ${s.name}`, changes: [{ field: 'Mức giảm', before: 'Chờ duyệt', after: ok ? short(p) : 'Giữ mức cũ' }] });
    notify({ at: now, kind: 'Học phí', title: `${ok ? 'Đã duyệt' : 'Từ chối'} giảm giá cho ${s.name}`, body: `${short(p)} · ${user!.name} ${ok ? 'đã duyệt, kế hoạch đã tính lại' : 'không duyệt'}.`, link: `/hoc-phi/ke-hoach/${s.code}`, to: { users: [p!.by] } });
    saveAll();
    toast(ok ? 'Đã duyệt giảm giá' : 'Đã từ chối giảm giá', s.name);
    onDone();
  }

  return (
    <div className="fe-pending">
      <Box icon="clock" tone="warn" title={`Chờ duyệt: ${short(p)} (− ${vnd(discountAmount(base, p))})`}>
        {p.reason} · đề xuất bởi {p.by}, gửi {APPROVER}.
        {can && (
          <span className="fe-decide">
            <button type="button" className="btn" onClick={() => decide(false)}>
              Từ chối
            </button>
            <button type="button" className="btn primary" onClick={() => decide(true)}>
              <Icon name="check" size={16} />
              Duyệt
            </button>
          </span>
        )}
      </Box>
    </div>
  );
}
