import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Icon } from '../../components/Icon';
import { Box, Modal } from '../../components/ui';
import { apiEnabled } from '../../data/api';
import { TODAY, type FunnelLead } from '../../data/funnel';
import { convertLead } from '../../data/s3api';
import { saveAll } from '../../data/store';
import { dispatchActivationEmail } from '../../data/email';
import { loginOf, nextStudentCode, STUDENTS, type Student } from '../../data/students';
import { USERS } from '../../data/users';
import { EMAIL_RE } from '../../data/validate';
import './convert.css';

type Props = {
  lead: FunnelLead;
  onClose: () => void;
  onDone: (code: string, sentTo: string, emailSent: boolean, tempPassword?: string, loginEmail?: string) => void;
};

// S3-03. Chuyển lead đã chốt thành hồ sơ học viên: dữ liệu lấy từ lead, mã học viên và email đăng nhập do hệ thống sinh.
// Có backend: POST /api/leads/:id/convert { sendTo } tạo hồ sơ, tài khoản Học viên và gửi email kích hoạt trong một lần.
export function ConvertDialog({ lead, onClose, onDone }: Props) {
  // Có backend thì mã do máy chủ sinh lúc tạo, trước đó chưa biết.
  const code = apiEnabled ? 'Tự sinh khi tạo' : nextStudentCode();
  const login = apiEnabled ? '<mã học viên>@tms.vn' : loginOf(Number(code.slice(4)) % 1000);
  const [sendTo, setSendTo] = useState(lead.email ?? '');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  function create() {
    if (!EMAIL_RE.test(sendTo.trim())) return setError('Nhập email hợp lệ để gửi thông tin đăng nhập');
    if (apiEnabled) {
      setBusy(true);
      return void convertLead(lead.id, sendTo.trim()).then(
        (r) => onDone(r.code, r.email, r.emailSent),
        (e: Error) => (setBusy(false), setError(e.message)),
      );
    }
    const tempPassword = 'Tms@2026';
    const s: Student = {
      code,
      name: lead.name,
      phone: lead.phone,
      email: sendTo.trim(),
      login,
      program: lead.program,
      status: 'Mới nhập học',
      enrolledAt: TODAY,
      counselor: lead.owner,
      source: lead.source,
      notes: [],
      history: [],
      leadId: lead.id,
    };
    STUDENTS.unshift(s);
    const newId = Math.max(0, ...USERS.map((u) => u.id)) + 1;
    USERS.unshift({
      id: newId,
      name: s.name,
      displayName: s.name,
      email: login,
      contactEmail: sendTo.trim(),
      phone: s.phone,
      roles: [0],
      status: 'Chờ kích hoạt',
      tempPassword,
      code: s.code,
    });
    saveAll();

    void dispatchActivationEmail({
      toEmail: sendTo.trim(),
      fullName: s.name,
      loginEmail: login,
      tempPassword,
      roleName: 'Học viên',
    });

    onDone(s.code, s.email, true, tempPassword, login);
  }

  const facts: [string, string][] = [
    ['Họ và tên', lead.name],
    ['Số điện thoại', lead.phone],
    ['Email liên hệ', lead.email ?? 'Chưa có'],
    ['Chương trình quan tâm', lead.program],
    ['Nguồn', lead.source],
    ['Tư vấn viên', lead.owner],
  ];

  return (
    <Modal onClose={onClose} label="Chuyển lead thành học viên" wide>
      <h3 style={{ marginBottom: 4 }}>Chuyển lead thành học viên</h3>
      <p className="hint" style={{ fontSize: 14, marginBottom: 18 }}>
        Thông tin dưới đây lấy từ lead, bạn không phải nhập lại. Kiểm tra rồi xác nhận.
      </p>
      <dl className="cv-facts">
        {facts.map(([k, v]) => (
          <div key={k}>
            <dt>{k}</dt>
            <dd>{v}</dd>
          </div>
        ))}
      </dl>
      <div className="grid2">
        <div className="field">
          <label htmlFor="cv-code">Mã học viên</label>
          <div className="inp lead">
            <Icon name="lock" />
            <input id="cv-code" value={code} disabled />
          </div>
          <p className="hint">Hệ thống tự sinh</p>
        </div>
        <div className="field">
          <label htmlFor="cv-login">Email đăng nhập</label>
          <div className="inp lead">
            <Icon name="lock" />
            <input id="cv-login" value={login} disabled />
          </div>
          <p className="hint">Tự cấp theo mã vai trò Học viên</p>
        </div>
      </div>
      <div className="field">
        <label htmlFor="cv-send">Gửi thông tin đăng nhập tới</label>
        <div className={'inp lead' + (error ? ' bad' : '')}>
          <Icon name="mail" />
          <input
            id="cv-send"
            type="email"
            value={sendTo}
            placeholder="email@vi-du.com"
            onChange={(e) => {
              setSendTo(e.target.value);
              setError('');
            }}
          />
        </div>
        {error ? <p className="err">{error}</p> : <p className="hint">Email kích hoạt kèm mật khẩu tạm sẽ gửi tới địa chỉ này</p>}
      </div>
      <Box icon="lock" tone="warn" title="Sau khi chuyển, lead này không sửa được nữa">
        Lead chuyển sang trạng thái Đã nhập học. Mọi thay đổi về sau thực hiện ở hồ sơ học viên.
      </Box>
      <div className="acts">
        <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className="btn primary" style={{ width: 164 }} disabled={busy} onClick={create}>
          {busy ? 'Đang tạo…' : 'Tạo học viên'}
        </button>
      </div>
    </Modal>
  );
}

/** Phần đầu ngăn lead (S3-02): nút chuyển với lead ở cột Chốt, hoặc kết quả đã chuyển với lead đã khoá. */
export function ConvertBlock({ lead, canWrite, onConvert }: { lead: FunnelLead; canWrite: boolean; onConvert: () => void }) {
  if (lead.student) {
    const s = STUDENTS.find((x) => x.code === lead.student);
    return (
      <div className="cv-done">
        <Box icon="check" tone="ok" title={'Đã chuyển thành học viên ' + lead.student}>
          {s ? `Tài khoản ${s.login} đã được tạo. Email kích hoạt đã gửi tới ${s.email}.` : `Tài khoản ${lead.student}@tms.vn đã được tạo.`}
        </Box>
        <Link to={'/hoc-vien/' + lead.student} className="btn" style={{ width: 200 }}>
          Mở hồ sơ học viên
          <Icon name="go" size={16} />
        </Link>
        <Box icon="lock" tone="info" title="Lead này chỉ còn xem được">
          Không sửa thông tin, không đổi trạng thái và không ghi thêm nhật ký. Thẻ không kéo được trên bảng phễu.
        </Box>
      </div>
    );
  }
  if (lead.stage !== 'Chốt' || !canWrite) return null;
  return (
    <div className="cv-cta">
      <div>
        <b>Khách đã chốt đăng ký</b>
        <span>Tạo hồ sơ học viên và tài khoản đăng nhập từ thông tin của lead.</span>
      </div>
      <button type="button" className="btn primary" onClick={onConvert}>
        <Icon name="cap" size={16} />
        Chuyển thành học viên
      </button>
    </div>
  );
}
