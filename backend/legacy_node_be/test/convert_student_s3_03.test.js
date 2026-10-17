const assert = require('node:assert/strict');
const { after, before, beforeEach, describe, test } = require('node:test');
const app = require('../src/app');
const { seedSampleLeads, leads } = require('../src/data/leads');
const { seedSampleStudents, students } = require('../src/data/students');
const { resetUsers } = require('../src/data/users');

let server;
let baseUrl;
const tokens = {};

async function api(method, path, { token, body } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const contentType = response.headers.get('content-type') || '';
  const responseBody = contentType.includes('application/json') ? await response.json() : null;
  return { status: response.status, body: responseBody };
}

async function login(email, password) {
  const { status, body } = await api('POST', '/api/auth/login', { body: { email, password } });
  assert.equal(status, 200, `Login failed for ${email}`);
  return body;
}

before(async () => {
  server = app.listen(0);
  await new Promise((resolve) => server.once('listening', resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;

  tokens.admin = (await login('admin@example.com', 'admin123')).token;
  tokens.manager = (await login('manager@example.com', 'manager123')).token; // TrainingManager
  tokens.admissions1 = (await login('admissions@example.com', 'admissions123')).token; // id: 5 (Lê Thị Thu Hà)
  tokens.admissions2 = (await login('tuvan@example.com', 'tuvan123')).token; // id: 6 (Hoàng Văn Tư)
  tokens.instructor = (await login('instructor@example.com', 'instructor123')).token;
  tokens.student = (await login('student@example.com', 'student123')).token;
});

after(() => new Promise((resolve) => server.close(resolve)));

beforeEach(() => {
  resetUsers();
  seedSampleLeads();
  seedSampleStudents();
});

describe('S3-03 (IDTTX-211 / Subtask IDTTX-240): Chuyển lead đã chốt thành hồ sơ học viên', () => {
  test('1. Lấy trước mã học viên và email đăng nhập tự sinh tiếp theo (GET /api/students/next-code)', async () => {
    const res = await api('GET', '/api/students/next-code', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(res.body.nextCode, 'HV260143');
    assert.equal(res.body.nextLogin, 'S25200212143@tms.vn');
  });

  test('2. Chuyển lead ở trạng thái "Chốt" thành học viên thành công (POST /api/leads/:id/convert)', async () => {
    // Lead 5 (Trần Minh Đức) đang ở giai đoạn Chốt (WON) của counselor 6 (Hoàng Văn Tư)
    const res = await api('POST', '/api/leads/5/convert', {
      token: tokens.admissions2,
      body: {
        sendTo: 'duc.tm@gmail.com',
      },
    });

    assert.equal(res.status, 201);
    assert.equal(res.body.success, true);
    assert.ok(res.body.data);

    // Tiêu chí 1: Dữ liệu lead được chuyển sang hồ sơ học viên, không phải nhập lại
    assert.equal(res.body.data.name, 'Trần Minh Đức');
    assert.equal(res.body.data.phone, '0903456789');
    assert.equal(res.body.data.email, 'duc.tm@gmail.com');
    assert.equal(res.body.data.program, 'Lập trình Frontend React');
    assert.equal(res.body.data.status, 'Mới nhập học');
    assert.equal(res.body.data.leadId, 5);

    // Tiêu chí 2: Hệ thống sinh mã học viên tự động
    assert.equal(res.body.data.code, 'HV260143');

    // Tiêu chí 3: Tài khoản đăng nhập cho học viên được tạo và gửi email
    assert.equal(res.body.data.login, 'S25200212143@tms.vn');
    assert.ok(res.body.user);
    assert.equal(res.body.user.email.toLowerCase(), 's25200212143@tms.vn');
    assert.ok(res.body.user.roles.includes('Student'));
    assert.equal(res.body.emailSentTo, 'duc.tm@gmail.com');
  });

  test('3. Tài khoản đăng nhập mới tạo có thể đăng nhập được vào hệ thống với vai trò Student', async () => {
    await api('POST', '/api/leads/5/convert', {
      token: tokens.manager,
      body: { sendTo: 'duc.tm@gmail.com' },
    });

    // Đăng nhập bằng tài khoản sinh tự động và mật khẩu mặc định
    const loginRes = await api('POST', '/api/auth/login', {
      body: {
        email: 'S25200212143@tms.vn',
        password: 'Student@123',
      },
    });

    assert.equal(loginRes.status, 200);
    assert.equal(loginRes.body.user.name, 'Trần Minh Đức');
    assert.ok(loginRes.body.user.roles.includes('Student'));
  });

  test('4. Lead chuyển sang trạng thái Đã nhập học và tự động ghi nhật ký chuyển đổi', async () => {
    await api('POST', '/api/leads/5/convert', {
      token: tokens.admissions2,
      body: { sendTo: 'duc.tm@gmail.com' },
    });

    // Kiểm tra thông tin lead sau khi chuyển
    const detailRes = await api('GET', '/api/leads/5', { token: tokens.admissions2 });
    assert.equal(detailRes.status, 200);
    const lead = detailRes.body.data;

    assert.equal(lead.student, 'HV260143');
    assert.equal(lead.isConverted, true);
    assert.equal(lead.isLocked, true);
    assert.equal(lead.subStatus, 'Đã nhập học');

    // Kiểm tra dòng thời gian (timeline) và nhật ký chăm sóc (care logs)
    const latestTimeline = lead.timeline[0];
    assert.equal(latestTimeline.type, 'STATUS_CHANGE');
    assert.match(latestTimeline.content, /HV260143/);
    assert.match(latestTimeline.content, /duc\.tm@gmail\.com/);

    const logsRes = await api('GET', '/api/leads/5/logs', { token: tokens.admissions2 });
    assert.equal(logsRes.status, 200);
    assert.ok(logsRes.body.data.some((l) => l.text.includes('HV260143')));
  });

  test('5. Lead chuyển sang trạng thái Đã nhập học và KHÔNG sửa được nữa (khoá kéo thả đổi stage)', async () => {
    // Chuyển lead 5 thành học viên
    await api('POST', '/api/leads/5/convert', {
      token: tokens.admissions2,
      body: { sendTo: 'duc.tm@gmail.com' },
    });

    // Cố kéo thả đổi stage sang 'Đang tư vấn' -> Bị chặn với lỗi 400
    const moveRes = await api('PATCH', '/api/leads/5/stage', {
      token: tokens.admissions2,
      body: { stage: 'Đang tư vấn' },
    });

    assert.equal(moveRes.status, 400);
    assert.equal(moveRes.body.code, 'LEAD_LOCKED');
    assert.match(moveRes.body.message, /Đã nhập học và không sửa được nữa/i);
  });

  test('6. Lead đã nhập học KHÔNG sửa được trạng thái status (PATCH /api/leads/:id/status)', async () => {
    await api('POST', '/api/leads/5/convert', {
      token: tokens.admissions2,
      body: { sendTo: 'duc.tm@gmail.com' },
    });

    const statusRes = await api('PATCH', '/api/leads/5/status', {
      token: tokens.admissions2,
      body: { status: 'CONSULTING' },
    });

    assert.equal(statusRes.status, 400);
    assert.equal(statusRes.body.code, 'LEAD_LOCKED');
  });

  test('7. Lead đã nhập học KHÔNG thêm được nhật ký chăm sóc mới (POST /api/leads/:id/logs)', async () => {
    await api('POST', '/api/leads/5/convert', {
      token: tokens.admissions2,
      body: { sendTo: 'duc.tm@gmail.com' },
    });

    const logRes = await api('POST', '/api/leads/5/logs', {
      token: tokens.admissions2,
      body: { text: 'Gọi lại hỏi thăm học viên' },
    });

    assert.equal(logRes.status, 400);
    assert.equal(logRes.body.code, 'LEAD_LOCKED');
  });

  test('8. Chuyển lead CHƯA chốt (ví dụ đang ở cột "Đang tư vấn") -> Bị từ chối với lỗi 400 (LEAD_NOT_WON)', async () => {
    // Lead 1 (Vũ Minh Anh) đang ở cột "Đang tư vấn", chưa chốt
    const res = await api('POST', '/api/leads/1/convert', {
      token: tokens.admissions1,
      body: { sendTo: 'minhanh.vu@gmail.com' },
    });

    assert.equal(res.status, 400);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'LEAD_NOT_WON');
    assert.match(res.body.message, /Chỉ lead đã chốt/i);
  });

  test('9. Chuyển lead ĐÃ chuyển trước đó -> Bị từ chối với lỗi 409 (LEAD_ALREADY_CONVERTED)', async () => {
    // Chuyển lần 1
    await api('POST', '/api/leads/5/convert', {
      token: tokens.admissions2,
      body: { sendTo: 'duc.tm@gmail.com' },
    });

    // Cố chuyển lần 2 trên cùng một lead
    const res = await api('POST', '/api/leads/5/convert', {
      token: tokens.admissions2,
      body: { sendTo: 'duc.tm@gmail.com' },
    });

    assert.equal(res.status, 409);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'LEAD_ALREADY_CONVERTED');
    assert.match(res.body.message, /đã được chuyển thành học viên/i);
  });

  test('10. Email gửi thông tin không đúng định dạng -> Bị từ chối với lỗi 400 (INVALID_EMAIL)', async () => {
    const res = await api('POST', '/api/leads/5/convert', {
      token: tokens.admissions2,
      body: { sendTo: 'email-sai-dinh-dang' },
    });

    assert.equal(res.status, 400);
    assert.equal(res.body.code, 'INVALID_EMAIL');
    assert.match(res.body.message, /email hợp lệ/i);
  });

  test('11. Phân quyền: Tư vấn viên chỉ chuyển được lead được phân công cho mình', async () => {
    // Lead 5 được giao cho counselor 6 (Hoàng Văn Tư). Counselor 5 (Lê Thị Thu Hà) cố chuyển sẽ nhận 403
    const res = await api('POST', '/api/leads/5/convert', {
      token: tokens.admissions1,
      body: { sendTo: 'duc.tm@gmail.com' },
    });

    assert.equal(res.status, 403);
    assert.equal(res.body.code, 'FORBIDDEN');
    assert.match(res.body.message, /chỉ có quyền/i);

    // Admin và Quản lý đào tạo (TrainingManager) chuyển được
    const managerRes = await api('POST', '/api/leads/5/convert', {
      token: tokens.manager,
      body: { sendTo: 'duc.tm@gmail.com' },
    });
    assert.equal(managerRes.status, 201);
  });

  test('12. Phân quyền: Giảng viên và Học viên bị chặn khỏi API chuyển đổi', async () => {
    const instructorRes = await api('POST', '/api/leads/5/convert', {
      token: tokens.instructor,
      body: { sendTo: 'duc.tm@gmail.com' },
    });
    assert.equal(instructorRes.status, 403);

    const studentRes = await api('POST', '/api/leads/5/convert', {
      token: tokens.student,
      body: { sendTo: 'duc.tm@gmail.com' },
    });
    assert.equal(studentRes.status, 403);
  });

  test('13. Tra cứu danh sách học viên (GET /api/students) tìm thấy học viên vừa chuyển', async () => {
    await api('POST', '/api/leads/5/convert', {
      token: tokens.manager,
      body: { sendTo: 'duc.tm@gmail.com' },
    });

    // Tìm kiếm theo tên
    const searchRes = await api('GET', '/api/students?search=Trần Minh Đức', { token: tokens.manager });
    assert.equal(searchRes.status, 200);
    assert.equal(searchRes.body.students.length >= 1, true);
    assert.equal(searchRes.body.students[0].code, 'HV260143');

    // Lọc theo trạng thái 'Mới nhập học'
    const statusRes = await api('GET', '/api/students?status=Mới nhập học', { token: tokens.manager });
    assert.equal(statusRes.status, 200);
    assert.ok(statusRes.body.students.some((s) => s.code === 'HV260143'));
  });

  test('14. Tra cứu chi tiết một học viên (GET /api/students/:code)', async () => {
    await api('POST', '/api/leads/5/convert', {
      token: tokens.manager,
      body: { sendTo: 'duc.tm@gmail.com' },
    });

    const res = await api('GET', '/api/students/HV260143', { token: tokens.admissions1 });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.code, 'HV260143');
    assert.equal(res.body.data.name, 'Trần Minh Đức');
    assert.equal(res.body.data.login, 'S25200212143@tms.vn');
  });

  test('15. Tương thích tiền tố /admissions/leads/:id/convert (theo chuẩn frontend React)', async () => {
    // Chuyển lead 2 sau khi đổi sang Chốt
    const lead2 = leads.find((l) => String(l.id) === '2');
    lead2.stage = 'Chốt';
    lead2.status = 'WON';

    const res = await api('POST', '/admissions/leads/2/convert', {
      token: tokens.admissions1,
      body: { sendTo: 'bao.ngo@hotmail.com' },
    });

    assert.equal(res.status, 201);
    assert.equal(res.body.success, true);
    assert.ok(res.body.data.code.startsWith('HV26'));
  });
});
