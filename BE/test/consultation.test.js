const assert = require('node:assert/strict');
const { after, before, beforeEach, describe, test } = require('node:test');
const app = require('../src/app');
const { LEAD_SOURCE, LEAD_STATUS } = require('../src/constants/leads');
const { clearLeads, getAllLeads } = require('../src/data/leads');
const { consultationLimiter } = require('../src/routes/consultation.routes');

let server;
let baseUrl;

async function postConsultation(body, { endpoint = '/api/public/consultation-requests', headers = {} } = {}) {
  const reqHeaders = {
    'Content-Type': 'application/json',
    ...headers,
  };

  const response = await fetch(`${baseUrl}${endpoint}`, {
    method: 'POST',
    headers: reqHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const responseBody = await response.json();
  return { status: response.status, body: responseBody, headers: response.headers };
}

before(async () => {
  server = app.listen(0);
  await new Promise((resolve) => server.once('listening', resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

after(() => new Promise((resolve) => server.close(resolve)));

beforeEach(() => {
  clearLeads();
  consultationLimiter.reset();
});

describe('S2-08 – EP-03: Đăng ký tư vấn công khai (Public Consultation Requests)', () => {
  test('TC01: Gửi đầy đủ thông tin hợp lệ -> tạo Lead thành công với trạng thái Mới (201 Created)', async () => {
    const payload = {
      fullName: 'Nguyễn Văn A',
      phone: '0912345678',
      email: 'nguyenvana@gmail.com',
      course: 'Lập trình Web Front-end',
      message: 'Tôi muốn được tư vấn lộ trình học cho người mới bắt đầu',
      preferredTime: 'Buổi tối các ngày trong tuần',
    };

    const res = await postConsultation(payload);
    assert.equal(res.status, 201);
    assert.equal(res.body.success, true);
    assert.match(res.body.message, /Cảm ơn bạn đã đăng ký tư vấn/);
    assert.ok(res.body.data);
    assert.equal(res.body.data.fullName, 'Nguyễn Văn A');
    assert.equal(res.body.data.phone, '0912345678');
    assert.equal(res.body.data.email, 'nguyenvana@gmail.com');
    assert.equal(res.body.data.course, 'Lập trình Web Front-end');
    assert.equal(res.body.data.status, LEAD_STATUS.NEW);
    assert.ok(res.body.data.createdAt);

    // Kiểm tra trong data store
    const storedLeads = getAllLeads();
    assert.equal(storedLeads.length, 1);
    assert.equal(storedLeads[0].fullName, 'Nguyễn Văn A');
    assert.equal(storedLeads[0].source, LEAD_SOURCE.WEBSITE);
    assert.equal(storedLeads[0].status, LEAD_STATUS.NEW);
  });

  test('TC02: Gửi chỉ với thông tin tối thiểu bắt buộc (Họ tên + Số điện thoại) -> thành công (201 Created)', async () => {
    const payload = {
      fullName: 'Trần Thị B',
      phone: '0987654321',
    };

    const res = await postConsultation(payload);
    assert.equal(res.status, 201);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.fullName, 'Trần Thị B');
    assert.equal(res.body.data.phone, '0987654321');
    assert.equal(res.body.data.email, null);
    assert.equal(res.body.data.status, LEAD_STATUS.NEW);
  });

  test('TC03: Hỗ trợ tên trường dạng snake_case (full_name) và alias /api/consultation-requests', async () => {
    const payload = {
      full_name: 'Lê Hoàng C',
      phone: '+84905123456',
      notes: 'Quan tâm khóa IELTS',
    };

    const res = await postConsultation(payload, { endpoint: '/api/consultation-requests' });
    assert.equal(res.status, 201);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.fullName, 'Lê Hoàng C');
  });

  test('TC04: Từ chối khi thiếu họ và tên hoặc họ tên chỉ chứa khoảng trắng (400 Bad Request)', async () => {
    const res1 = await postConsultation({ phone: '0912345678' });
    assert.equal(res1.status, 400);
    assert.equal(res1.body.success, false);
    assert.equal(res1.body.code, 'VALIDATION_ERROR');
    assert.ok(res1.body.errors.some((err) => err.includes('họ và tên')));

    const res2 = await postConsultation({ fullName: '   ', phone: '0912345678' });
    assert.equal(res2.status, 400);
    assert.ok(res2.body.errors.some((err) => err.includes('họ và tên')));
  });

  test('TC05: Từ chối khi thiếu số điện thoại hoặc số điện thoại chỉ chứa khoảng trắng (400 Bad Request)', async () => {
    const res1 = await postConsultation({ fullName: 'Phạm Văn D' });
    assert.equal(res1.status, 400);
    assert.equal(res1.body.success, false);
    assert.equal(res1.body.code, 'VALIDATION_ERROR');
    assert.ok(res1.body.errors.some((err) => err.includes('số điện thoại')));

    const res2 = await postConsultation({ fullName: 'Phạm Văn D', phone: '   ' });
    assert.equal(res2.status, 400);
    assert.ok(res2.body.errors.some((err) => err.includes('số điện thoại')));
  });

  test('TC06: Từ chối số điện thoại sai định dạng (chứa chữ cái, không đủ số, sai tiền tố) (400 Bad Request)', async () => {
    const invalidPhones = ['abc1234567', '09123', '012345678912345678901', '1234567890'];

    for (const phone of invalidPhones) {
      const res = await postConsultation({ fullName: 'Người Dùng Test', phone });
      assert.equal(res.status, 400, `Phone ${phone} should be rejected`);
      assert.ok(res.body.errors.some((err) => err.includes('Số điện thoại')));
    }
  });

  test('TC07: Từ chối email sai định dạng nếu người dùng có nhập email (400 Bad Request)', async () => {
    const invalidEmails = ['invalid-email', 'abc@', '@domain.com', 'test@domain'];

    for (const email of invalidEmails) {
      const res = await postConsultation({
        fullName: 'Người Dùng Test',
        phone: '0912345678',
        email,
      });
      assert.equal(res.status, 400, `Email ${email} should be rejected`);
      assert.ok(res.body.errors.some((err) => err.includes('Email')));
    }
  });

  test('TC08: Không yêu cầu xác thực hoặc đăng nhập (Hoạt động bình thường không có Authorization header)', async () => {
    const res = await postConsultation(
      { fullName: 'Khách Vãng Lai', phone: '0912345678' },
      { headers: { Authorization: '' } }
    );
    assert.equal(res.status, 201);
    assert.equal(res.body.success, true);
  });

  test('TC09: Chống Mass Assignment - Không cho phép khách tự chỉ định status, assignedCounselorId hoặc id', async () => {
    const payload = {
      id: '9999',
      fullName: 'Hacker Thử Nghiệm',
      phone: '0912345678',
      status: 'WON', // Cố tình đổi sang trạng thái Chốt
      assignedCounselorId: 1, // Cố tình tự gán nhân viên
      convertedStudentId: 5,
      rejectReason: 'Không có',
    };

    const res = await postConsultation(payload);
    assert.equal(res.status, 201);

    // Dữ liệu tạo ra phải được bảo vệ
    const storedLead = getAllLeads()[0];
    assert.notEqual(storedLead.id, '9999');
    assert.equal(storedLead.status, LEAD_STATUS.NEW);
    assert.equal(storedLead.assignedCounselorId, null);
    assert.equal(storedLead.convertedStudentId, null);
    assert.equal(storedLead.rejectReason, null);
  });

  test('TC10: Làm sạch nội dung (Sanitization) - Loại bỏ các thẻ độc hại XSS trong họ tên và nội dung', async () => {
    const payload = {
      fullName: '<script>alert("XSS")</script>Nguyễn Văn E<b> bold</b>',
      phone: '0912345678',
      message: 'Tôi muốn tư vấn <iframe src="evil.com"></iframe> ngay!',
    };

    const res = await postConsultation(payload);
    assert.equal(res.status, 201);
    assert.equal(res.body.data.fullName, 'Nguyễn Văn E bold');

    const storedLead = getAllLeads()[0];
    assert.equal(storedLead.fullName, 'Nguyễn Văn E bold');
    assert.equal(storedLead.notes, 'Tôi muốn tư vấn  ngay!');
    assert.ok(!storedLead.notes.includes('<iframe'));
    assert.ok(!storedLead.fullName.includes('<script'));
  });

  test('TC11: Chống gửi trùng lặp nhiều lần trong thời gian ngắn (Debounce double-click)', async () => {
    const payload = {
      fullName: 'Khách Hàng Nhanh Tay',
      phone: '0933112233',
      message: 'Bấm gửi liên tiếp 2 lần',
    };

    // Lần 1: Thành công
    const res1 = await postConsultation(payload);
    assert.equal(res1.status, 201);

    // Lần 2 (ngay lập tức trong vòng 10 giây với cùng số điện thoại): Bị từ chối để chống tạo 2 bản ghi trùng
    const res2 = await postConsultation(payload);
    assert.equal(res2.status, 409);
    assert.equal(res2.body.code, 'DUPLICATE_SUBMISSION');
    assert.match(res2.body.message, /không bấm gửi liên tục/);

    // Chỉ có đúng 1 bản ghi được tạo trong CSDL/bộ nhớ
    const stored = getAllLeads().filter((l) => l.phone === '0933112233');
    assert.equal(stored.length, 1);
  });

  test('TC12: Chống spam bằng Rate Limiter (Quá 5 requests từ cùng 1 IP trong 1 phút -> 429)', async () => {
    // Gửi 5 request hợp lệ liên tiếp với các số điện thoại khác nhau từ cùng 1 IP
    for (let i = 1; i <= 5; i++) {
      const res = await postConsultation({
        fullName: `Khách Thử Nghiệm ${i}`,
        phone: `091234560${i}`,
      });
      assert.equal(res.status, 201, `Request ${i} should succeed`);
    }

    // Request thứ 6: Phải bị chặn bởi Rate Limiter
    const blockedRes = await postConsultation({
      fullName: 'Khách Thử Nghiệm 6',
      phone: '0912345606',
    });

    assert.equal(blockedRes.status, 429);
    assert.equal(blockedRes.body.success, false);
    assert.equal(blockedRes.body.code, 'RATE_LIMIT_EXCEEDED');
    assert.match(blockedRes.body.message, /quá nhiều yêu cầu/);
  });
});
