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

describe('S2-10 (IDTTX-165 / Subtask IDTTX-195): Phân công lead cho tư vấn viên', () => {
  test('Quản lý đào tạo (TrainingManager) có thể phân công một hoặc nhiều lead cùng lúc', async () => {
    // Phân công đồng thời lead 3 và lead 4 cho tư vấn viên 5 (Lê Thị Thu Hà)
    const assignRes = await api('POST', '/api/leads/assign', {
      token: tokens.manager,
      body: {
        leadIds: ['3', '4'],
        counselorId: '5',
        note: 'Giao chăm sóc khách hàng mới từ chiến dịch tuyển sinh',
      },
    });

    assert.equal(assignRes.status, 200);
    assert.equal(assignRes.body.success, true);
    assert.equal(assignRes.body.data.updatedCount, 2);

    // Kiểm tra chi tiết lead 3 sau khi được phân công
    const lead3Res = await api('GET', '/api/leads/3', { token: tokens.manager });
    assert.equal(lead3Res.status, 200);
    assert.equal(lead3Res.body.data.assignedCounselorId, '5');
    assert.equal(lead3Res.body.data.assignedCounselorName, 'Lê Thị Thu Hà (Tư vấn viên)');

    // Kiểm tra chi tiết lead 4 sau khi được phân công
    const lead4Res = await api('GET', '/api/leads/4', { token: tokens.manager });
    assert.equal(lead4Res.status, 200);
    assert.equal(lead4Res.body.data.assignedCounselorId, '5');
  });

  test('Ghi nhận đầy đủ lịch sử chuyển giao lead (lead_assignments)', async () => {
    // Phân công lead 3
    await api('POST', '/api/leads/assign', {
      token: tokens.manager,
      body: {
        leadIds: ['3'],
        counselorId: '5',
        note: 'Phân công đợt 1',
      },
    });

    // Chuyển giao tiếp lead 3 từ tư vấn viên 5 sang tư vấn viên 6
    await api('POST', '/api/leads/assign', {
      token: tokens.manager,
      body: {
        leadIds: ['3'],
        counselorId: '6',
        note: 'Chuyển giao do tư vấn viên 5 bận',
      },
    });

    // Xem lịch sử chuyển giao
    const historyRes = await api('GET', '/api/leads/3/assignments', { token: tokens.manager });
    assert.equal(historyRes.status, 200);
    assert.equal(historyRes.body.data.length >= 2, true);

    const latest = historyRes.body.data[0];
    assert.equal(latest.previousCounselorId, '5');
    assert.equal(latest.newCounselorId, '6');
    assert.equal(latest.assignedBy, '4'); // ID của TrainingManager
    assert.equal(latest.note, 'Chuyển giao do tư vấn viên 5 bận');
  });

  test('Tư vấn viên chỉ nhìn thấy lead được giao cho mình (S2-10)', async () => {
    // Tư vấn viên 1 (id: 5) gọi API danh sách lead -> hệ thống tự động lọc chỉ trả về lead của chính họ
    const res = await api('GET', '/api/leads', { token: tokens.admissions1 });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);

    // Mọi lead trả về phải thuộc về counselorId = 5
    for (const lead of res.body.data) {
      assert.equal(String(lead.assignedCounselorId), '5');
    }

    // Tư vấn viên thử truy cập trực tiếp ID lead của tư vấn viên khác (lead 5 được giao cho tư vấn viên 6)
    const forbiddenRes = await api('GET', '/api/leads/5', { token: tokens.admissions1 });
    assert.equal(forbiddenRes.status, 403);
    assert.equal(forbiddenRes.body.code, 'FORBIDDEN');
  });

  test('Chỉ Quản lý đào tạo hoặc Admin mới được quyền phân công lead', async () => {
    // Giảng viên thử phân công -> 403
    const resInst = await api('POST', '/api/leads/assign', {
      token: tokens.instructor,
      body: { leadIds: ['3'], counselorId: '5' },
    });
    assert.equal(resInst.status, 403);

    // Học viên thử phân công -> 403
    const resStudent = await api('POST', '/api/leads/assign', {
      token: tokens.student,
      body: { leadIds: ['3'], counselorId: '5' },
    });
    assert.equal(resStudent.status, 403);

    // Tư vấn viên không thể tự phân công cho mình hoặc người khác -> 403
    const resAdv = await api('POST', '/api/leads/assign', {
      token: tokens.admissions1,
      body: { leadIds: ['3'], counselorId: '5' },
    });
    assert.equal(resAdv.status, 403);

    // Admin có quyền phân công hợp lệ -> 200
    const resAdmin = await api('POST', '/api/leads/assign', {
      token: tokens.admin,
      body: { leadIds: ['3'], counselorId: '6', note: 'Admin phân bổ' },
    });
    assert.equal(resAdmin.status, 200);
    assert.equal(resAdmin.body.success, true);
  });

  test('Lấy danh sách tư vấn viên phục vụ dropdown phân công (GET /api/leads/counselors)', async () => {
    const res = await api('GET', '/api/leads/counselors', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(Array.isArray(res.body.data), true);
    assert.equal(res.body.data.length >= 2, true);
  });

  test('Báo lỗi hợp lệ khi thiếu thông tin hoặc tư vấn viên không tồn tại', async () => {
    // Không có leadIds
    const resEmpty = await api('POST', '/api/leads/assign', {
      token: tokens.manager,
      body: { leadIds: [], counselorId: '5' },
    });
    assert.equal(resEmpty.status, 400);

    // counselorId không tồn tại
    const resNotFound = await api('POST', '/api/leads/assign', {
      token: tokens.manager,
      body: { leadIds: ['3'], counselorId: '9999' },
    });
    assert.equal(resNotFound.status, 404);
  });
});
