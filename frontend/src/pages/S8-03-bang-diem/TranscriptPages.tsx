import { useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Box, Modal, Pill } from '../../components/ui';
import { useAuth, type User } from '../../data/auth';
import { TODAY } from '../../data/funnel';
import { num, resultTone, transcriptNo, transcriptOf } from '../../data/grades';
import { viDate } from '../../data/leads';
import { STUDENTS } from '../../data/students';
import { Breakdown, MODULE } from '../S8-02-diem-tong-ket/FinalPages';
import '../S7-03-thanh-toan/receipt.css';
import './transcript.css';

/** Học viên chỉ xem bảng điểm của mình; nhân viên xem theo mã trên đường dẫn. */
const codeFor = (user: User | null, param?: string) => (user?.active === 0 ? STUDENTS.find((s) => s.login === user.email)?.code : param);
const mm = (iso: string) => `${iso.slice(5, 7)}/${iso.slice(0, 4)}`;

// S8-03. Bảng điểm toàn khoá: môn đã chốt điểm, điểm tổng kết, kết quả, điểm trung bình theo trọng số môn; xuất PDF.
export function TranscriptPage() {
  const { user } = useAuth();
  const { code: param } = useParams();
  const [open, setOpen] = useState<string | null>(null);
  const code = codeFor(user, param);
  if (!code && user?.active !== 0) return <Navigate to="/diem-tong-ket" replace />;
  const t = code ? transcriptOf(code) : null;
  if (!t)
    return (
      <AppLayout crumb={[MODULE, 'Bảng điểm toàn khoá']} module={MODULE}>
        <p className="empty">Chưa có bảng điểm: học viên chưa được xếp lớp.</p>
      </AppLayout>
    );
  const failed = t.rows.filter((r) => r.f.result !== 'Đạt');
  const formula = t.rows.filter((r) => r.f.total != null);
  const pick = t.rows.find((r) => r.s.code === open);

  return (
    <AppLayout crumb={[MODULE, 'Bảng điểm toàn khoá']} module={MODULE}>
      {user?.active !== 0 && (
        <Link to="/diem-tong-ket" className="back">
          <Icon name="back" size={16} />
          Điểm tổng kết môn
        </Link>
      )}
      <div className="h">
        <div>
          <h2>Bảng điểm toàn khoá{user?.active !== 0 ? ` của ${t.st.name}` : ''}</h2>
          <p>
            {t.st.program} · lớp {t.k.code}
          </p>
        </div>
        <Link className="btn" to={`/bang-diem/${t.st.code}/in`} target="_blank">
          <Icon name="dl" size={16} />
          Xuất PDF
        </Link>
      </div>
      <div className="fe-stats">
        <div className="stat info">
          <b>{t.gpa == null ? '–' : num(t.gpa)}</b>
          <span>Điểm trung bình toàn khoá</span>
        </div>
        <div className="stat">
          <b>
            {t.passed} / {t.subjects}
          </b>
          <span>Số môn đã đạt</span>
        </div>
        <div className="stat">
          <b>
            {t.done} / {t.total}
          </b>
          <span>Số buổi đã học</span>
        </div>
        <div className={'stat ' + (t.complete ? 'ok' : 'bad')}>
          <b>{t.complete ? 'Đủ' : 'Chưa đủ'}</b>
          <span>Điều kiện hoàn thành khoá</span>
        </div>
      </div>
      <div className="panel">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Mã môn</th>
                <th>Môn học</th>
                <th className="num">Số buổi</th>
                <th className="num">Trọng số</th>
                <th className="num">Điểm tổng kết</th>
                <th>Kết quả</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {t.rows.map((r) => (
                <tr key={r.s.code}>
                  <td className="code">{r.s.code}</td>
                  <td className="strong">{r.s.name}</td>
                  <td className="num hint">{r.s.sessions}</td>
                  <td className="num hint">{r.weight}</td>
                  <td className={'num strong' + (r.f.result === 'Đạt' ? '' : ' bad-t')}>{r.f.total == null ? '–' : num(r.f.total)}</td>
                  <td>
                    <Pill tone={resultTone(r.f.result)}>{r.f.result}</Pill>
                  </td>
                  <td className="fe-used">
                    <button type="button" className="link" onClick={() => setOpen(r.s.code)}>
                      Xem cách tính
                    </button>
                  </td>
                </tr>
              ))}
              {!t.rows.length && (
                <tr>
                  <td colSpan={7} className="empty">
                    Chưa có môn nào được chốt điểm.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {formula.length > 0 && (
          <p className="hint fe-pad">
            Điểm trung bình toàn khoá tính theo trọng số môn: ({formula.map((r) => `${num(r.f.total!)}×${r.weight}`).join(' + ')}) ÷ {formula.reduce((n, r) => n + r.weight, 0)} = {num(t.gpa!)}
          </p>
        )}
      </div>
      {failed.length > 0 && (
        <div style={{ marginTop: 16 }}>
          <Box icon="alert" tone="warn" title={`Môn ${failed.map((r) => r.s.code).join(', ')} chưa đạt`}>
            Bạn cần đạt toàn bộ môn để đủ điều kiện hoàn thành khoá. Liên hệ Quản lý đào tạo về việc học lại hoặc thi lại.
          </Box>
        </div>
      )}
      {t.rows.length < t.subjects && <p className="hint" style={{ marginTop: 12 }}>Chỉ hiện môn đã chốt điểm. Còn {t.subjects - t.rows.length} môn chưa chốt hoặc chưa học.</p>}
      {pick && (
        <Modal onClose={() => setOpen(null)} label="Cách tính điểm">
          <p className="hint" style={{ marginBottom: 8 }}>
            {pick.s.code} · {pick.s.name}
          </p>
          <Breakdown name={t.st.name} f={pick.f} />
        </Modal>
      )}
    </AppLayout>
  );
}

// S8-03. Mẫu PDF bảng điểm: in hoặc lưu PDF bằng hộp thoại in của trình duyệt.
export function TranscriptPrintPage() {
  const { user } = useAuth();
  const { code: param } = useParams();
  if (!user) return <Navigate to="/dang-nhap" replace />;
  const code = codeFor(user, param);
  const t = code && (user.active !== 0 || code === param) ? transcriptOf(code) : null;
  if (!t) return <p className="empty">Không tìm thấy bảng điểm.</p>;
  const no = transcriptNo(t.st.code, TODAY.slice(0, 4));
  const end = t.k.subjects.length ? `${mm(t.k.start)} – ${t.complete ? mm(TODAY) : 'nay'}` : '';
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
            <span>Tầng 3, cơ sở Cầu Giấy, Hà Nội</span>
          </div>
          <div className="rc-title">
            <b>BẢNG ĐIỂM TOÀN KHOÁ</b>
            <span>Số: {no}</span>
          </div>
        </header>
        <dl>
          <div><dt>Họ và tên</dt><dd>{t.st.name}</dd></div>
          <div><dt>Mã học viên</dt><dd>{t.st.code}</dd></div>
          <div><dt>Ngày sinh</dt><dd>{t.st.birth ? viDate(t.st.birth) : '–'}</dd></div>
          <div><dt>Chương trình</dt><dd>{t.st.program}</dd></div>
          <div><dt>Lớp</dt><dd>{t.k.code}</dd></div>
          <div><dt>Thời gian học</dt><dd>{end}</dd></div>
        </dl>
        <table>
          <thead>
            <tr>
              <th>Mã môn</th>
              <th>Môn học</th>
              <th className="num">Số buổi</th>
              <th className="num">Điểm tổng kết</th>
              <th>Kết quả</th>
            </tr>
          </thead>
          <tbody>
            {t.rows.map((r) => (
              <tr key={r.s.code}>
                <td>{r.s.code}</td>
                <td>{r.s.name}</td>
                <td className="num">{r.s.sessions}</td>
                <td className="num">{r.f.total == null ? '–' : num(r.f.total)}</td>
                <td>{r.f.result}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="rc-sum">
          <div>
            <span>
              Điểm trung bình toàn khoá
              <br />
              <small>Thang điểm 10 · tính theo trọng số môn</small>
            </span>
            <b className="tr-gpa">{t.gpa == null ? '–' : num(t.gpa)}</b>
          </div>
        </div>
        <p>
          Tỷ lệ chuyên cần: {t.attendance ?? '–'}% &nbsp;&nbsp; Số môn đạt: {t.passed}/{t.subjects}
        </p>
        <div className="rc-sign">
          <div />
          <div>
            <span>
              Hà Nội, ngày {TODAY.slice(8)} tháng {TODAY.slice(5, 7)} năm {TODAY.slice(0, 4)}
            </span>
            <b>Quản lý đào tạo</b>
            <em>LE HOANG NAM</em>
          </div>
        </div>
        <footer>
          <span>
            Ngày cấp: {viDate(TODAY)} · Mã tra cứu: {no} · Bảng điểm xuất từ hệ thống TMS, chỉ gồm các môn đã chốt điểm.
          </span>
        </footer>
      </article>
    </div>
  );
}
