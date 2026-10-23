import { useState } from 'react';
import { Link } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Filter, Pager, paginate, Search, usePaging } from '../../components/ui';
import { TODAY } from '../../data/funnel';
import { viDate } from '../../data/leads';
import { STUDENTS } from '../../data/students';
import { debtOf, PLANS } from '../../data/tuition';
import { vnd } from '../../data/vnd';
import { download, toXlsx } from '../../data/xlsx';
import { FeeTabs, MODULE } from '../S7-01-bieu-phi/FeeTablePage';
import '../S7-01-bieu-phi/fees.css';

/** Mọi học viên có kế hoạch, kèm số liệu công nợ; quá hạn lâu và nợ nhiều lên trước. */
export const debtRows = () =>
  STUDENTS.filter((s) => PLANS[s.code])
    .map((s) => ({ s, ...debtOf(s.code) }))
    .sort((a, b) => b.overdue - a.overdue || b.owed - a.owed);

// S7-05. Bảng công nợ: phải thu, đã thu, còn nợ, số ngày quá hạn; lọc nhóm quá hạn; tổng cộng trên toàn bộ kết quả lọc; xuất Excel.
export function DebtPage() {
  const [q, setQ] = useState('');
  const [cls, setCls] = useState('');
  const [program, setProgram] = useState('');
  const [late, setLate] = useState(false);
  const { page, setPage, size, setSize } = usePaging();
  const reset = <T,>(f: (v: T) => void) => (v: T) => (f(v), setPage(1));
  const all = debtRows();
  const k = q.trim().toLowerCase();
  const base = all.filter(({ s }) => (!k || s.code.toLowerCase().includes(k) || s.name.toLowerCase().includes(k)) && (!cls || s.cls === cls) && (!program || s.program === program));
  const lateCount = base.filter((r) => r.overdue > 0).length;
  const rows = late ? base.filter((r) => r.overdue > 0) : base;
  const view = paginate(rows, page, size);
  const sum = (f: (r: (typeof rows)[number]) => number) => rows.reduce((n, r) => n + f(r), 0);

  function exportXlsx() {
    const data = [
      ['Mã học viên', 'Họ tên', 'Lớp', 'Chương trình', 'Phải thu', 'Đã thu', 'Còn nợ', 'Số ngày quá hạn', 'Đợt kế tiếp', 'Hạn đợt kế tiếp'],
      ...rows.map((r) => [r.s.code, r.s.name, r.s.cls ?? '', r.s.program, r.due, r.paid, r.owed, r.overdue, r.next ? `Đợt ${r.next.no}` : '', r.next ? viDate(r.next.due) : '']),
      ['', 'Tổng cộng', '', '', sum((r) => r.due), sum((r) => r.paid), sum((r) => r.owed), '', '', ''],
    ];
    download(toXlsx(data, 'Công nợ'), `cong-no-${TODAY}.xlsx`);
  }

  return (
    <AppLayout crumb={[MODULE, 'Công nợ']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Bảng công nợ</h2>
          <p>{all.length} học viên đang có kế hoạch thanh toán</p>
        </div>
        <button type="button" className="btn" onClick={exportXlsx}>
          <Icon name="dl" size={16} />
          Xuất Excel
        </button>
      </div>
      <FeeTabs />
      <div className="bar">
        <Search placeholder="Tìm theo mã, tên học viên" value={q} onChange={reset(setQ)} width={260} />
        <Filter label="Lớp" value={cls} options={[...new Set(all.map((r) => r.s.cls).filter(Boolean) as string[])].sort()} onChange={reset(setCls)} width={180} />
        <Filter label="Chương trình" value={program} options={[...new Set(all.map((r) => r.s.program))]} onChange={reset(setProgram)} width={270} />
        <button type="button" className={'fe-toggle' + (late ? ' on' : '')} aria-pressed={late} onClick={() => reset(setLate)(!late)}>
          <Icon name={late ? 'check' : 'alert'} size={16} />
          Chỉ nhóm đang quá hạn ({lateCount})
        </button>
      </div>
      <div className="panel">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Học viên</th>
                <th>Lớp</th>
                <th className="num">Phải thu</th>
                <th className="num">Đã thu</th>
                <th className="num">Còn nợ</th>
                <th>Quá hạn</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {view.items.map((r) => (
                <tr key={r.s.code}>
                  <td>
                    <Link to={`/hoc-phi/cong-no/${r.s.code}`} className="strong">
                      {r.s.name}
                    </Link>
                    <div className="hint">{r.s.code}</div>
                  </td>
                  <td>{r.s.cls ?? '–'}</td>
                  <td className="num">{vnd(r.due)}</td>
                  <td className="num">{vnd(r.paid)}</td>
                  <td className={'num' + (r.owed ? ' bad-t' : '')}>{vnd(r.owed)}</td>
                  <td>{r.overdue > 0 ? <span className={'fe-late' + (r.overdue > 30 ? ' bad' : '')}>{r.overdue} ngày</span> : <span className="hint">–</span>}</td>
                  <td className="fe-used">
                    {r.owed > 0 && (
                      <Link className="link" to={`/hoc-phi/cong-no/${r.s.code}`}>
                        Thu tiền
                      </Link>
                    )}
                  </td>
                </tr>
              ))}
              {!view.items.length && (
                <tr>
                  <td colSpan={7} className="empty">
                    Không có học viên khớp bộ lọc.
                  </td>
                </tr>
              )}
            </tbody>
            {rows.length > 0 && (
              <tfoot>
                <tr className="fe-total">
                  <td colSpan={2}>
                    Tổng cộng {rows.length} học viên{late ? ' quá hạn' : ''}
                  </td>
                  <td className="num">{vnd(sum((r) => r.due))}</td>
                  <td className="num">{vnd(sum((r) => r.paid))}</td>
                  <td className="num bad-t">{vnd(sum((r) => r.owed))}</td>
                  <td colSpan={2} />
                </tr>
              </tfoot>
            )}
          </table>
        </div>
        <Pager page={view.page} pages={view.pages} onPage={setPage} size={size} onSize={setSize}>
          Đang hiện {view.from} đến {view.to} trong {rows.length} học viên · tổng cộng tính trên cả {rows.length}
        </Pager>
      </div>
    </AppLayout>
  );
}
