const assert = require('node:assert/strict');
const { after, before, beforeEach, describe, test } = require('node:test');
const express = require('express');
const app = require('../src/app');
const { ROLES } = require('../src/constants/roles');
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
  tokens.manager = (await login('manager@example.com', 'manager123')).token;
  tokens.admissions1 = (await login('admissions@example.com', 'admissions123')).token; // id: 5
  tokens.admissions2 = (await login('tuvan@example.com', 'tuvan123')).token; // id: 6
  tokens.instructor = (await login('instructor@example.com', 'instructor123')).token;
  tokens.student = (await login('student@example.com', 'student123')).token;
});

after(() => new Promise((resolve) => server.close(resolve)));

beforeEach(() => {
  seedSampleLeads();
});

describe('S2-10 (IDTTX-165 / IDTTX-195): Phân công lead cho tư vấn viên', () => {
  test('Quản lý đào tạo (TrainingManager) có thể phân công một hoặc nhiều lead cùng lúc', async () => {
    // Lead 3 và Lead 4 ban đầu là unassigned
    const assignRes = await api('POST', '/api/leads/assign', {
      token: tokens.manager,
      body: {
        leadIds: ['3', '4'],
        counselorId: '5',
        note: 'Giao chăm sóc khách hàng mới từ chiến dịch tuần này',
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

    // Kiểm tra lịch sử chuyển giao được ghi nhận đầy đủ (S2-10: Có ghi lịch sử chuyển giao)
    const historyRes = await api('GET', '/api/leads/3/assignments', { token: tokens.manager });
    assert.equal(historyRes.status, 200);
    assert.equal(historyRes.body.data.length >= 1, true);
    const lastAssignment = historyRes.body.data[0];
    assert.equal(lastAssignment.newCounselorId, '5');
    assert.equal(lastAssignment.assignedBy, '4'); // ID của TrainingManager
    assert.equal(lastAssignment.note, 'Giao chăm sóc khách hàng mới từ chiến dịch tuần này');
  });

  test('Tư vấn viên chỉ nhìn thấy lead được giao cho mình (S2-10 & IDTTX-195)', async () => {
    // admissions1 (id: 5) gọi GET /api/leads -> hệ thống tự động lọc chỉ lấy lead được gán cho id 5
    const res = await api('GET', '/api/leads', { token: tokens.admissions1 });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);

    // Mọi lead trả về phải thuộc về counselorId = 5
    for (const lead of res.body.data) {
      assert.equal(String(lead.assignedCounselorId), '5');
    }

    // Tư vấn viên thử truy cập trực tiếp ID lead của tư vấn viên khác -> Bị từ chối 403
    // Lead 5 được gán cho counselorId = 6
    const forbiddenRes = await api('GET', '/api/leads/5', { token: tokens.admissions1 });
    assert.equal(forbiddenRes.status, 403);
    assert.equal(forbiddenRes.body.code, 'FORBIDDEN');
  });

  test('Chỉ Quản lý đào tạo hoặc Admin mới được quyền phân công lead', async () => {
    // Giảng viên không có quyền phân công
    const resInst = await api('POST', '/api/leads/assign', {
      token: tokens.instructor,
      body: { leadIds: ['3'], counselorId: '5' },
    });
    assert.equal(resInst.status, 403);

    // Tư vấn viên không thể tự phân công cho mình hoặc người khác
    const resAdv = await api('POST', '/api/leads/assign', {
      token: tokens.admissions1,
      body: { leadIds: ['3'], counselorId: '5' },
    });
    assert.equal(resAdv.status, 403);

    // Admin có quyền phân công
    const resAdmin = await api('POST', '/api/leads/assign', {
      token: tokens.admin,
      body: { leadIds: ['3'], counselorId: '6', note: 'Admin phân bổ' },
    });
    assert.equal(resAdmin.status, 200);
    assert.equal(resAdmin.body.success, true);
  });

  test('Báo lỗi hợp lệ khi thiếu thông tin phân công', async () => {
    // Thiếu leadIds
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

describe('S2-11 (IDTTX-166 / IDTTX-198): Tìm kiếm và lọc lead theo nhiều điều kiện', () => {
  test('Tìm nhanh theo tên khách hàng', async () => {
    const res = await api('GET', '/api/leads?search=Minh Anh', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.length >= 1, true);
    assert.match(res.body.data[0].fullName, /Minh Anh/i);
  });

  test('Tìm nhanh theo số điện thoại (hỗ trợ partial match)', async () => {
    const res = await api('GET', '/api/leads?search=0988', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.length >= 1, true);
    assert.match(res.body.data[0].phone, /0988/);
  });

  test('Lọc theo trạng thái (status)', async () => {
    // Hỗ trợ cả enum chuẩn lẫn nhãn frontend
    const res = await api('GET', '/api/leads?status=CONSULTING', { token: tokens.manager });
    assert.equal(res.status, 200);
    for (const lead of res.body.data) {
      assert.equal(lead.status, 'CONSULTING');
    }

    const resAlias = await api('GET', '/api/leads?status=counseling', { token: tokens.manager });
    assert.equal(resAlias.status, 200);
    for (const lead of resAlias.body.data) {
      assert.equal(lead.status, 'CONSULTING');
    }
  });

  test('Lọc theo nguồn (source)', async () => {
    const res = await api('GET', '/api/leads?source=FACEBOOK', { token: tokens.manager });
    assert.equal(res.status, 200);
    for (const lead of res.body.data) {
      assert.equal(lead.source, 'FACEBOOK');
    }
  });

  test('Lọc theo người phụ trách (assignedCounselorId)', async () => {
    const res = await api('GET', '/api/leads?counselorId=5', { token: tokens.manager });
    assert.equal(res.status, 200);
    for (const lead of res.body.data) {
      assert.equal(String(lead.assignedCounselorId), '5');
    }

    // Lọc các lead chưa phân công
    const resUnassigned = await api('GET', '/api/leads?counselorId=unassigned', { token: tokens.manager });
    assert.equal(resUnassigned.status, 200);
    for (const lead of resUnassigned.body.data) {
      assert.equal(lead.assignedCounselorId, null);
    }
  });

  test('Lọc theo khoảng thời gian "tháng trước" (datePreset=last-month) để tìm lại cuộc trao đổi', async () => {
    const res = await api('GET', '/api/leads?datePreset=last-month', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    // Lead 1 và Lead 5 được tạo vào tháng trước trong seed data
    assert.equal(res.body.data.length >= 2, true);
  });

  test('Ghi nhật ký tương tác / cuộc gọi vào hồ sơ lead', async () => {
    const interactionRes = await api('POST', '/api/leads/1/interactions', {
      token: tokens.admissions1,
      body: {
        type: 'CALL',
        title: 'Cuộc gọi tư vấn học phí',
        content: 'Khách hàng gọi lại hỏi về chính sách ưu đãi học phí đóng 1 lần.',
        durationSeconds: 240,
      },
    });

    assert.equal(interactionRes.status, 201);
    assert.equal(interactionRes.body.success, true);
    assert.equal(interactionRes.body.data.title, 'Cuộc gọi tư vấn học phí');

    // Kiểm tra timeline đã được cập nhật
    const detailRes = await api('GET', '/api/leads/1', { token: tokens.admissions1 });
    assert.equal(detailRes.status, 200);
    const topTimeline = detailRes.body.data.timeline[0];
    assert.equal(topTimeline.title, 'Cuộc gọi tư vấn học phí');
  });

  test('Lấy thống kê phễu tuyển sinh (/api/leads/stats)', async () => {
    const res = await api('GET', '/api/leads/stats', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(typeof res.body.data.total, 'number');
    assert.equal(typeof res.body.data.unassigned, 'number');
  });
});
