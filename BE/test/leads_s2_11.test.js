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

describe('S2-11 (IDTTX-166 / Subtask IDTTX-198): Tìm kiếm và lọc lead theo nhiều điều kiện', () => {
  test('1. Tìm nhanh theo tên khách hàng (case-insensitive)', async () => {
    const res = await api('GET', '/api/leads?search=Minh Anh', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.length >= 1, true);
    assert.match(res.body.data[0].fullName, /Minh Anh/i);
  });

  test('2. Tìm nhanh theo số điện thoại (hỗ trợ partial match đầu số)', async () => {
    const res = await api('GET', '/api/leads?search=0988', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.length >= 1, true);
    assert.match(res.body.data[0].phone, /0988/);
  });

  test('3. Lọc theo trạng thái (status: chuẩn enum và alias tiếng Việt)', async () => {
    // Chuẩn enum
    const resEnum = await api('GET', '/api/leads?status=CONSULTING', { token: tokens.manager });
    assert.equal(resEnum.status, 200);
    for (const lead of resEnum.body.data) {
      assert.equal(lead.status, 'CONSULTING');
    }

    // Alias frontend / tiếng Việt
    const resAlias = await api('GET', '/api/leads?status=counseling', { token: tokens.manager });
    assert.equal(resAlias.status, 200);
    for (const lead of resAlias.body.data) {
      assert.equal(lead.status, 'CONSULTING');
    }
  });

  test('4. Lọc theo nguồn tiếp cận (source)', async () => {
    const res = await api('GET', '/api/leads?source=FACEBOOK', { token: tokens.manager });
    assert.equal(res.status, 200);
    for (const lead of res.body.data) {
      assert.equal(lead.source, 'FACEBOOK');
    }
  });

  test('5. Lọc theo người phụ trách (counselorId) và lead chưa phân công', async () => {
    // Lọc lead của tư vấn viên 5
    const res = await api('GET', '/api/leads?counselorId=5', { token: tokens.manager });
    assert.equal(res.status, 200);
    for (const lead of res.body.data) {
      assert.equal(String(lead.assignedCounselorId), '5');
    }

    // Lọc lead chưa ai phụ trách (unassigned)
    const resUnassigned = await api('GET', '/api/leads?counselorId=unassigned', { token: tokens.manager });
    assert.equal(resUnassigned.status, 200);
    for (const lead of resUnassigned.body.data) {
      assert.equal(lead.assignedCounselorId, null);
    }
  });

  test('6. Lọc theo khoảng thời gian "tháng trước" (datePreset=last-month) để tìm lại cuộc trao đổi', async () => {
    const res = await api('GET', '/api/leads?datePreset=last-month', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    // Dữ liệu mẫu khởi tạo có lead tạo từ tháng trước
    assert.equal(res.body.data.length >= 2, true);
  });

  test('7. Lọc theo khoảng ngày cụ thể (createdFrom, createdTo)', async () => {
    const now = new Date();
    const todayStr = now.toISOString().split('T')[0];
    const res = await api('GET', `/api/leads?createdFrom=${todayStr}&createdTo=${todayStr}`, {
      token: tokens.manager,
    });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    for (const lead of res.body.data) {
      assert.equal(lead.createdAt.startsWith(todayStr), true);
    }
  });

  test('8. Ghi nhật ký tương tác / cuộc gọi vào hồ sơ lead', async () => {
    const interactionRes = await api('POST', '/api/leads/1/interactions', {
      token: tokens.admissions1,
      body: {
        type: 'CALL',
        title: 'Cuộc gọi tư vấn học phí tháng trước',
        content: 'Khách hàng hỏi về chính sách đóng học phí chia đợt.',
        durationSeconds: 240,
      },
    });

    assert.equal(interactionRes.status, 201);
    assert.equal(interactionRes.body.success, true);
    assert.equal(interactionRes.body.data.title, 'Cuộc gọi tư vấn học phí tháng trước');

    // Kiểm tra dòng thời gian (timeline) của lead có cuộc trao đổi
    const detailRes = await api('GET', '/api/leads/1', { token: tokens.admissions1 });
    assert.equal(detailRes.status, 200);
    const topTimeline = detailRes.body.data.timeline[0];
    assert.equal(topTimeline.title, 'Cuộc gọi tư vấn học phí tháng trước');
    assert.equal(topTimeline.content, 'Khách hàng hỏi về chính sách đóng học phí chia đợt.');
  });

  test('9. Lấy thống kê phễu tuyển sinh (GET /api/leads/stats)', async () => {
    const res = await api('GET', '/api/leads/stats', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(typeof res.body.data.total, 'number');
    assert.equal(typeof res.body.data.new, 'number');
    assert.equal(typeof res.body.data.consulting, 'number');
  });

  test('10. Hỗ trợ phân trang và sắp xếp dữ liệu (page, limit, sortBy, sortOrder)', async () => {
    const res = await api('GET', '/api/leads?page=1&limit=2&sortBy=fullName&sortOrder=asc', {
      token: tokens.manager,
    });
    assert.equal(res.status, 200);
    assert.equal(res.body.data.length, 2);
    assert.equal(res.body.pagination.page, 1);
    assert.equal(res.body.pagination.limit, 2);
    assert.equal(res.body.pagination.total >= 5, true);
  });
});
