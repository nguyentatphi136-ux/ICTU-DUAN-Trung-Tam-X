const assert = require('node:assert/strict');
const { after, before, describe, test } = require('node:test');
const express = require('express');
const app = require('../src/app');
const { ROLES } = require('../src/constants/roles');
const { authenticate, authorize } = require('../src/middleware/auth');

/**
 * Tác vụ Jira: IDTTX-24 [BE] Gán và thu hồi vai trò của một người dùng
 * Đặc tả: Là Quản trị hệ thống, tôi muốn gán và thu hồi vai trò của một người dùng, 
 *         để xử lý được trường hợp một người vừa là giảng viên vừa là quản lý đào tạo.
 * Tiêu chí:
 *   1. Một người dùng có thể giữ nhiều vai trò cùng lúc.
 *   2. Thay đổi vai trò có hiệu lực ngay ở thao tác kế tiếp, không cần đăng nhập lại.
 *   3. Không thể tự thu hồi vai trò quản trị của chính mình.
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */

let server;
let baseUrl;
const tokens = {};

const testApp = express();
const ok = (req, res) => res.status(200).json({ success: true, message: 'Được phép truy cập' });

// Route yêu cầu quyền TrainingManager
testApp.get('/test/training-manager-only', authenticate, authorize(ROLES.TRAINING_MANAGER), ok);
// Route yêu cầu quyền Instructor
testApp.get('/test/instructor-only', authenticate, authorize(ROLES.INSTRUCTOR), ok);
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

describe('IDTTX-24: Tiêu chí 1 - Một người dùng có thể giữ nhiều vai trò cùng lúc (vừa Giảng viên vừa Quản lý đào tạo)', () => {
  test('User ban đầu chỉ là Giảng viên (Instructor)', async () => {
    const me = await api('GET', '/api/auth/me', { token: tokens.instructor });
    assert.equal(me.status, 200);
    assert.deepEqual(me.body.user.roles, ['Instructor']);
  });

  test('Gán thêm vai trò TrainingManager: user có cả 2 vai trò cùng lúc', async () => {
    const res = await assignRole('2', 'TrainingManager');
    assert.equal(res.status, 201);
    assert.ok(res.body.user.roles.includes('Instructor'));
    assert.ok(res.body.user.roles.includes('TrainingManager'));

    // Kiểm tra danh sách roles qua GET /admin/users/:id/roles
    const listed = await api('GET', '/admin/users/2/roles', { token: tokens.admin });
    assert.equal(listed.status, 200);
    assert.ok(listed.body.roles.includes('Instructor'));
    assert.ok(listed.body.roles.includes('TrainingManager'));
  });

  test('User được truy cập cả hai phân hệ Giảng viên và Quản lý đào tạo', async () => {
    const checkInstructor = await api('GET', '/test/instructor-only', { token: tokens.instructor });
    assert.equal(checkInstructor.status, 200, 'Truy cập được chức năng Giảng viên');

    const checkEduManager = await api('GET', '/test/training-manager-only', { token: tokens.instructor });
    assert.equal(checkEduManager.status, 200, 'Truy cập được chức năng Quản lý đào tạo');
  });
});

describe('IDTTX-24: Tiêu chí 2 - Thay đổi vai trò có hiệu lực ngay ở thao tác kế tiếp, không cần đăng nhập lại', () => {
  test('Gán vai trò Admin cho học viên -> Có hiệu lực ngay lập tức bằng token hiện có', async () => {
    // Trước khi gán: bị chặn khỏi route Admin (403)
    const beforeAssign = await api('GET', '/admin/roles', { token: tokens.student });
    assert.equal(beforeAssign.status, 403);

    // Admin gán vai trò Admin cho học viên (ID: 3)
    const assignRes = await assignRole('3', 'Admin');
    assert.equal(assignRes.status, 201);

    // Thao tác kế tiếp ngay lập tức với token cũ của học viên -> Được phép (200) mà KHÔNG cần login lại
    const afterAssign = await api('GET', '/admin/roles', { token: tokens.student });
    assert.equal(afterAssign.status, 200, 'Quyền mới có hiệu lực ngay không cần đăng nhập lại');
  });

  test('Thu hồi vai trò Admin -> Bị chặn ngay lập tức ở thao tác tiếp theo', async () => {
    // Thu hồi Admin của học viên (ID: 3)
    const revokeRes = await revokeRole('3', 'Admin');
    assert.equal(revokeRes.status, 200);

    // Thao tác kế tiếp ngay lập tức với token cũ -> Bị chặn ngay (403)
    const afterRevoke = await api('GET', '/admin/roles', { token: tokens.student });
    assert.equal(afterRevoke.status, 403, 'Thu hồi quyền có hiệu lực ngay lập tức');
  });
});

describe('IDTTX-24: Tiêu chí 3 - Admin tuyệt đối KHÔNG THỂ tự thu hồi vai trò quản trị của chính mình', () => {
  test('Admin tự thu hồi Admin của chính mình -> Bị từ chối HTTP 409 (CANNOT_REVOKE_OWN_ADMIN)', async () => {
    const res = await revokeRole('1', 'Admin', tokens.admin);
    assert.equal(res.status, 409);
    assert.equal(res.body.code, 'CANNOT_REVOKE_OWN_ADMIN');

    // Vai trò Admin vẫn được bảo vệ
    const checkRoles = await api('GET', '/admin/users/1/roles', { token: tokens.admin });
    assert.deepEqual(checkRoles.body.roles, ['Admin']);
  });

  test('Admin vẫn có quyền thu hồi vai trò Admin của tài khoản khác', async () => {
    await assignRole('3', 'Admin');
    const revokeOther = await revokeRole('3', 'Admin');
    assert.equal(revokeOther.status, 200);
  });
});

describe('IDTTX-24: Tiêu chí 4 - Tính toàn vẹn và thông báo lỗi rõ ràng', () => {
  test('Gán trùng vai trò đã có -> Báo lỗi HTTP 409 (ROLE_ALREADY_ASSIGNED)', async () => {
    const res = await assignRole('2', 'Instructor');
    assert.equal(res.status, 409);
    assert.equal(res.body.code, 'ROLE_ALREADY_ASSIGNED');
  });

  test('Thu hồi vai trò người dùng không có -> Báo lỗi HTTP 404 (ROLE_NOT_ASSIGNED)', async () => {
    const res = await revokeRole('3', 'Accountant');
    assert.equal(res.status, 404);
    assert.equal(res.body.code, 'ROLE_NOT_ASSIGNED');
  });

  test('Gán hoặc thu hồi vai trò không tồn tại trong danh mục -> Báo lỗi HTTP 400', async () => {
    assert.equal((await assignRole('2', 'SuperHero')).status, 400);
    assert.equal((await revokeRole('2', 'SuperHero')).status, 400);
  });
});
