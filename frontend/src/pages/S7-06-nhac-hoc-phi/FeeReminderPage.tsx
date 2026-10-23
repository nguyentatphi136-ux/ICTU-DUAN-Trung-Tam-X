import { useEffect, useState } from 'react';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Modal, Pager, paginate, Pill, usePaging } from '../../components/ui';
import { BEFORE_DAYS, dueReminders, FEE_MAILS, runFeeReminders, subjectOf, type FeeMail } from '../../data/feeReminders';
import { TODAY, viDateTime } from '../../data/funnel';
import { onNotice } from '../../data/notifications';
import { viDate } from '../../data/leads';
import { vnd } from '../../data/vnd';
import { FeeTabs, MODULE } from '../S7-01-bieu-phi/FeeTablePage';
import '../S7-01-bieu-phi/fees.css';
import './mail.css';

type Sample = Omit<FeeMail, 'at' | 'app' | 'mail'>;
const SAMPLE: Sample = { student: 'HV260142', name: 'DO TUAN KIET', email: 'kiet.dt@gmail.com', no: 2, program: 'Lập trình web Full-stack', kind: 'Trước hạn', days: 3, owed: 1565000, amount: 2565000, paid: 1000000, due: '2026-11-25' };

/** Mẫu email nhắc học phí, dùng cho cả xem trước và xem lại email đã gửi. */
export function FeeMailView({ m }: { m: Sample }) {
  const before = m.kind === 'Trước hạn';
  return (
    <div className="ml">
      <p className="ml-subject">Tiêu đề: {subjectOf(m)}</p>
      <div className="ml-card">
        <div className="ml-top">TMS.</div>
        <div className="ml-body">
          <p>Chào {m.name},</p>
          <span className={'ml-badge' + (before ? '' : ' bad')}>{before ? `Còn ${m.days} ngày` : `Quá hạn ${m.days} ngày`}</span>
          <h4>
            Học phí đợt {m.no} của bạn {before ? 'sắp đến hạn' : 'đã quá hạn'}
          </h4>
          <dl>
            <div>
              <dt>Đợt</dt>
              <dd>
                Đợt {m.no} · {m.program}
              </dd>
            </div>
            <div>
              <dt>{before ? 'Số tiền còn phải đóng' : 'Số tiền còn nợ'}</dt>
              <dd className={before ? undefined : 'bad-t'}>{vnd(m.owed)}</dd>
            </div>
            <div>
              <dt>{before ? 'Hạn thanh toán' : 'Số ngày quá hạn'}</dt>
              <dd className={before ? undefined : 'bad-t'}>{before ? viDate(m.due) : `${m.days} ngày`}</dd>
            </div>
            <div>
              <dt>Đã đóng</dt>
              <dd>
                {vnd(m.paid)} / {vnd(m.amount)}
              </dd>
            </div>
          </dl>
          <span className="ml-cta">Xem học phí của tôi</span>
          <p className="hint">
            {before
              ? 'Bạn có thể đóng tiền mặt tại quầy kế toán hoặc chuyển khoản theo thông tin trong mục Học phí của tôi.'
              : 'Nếu bạn đã đóng trong hôm nay, vui lòng bỏ qua email này. Cần hỗ trợ, liên hệ phòng kế toán.'}
          </p>
        </div>
        <p className="ml-foot">Bạn nhận email này vì có đợt học phí sắp hoặc đã đến hạn tại TMS.</p>
      </div>
    </div>
  );
}

// S7-06. Nhắc học phí: trước hạn 3 ngày và khi quá hạn, qua email và thông báo trong ứng dụng; mỗi đợt tối đa một lần mỗi ngày.
export function FeeReminderPage() {
  const toast = useToast();
  const [, setTick] = useState(0);
  const [open, setOpen] = useState<FeeMail | null>(null);
  // Chuông chạy tác vụ nhắc khi mở ứng dụng; vẽ lại khi có thông báo mới để số liệu khớp.
  useEffect(() => onNotice(() => setTick((t) => t + 1)), []);
  const { page, setPage, size, setSize } = usePaging(10);
  const today = dueReminders();
  const view = paginate(FEE_MAILS, page, size);

  return (
    <AppLayout crumb={[MODULE, 'Nhắc học phí']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Nhắc học phí</h2>
          <p>
            Tác vụ định kỳ chạy mỗi ngày một lần lúc 07:00: nhắc trước hạn {BEFORE_DAYS} ngày và mỗi ngày khi quá hạn. Mỗi đợt chỉ nhận tối đa một email mỗi ngày.
          </p>
        </div>
        <button
          type="button"
          className="btn primary"
          onClick={() => {
            const n = runFeeReminders();
            setTick((t) => t + 1);
            toast(n ? `Đã nhắc ${n} đợt học phí` : 'Hôm nay đã nhắc đủ', n ? `Ngày ${viDate(TODAY)}` : 'Không gửi trùng cho cùng một đợt trong ngày');
          }}
        >
          <Icon name="mail" size={16} />
          Chạy tác vụ hôm nay
        </button>
      </div>
      <FeeTabs />
      <div className="fe-stats">
        <div className="stat warn">
          <b>{today.filter((r) => r.kind === 'Trước hạn').length}</b>
          <span>Đợt đến hạn sau {BEFORE_DAYS} ngày</span>
        </div>
        <div className="stat bad">
          <b>{today.filter((r) => r.kind === 'Quá hạn').length}</b>
          <span>Đợt đang quá hạn</span>
        </div>
        <div className="stat">
          <b>{FEE_MAILS.filter((m) => m.at.startsWith(TODAY)).length}</b>
          <span>Đã nhắc hôm nay</span>
        </div>
        <div className="stat">
          <b>{FEE_MAILS.length}</b>
          <span>Tổng số lần đã nhắc</span>
        </div>
      </div>
      <div className="nh-grid">
        <section>
          <h3>1. Trước hạn {BEFORE_DAYS} ngày</h3>
          <FeeMailView m={SAMPLE} />
        </section>
        <section>
          <h3>2. Khi quá hạn</h3>
          <FeeMailView m={{ ...SAMPLE, kind: 'Quá hạn', days: 1 }} />
        </section>
      </div>
      <p className="hint" style={{ margin: '12px 0 24px' }}>
        Thông báo trong ứng dụng dùng mẫu chuông ở S6-09 (loại Học phí). Học viên tắt "Học phí sắp đến hạn" ở cài đặt thông báo thì không nhận kênh đó.
      </p>
      <div className="panel">
        <div className="fe-head">
          <h3>Lịch sử nhắc</h3>
        </div>
        <div className="scroll">
          <table className="rows">
            <thead>
              <tr>
                <th>Thời điểm</th>
                <th>Học viên</th>
                <th>Tiêu đề</th>
                <th className="num">Còn nợ</th>
                <th>Kênh</th>
              </tr>
            </thead>
            <tbody>
              {view.items.map((m, i) => (
                <tr key={i} tabIndex={0} onClick={() => setOpen(m)} onKeyDown={(e) => e.key === 'Enter' && setOpen(m)}>
                  <td>{viDateTime(m.at)}</td>
                  <td>
                    <b className="strong">{m.name}</b>
                    <div className="hint">{m.student}</div>
                  </td>
                  <td>{subjectOf(m)}</td>
                  <td className="num bad-t">{vnd(m.owed)}</td>
                  <td>
                    {m.mail && <Pill tone="info">Email</Pill>} {m.app && <Pill>Ứng dụng</Pill>}
                  </td>
                </tr>
              ))}
              {!FEE_MAILS.length && (
                <tr>
                  <td colSpan={5} className="empty">
                    Chưa gửi nhắc nào. Tác vụ chạy lúc 07:00 mỗi ngày, hoặc bấm Chạy tác vụ hôm nay.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pager page={view.page} pages={view.pages} onPage={setPage} size={size} onSize={setSize}>
          {view.from} đến {view.to} trong {FEE_MAILS.length} lần nhắc
        </Pager>
      </div>
      {open && (
        <Modal onClose={() => setOpen(null)} label="Email đã gửi">
          <p className="hint" style={{ marginBottom: 8 }}>
            Gửi tới {open.email} lúc {viDateTime(open.at)}
          </p>
          <FeeMailView m={open} />
        </Modal>
      )}
    </AppLayout>
  );
}
