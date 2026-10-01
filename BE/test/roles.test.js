const assert = require('node:assert/strict');
const { after, before, describe, test } = require('node:test');
const express = require('express');
const app = require('../src/app');
const { ROLES } = require('../src/constants/roles');
const { authenticate, authorize } = require('../src/middleware/auth');

let server;
let baseUrl;
const tokens = {};

// Route thăm dò chỉ dùng trong test để kiểm tra bộ lọc với vai trò không phải Admin.
const testApp = express();
const ok = (req, res) => res.status(200).json({ success: true });
testApp.get('/probe/training-manager', authenticate, authorize(ROLES.TRAINING_MANAGER), ok);
testApp.get('/probe/staff', authenticate, authorize(ROLES.ACCOUNTANT, ROLES.TEACHING_ASSISTANT), ok);
testApp.use(app);

async function api(method, path, { token, body } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  return { status: response.status, body: await response.json() };
}

async function login(email, password) {
  const { status, body } = await api('POST', '/api/auth/login', { body: { email, password } });
  assert.equal(status, 200);
  return body;
}

const assignRole = (userId, role, token = tokens.admin) =>
  api('POST', `/admin/users/${userId}/roles`, { token, body: { role } });

const revokeRole = (userId, roleId, token = tokens.admin) =>
  api('DELETE', `/admin/users/${userId}/roles/${roleId}`, { token });

before(async () => {
  server = testApp.listen(0);
  await new Promise((resolve) => server.once('listening', resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;

  tokens.admin = (await login('admin@example.com', 'admin123')).token;
  tokens.instructor = (await login('instructor@example.com', 'instructor123')).token;
  tokens.student = (await login('student@example.com', 'student123')).token;
});

after(() => new Promise((resolve) => server.close(resolve)));

describe('Đăng nhập và bộ lọc ủy quyền', () => {
  test('đăng nhập trả về danh sách vai trò', async () => {
    const body = await login('instructor@example.com', 'instructor123');
    assert.deepEqual(body.user.roles, ['Instructor']);
  });

  test('GET /api/auth/me yêu cầu token hợp lệ', async () => {
    assert.equal((await api('GET', '/api/auth/me')).status, 401);
    assert.equal((await api('GET', '/api/auth/me', { token: 'not-a-jwt' })).status, 401);

    const { status, body } = await api('GET', '/api/auth/me', { token: tokens.student });
    assert.equal(status, 200);
    assert.equal(body.user.email, 'student@example.com');
    assert.deepEqual(body.user.roles, ['Student']);
  });

  test('người không phải Admin bị chặn khỏi API quản trị', async () => {
    const { status, body } = await api('GET', '/admin/roles', { token: tokens.student });
    assert.equal(status, 403);
    assert.equal(body.code, 'FORBIDDEN');

    assert.equal((await assignRole('3', 'Admin', tokens.student)).status, 403);
    assert.equal((await revokeRole('2', 'Instructor', tokens.student)).status, 403);
    assert.equal((await assignRole('3', 'Admin', null)).status, 401);
  });

  test('Admin xem được danh mục vai trò', async () => {
    const { status, body } = await api('GET', '/admin/roles', { token: tokens.admin });
    assert.equal(status, 200);
    assert.deepEqual(
      body.roles.map(({ role }) => role),
      ['Admin', 'TrainingManager', 'Instructor', 'TeachingAssistant', 'Admissions', 'Accountant', 'Student'],
    );
  });

  test('authorize() báo lỗi khi khai báo vai trò sai', () => {
    assert.throws(() => authorize());
    assert.throws(() => authorize('Teacher'));
    assert.throws(() => authorize(undefined));
  });
});

describe('POST /admin/users/:id/roles: gán nhiều vai trò cho một user', () => {
  test('gán thêm nhiều vai trò, các vai trò cũ được giữ nguyên', async () => {
    const first = await assignRole('2', 'TeachingAssistant');
    assert.equal(first.status, 201);
    assert.deepEqual(first.body.user.roles, ['Instructor', 'TeachingAssistant']);

    const second = await assignRole('2', 'TrainingManager');
    assert.equal(second.status, 201);
    assert.deepEqual(second.body.user.roles, ['Instructor', 'TeachingAssistant', 'TrainingManager']);

    const listed = await api('GET', '/admin/users/2/roles', { token: tokens.admin });
    assert.deepEqual(listed.body.roles, ['Instructor', 'TeachingAssistant', 'TrainingManager']);

    const me = await api('GET', '/api/auth/me', { token: tokens.instructor });
    assert.deepEqual(me.body.user.roles, ['Instructor', 'TeachingAssistant', 'TrainingManager']);
  });

  test('user nhiều vai trò được qua bộ lọc nhờ bất kỳ vai trò nào', async () => {
    assert.equal((await api('GET', '/probe/training-manager', { token: tokens.instructor })).status, 200);
    assert.equal((await api('GET', '/probe/staff', { token: tokens.instructor })).status, 200);
    assert.equal((await api('GET', '/probe/staff', { token: tokens.student })).status, 403);
  });

  test('gán trùng vai trò đã có', async () => {
    const { status, body } = await assignRole('2', 'TeachingAssistant');
    assert.equal(status, 409);
    assert.equal(body.code, 'ROLE_ALREADY_ASSIGNED');
  });

  test('gán vai trò không hợp lệ hoặc cho user không tồn tại', async () => {
    assert.equal((await assignRole('2', 'Teacher')).status, 400);
    assert.equal((await assignRole('2', ['Admin'])).status, 400);
    assert.equal((await api('POST', '/admin/users/2/roles', { token: tokens.admin })).status, 400);
    assert.equal((await assignRole('999', 'Student')).status, 404);
  });
});

describe('Thay đổi vai trò có hiệu lực ngay ở thao tác kế tiếp', () => {
  test('gán rồi thu hồi Admin với cùng một token đã cấp', async () => {
    // Token của học viên được cấp khi chỉ có vai trò Student.
    assert.equal((await api('GET', '/admin/roles', { token: tokens.student })).status, 403);

    assert.equal((await assignRole('3', 'Admin')).status, 201);
    assert.equal((await api('GET', '/admin/roles', { token: tokens.student })).status, 200);

    assert.equal((await revokeRole('3', 'Admin')).status, 200);
    assert.equal((await api('GET', '/admin/roles', { token: tokens.student })).status, 403);
  });

  test('thu hồi vai trò phụ chặn ngay route của vai trò đó', async () => {
    assert.equal((await api('GET', '/probe/training-manager', { token: tokens.instructor })).status, 200);
    assert.equal((await revokeRole('2', 'TrainingManager')).status, 200);
    assert.equal((await api('GET', '/probe/training-manager', { token: tokens.instructor })).status, 403);
  });

  test('tài khoản bị khoá bị chặn ngay cả khi token còn hạn', async () => {
    await api('PUT', '/admin/users/2', { token: tokens.admin, body: { status: 'locked' } });
    const locked = await api('GET', '/api/auth/me', { token: tokens.instructor });
    assert.equal(locked.status, 403);
    assert.equal(locked.body.code, 'ACCOUNT_INACTIVE');

    await api('PUT', '/admin/users/2', { token: tokens.admin, body: { status: 'active' } });
    assert.equal((await api('GET', '/api/auth/me', { token: tokens.instructor })).status, 200);
  });
});

describe('DELETE /admin/users/:id/roles/:roleId: thu hồi vai trò', () => {
  test('thu hồi một vai trò, các vai trò còn lại được giữ nguyên', async () => {
    const { status, body } = await revokeRole('2', 'TeachingAssistant');
    assert.equal(status, 200);
    assert.deepEqual(body.user.roles, ['Instructor']);

    const listed = await api('GET', '/admin/users/2/roles', { token: tokens.admin });
    assert.deepEqual(listed.body.roles, ['Instructor']);
  });

  test('thu hồi vai trò user không có', async () => {
    const { status, body } = await revokeRole('3', 'Accountant');
    assert.equal(status, 404);
    assert.equal(body.code, 'ROLE_NOT_ASSIGNED');
  });

  test('thu hồi với roleId không hợp lệ hoặc user không tồn tại', async () => {
    assert.equal((await revokeRole('3', 'Teacher')).status, 400);
    assert.equal((await revokeRole('999', 'Student')).status, 404);
  });
});

describe('Chặn tự thu hồi vai trò quản trị của chính mình', () => {
  test('Admin tự thu hồi Admin bị từ chối và vẫn giữ quyền', async () => {
    const { status, body } = await revokeRole('1', 'Admin');
    assert.equal(status, 409);
    assert.equal(body.code, 'CANNOT_REVOKE_OWN_ADMIN');

    const listed = await api('GET', '/admin/users/1/roles', { token: tokens.admin });
    assert.deepEqual(listed.body.roles, ['Admin']);
    assert.equal((await api('GET', '/admin/roles', { token: tokens.admin })).status, 200);
  });

  test('Admin vẫn thu hồi được Admin của người khác', async () => {
    await assignRole('3', 'Admin');
    assert.equal((await revokeRole('3', 'Admin')).status, 200);
  });

  test('Admin không thể tự khoá tài khoản của mình', async () => {
    const { status, body } = await api('PUT', '/admin/users/1', { token: tokens.admin, body: { status: 'locked' } });
    assert.equal(status, 409);
    assert.equal(body.code, 'CANNOT_DEACTIVATE_SELF');
  });
});

describe('PUT /admin/users/:id', () => {
  test('không còn nhận trường role', async () => {
    const { status } = await api('PUT', '/admin/users/3', { token: tokens.admin, body: { role: 'Admin' } });
    assert.equal(status, 400);

    const me = await api('GET', '/api/auth/me', { token: tokens.student });
    assert.deepEqual(me.body.user.roles, ['Student']);
  });
});
