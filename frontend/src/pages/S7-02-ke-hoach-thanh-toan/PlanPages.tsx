import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Filter, Pager, paginate, Pill, Search, usePaging } from '../../components/ui';
import { audit } from '../../data/audit';
import { useAuth } from '../../data/auth';
import { findClass } from '../../data/classes';
import { viDate } from '../../data/leads';
import { ROLES } from '../../data/permissions';
import { addDays } from '../../data/schedule';
import { saveAll } from '../../data/store';
import { STUDENTS, type Student } from '../../data/students';
import { debtOf, discountAmount, discountLabel, feeOf, fromTemplate, paidOn, PLANS, TEMPLATES, type Installment } from '../../data/tuition';
import { parseVnd, typingVnd, vnd } from '../../data/vnd';
import { canEditFees, FeeTabs, MODULE } from '../S7-01-bieu-phi/FeeTablePage';
import '../S7-01-bieu-phi/fees.css';
import { DiscountDialog, PendingDiscount } from '../S7-04-giam-gia/DiscountDialog';

const now = () => new Date().toTimeString().slice(0, 5);
const pct = (a: number, b: number) => (b ? ((100 * a) / b).toLocaleString('vi-VN', { maximumFractionDigits: 1 }) + '%' : '–');
export const studentLine = (s: Student) => `${s.code} · ${s.program} · ${s.cls ? 'lớp ' + s.cls : 'chưa xếp lớp'}`;

// S7-02. Danh sách học viên và kế hoạch thanh toán; chọn lớp để sinh hàng loạt.
export function PlanListPage() {
  const nav = useNavigate();
  const [q, setQ] = useState('');
  const [cls, setCls] = useState('');
  const [has, setHas] = useState('');
  const { page, setPage, size, setSize } = usePaging();
  const reset = (f: (v: string) => void) => (v: string) => (f(v), setPage(1));
  const k = q.trim().toLowerCase();
  const rows = STUDENTS.filter(
    (s) =>
      (!k || s.code.toLowerCase().includes(k) || s.name.toLowerCase().includes(k)) &&
      (!cls || s.cls === cls) &&
      (!has || (has === 'Đã có kế hoạch') === !!PLANS[s.code]),
  );
  const view = paginate(rows, page, size);
  const classes = [...new Set(STUDENTS.map((s) => s.cls).filter(Boolean) as string[])].sort();

  return (
    <AppLayout crumb={[MODULE, 'Kế hoạch thanh toán']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Kế hoạch thanh toán</h2>
          <p>{Object.keys(PLANS).length} học viên đã có kế hoạch, {STUDENTS.length - Object.keys(PLANS).length} học viên chưa có</p>
        </div>
        <button type="button" className="btn primary" disabled={!cls} title={cls ? undefined : 'Chọn một lớp ở bộ lọc trước'} onClick={() => nav(`/hoc-phi/ke-hoach/lop/${cls}`)}>
          <Icon name="plus" size={16} />
          Sinh kế hoạch cho {cls || 'lớp'}
        </button>
      </div>
      <FeeTabs />
      <div className="bar">
        <Search placeholder="Tìm theo mã, tên học viên" value={q} onChange={reset(setQ)} width={280} />
        <Filter label="Lớp" value={cls} options={classes} onChange={reset(setCls)} width={180} />
        <Filter label="Kế hoạch" value={has} options={['Đã có kế hoạch', 'Chưa có kế hoạch']} onChange={reset(setHas)} width={240} />
      </div>
      <div className="panel">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Học viên</th>
                <th>Lớp</th>
                <th>Chương trình</th>
                <th className="num">Phải đóng</th>
                <th>Số đợt</th>
                <th>Kế hoạch</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {view.items.map((s) => {
                const d = debtOf(s.code);
                return (
                  <tr key={s.code}>
                    <td>
                      <b className="strong">{s.name}</b>
                      <div className="hint">{s.code}</div>
                    </td>
                    <td>{s.cls ?? '–'}</td>
                    <td>{s.program}</td>
                    <td className="num strong">{vnd(d.plan ? d.due : d.base)}</td>
                    <td>{d.plan ? `${d.items.length} đợt` : '–'}</td>
                    <td>{d.plan ? <Pill tone="ok">Đã có</Pill> : <Pill>Chưa có</Pill>}</td>
                    <td className="fe-used">
                      <Link className="link" to={`/hoc-phi/ke-hoach/${s.code}`}>
                        {d.plan ? 'Xem kế hoạch' : 'Lập kế hoạch'}
                      </Link>
                      {d.plan && (
                        <Link className="link" to={`/hoc-phi/cong-no/${s.code}`}>
                          Thu tiền
                        </Link>
                      )}
                    </td>
                  </tr>
                );
              })}
              {!view.items.length && (
                <tr>
                  <td colSpan={7} className="empty">
                    Không có học viên khớp bộ lọc.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pager page={view.page} pages={view.pages} onPage={setPage} size={size} onSize={setSize}>
          {view.from} đến {view.to} trong {rows.length} học viên
        </Pager>
      </div>
    </AppLayout>
  );
}

type Row = { label: string; amount: string; due: string; paid: number };
const toRows = (code: string, items: Installment[]): Row[] => items.map((it) => ({ label: it.label, amount: typingVnd(String(it.amount)), due: it.due, paid: paidOn(code, it.no) }));

// S7-02. Kế hoạch của một học viên: chia học phí thành nhiều đợt, áp dụng mẫu hoặc nhập tay; tổng các đợt phải bằng số phải đóng sau giảm giá.
export function PlanPage() {
  const { code } = useParams();
  const s = STUDENTS.find((x) => x.code === code);
  return s ? <PlanEditor key={s.code} s={s} /> : <AppLayout crumb={[MODULE, 'Kế hoạch thanh toán']} module={MODULE}><p className="empty">Không tìm thấy học viên {code}.</p></AppLayout>;
}

function PlanEditor({ s }: { s: Student }) {
  const { user } = useAuth();
  const toast = useToast();
  const edit = canEditFees(user?.active ?? -1);
  const fee = feeOf(s);
  const plan = PLANS[s.code];
  const base = fee?.amount ?? 0;
  const payable = base - discountAmount(base, plan?.discount);
  const paid = debtOf(s.code).paid;
  const dues = (gaps: number[]) => gaps.map((g) => addDays(s.enrolledAt, g));
  const [tpl, setTpl] = useState(1);
  const [rows, setRows] = useState<Row[]>(() => (plan ? toRows(s.code, plan.items) : toRows(s.code, fromTemplate(TEMPLATES[1], payable, dues(TEMPLATES[1].gaps)))));
  const [error, setError] = useState('');
  const [disc, setDisc] = useState(false);
  const reload = () => (setDisc(false), setError(''), setRows(toRows(s.code, PLANS[s.code].items)));
  const total = rows.reduce((n, r) => n + (parseVnd(r.amount) || 0), 0);
  const diff = payable - total;
  const anyPaid = rows.some((r) => r.paid > 0);
  const set = (i: number, patch: Partial<Row>) => setRows(rows.map((r, j) => (j === i ? { ...r, ...patch } : r)));

  function save() {
    const items = rows.map((r, i) => ({ no: i + 1, label: r.label.trim() || `Đợt ${i + 1}`, amount: parseVnd(r.amount), due: r.due }));
    const bad = items.find((it) => !(it.amount > 0));
    if (bad) return setError(`Đợt ${bad.no} chưa có số tiền`);
    const under = rows.findIndex((r, i) => items[i].amount < r.paid);
    if (under >= 0) return setError(`Đợt ${under + 1} đã thu ${vnd(rows[under].paid)}, số tiền không được thấp hơn`);
    if (items.some((it) => !it.due)) return setError('Đợt nào cũng cần hạn thanh toán');
    if (items.some((it, i) => i && it.due <= items[i - 1].due)) return setError('Hạn của đợt sau phải sau hạn của đợt trước');
    if (diff) return setError(`Tổng các đợt ${diff > 0 ? 'thiếu' : 'thừa'} ${vnd(Math.abs(diff))} so với số phải đóng`);
    const before = plan ? `${plan.items.length} đợt` : 'Chưa có';
    PLANS[s.code] = { ...plan, student: s.code, items };
    audit({ at: new Date().toISOString().slice(0, 10) + 'T' + now(), by: user!.name, role: ROLES[user!.active], entity: 'Học phí', action: plan ? 'Sửa kế hoạch thanh toán' : 'Lập kế hoạch thanh toán', target: `${s.code} · ${s.name}`, changes: [{ field: 'Kế hoạch', before, after: `${items.length} đợt · ${vnd(payable)}` }] });
    saveAll();
    setError('');
    toast('Đã lưu kế hoạch thanh toán', `${s.name} · ${items.length} đợt`);
  }

  return (
    <AppLayout crumb={[MODULE, 'Kế hoạch thanh toán', s.name]} module={MODULE}>
      <Link to="/hoc-phi/ke-hoach" className="back">
        <Icon name="back" size={16} />
        Kế hoạch thanh toán
      </Link>
      <div className="h">
        <div>
          <h2>{s.name}</h2>
          <p>
            {studentLine(s)}
            {fee ? ` · biểu phí v${fee.v}` : ''}
          </p>
        </div>
      </div>
      <div className="fe-stats">
        <div className="stat">
          <b>{vnd(base)}</b>
          <span>Học phí theo biểu phí</span>
        </div>
        <div className="stat ok">
          <b>{plan?.discount ? '− ' + vnd(base - payable) : '0 ₫'}</b>
          <span>{discountLabel(plan?.discount)}</span>
          {edit && plan && (
            <button type="button" className="link fe-edit" onClick={() => setDisc(true)}>
              Sửa
            </button>
          )}
        </div>
        <div className="stat info">
          <b>{vnd(payable)}</b>
          <span>Phải đóng sau giảm giá</span>
        </div>
        <div className="stat">
          <b>{vnd(paid)}</b>
          <span>Đã thu</span>
        </div>
      </div>
      <PendingDiscount s={s} onDone={reload} />
      <div className="panel">
        <div className="fe-head">
          <h3>Các đợt thanh toán</h3>
          {edit && (
            <div className="fe-tpl">
              <select aria-label="Mẫu chia đợt" className="inp" value={tpl} onChange={(e) => setTpl(+e.target.value)}>
                {TEMPLATES.map((t, i) => (
                  <option key={t.name} value={i}>
                    Mẫu: {t.name}
                  </option>
                ))}
              </select>
              <button
                type="button"
                className="btn"
                disabled={anyPaid}
                title={anyPaid ? 'Đã có đợt thu tiền, chỉ sửa tay các đợt chưa thu' : undefined}
                onClick={() => {
                  setRows(toRows(s.code, fromTemplate(TEMPLATES[tpl], payable, dues(TEMPLATES[tpl].gaps))));
                  setError('');
                }}
              >
                Áp dụng mẫu
              </button>
            </div>
          )}
        </div>
        <table>
          <thead>
            <tr>
              <th>Đợt</th>
              <th>Mốc</th>
              <th className="num">Số tiền</th>
              <th className="num">Tỷ lệ</th>
              <th>Hạn thanh toán</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {rows.map((r, i) => {
              const lock = !edit || r.paid > 0;
              return (
                <tr key={i}>
                  <td className="strong">Đợt {i + 1}</td>
                  <td>{lock ? r.label : <input className="fe-cell" aria-label={`Mốc đợt ${i + 1}`} value={r.label} onChange={(e) => set(i, { label: e.target.value })} />}</td>
                  <td className="num">
                    {!edit ? (
                      vnd(parseVnd(r.amount))
                    ) : (
                      <div className="inp fe-money fe-cell">
                        <input aria-label={`Số tiền đợt ${i + 1}`} inputMode="numeric" value={r.amount} onChange={(e) => set(i, { amount: typingVnd(e.target.value) })} />
                        <span>₫</span>
                      </div>
                    )}
                  </td>
                  <td className="num">{pct(parseVnd(r.amount) || 0, payable)}</td>
                  <td>{!edit ? viDate(r.due) : <input className="fe-cell" type="date" aria-label={`Hạn đợt ${i + 1}`} value={r.due} onChange={(e) => set(i, { due: e.target.value })} />}</td>
                  <td className="fe-used">
                    {r.paid > 0 ? (
                      <span className="hint">Đã thu {vnd(r.paid)}</span>
                    ) : (
                      !lock &&
                      rows.length > 1 && (
                        <button type="button" className="link danger" onClick={() => setRows(rows.filter((_, j) => j !== i))}>
                          Xoá đợt
                        </button>
                      )
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {edit && (
          <div className="fe-foot">
            <button type="button" className="link" onClick={() => setRows([...rows, { label: 'Đợt bổ sung', amount: diff > 0 ? typingVnd(String(diff)) : '', due: addDays(rows.at(-1)?.due || s.enrolledAt, 30), paid: 0 }])}>
              + Thêm đợt
            </button>
          </div>
        )}
        <div className={'fe-sum ' + (diff ? 'bad' : 'ok')}>
          <span>
            <Icon name={diff ? 'alert' : 'check'} size={16} />
            {diff ? `Tổng các đợt ${diff > 0 ? 'thiếu' : 'thừa'} ${vnd(Math.abs(diff))} so với số phải đóng` : 'Tổng các đợt bằng số phải đóng sau giảm giá'}
          </span>
          <b>
            {total.toLocaleString('vi-VN')} / {vnd(payable)}
          </b>
        </div>
      </div>
      {error && <p className="err">{error}</p>}
      {edit && (
        <div className="fe-actions">
          <button type="button" className="btn fe-left" disabled={!plan} title={plan ? undefined : 'Lưu kế hoạch trước khi áp dụng giảm giá'} onClick={() => setDisc(true)}>
            <Icon name="award" size={16} />
            Học bổng, giảm giá
          </button>
          <Link className="btn" to="/hoc-phi/ke-hoach">
            Huỷ
          </Link>
          <button type="button" className="btn primary" onClick={save}>
            Lưu kế hoạch
          </button>
        </div>
      )}
      {disc && <DiscountDialog s={s} onClose={() => setDisc(false)} onDone={reload} />}
    </AppLayout>
  );
}

// S7-02. Sinh kế hoạch hàng loạt cho cả lớp theo một mẫu. Học viên đã có kế hoạch hoặc đã thu tiền được bỏ qua để không ghi đè.
export function BulkPlanPage() {
  const { cls = '' } = useParams();
  const nav = useNavigate();
  const toast = useToast();
  const { user } = useAuth();
  const k = findClass(cls);
  const start = k?.start ?? new Date().toISOString().slice(0, 10);
  const roster = STUDENTS.filter((s) => s.cls === cls);
  const [tpl, setTpl] = useState(1);
  const t = TEMPLATES[tpl];
  const [dues, setDues] = useState(() => TEMPLATES.map((x) => x.gaps.map((g) => addDays(start, g))));
  const skip = (s: Student) => (debtOf(s.code).paid > 0 ? 'Đã thu tiền, bỏ qua' : PLANS[s.code] ? 'Đã có kế hoạch, bỏ qua' : '');
  const todo = roster.filter((s) => !skip(s));

  function run() {
    for (const s of todo) {
      const base = feeOf(s)?.amount ?? 0;
      PLANS[s.code] = { student: s.code, items: fromTemplate(t, base, dues[tpl]) };
    }
    audit({ at: new Date().toISOString().slice(0, 10) + 'T' + now(), by: user!.name, role: ROLES[user!.active], entity: 'Học phí', action: 'Sinh kế hoạch hàng loạt', target: `Lớp ${cls} · ${todo.length} học viên`, changes: [{ field: 'Mẫu chia đợt', before: '–', after: t.name }] });
    saveAll();
    toast(`Đã sinh kế hoạch cho ${todo.length} học viên`, `Lớp ${cls} · ${t.name}`);
    nav('/hoc-phi/ke-hoach');
  }

  return (
    <AppLayout crumb={[MODULE, 'Kế hoạch thanh toán', 'Sinh cho lớp ' + cls]} module={MODULE}>
      <div className="h">
        <div>
          <h2>Sinh kế hoạch thanh toán cho lớp {cls}</h2>
          <p>
            {k?.program ?? '–'} · khai giảng {viDate(start)} · {roster.length} học viên
          </p>
        </div>
      </div>
      <div className="fe-grid">
        <section className="panel fe-pad">
          <h3>Mẫu chia đợt</h3>
          <div className="fe-radios" role="radiogroup" aria-label="Mẫu chia đợt">
            {TEMPLATES.map((x, i) => (
              <button key={x.name} type="button" role="radio" aria-checked={tpl === i} className={tpl === i ? 'on' : undefined} onClick={() => setTpl(i)}>
                <b>{x.name}</b>
                <span>{x.hint}</span>
              </button>
            ))}
          </div>
          <h3>Hạn của từng đợt</h3>
          {t.pct.map((p, i) => (
            <div className="fe-due" key={i}>
              <label htmlFor={`due-${i}`}>
                Đợt {i + 1} <span className="hint">· {p}%</span>
              </label>
              <div className="inp">
                <input id={`due-${i}`} type="date" value={dues[tpl][i]} onChange={(e) => setDues(dues.map((d, j) => (j === tpl ? d.map((v, m) => (m === i ? e.target.value : v)) : d)))} />
              </div>
            </div>
          ))}
        </section>
        <section className="panel">
          <div className="scroll">
            <table>
              <thead>
                <tr>
                  <th>Học viên</th>
                  <th className="num">Phải đóng</th>
                  <th>Giảm giá</th>
                  <th>Kết quả</th>
                </tr>
              </thead>
              <tbody>
                {roster.map((s) => {
                  const d = debtOf(s.code);
                  const why = skip(s);
                  return (
                    <tr key={s.code}>
                      <td>
                        <b className="strong">{s.name}</b>
                        <div className="hint">{s.code}</div>
                      </td>
                      <td className="num">{vnd(d.plan ? d.due : d.base)}</td>
                      <td>{d.plan?.discount ? discountLabel(d.plan.discount).split(' · ')[0].replace('Giảm giá ', '') : '–'}</td>
                      <td>{why ? <Pill>{why}</Pill> : <Pill tone="ok">Sẽ sinh {t.pct.length} đợt</Pill>}</td>
                    </tr>
                  );
                })}
                {!roster.length && (
                  <tr>
                    <td colSpan={4} className="empty">
                      Lớp chưa có học viên.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>
      <div className="panel fe-pad fe-bulk">
        <div>
          <b>
            Sinh kế hoạch cho {todo.length} học viên, bỏ qua {roster.length - todo.length}
          </b>
          <p className="hint">Số tiền mỗi đợt tính trên học phí theo biểu phí của từng người. Học viên đã có kế hoạch được giữ nguyên.</p>
        </div>
        <Link className="btn" to="/hoc-phi/ke-hoach">
          Huỷ
        </Link>
        <button type="button" className="btn primary" disabled={!todo.length || !canEditFees(user?.active ?? -1)} onClick={run}>
          Sinh kế hoạch
        </button>
      </div>
    </AppLayout>
  );
}
