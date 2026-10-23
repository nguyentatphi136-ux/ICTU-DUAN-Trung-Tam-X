import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Modal, Pill } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { TODAY } from '../../data/funnel';
import { viDate } from '../../data/leads';
import { STUDENTS, findStudentByAuth } from '../../data/students';
import { daysBetween, debtOf, discountLabel, itemTone, netOf, PAYMENTS, type Payment } from '../../data/tuition';
import { vnd } from '../../data/vnd';
import { MODULE } from '../S7-01-bieu-phi/FeeTablePage';
import '../S7-01-bieu-phi/fees.css';
import '../S7-03-thanh-toan/receipt.css';
import './myfees.css';
import { apiEnabled, getMyPaymentsApi, getReceiptPdfUrl, type StudentPaymentInfoDTO } from '../../data/paymentApi';
import { ReceiptView } from '../S7-03-thanh-toan/PaymentPages';

// S7-07. Học phí của tôi: học viên xem các đợt kèm số tiền, hạn, trạng thái; xem và tải lại biên lai các khoản đã đóng.
export function MyFeesPage() {
  const { user } = useAuth();
  const authStudent = findStudentByAuth(user);
  const s = authStudent || (user?.active === 0 ? STUDENTS.find((x) => x.code === 'HV260101') : undefined);
  const [apiData, setApiData] = useState<StudentPaymentInfoDTO | null>(null);
  const [previewPayment, setPreviewPayment] = useState<Payment | null>(null);

  useEffect(() => {
    if (apiEnabled) {
      getMyPaymentsApi()
        .then((data) => setApiData(data))
        .catch(() => {});
    }
  }, []);

  if (!s && !apiData && user?.active !== 0)
    return (
      <AppLayout crumb={[MODULE, 'Học phí của tôi']} module={MODULE}>
        <p className="empty">Trang này dành cho học viên. Kế toán xem học phí của từng học viên ở Bảng công nợ.</p>
      </AppLayout>
    );

  const studentCode = apiData?.studentCode || s?.code || 'HV260101';
  const studentName = apiData?.studentName || s?.name || user?.name || 'Nguyễn Minh Anh';
  const studentProgram = apiData?.programName || s?.program || 'Lập trình web Full-stack';
  const studentClass = apiData?.classCode || s?.cls;

  const d = debtOf(studentCode);
  const pays = PAYMENTS.filter((p) => p.student === studentCode).reverse();
  const left = d.next ? daysBetween(TODAY, d.next.due) : 0;

  return (
    <AppLayout crumb={[MODULE, 'Học phí của tôi']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Học phí của tôi: {studentName}</h2>
          <p>
            {studentProgram} · {studentClass ? `lớp ${studentClass}` : 'chưa xếp lớp'} · Mã HV: <b>{studentCode}</b>
          </p>
        </div>
      </div>
      {!d.plan ? (
        <div className="panel fe-pad">Phòng kế toán chưa lập kế hoạch thanh toán cho bạn. Học phí theo biểu phí hiện tại là {vnd(d.base)}.</div>
      ) : (
        <>
          <div className="fe-stats mf-stats">
            <div className="stat">
              <b>{vnd(d.due)}</b>
              <span>{d.plan.discount ? `Phải đóng (đã ${discountLabel(d.plan.discount).split(' · ')[0].toLowerCase()})` : 'Phải đóng'}</span>
            </div>
            <div className="stat ok">
              <b>{vnd(d.paid)}</b>
              <span>Đã đóng</span>
            </div>
            <div className={'stat' + (d.owed ? ' bad' : ' ok')}>
              <b>{vnd(d.owed)}</b>
              <span>Còn phải đóng</span>
            </div>
            <div className={'stat mf-next' + (left < 0 ? ' bad' : '')}>
              {d.next ? (
                <>
                  <span>Đợt kế tiếp</span>
                  <b>{vnd(d.next.owed)}</b>
                  <span>
                    Hạn {viDate(d.next.due)} · {left < 0 ? `quá hạn ${-left} ngày` : left === 0 ? 'hạn hôm nay' : `còn ${left} ngày`}
                  </span>
                </>
              ) : (
                <>
                  <b>Đã đóng đủ</b>
                  <span>Cảm ơn bạn</span>
                </>
              )}
            </div>
          </div>
          <div className="panel">
            <div className="fe-head">
              <h3>Các đợt thanh toán (AC1)</h3>
            </div>
            <div className="mf-items">
              {d.items.map((it) => (
                <div key={it.no} className="mf-item">
                  <b>Đợt {it.no}</b>
                  <div className="mf-amt">
                    <b>{vnd(it.amount)}</b>
                    {it.paid > 0 && it.owed > 0 && <span className="hint">Đã đóng {vnd(it.paid)}</span>}
                  </div>
                  <div className={'num' + (it.owed ? ' bad-t' : ' hint')}>{it.owed ? `Còn ${vnd(it.owed)}` : '0 ₫'}</div>
                  <div className={it.owed && it.due < TODAY ? 'bad-t' : 'hint'}>Hạn {viDate(it.due)}</div>
                  <div>
                    <Pill tone={itemTone(it.state)}>{it.state}</Pill>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </>
      )}
      <div className="mf-grid">
        <div className="panel">
          <div className="fe-head">
            <h3>Biên lai các khoản đã đóng (AC2)</h3>
          </div>
          <div className="scroll">
            <table>
              <thead>
                <tr>
                  <th>Mã biên lai</th>
                  <th>Ngày đóng</th>
                  <th className="num">Số tiền</th>
                  <th className="mf-hide">Hình thức</th>
                  <th style={{ textAlign: 'right' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {pays.map((p) => (
                  <tr key={p.id}>
                    <td className="code">{p.id}</td>
                    <td>{viDate(p.at)}</td>
                    <td className="num">{vnd(netOf(p))}</td>
                    <td className="mf-hide">{p.method}</td>
                    <td className="fe-used" style={{ textAlign: 'right' }}>
                      <button
                        type="button"
                        className="link"
                        style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, marginRight: 12, font: 'inherit' }}
                        onClick={() => setPreviewPayment(p)}
                      >
                        Xem
                      </button>
                      <Link className="link" to={`/hoc-phi/bien-lai/${p.id}?print=1`} target="_blank" title="In hoặc lưu PDF">
                        Tải PDF
                      </Link>
                    </td>
                  </tr>
                ))}
                {!pays.length && (
                  <tr>
                    <td colSpan={5} className="empty">
                      Chưa có khoản đã đóng.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
        <aside className="panel fe-pad mf-how">
          <h3>Cách đóng học phí</h3>
          <p className="hint">Tiền mặt tại quầy kế toán, hoặc chuyển khoản:</p>
          <dl>
            <dt>Ngân hàng</dt>
            <dd>Vietcombank · chi nhánh Cầu Giấy</dd>
            <dt>Số tài khoản</dt>
            <dd>0011 0000 00000</dd>
            <dt>Nội dung chuyển khoản</dt>
            <dd className="code">
              {studentCode} DOT{d.next?.no ?? 1}
            </dd>
          </dl>
        </aside>
      </div>

      {previewPayment && s && (
        <Modal onClose={() => setPreviewPayment(null)} label="Biên lai học phí" wide>
          <div style={{ padding: '8px 12px 16px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <h3 style={{ margin: 0, fontSize: 18 }}>Biên lai thu học phí: {previewPayment.id}</h3>
              <div style={{ display: 'flex', gap: 8 }}>
                <button type="button" className="btn primary" onClick={() => window.print()}>
                  <Icon name="dl" size={16} />
                  In / Lưu PDF
                </button>
                <button type="button" className="btn" onClick={() => setPreviewPayment(null)}>
                  Đóng
                </button>
              </div>
            </div>
            <div style={{ maxHeight: '75vh', overflowY: 'auto' }}>
              <ReceiptView p={previewPayment} s={s} />
            </div>
          </div>
        </Modal>
      )}
    </AppLayout>
  );
}
