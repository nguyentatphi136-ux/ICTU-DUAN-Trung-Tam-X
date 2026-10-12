import { useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Modal, Pill } from '../../components/ui';
import { audit } from '../../data/audit';
import { useAuth } from '../../data/auth';
import { TODAY, viDateTime } from '../../data/funnel';
import { viDate } from '../../data/leads';
import { ROLES } from '../../data/permissions';
import { saveAll } from '../../data/store';
import { STUDENTS, type Student } from '../../data/students';
import { ADJUSTMENTS, debtOf, discountLabel, itemTone, netOf, PAYMENTS, PLANS, type AdjustKind, type Method, type Payment } from '../../data/tuition';
import { parseVnd, typingVnd, vnd, vndWords } from '../../data/vnd';
import { canEditFees, MODULE, MoneyInput } from '../S7-01-bieu-phi/FeeTablePage';
import '../S7-01-bieu-phi/fees.css';
import { studentLine } from '../S7-02-ke-hoach-thanh-toan/PlanPages';
import './receipt.css';

const pad = (n: number) => String(n).padStart(6, '0');
const nextId = (prefix: string, ids: string[]) => {
  const y = TODAY.slice(0, 4);
  const n = Math.max(0, ...ids.filter((x) => x.startsWith(`${prefix}-${y}-`)).map((x) => Number(x.slice(-6))));
  return `${prefix}-${y}-${pad(n + 1)}`;
};
const clock = () => new Date().toTimeString().slice(0, 5);
const openReceipt = (id: string) => window.open(`/hoc-phi/bien-lai/${id}`, '_blank');

// S7-03. Học phí của một học viên (Kế toán): các đợt và trạng thái, ghi nhận thanh toán, biên lai, bút toán điều chỉnh.
export function StudentFeePage() {
  const { code } = useParams();
  const s = STUDENTS.find((x) => x.code === code);
  if (!s) return <AppLayout crumb={[MODULE, 'Công nợ']} module={MODULE}><p className="empty">Không tìm thấy học viên {code}.</p></AppLayout>;
  return <FeeDetail key={s.code} s={s} />;
}

function FeeDetail({ s }: { s: Student }) {
  const { user } = useAuth();
  const edit = canEditFees(user?.active ?? -1);
  const [dialog, setDialog] = useState<{ pay: number } | 'adjust' | null>(null);
  const [, setTick] = useState(0);
  const d = debtOf(s.code);
  const pays = PAYMENTS.filter((p) => p.student === s.code).reverse();
  const adjs = ADJUSTMENTS.filter((a) => pays.some((p) => p.id === a.payment));
  const done = () => (setDialog(null), setTick((t) => t + 1));

  return (
    <AppLayout crumb={[MODULE, 'Công nợ', s.name]} module={MODULE}>
      <Link to="/hoc-phi/cong-no" className="back">
        <Icon name="back" size={16} />
        Bảng công nợ
      </Link>
      <div className="h">
        <div>
          <h2>{s.name}</h2>
          <p>{studentLine(s)}</p>
        </div>
        {edit && d.plan && (
          <button type="button" className="btn primary" disabled={!d.next} onClick={() => setDialog({ pay: d.next!.no })}>
            Ghi nhận thanh toán
          </button>
        )}
      </div>
      {!d.plan ? (
        <div className="panel fe-pad">
          Học viên chưa có kế hoạch thanh toán.{' '}
          <Link className="link" to={`/hoc-phi/ke-hoach/${s.code}`}>
            Lập kế hoạch
          </Link>
        </div>
      ) : (
        <>
          <div className="fe-stats">
            <div className="stat">
              <b>{vnd(d.due)}</b>
              <span>{d.plan.discount ? `Phải thu sau ${discountLabel(d.plan.discount).split(' · ')[0].toLowerCase()}` : 'Phải thu'}</span>
            </div>
            <div className="stat ok">
              <b>{vnd(d.paid)}</b>
              <span>Đã thu</span>
            </div>
            <div className={'stat' + (d.owed ? ' bad' : '')}>
              <b>{vnd(d.owed)}</b>
              <span>Còn nợ</span>
            </div>
            <div className={'stat' + (d.overdue ? ' bad' : '')}>
              <b>{d.overdue} ngày</b>
              <span>Quá hạn</span>
            </div>
          </div>
          <div className="panel">
            <div className="fe-head">
              <h3>Các đợt thanh toán</h3>
              <Link className="link" to={`/hoc-phi/ke-hoach/${s.code}`}>
                Xem kế hoạch
              </Link>
            </div>
            <table>
              <thead>
                <tr>
                  <th>Đợt</th>
                  <th className="num">Số tiền</th>
                  <th className="num">Đã thu</th>
                  <th className="num">Còn nợ</th>
                  <th>Hạn thanh toán</th>
                  <th>Trạng thái</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {d.items.map((it) => (
                  <tr key={it.no}>
                    <td className="strong">Đợt {it.no}</td>
                    <td className="num strong">{vnd(it.amount)}</td>
                    <td className="num">{vnd(it.paid)}</td>
                    <td className={'num' + (it.owed ? ' bad-t' : '')}>{vnd(it.owed)}</td>
                    <td className={it.owed && it.due < TODAY ? 'bad-t' : undefined}>{viDate(it.due)}</td>
                    <td>
                      <Pill tone={itemTone(it.state)}>{it.state}</Pill>
                    </td>
                    <td className="fe-used">
                      {edit && it.owed > 0 && (
                        <button type="button" className="link" onClick={() => setDialog({ pay: it.no })}>
                          Thu tiền
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
      <div className="panel" style={{ marginTop: 16 }}>
        <div className="fe-head">
          <h3>Các khoản đã ghi nhận</h3>
        </div>
        <div className="scroll">
          <table className="fe-pays">
            <thead>
              <tr>
                <th>Mã biên lai</th>
                <th>Ngày thu</th>
                <th>Đợt</th>
                <th className="num">Số tiền</th>
                <th>Hình thức</th>
                <th>Người thu</th>
                <th>Ghi chú</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {pays.map((p) => {
                const net = netOf(p);
                return (
                  <tr key={p.id}>
                    <td className="code">{p.id}</td>
                    <td>{viDate(p.at)}</td>
                    <td>Đợt {p.no}</td>
                    <td className="num strong">
                      {net !== p.amount && <s className="hint">{vnd(p.amount)}</s>} {vnd(net)}
                    </td>
                    <td>{p.method}</td>
                    <td>{p.by}</td>
                    <td className="hint">{[p.ref, p.note].filter(Boolean).join(' · ') || '–'}</td>
                    <td className="fe-used">
                      <button type="button" className="link" onClick={() => openReceipt(p.id)}>
                        In · PDF
                      </button>
                    </td>
                  </tr>
                );
              })}
              {!pays.length && (
                <tr>
                  <td colSpan={8} className="empty">
                    Chưa có khoản thu nào.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {adjs.length > 0 && (
          <div className="fe-adjs">
            <b>Bút toán điều chỉnh</b>
            {adjs.map((a) => (
              <p key={a.id}>
                <span className="code">{a.id}</span> · {viDateTime(a.at)} · {a.kind} {a.payment} {a.delta > 0 ? '+' : '−'}
                {vnd(Math.abs(a.delta))} · {a.by} · {a.reason}
              </p>
            ))}
          </div>
        )}
        <div className="fe-foot fe-note">
          <span className="hint">Khoản đã ghi nhận không xoá được. Nếu ghi sai, hãy lập bút toán điều chỉnh.</span>
          {edit && pays.length > 0 && (
            <button type="button" className="link" onClick={() => setDialog('adjust')}>
              Lập bút toán điều chỉnh
            </button>
          )}
        </div>
      </div>
      {dialog === 'adjust' ? <AdjustDialog s={s} pays={pays} onClose={() => setDialog(null)} onDone={done} /> : dialog && <PayDialog s={s} no={dialog.pay} onClose={() => setDialog(null)} onDone={done} />}
    </AppLayout>
  );
}

function PayDialog({ s, no: first, onClose, onDone }: { s: Student; no: number; onClose: () => void; onDone: () => void }) {
  const { user } = useAuth();
  const toast = useToast();
  const items = debtOf(s.code).items.filter((it) => it.owed > 0);
  const [no, setNo] = useState(first);
  const it = items.find((x) => x.no === no) ?? items[0];
  const [amount, setAmount] = useState('');
  const [method, setMethod] = useState<Method>('Tiền mặt');
  const [at, setAt] = useState(TODAY);
  const [ref, setRef] = useState('');
  const [note, setNote] = useState('');
  const n = parseVnd(amount);
  const over = n > it.owed ? n - it.owed : 0;
  const [error, setError] = useState('');

  function save() {
    if (!(n > 0)) return setError('Nhập số tiền thu');
    if (!at || at > TODAY) return setError('Ngày thu không được sau hôm nay');
    if (method === 'Chuyển khoản' && !ref.trim()) return setError('Nhập mã giao dịch ngân hàng');
    const p: Payment = { id: nextId('BL', PAYMENTS.map((x) => x.id)), student: s.code, no: it.no, amount: n, at, time: clock(), method, by: user!.name, ref: method === 'Chuyển khoản' ? ref.trim() : undefined, note: note.trim() || undefined };
    PAYMENTS.push(p);
    audit({ at: TODAY + 'T' + p.time, by: user!.name, role: ROLES[user!.active], entity: 'Học phí', action: 'Ghi nhận thanh toán', target: `${s.code} · Đợt ${it.no}`, changes: [{ field: 'Đã thu', before: vnd(it.paid), after: vnd(it.paid + n) }], note: p.id });
    saveAll();
    toast(`Đã ghi nhận ${vnd(n)}`, `Biên lai ${p.id}`);
    openReceipt(p.id);
    onDone();
  }

  return (
    <Modal onClose={onClose} label="Ghi nhận thanh toán">
      <h3 style={{ marginBottom: 4 }}>Ghi nhận thanh toán</h3>
      <p className="hint" style={{ fontSize: 14, marginBottom: 16 }}>
        {s.name} · {s.code}
      </p>
      <div className="field">
        <label htmlFor="pay-no">Đợt thanh toán</label>
        <div className="inp">
          <select id="pay-no" value={it.no} onChange={(e) => setNo(+e.target.value)}>
            {items.map((x) => (
              <option key={x.no} value={x.no}>
                Đợt {x.no} · còn nợ {vnd(x.owed)} · hạn {viDate(x.due)}
              </option>
            ))}
          </select>
        </div>
      </div>
      <div className="field">
        <label htmlFor="pay-amount">
          Số tiền thu
          <button type="button" className="link fe-fill" onClick={() => setAmount(typingVnd(String(it.owed)))}>
            Thu đủ {vnd(it.owed)}
          </button>
        </label>
        <MoneyInput id="pay-amount" value={amount} onChange={setAmount} bad={!!over} />
        {over > 0 && (
          <p className="err">
            Vượt số còn nợ của đợt {vnd(over)}. Chỉ thu tối đa {vnd(it.owed)}, phần dư ghi vào đợt sau bằng một khoản thu riêng.
          </p>
        )}
      </div>
      <div className="grid2">
        <div className="field">
          <label>Hình thức</label>
          <div className="seg fe-seg" role="radiogroup" aria-label="Hình thức">
            {(['Tiền mặt', 'Chuyển khoản'] as Method[]).map((m) => (
              <button key={m} type="button" role="radio" aria-checked={method === m} className={method === m ? 'on' : undefined} onClick={() => setMethod(m)}>
                {m}
              </button>
            ))}
          </div>
        </div>
        <div className="field">
          <label htmlFor="pay-at">Ngày thu</label>
          <div className="inp">
            <input id="pay-at" type="date" max={TODAY} value={at} onChange={(e) => setAt(e.target.value)} />
          </div>
        </div>
      </div>
      <div className="grid2">
        <div className="field">
          <label htmlFor="pay-by">Người thu</label>
          <div className="inp">
            <input id="pay-by" value={user?.name ?? ''} readOnly />
          </div>
        </div>
        {method === 'Chuyển khoản' && (
          <div className="field">
            <label htmlFor="pay-ref">Mã giao dịch ngân hàng</label>
            <div className="inp">
              <input id="pay-ref" value={ref} onChange={(e) => setRef(e.target.value)} placeholder="FT26292418830" />
            </div>
          </div>
        )}
      </div>
      <div className="field">
        <label htmlFor="pay-note">Ghi chú</label>
        <div className="inp">
          <input id="pay-note" value={note} onChange={(e) => setNote(e.target.value)} />
        </div>
      </div>
      <p className="hint" style={{ marginBottom: 12 }}>
        Thu ít hơn số còn nợ thì đợt chuyển sang Thanh toán một phần.
      </p>
      {error && <p className="err" style={{ marginBottom: 12 }}>{error}</p>}
      <div className="acts">
        <button type="button" className="btn" style={{ width: 90 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className="btn primary" disabled={!!over} onClick={save}>
          Ghi nhận và in biên lai
        </button>
      </div>
    </Modal>
  );
}

const KINDS: AdjustKind[] = ['Giảm khoản thu', 'Tăng khoản thu', 'Huỷ toàn bộ'];

function AdjustDialog({ s, pays, onClose, onDone }: { s: Student; pays: Payment[]; onClose: () => void; onDone: () => void }) {
  const { user } = useAuth();
  const toast = useToast();
  const [id, setId] = useState(pays[0].id);
  const p = pays.find((x) => x.id === id)!;
  const net = netOf(p);
  const room = debtOf(s.code).items.find((x) => x.no === p.no)?.owed ?? 0;
  const [kind, setKind] = useState<AdjustKind>('Giảm khoản thu');
  const [amount, setAmount] = useState('');
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const n = kind === 'Huỷ toàn bộ' ? net : parseVnd(amount) || 0;
  const after = kind === 'Tăng khoản thu' ? net + n : net - n;
  const code = nextId('ĐC', ADJUSTMENTS.map((a) => a.id));

  function save() {
    if (!(n > 0)) return setError('Nhập số tiền điều chỉnh');
    if (kind === 'Giảm khoản thu' && n > net) return setError(`Không vượt số tiền gốc ${vnd(net)}`);
    if (kind === 'Tăng khoản thu' && n > room) return setError(`Đợt ${p.no} chỉ còn nợ ${vnd(room)}`);
    if (!reason.trim()) return setError('Nhập lý do điều chỉnh');
    const delta = kind === 'Tăng khoản thu' ? n : -n;
    const at = TODAY + 'T' + clock();
    ADJUSTMENTS.push({ id: code, payment: p.id, kind, delta, reason: reason.trim(), by: user!.name, at });
    audit({ at, by: user!.name, role: ROLES[user!.active], entity: 'Học phí', action: 'Bút toán điều chỉnh', target: `${s.code} · ${p.id}`, changes: [{ field: 'Khoản thu', before: vnd(net), after: vnd(after) }], note: `${code}: ${reason.trim()}` });
    saveAll();
    toast(`Đã lập bút toán ${code}`, `${p.id}: ${vnd(net)} thành ${vnd(after)}`);
    onDone();
  }

  return (
    <Modal onClose={onClose} label="Lập bút toán điều chỉnh">
      <h3 style={{ marginBottom: 4 }}>Lập bút toán điều chỉnh</h3>
      <p className="hint" style={{ fontSize: 14, marginBottom: 16 }}>
        Khoản đã ghi nhận không xoá được. Bút toán điều chỉnh tạo một dòng mới trừ hoặc cộng vào khoản gốc.
      </p>
      <div className="field">
        <label htmlFor="adj-p">Khoản gốc</label>
        <div className="inp">
          <select id="adj-p" value={id} onChange={(e) => setId(e.target.value)}>
            {pays.map((x) => (
              <option key={x.id} value={x.id}>
                {x.id} · {viDate(x.at)} · Đợt {x.no} · {vnd(netOf(x))}
              </option>
            ))}
          </select>
        </div>
      </div>
      <div className="field">
        <label>Loại điều chỉnh</label>
        <div className="seg fe-seg" role="radiogroup" aria-label="Loại điều chỉnh">
          {KINDS.map((k) => (
            <button key={k} type="button" role="radio" aria-checked={kind === k} className={kind === k ? 'on' : undefined} onClick={() => setKind(k)}>
              {k}
            </button>
          ))}
        </div>
      </div>
      <div className="grid2">
        <div className="field">
          <label htmlFor="adj-amount">Số tiền điều chỉnh</label>
          {kind === 'Huỷ toàn bộ' ? (
            <div className="inp">
              <input id="adj-amount" value={net.toLocaleString('vi-VN')} readOnly />
            </div>
          ) : (
            <MoneyInput id="adj-amount" value={amount} onChange={setAmount} />
          )}
          <p className="hint">{kind === 'Tăng khoản thu' ? `Không vượt số còn nợ của đợt ${vnd(room)}` : 'Không vượt số tiền gốc'}</p>
        </div>
        <div className="field">
          <label>Khoản thu sau điều chỉnh</label>
          <p className="fe-after">{vnd(Math.max(0, after))}</p>
        </div>
      </div>
      <div className="field">
        <label htmlFor="adj-reason">Lý do</label>
        <textarea id="adj-reason" className="inp" rows={2} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Ví dụ: nhập nhầm số tiền, thực thu theo phiếu thu tay" />
        <p className="hint">Bắt buộc</p>
      </div>
      <p className="fe-info">
        <Icon name="file" size={16} />
        Sinh mã {code}, ghi vào nhật ký thao tác
      </p>
      {error && <p className="err" style={{ marginBottom: 12 }}>{error}</p>}
      <div className="acts">
        <button type="button" className="btn" style={{ width: 90 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className="btn primary" style={{ width: 90 }} onClick={save}>
          Lập
        </button>
      </div>
    </Modal>
  );
}

// S7-03. Biên lai in được và lưu PDF bằng hộp thoại in của trình duyệt. Học viên chỉ xem được biên lai của mình (S7-07).
export function ReceiptPage() {
  const { id } = useParams();
  const { user } = useAuth();
  if (!user) return <Navigate to="/dang-nhap" replace />;
  const p = PAYMENTS.find((x) => x.id === id);
  const s = STUDENTS.find((x) => x.code === p?.student);
  if (!p || !s || (user.active === 0 && s.login !== user.email)) return <p className="empty">Không tìm thấy biên lai {id}.</p>;
  const it = PLANS[s.code]?.items.find((x) => x.no === p.no);
  const net = netOf(p);
  const owed = debtOf(s.code).items.find((x) => x.no === p.no)?.owed ?? 0;
  const adjs = ADJUSTMENTS.filter((a) => a.payment === p.id);
  return (
    <div className="rc-wrap">
      <div className="rc-tools">
        <button type="button" className="btn primary" onClick={() => window.print()}>
          <Icon name="dl" size={16} />
          In hoặc lưu PDF
        </button>
      </div>
      <article className="rc">
        <header>
          <div>
            <b>TRUNG TÂM ĐÀO TẠO TMS</b>
            <span>Tầng 3, cơ sở Cầu Giấy, Hà Nội · 024 0000 0000</span>
          </div>
          <div className="rc-title">
            <b>BIÊN LAI THU HỌC PHÍ</b>
            <span>Số: {p.id}</span>
          </div>
        </header>
        <dl>
          <div><dt>Họ và tên học viên</dt><dd>{s.name}</dd></div>
          <div><dt>Mã học viên</dt><dd>{s.code}</dd></div>
          <div><dt>Ngày thu</dt><dd>{viDate(p.at)}</dd></div>
          <div><dt>Chương trình</dt><dd>{s.program}</dd></div>
          <div><dt>Lớp</dt><dd>{s.cls ?? 'Chưa xếp lớp'}</dd></div>
          <div><dt>Hình thức</dt><dd>{p.method}{p.ref ? ` · ${p.ref}` : ''}</dd></div>
        </dl>
        <table>
          <thead>
            <tr>
              <th>Nội dung</th>
              <th className="num">Phải thu</th>
              <th className="num">Thu lần này</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>
                <b>Học phí đợt {p.no}</b>
                {it && <div className="hint">Hạn thanh toán {viDate(it.due)}</div>}
              </td>
              <td className="num">{it ? vnd(it.amount) : '–'}</td>
              <td className="num">{vnd(p.amount)}</td>
            </tr>
            {adjs.map((a) => (
              <tr key={a.id}>
                <td>
                  Điều chỉnh {a.id}
                  <div className="hint">{a.reason}</div>
                </td>
                <td />
                <td className="num">
                  {a.delta > 0 ? '+' : '−'}
                  {vnd(Math.abs(a.delta))}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="rc-sum">
          <div>
            <span>Số tiền đã thu</span>
            <b>{vnd(net)}</b>
          </div>
          <p>Bằng chữ: {vndWords(net)}</p>
          <div>
            <span>Đợt {p.no} còn nợ sau lần thu này</span>
            <b className={owed ? 'bad-t' : undefined}>{vnd(owed)}</b>
          </div>
        </div>
        {p.note && <p className="hint">Ghi chú: {p.note}.</p>}
        <div className="rc-sign">
          <div>
            <b>Người nộp tiền</b>
            <span>(Ký, ghi rõ họ tên)</span>
          </div>
          <div>
            <b>Người thu tiền</b>
            <span>(Ký, ghi rõ họ tên)</span>
            <em>{p.by}</em>
          </div>
        </div>
        <footer>
          <b>Mã tra cứu: {p.id}</b>
          <span>
            Biên lai lập trên hệ thống TMS lúc {viDate(p.at)} {p.time}. Tra cứu trong mục Học phí của tôi.
          </span>
        </footer>
      </article>
    </div>
  );
}
