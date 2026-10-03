const assert = require('node:assert/strict');
const { after, before, describe, test } = require('node:test');
const app = require('../src/app');

describe('Chức năng S1-10: Khóa và mở khóa tài khoản', () => {
  let server;
  let baseUrl;
  let adminToken;
  let instructorToken;

  async function api(method, path, { token, body } = {}) {
    const headers = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    if (body !== undefined) headers['Content-Type'] = 'application/json';

    const res = await fetch(`${baseUrl}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    const json = await res.json().catch(() => null);
    return { status: res.status, body: json };
  }

  before(async () => {
    server = app.listen(0);
    await new Promise((resolve) => server.once('listening', resolve));
    baseUrl = `http://127.0.0.1:${server.address().port}`;

    const adminLogin = await api('POST', '/api/auth/login', {
      body: { email: 'admin@example.com', password: 'admin123' },
    });
    assert.equal(adminLogin.status, 200);
    adminToken = adminLogin.body.token;

    const instLogin = await api('POST', '/api/auth/login', {
      body: { email: 'instructor@example.com', password: 'instructor123' },
    });
    assert.equal(instLogin.status, 200);
    instructorToken = instLogin.body.token;
  });

  after(() => new Promise((resolve) => server.close(resolve)));

  test('1. Bắt buộc ghi lý do khi khóa tài khoản (HTTP 400 REASON_REQUIRED)', async () => {
    const res = await api('PUT', '/admin/users/2/status', {
      token: adminToken,
      body: { status: 'LOCKED' }, // Thiếu lockedReason
    });
    assert.equal(res.status, 400);
    assert.equal(res.body.code, 'REASON_REQUIRED');
  });

  test('2. Chặn Admin tự khóa tài khoản của chính mình (HTTP 409 CANNOT_DEACTIVATE_SELF)', async () => {
    const res = await api('PUT', '/admin/users/1/status', {
      token: adminToken,
      body: { status: 'LOCKED', lockedReason: 'Thử tự khóa mình' },
    });
    assert.equal(res.status, 409);
    assert.equal(res.body.code, 'CANNOT_DEACTIVATE_SELF');
  });

  test('3. Khóa tài khoản thành công, ghi lý do và trả về cảnh báo lớp học cần bàn giao', async () => {
    const res = await api('PUT', '/admin/users/2/status', {
      token: adminToken,
      body: { status: 'LOCKED', lockedReason: 'Nghỉ việc chưa bàn giao giao án' },
    });
    assert.equal(res.status, 200);
    assert.equal(res.body.user.status, 'locked');
    assert.equal(res.body.user.lockedReason, 'Nghỉ việc chưa bàn giao giao án');
    assert.equal(res.body.requiresHandover, true);
    assert.ok(Array.isArray(res.body.assignedClasses));
    assert.ok(res.body.assignedClasses.length > 0);
  });

  test('4. Sau khi khóa, token của tài khoản bị khóa bị từ chối truy cập ngay lập tức', async () => {
    const checkMe = await api('GET', '/api/auth/me', { token: instructorToken });
    assert.equal(checkMe.status, 403);
    assert.equal(checkMe.body.code, 'ACCOUNT_INACTIVE');
  });

  test('5. Mở khóa tài khoản thành công (trở về active, xóa lý do khóa)', async () => {
    const res = await api('PUT', '/admin/users/2/status', {
      token: adminToken,
      body: { status: 'ACTIVE' },
    });
    assert.equal(res.status, 200);
    assert.equal(res.body.user.status, 'active');
    assert.equal(res.body.user.lockedReason, null);

    // Sau khi mở khóa, tài khoản truy cập lại bình thường
    const checkMe = await api('GET', '/api/auth/me', { token: instructorToken });
    assert.equal(checkMe.status, 200);
    assert.equal(checkMe.body.user.email, 'instructor@example.com');
  });
});
