import { useState } from 'react';
import { Icon } from '../../components/Icon';
import { ConfirmDialog, Drawer } from '../../components/ui';
import { LEAD_STATUSES, samePhone, SOURCES, type Lead, type LeadStatus } from '../../data/leads';
import { PROGRAMS } from '../../data/training';
import { isPhone, PHONE_MSG } from '../../data/validate';

type Props = {
  lead: Lead | null;
  leads: Lead[];
  readOnly: boolean;
  canDelete: boolean;
  onOpen: (l: Lead) => void;
  onClose: () => void;
  onSave: (l: Lead) => void;
  onDelete: (l: Lead) => void;
};

// S2-09. Thêm và sửa lead. Số điện thoại trùng lead đã có thì cảnh báo, vẫn cho lưu nếu người dùng muốn.
export function LeadDrawer({ lead, leads, readOnly, canDelete, onOpen, onClose, onSave, onDelete }: Props) {
  const [f, setF] = useState({
    name: lead?.name ?? '',
    phone: lead?.phone ?? '',
    email: lead?.email ?? '',
    source: lead?.source ?? SOURCES[0],
    program: lead?.program ?? '',
    status: lead?.status ?? ('Mới' as LeadStatus),
    note: lead?.note ?? '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [confirming, setConfirming] = useState(false);
  const set = (k: keyof typeof f) => (e: { target: { value: string } }) => {
    setF((x) => ({ ...x, [k]: e.target.value }));
    setErrors((x) => ({ ...x, [k]: '' }));
  };
  const dup = f.phone.replace(/\D/g, '').length >= 10 ? leads.find((l) => l.id !== lead?.id && samePhone(l.phone, f.phone)) : undefined;

  function save() {
    const e: Record<string, string> = {};
    if (!f.name.trim()) e.name = 'Vui lòng nhập họ và tên';
    if (!isPhone(f.phone)) e.phone = PHONE_MSG;
    if (!f.program) e.program = 'Chọn chương trình quan tâm';
    setErrors(e);
    if (Object.keys(e).length) return;
    onSave({
      id: lead?.id ?? 0,
      owner: lead?.owner ?? '',
      createdAt: lead?.createdAt ?? new Date().toISOString().slice(0, 10),
      name: f.name.trim().toUpperCase(),
      phone: f.phone.trim(),
      email: f.email.trim() || undefined,
      source: f.source,
      program: f.program,
      status: f.status,
      note: f.note.trim() || undefined,
    });
  }

  const select = (k: 'source' | 'program' | 'status', label: string, options: string[]) => (
    <div className="field">
      <label htmlFor={'ld-' + k}>{label}</label>
      <div className={'inp' + (errors[k] ? ' bad' : '')}>
        <select id={'ld-' + k} value={f[k]} onChange={set(k)} disabled={readOnly}>
          {k === 'program' && <option value="">Chọn chương trình</option>}
          {options.map((o) => (
            <option key={o}>{o}</option>
          ))}
        </select>
      </div>
      {errors[k] && <p className="err">{errors[k]}</p>}
    </div>
  );

  return (
    <>
    {confirming && lead && (
      <ConfirmDialog title="Chuyển lead vào thùng rác?" onClose={() => setConfirming(false)} onConfirm={() => onDelete(lead)}>
        Lead <b>{lead.name}</b> ({lead.phone}) sẽ ẩn khỏi danh sách và phễu.
      </ConfirmDialog>
    )}
    <Drawer
      title={lead ? (readOnly ? 'Thông tin lead' : 'Sửa lead') : 'Thêm lead'}
      onClose={onClose}
      footer={
        <>
          {lead && canDelete && (
            <button type="button" className="btn outline-danger" style={{ marginRight: 'auto' }} onClick={() => setConfirming(true)}>
              <Icon name="trash" size={16} />
              Chuyển vào thùng rác
            </button>
          )}
          <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
            {readOnly ? 'Đóng' : 'Huỷ'}
          </button>
          {!readOnly && (
            <button type="button" className="btn primary" style={{ width: 130 }} onClick={save}>
              Lưu lead
            </button>
          )}
        </>
      }
    >
      <fieldset disabled={readOnly} className="ld-fields">
        <div className="field">
          <label htmlFor="ld-name">Họ và tên</label>
          <div className={'inp' + (errors.name ? ' bad' : '')}>
            <input id="ld-name" value={f.name} onChange={set('name')} />
          </div>
          {errors.name && <p className="err">{errors.name}</p>}
        </div>
        <div className="field">
          <label htmlFor="ld-phone">Số điện thoại</label>
          <div className={'inp lead' + (errors.phone ? ' bad' : dup ? ' warn' : '')}>
            <Icon name="phone" />
            <input id="ld-phone" inputMode="tel" value={f.phone} onChange={set('phone')} />
          </div>
          {errors.phone && <p className="err">{errors.phone}</p>}
        </div>
        {dup && (
          <div className="box warn ld-dup">
            <Icon name="alert" size={20} />
            <div>
              <b>Số này trùng với một lead đã có</b>
              {dup.name} · {dup.status} · phụ trách: {dup.owner || 'chưa phân công'}. Bạn nên mở lead đã có thay vì tạo mới.
              <button type="button" className="link" onClick={() => onOpen(dup)}>
                Mở lead đã có
              </button>
            </div>
          </div>
        )}
        <div className="field">
          <label htmlFor="ld-email">Email</label>
          <div className="inp lead">
            <Icon name="mail" />
            <input id="ld-email" type="email" value={f.email} onChange={set('email')} />
          </div>
        </div>
        <div className="grid2">
          {select('source', 'Nguồn', SOURCES)}
          {select('program', 'Chương trình quan tâm', PROGRAMS.map((p) => p.name))}
        </div>
        {lead && select('status', 'Trạng thái', LEAD_STATUSES)}
        <div className="field">
          <label htmlFor="ld-note">Ghi chú</label>
          <div className="inp">
            <textarea id="ld-note" value={f.note} placeholder="Thông tin thêm về nhu cầu của khách" onChange={set('note')} />
          </div>
        </div>
      </fieldset>
    </Drawer>
    </>
  );
}
