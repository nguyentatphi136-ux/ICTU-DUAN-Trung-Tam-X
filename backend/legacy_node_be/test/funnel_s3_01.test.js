const assert = require('node:assert/strict');
const { after, before, beforeEach, describe, test } = require('node:test');
const app = require('../src/app');
const { seedSampleLeads } = require('../src/data/leads');

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
  seedSampleLeads();
});

describe('S3-01 (IDTTX-209 / Subtask IDTTX-222): Phễu lead dạng Kanban và lý do từ chối', () => {
  test('1. Bảng phễu Kanban gồm đủ 6 cột và hiển thị số lượng lead mỗi cột', async () => {
    const res = await api('GET', '/api/funnel', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);

    // Kiểm tra cấu trúc 6 cột
    const expectedStages = ['Mới', 'Đã liên hệ', 'Đang tư vấn', 'Hẹn học thử', 'Chốt', 'Từ chối'];
    assert.ok(res.body.columns);
    for (const stage of expectedStages) {
      assert.ok(Array.isArray(res.body.columns[stage]), `Thiếu cột ${stage}`);
      assert.equal(typeof res.body.counts[stage], 'number', `Thiếu count cho cột ${stage}`);
    }

    // Tổng số lượng
    assert.equal(res.body.total >= 8, true);
    assert.equal(typeof res.body.lateCount, 'number');
  });

  test('2. Danh mục lý do từ chối chuẩn phục vụ hộp thoại chọn lý do', async () => {
    const res = await api('GET', '/api/funnel/reasons', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.ok(Array.isArray(res.body.data));
    assert.ok(res.body.data.includes('Học phí cao'));
    assert.ok(res.body.data.includes('Lịch học không phù hợp'));
    assert.ok(res.body.data.includes('Đã chọn trung tâm khác'));
    assert.ok(res.body.data.includes('Không liên lạc được'));
    assert.ok(res.body.data.includes('Chưa có nhu cầu lúc này'));
    assert.ok(res.body.data.includes('Lý do khác'));
  });

  test('3. Tìm kiếm lead trên bảng phễu theo tên hoặc số điện thoại', async () => {
    // Tìm theo tên
    const resName = await api('GET', '/api/funnel?search=Minh Anh', { token: tokens.manager });
    assert.equal(resName.status, 200);
    assert.equal(resName.body.leads.length >= 1, true);
    assert.match(resName.body.leads[0].name, /Minh Anh/i);

    // Tìm theo số điện thoại
    const resPhone = await api('GET', '/api/funnel?search=0988', { token: tokens.manager });
    assert.equal(resPhone.status, 200);
    assert.equal(resPhone.body.leads.length >= 1, true);
    assert.match(resPhone.body.leads[0].phone, /0988/);
  });

  test('4. Lọc chỉ hiện các lead quá hạn gọi lại (onlyLate=true)', async () => {
    const res = await api('GET', '/api/funnel?onlyLate=true', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.leads.length >= 1, true);
    for (const lead of res.body.leads) {
      assert.equal(lead.overdueDays > 0, true);
      assert.notEqual(lead.stage, 'Chốt');
      assert.notEqual(lead.stage, 'Từ chối');
    }
  });

  test('5. Kéo thả đổi trạng thái lead (Mới -> Đã liên hệ)', async () => {
    // Lead 3 đang ở trạng thái 'Mới'
    const moveRes = await api('PATCH', '/api/leads/3/stage', {
      token: tokens.manager,
      body: { stage: 'Đã liên hệ' },
    });

    assert.equal(moveRes.status, 200);
    assert.equal(moveRes.body.success, true);
    assert.equal(moveRes.body.data.stage, 'Đã liên hệ');
    assert.equal(moveRes.body.data.status, 'CONTACTED');
  });

  test('6. Tự động ghi nhật ký mỗi lần chuyển trạng thái', async () => {
    // Lead 1 chuyển từ 'Đang tư vấn' sang 'Hẹn học thử'
    const moveRes = await api('PATCH', '/api/leads/1/stage', {
      token: tokens.admissions1,
      body: { stage: 'Hẹn học thử' },
    });
    assert.equal(moveRes.status, 200);

    // Kiểm tra nhật ký tương tác / timeline có dòng trạng thái mới
    const detailRes = await api('GET', '/api/leads/1', { token: tokens.admissions1 });
    assert.equal(detailRes.status, 200);
    const latestTimeline = detailRes.body.data.timeline[0];
    assert.equal(latestTimeline.type, 'STATUS_CHANGE');
    assert.match(latestTimeline.content, /Đang tư vấn → Hẹn học thử/);
    assert.match(latestTimeline.author, /Lê Thị Thu Hà/);


    // Kiểm tra API nhật ký chăm sóc (care logs)
    const logsRes = await api('GET', '/api/leads/1/logs', { token: tokens.admissions1 });
    assert.equal(logsRes.status, 200);
    assert.ok(logsRes.body.data.some((log) => log.kind === 'Đổi trạng thái' && log.text.includes('Hẹn học thử')));
  });

  test('7. Chuyển sang "Từ chối" KHÔNG chọn lý do -> Bị từ chối với mã lỗi 400', async () => {
    const res = await api('PATCH', '/api/leads/1/stage', {
      token: tokens.admissions1,
      body: { stage: 'Từ chối' }, // Không gửi reason
    });

    assert.equal(res.status, 400);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'REJECT_REASON_REQUIRED');
    assert.match(res.body.message, /Bắt buộc chọn lý do/i);
  });

  test('8. Chuyển sang "Từ chối" chọn "Lý do khác" nhưng thiếu ghi chú -> Bị từ chối với mã lỗi 400', async () => {
    const res = await api('PATCH', '/api/leads/1/stage', {
      token: tokens.admissions1,
      body: {
        stage: 'Từ chối',
        reason: 'Lý do khác',
        note: '', // Rỗng
      },
    });

    assert.equal(res.status, 400);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'REJECT_NOTE_REQUIRED');
    assert.match(res.body.message, /ghi rõ lý do/i);
  });

  test('9. Chuyển sang "Từ chối" CÓ lý do hợp lệ -> Thành công và ghi nhận lý do', async () => {
    const res = await api('PATCH', '/api/leads/1/stage', {
      token: tokens.admissions1,
      body: {
        stage: 'Từ chối',
        reason: 'Học phí cao',
      },
    });

    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.stage, 'Từ chối');
    assert.equal(res.body.data.status, 'REJECTED');
    assert.equal(res.body.data.reason, 'Học phí cao');

    // Kiểm tra nhật ký có đính kèm lý do từ chối
    const logsRes = await api('GET', '/api/leads/1/logs', { token: tokens.admissions1 });
    assert.equal(logsRes.status, 200);
    const latestLog = logsRes.body.data[0];
    assert.equal(latestLog.kind, 'Đổi trạng thái');
    assert.match(latestLog.text, /Từ chối · Học phí cao/);
  });

  test('10. Rời khỏi cột "Từ chối" quay lại "Đang tư vấn" -> Tự động xóa bỏ lý do từ chối cũ', async () => {
    // Lead 7 ban đầu ở cột 'Từ chối' với reason: 'Học phí cao'
    const moveRes = await api('PATCH', '/api/leads/7/stage', {
      token: tokens.admissions1,
      body: { stage: 'Đang tư vấn' },
    });

    assert.equal(moveRes.status, 200);
    assert.equal(moveRes.body.data.stage, 'Đang tư vấn');
    assert.equal(moveRes.body.data.reason, null);
  });

  test('11. Ghi nhận nhật ký chăm sóc thủ công (Gọi, Nhắn, Gặp)', async () => {
    const addLogRes = await api('POST', '/api/leads/1/logs', {
      token: tokens.admissions1,
      body: {
        kind: 'Gọi',
        text: 'Khách quan tâm lớp tối, xin gửi lịch khai giảng tháng 10.',
      },
    });

    assert.equal(addLogRes.status, 201);
    assert.equal(addLogRes.body.success, true);
    assert.equal(addLogRes.body.data.kind, 'Gọi');
    assert.match(addLogRes.body.data.by, /Lê Thị Thu Hà/);

    const logsRes = await api('GET', '/api/leads/1/logs', { token: tokens.admissions1 });
    assert.equal(logsRes.status, 200);
    assert.equal(logsRes.body.data[0].text, 'Khách quan tâm lớp tối, xin gửi lịch khai giảng tháng 10.');
  });

  test('12. Phân quyền: Tư vấn viên chỉ xem và đổi lead được giao cho mình', async () => {
    // Lead 5 được giao cho counselor 6 (Hoàng Văn Tư). Counselor 5 (Lê Thị Thu Hà) cố đổi sẽ nhận 403
    const moveRes = await api('PATCH', '/api/leads/5/stage', {
      token: tokens.admissions1,
      body: { stage: 'Đang tư vấn' },
    });
    assert.equal(moveRes.status, 403);
    assert.equal(moveRes.body.code, 'FORBIDDEN');

    // Admin và Quản lý đào tạo (TrainingManager) đổi được
    const managerMoveRes = await api('PATCH', '/api/leads/5/stage', {
      token: tokens.manager,
      body: { stage: 'Đang tư vấn' },
    });
    assert.equal(managerMoveRes.status, 200);
  });

  test('13. Phân quyền: Giảng viên và Học viên bị chặn khỏi Bảng phễu', async () => {
    const instructorRes = await api('GET', '/api/funnel', { token: tokens.instructor });
    assert.equal(instructorRes.status, 403);

    const studentRes = await api('GET', '/api/funnel', { token: tokens.student });
    assert.equal(studentRes.status, 403);
  });

  test('14. Tương thích tiền tố /admissions/funnel và /admissions/leads/:id/stage', async () => {
    // GET /admissions/funnel
    const funnelRes = await api('GET', '/admissions/funnel', { token: tokens.manager });
    assert.equal(funnelRes.status, 200);
    assert.ok(funnelRes.body.columns);

    // PATCH /admissions/leads/2/stage
    const patchRes = await api('PATCH', '/admissions/leads/2/stage', {
      token: tokens.admissions1,
      body: { stage: 'Chốt' },
    });
    assert.equal(patchRes.status, 200);
    assert.equal(patchRes.body.data.stage, 'Chốt');
  });
});
