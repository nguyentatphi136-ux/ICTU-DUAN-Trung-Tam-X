import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Box, Modal, Pill } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { appliedCount, currentFee, FEES, feeState, type FeeVersion } from '../../data/fees';
import { viDate } from '../../data/leads';
import { saveAll } from '../../data/store';
import { PROGRAMS } from '../../data/training';
import { parseVnd, typingVnd, vnd, vndWords } from '../../data/vnd';
import './fees.css';

export const MODULE = 'Học phí & công nợ';
/** Kế toán và quản trị được sửa học phí; vai trò khác chỉ xem. */
export const canEditFees = (role: number) => role === 4 || role === 6;

/** Thanh tab chung của module Học phí & công nợ. */
export function FeeTabs() {
  const tabs: [string, string][] = [
    ['/hoc-phi/bieu-phi', 'Biểu phí'],
    ['/hoc-phi/ke-hoach', 'Kế hoạch thanh toán'],
    ['/hoc-phi/cong-no', 'Công nợ'],
    ['/hoc-phi/bao-cao', 'Báo cáo doanh thu'],
    ['/hoc-phi/nhac-hoc-phi', 'Nhắc học phí'],
  ];
  return (
    <nav className="tabs">
      {tabs.map(([to, label]) => (
        <NavLink key={to} to={to} className={({ isActive }) => (isActive ? 'on' : undefined)}>
          {label}
        </NavLink>
      ))}
    </nav>
  );
}

/** Ô nhập tiền VND: tự thêm dấu chấm phân cách, hiện dòng đọc bằng chữ bên dưới. */
export function MoneyInput({ id, value, onChange, bad }: { id: string; value: string; onChange: (v: string) => void; bad?: boolean }) {
  const n = parseVnd(value);
  return (
    <>
      <div className={'inp fe-money' + (bad ? ' bad' : '')}>
        <input id={id} inputMode="numeric" value={value} onChange={(e) => onChange(typingVnd(e.target.value))} />
        <span>₫</span>
      </div>
      {n > 0 && <p className="hint">{vndWords(n)}</p>}
    </>
  );
}

const range = (f: FeeVersion) => (f.to ? `${viDate(f.from)} – ${viDate(f.to)}` : `Từ ${viDate(f.from)}`);
const overlap = (a: { from: string; to?: string }, b: FeeVersion) => a.from <= (b.to ?? '9999') && b.from <= (a.to ?? '9999');

// S7-01. Biểu phí theo chương trình: mỗi chương trình có nhiều phiên bản theo thời gian hiệu lực, số tiền định dạng VND.
// Phiên bản đã áp dụng cho học viên thì không sửa được, chỉ tạo phiên bản mới.
export function FeeTablePage() {
  const { user } = useAuth();
  const toast = useToast();
  const programs = PROGRAMS.filter((p) => FEES[p.name]);
  const [sel, setSel] = useState(programs[0]?.name ?? '');
  const [editing, setEditing] = useState<FeeVersion | 'new' | null>(null);
  const [, setTick] = useState(0);
  const edit = canEditFees(user?.active ?? -1);
  const list = [...(FEES[sel] ?? [])].sort((a, b) => b.v - a.v);

  return (
    <AppLayout crumb={[MODULE, 'Biểu phí']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Biểu phí theo chương trình</h2>
          <p>Mỗi chương trình có nhiều biểu phí theo thời gian hiệu lực</p>
        </div>
      </div>
      <FeeTabs />
      <div className="fe-grid">
        <nav className="panel fe-programs" aria-label="Chương trình">
          {programs.map((p) => (
            <button key={p.code} type="button" className={sel === p.name ? 'on' : undefined} onClick={() => setSel(p.name)}>
              <span>
                <small>{p.code}</small>
                <b>{p.name}</b>
              </span>
              <em>{currentFee(p.name) ? vnd(currentFee(p.name)!.amount) : '–'}</em>
            </button>
          ))}
        </nav>
        <section>
          <div className="panel">
            <div className="fe-head">
              <h3>{sel}</h3>
              {edit && (
                <button type="button" className="btn primary" onClick={() => setEditing('new')}>
                  <Icon name="plus" size={16} />
                  Tạo phiên bản mới
                </button>
              )}
            </div>
            <table>
              <thead>
                <tr>
                  <th>Phiên bản</th>
                  <th>Học phí</th>
                  <th>Hiệu lực</th>
                  <th>Trạng thái</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {list.map((f) => {
                  const n = appliedCount(sel, f);
                  const st = feeState(f);
                  return (
                    <tr key={f.v}>
                      <td className="strong">v{f.v}</td>
                      <td className="strong">{vnd(f.amount)}</td>
                      <td>{range(f)}</td>
                      <td>
                        <Pill tone={st === 'Đang áp dụng' ? 'ok' : st === 'Sắp áp dụng' ? 'wait' : undefined}>{st}</Pill>
                      </td>
                      <td className="fe-used">
                        {n ? (
                          `${n} học viên`
                        ) : edit ? (
                          <button type="button" className="link" onClick={() => setEditing(f)}>
                            Sửa
                          </button>
                        ) : (
                          'Chưa áp dụng'
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <Box icon="lock" tone="info" title="Biểu phí đã áp dụng cho học viên thì không sửa được">
            Muốn đổi học phí, hãy tạo phiên bản mới với ngày hiệu lực mới. Học viên đã nhập học giữ nguyên biểu phí cũ.
          </Box>
          <p className="hint">Học viên nhập học trong khoảng hiệu lực nào thì áp dụng biểu phí của khoảng đó. Các khoảng không được chồng lên nhau.</p>
        </section>
      </div>
      {editing && (
        <VersionDialog
          program={sel}
          f={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSave={(f) => {
            if (editing === 'new') FEES[sel] = [...(FEES[sel] ?? []), f];
            else Object.assign(editing, f);
            saveAll();
            setEditing(null);
            setTick((t) => t + 1);
            toast(editing === 'new' ? `Đã tạo biểu phí v${f.v}` : `Đã lưu biểu phí v${f.v}`, `${sel} · ${vnd(f.amount)}`);
          }}
        />
      )}
    </AppLayout>
  );
}

function VersionDialog({ program, f, onClose, onSave }: { program: string; f: FeeVersion | null; onClose: () => void; onSave: (f: FeeVersion) => void }) {
  const others = (FEES[program] ?? []).filter((x) => x !== f);
  const last = others.reduce((m, x) => (x.to && x.to > m ? x.to : m), '');
  const [amount, setAmount] = useState(f ? typingVnd(String(f.amount)) : '');
  const [from, setFrom] = useState(f?.from ?? '');
  const [to, setTo] = useState(f?.to ?? '');
  const [error, setError] = useState('');
  const v = f?.v ?? Math.max(0, ...others.map((x) => x.v)) + 1;

  function save() {
    const n = parseVnd(amount);
    if (!(n > 0)) return setError('Nhập học phí lớn hơn 0');
    if (n % 1000) return setError('Học phí làm tròn đến nghìn đồng');
    if (!from) return setError('Chọn ngày bắt đầu hiệu lực');
    if (to && to < from) return setError('Ngày kết thúc phải sau ngày bắt đầu');
    const clash = others.find((x) => overlap({ from, to: to || undefined }, x));
    if (clash) return setError(`Khoảng hiệu lực trùng với v${clash.v} (${range(clash)}). Đặt ngày kết thúc cho v${clash.v} hoặc chọn ngày khác.`);
    onSave({ v, amount: n, from, to: to || undefined });
  }

  return (
    <Modal onClose={onClose} label="Phiên bản biểu phí">
      <h3 style={{ marginBottom: 4 }}>{f ? `Sửa biểu phí v${v}` : 'Tạo phiên bản biểu phí mới'}</h3>
      <p className="hint" style={{ fontSize: 14, marginBottom: 16 }}>
        {program} · phiên bản v{v}
        {!f && last ? ` · phiên bản gần nhất kết thúc ${viDate(last)}` : ''}
      </p>
      <div className="field">
        <label htmlFor="fe-amount">Học phí</label>
        <MoneyInput id="fe-amount" value={amount} onChange={setAmount} bad={!!error && !(parseVnd(amount) > 0)} />
      </div>
      <div className="grid2">
        <div className="field">
          <label htmlFor="fe-from">Hiệu lực từ ngày</label>
          <div className="inp">
            <input id="fe-from" type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label htmlFor="fe-to">Đến ngày (không bắt buộc)</label>
          <div className="inp">
            <input id="fe-to" type="date" value={to} onChange={(e) => setTo(e.target.value)} />
          </div>
        </div>
      </div>
      {error && <p className="err" style={{ marginBottom: 12 }}>{error}</p>}
      <div className="acts">
        <button type="button" className="btn" style={{ width: 90 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className="btn primary" style={{ width: 140 }} onClick={save}>
          {f ? 'Lưu biểu phí' : 'Tạo phiên bản'}
        </button>
      </div>
    </Modal>
  );
}
