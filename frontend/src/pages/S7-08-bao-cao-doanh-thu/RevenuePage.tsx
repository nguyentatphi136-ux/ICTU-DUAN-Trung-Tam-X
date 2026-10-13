import { useState, type CSSProperties } from 'react';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { TODAY } from '../../data/funnel';
import { viDate } from '../../data/leads';
import { STUDENTS } from '../../data/students';
import { netOf, PAYMENTS, PLANS } from '../../data/tuition';
import { vnd } from '../../data/vnd';
import { download, toXlsx } from '../../data/xlsx';
import { FeeTabs, MODULE } from '../S7-01-bieu-phi/FeeTablePage';
import '../S7-01-bieu-phi/fees.css';
import './revenue.css';

const tr = (n: number) => (n / 1e6).toLocaleString('vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const months = (from: string, to: string) => {
  const out: string[] = [];
  for (let y = +from.slice(0, 4), m = +from.slice(5, 7); `${y}-${String(m).padStart(2, '0')}` <= to.slice(0, 7); m === 12 ? (y++, (m = 1)) : m++) out.push(`${y}-${String(m).padStart(2, '0')}`);
  return out;
};

/** Số liệu doanh thu trong kỳ. Đã thu tính theo ngày thu thực tế, đã trừ bút toán điều chỉnh; còn phải thu là nợ của các đợt có hạn tới ngày cuối kỳ. */
export function revenue(from: string, to: string, by: 'cls' | 'program') {
  const student = new Map(STUDENTS.map((s) => [s.code, s]));
  const key = (code: string) => (by === 'cls' ? (student.get(code)?.cls ?? 'Chưa xếp lớp') : (student.get(code)?.program ?? '–'));
  const groups = new Map<string, { paid: number; owed: number; late: number }>();
  const g = (k: string) => groups.get(k) ?? (groups.set(k, { paid: 0, owed: 0, late: 0 }), groups.get(k)!);
  const perMonth = new Map<string, number>();
  for (const p of PAYMENTS) {
    if (p.at < from || p.at > to) continue;
    const n = netOf(p);
    g(key(p.student)).paid += n;
    perMonth.set(p.at.slice(0, 7), (perMonth.get(p.at.slice(0, 7)) ?? 0) + n);
  }
  for (const code in PLANS) {
    for (const it of PLANS[code].items) {
      if (it.due > to) continue;
      const paid = PAYMENTS.filter((p) => p.student === code && p.no === it.no && p.at <= to).reduce((n, p) => n + netOf(p), 0);
      const owed = Math.max(0, it.amount - paid);
      if (!owed) continue;
      g(key(code)).owed += owed;
      if (it.due < TODAY) g(key(code)).late += owed;
    }
  }
  const rows = [...groups].map(([name, v]) => ({ name, ...v })).sort((a, b) => b.paid - a.paid);
  const total = rows.reduce((t, r) => ({ paid: t.paid + r.paid, owed: t.owed + r.owed, late: t.late + r.late }), { paid: 0, owed: 0, late: 0 });
  return { rows, total, months: months(from, to).map((m) => ({ m, paid: perMonth.get(m) ?? 0 })) };
}

// S7-08. Báo cáo doanh thu: chọn khoảng thời gian, nhóm theo lớp hoặc chương trình; đã thu, còn phải thu, biểu đồ đã thu theo tháng; xuất Excel.
export function RevenuePage() {
  const [from, setFrom] = useState('2026-05-01');
  const [to, setTo] = useState('2026-10-31');
  const [by, setBy] = useState<'cls' | 'program'>('program');
  const bad = !from || !to || from > to;
  const r = revenue(from, to, by);
  const max = Math.max(1, ...r.months.map((x) => x.paid));
  const label = by === 'cls' ? 'Lớp' : 'Chương trình';

  function exportXlsx() {
    download(
      toXlsx(
        [
          [`Báo cáo doanh thu từ ${viDate(from)} đến ${viDate(to)}`],
          [],
          [label, 'Đã thu (₫)', 'Còn phải thu (₫)', 'Trong đó đã quá hạn (₫)'],
          ...r.rows.map((x) => [x.name, x.paid, x.owed, x.late]),
          ['Tổng cộng', r.total.paid, r.total.owed, r.total.late],
          [],
          ['Tháng', 'Đã thu (₫)'],
          ...r.months.map((x) => [`${x.m.slice(5)}/${x.m.slice(0, 4)}`, x.paid]),
        ],
        'Doanh thu',
      ),
      `doanh-thu-${from}-${to}.xlsx`,
    );
  }

  return (
    <AppLayout crumb={[MODULE, 'Báo cáo doanh thu']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Báo cáo doanh thu</h2>
          <p>{bad ? 'Chọn khoảng thời gian hợp lệ' : `Từ ${viDate(from)} đến ${viDate(to)}`}</p>
        </div>
        <button type="button" className="btn" disabled={bad} onClick={exportXlsx}>
          <Icon name="dl" size={16} />
          Xuất Excel
        </button>
      </div>
      <FeeTabs />
      <div className="bar rv-bar">
        <div className="cl-range rv-range" role="group" aria-label="Khoảng thời gian">
          <input type="date" value={from} aria-label="Từ ngày" onChange={(e) => setFrom(e.target.value)} />
          <span>–</span>
          <input type="date" value={to} aria-label="Đến ngày" onChange={(e) => setTo(e.target.value)} />
        </div>
        <span className="hint">Nhóm theo</span>
        <div className="seg" role="radiogroup" aria-label="Nhóm theo">
          {(
            [
              ['cls', 'Lớp'],
              ['program', 'Chương trình'],
            ] as const
          ).map(([k, l]) => (
            <button key={k} type="button" role="radio" aria-checked={by === k} className={by === k ? 'on' : undefined} onClick={() => setBy(k)}>
              {l}
            </button>
          ))}
        </div>
      </div>
      {bad ? (
        <p className="err">Ngày bắt đầu phải trước ngày kết thúc.</p>
      ) : (
        <>
          <div className="stats rv-stats">
            <div className="stat info">
              <b>{vnd(r.total.paid)}</b>
              <span>Đã thu trong kỳ</span>
            </div>
            <div className="stat">
              <b>{vnd(r.total.owed)}</b>
              <span>Còn phải thu</span>
            </div>
            <div className="stat bad">
              <b>{vnd(r.total.late)}</b>
              <span>Trong đó đã quá hạn</span>
            </div>
          </div>
          <div className="rv-grid">
            <section className="panel fe-pad">
              <div className="fe-head" style={{ padding: 0, marginBottom: 12 }}>
                <h3>Đã thu theo tháng</h3>
                <span className="hint">triệu đồng</span>
              </div>
              <div className="rv-chart" role="img" aria-label="Biểu đồ đã thu theo tháng">
                {r.months.map((x) => (
                  <div key={x.m} className={'rv-col' + (x.m === TODAY.slice(0, 7) ? ' now' : '')}>
                    <span>{tr(x.paid)}</span>
                    <i style={{ '--h': `${(100 * x.paid) / max}%` } as CSSProperties} />
                    <em>T{+x.m.slice(5)}</em>
                  </div>
                ))}
              </div>
              <p className="hint">
                {to >= TODAY ? `Tháng ${+TODAY.slice(5, 7)} tính đến ngày ${viDate(TODAY)}. ` : ''}Cột đậm là tháng hiện tại.
              </p>
            </section>
            <section className="panel">
              <table>
                <thead>
                  <tr>
                    <th>{label}</th>
                    <th className="num">Đã thu</th>
                    <th className="num">Còn phải thu</th>
                  </tr>
                </thead>
                <tbody>
                  {r.rows.map((x) => (
                    <tr key={x.name}>
                      <td className="strong">{x.name}</td>
                      <td className="num strong">{tr(x.paid)} tr</td>
                      <td className="num hint">{tr(x.owed)} tr</td>
                    </tr>
                  ))}
                  {!r.rows.length && (
                    <tr>
                      <td colSpan={3} className="empty">
                        Không có khoản thu nào trong kỳ.
                      </td>
                    </tr>
                  )}
                </tbody>
                <tfoot>
                  <tr className="fe-total">
                    <td>Tổng cộng</td>
                    <td className="num">{tr(r.total.paid)} tr</td>
                    <td className="num">{tr(r.total.owed)} tr</td>
                  </tr>
                </tfoot>
              </table>
              <p className="hint fe-pad">Đã thu tính theo ngày thu thực tế, đã trừ các bút toán điều chỉnh. Còn phải thu là số nợ của các đợt tại ngày cuối kỳ.</p>
            </section>
          </div>
        </>
      )}
    </AppLayout>
  );
}
