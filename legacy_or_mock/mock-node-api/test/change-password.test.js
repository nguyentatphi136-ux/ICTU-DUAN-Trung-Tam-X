const assert = require('node:assert/strict');
const { after, before, describe, test } = require('node:test');
const app = require('../src/app');

let server;
let baseUrl;
let token;

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

before(async () => {
  server = app.listen(0);
  await new Promise((resolve) => server.once('listening', resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;

  const res = await api('POST', '/api/auth/login', {
    body: { email: 'student@example.com', password: 'student123' },
  });
  assert.equal(res.status, 200);
  token = res.body.token;
});

after(() => new Promise((resolve) => server.close(resolve)));

describe('Chức năng S1-04: Đổi mật khẩu khi đang đăng nhập', () => {
  test('1. Bị từ chối HTTP 401 nếu chưa đăng nhập (thiếu token)', async () => {
    const res = await api('POST', '/api/auth/change-password', {
      body: {
        currentPassword: 'student123',
        newPassword: 'newStudentPass123',
        confirmPassword: 'newStudentPass123',
      },
    });
    assert.equal(res.status, 401);
  });

  test('2. Báo lỗi khi mật khẩu hiện tại không chính xác', async () => {
    const res = await api('POST', '/api/auth/change-password', {
      token,
      body: {
        currentPassword: 'wrongCurrentPass123',
        newPassword: 'newStudentPass123',
        confirmPassword: 'newStudentPass123',
      },
    });
    assert.equal(res.status, 400);
    assert.equal(res.body.code, 'INVALID_CURRENT_PASSWORD');
    assert.equal(res.body.message, 'Mật khẩu hiện tại không chính xác');
  });

  test('3. Báo lỗi khi mật khẩu mới dưới 8 ký tự', async () => {
    const res = await api('POST', '/api/auth/change-password', {
      token,
      body: {
        currentPassword: 'student123',
        newPassword: 'short1',
        confirmPassword: 'short1',
      },
    });
    assert.equal(res.status, 400);
    assert.equal(res.body.message, 'Mật khẩu mới phải có tối thiểu 8 ký tự');
  });

  test('4. Báo lỗi khi mật khẩu mới không có cả chữ và số (chỉ có chữ hoặc chỉ có số)', async () => {
    const resNoDigits = await api('POST', '/api/auth/change-password', {
      token,
      body: {
        currentPassword: 'student123',
        newPassword: 'onlylettersallowed',
        confirmPassword: 'onlylettersallowed',
      },
    });
    assert.equal(resNoDigits.status, 400);
    assert.equal(resNoDigits.body.message, 'Mật khẩu mới phải bao gồm cả chữ cái và chữ số');

    const resNoLetters = await api('POST', '/api/auth/change-password', {
      token,
      body: {
        currentPassword: 'student123',
        newPassword: '1234567890',
        confirmPassword: '1234567890',
      },
    });
    assert.equal(resNoLetters.status, 400);
    assert.equal(resNoLetters.body.message, 'Mật khẩu mới phải bao gồm cả chữ cái và chữ số');
  });

  test('5. Báo lỗi khi xác nhận mật khẩu không trùng khớp', async () => {
    const res = await api('POST', '/api/auth/change-password', {
      token,
      body: {
        currentPassword: 'student123',
        newPassword: 'newStudentPass123',
        confirmPassword: 'differentStudentPass123',
      },
    });
    assert.equal(res.status, 400);
    assert.equal(res.body.message, 'Xác nhận mật khẩu mới không trùng khớp');
  });

  test('6. Báo lỗi khi mật khẩu mới trùng mật khẩu hiện tại', async () => {
    const res = await api('POST', '/api/auth/change-password', {
      token,
      body: {
        currentPassword: 'student123',
        newPassword: 'student123',
        confirmPassword: 'student123',
      },
    });
    assert.equal(res.status, 400);
    assert.equal(res.body.message, 'Mật khẩu mới không được trùng với mật khẩu hiện tại');
  });

  test('7. Đổi mật khẩu thành công và đăng nhập được bằng mật khẩu mới', async () => {
    const res = await api('POST', '/api/auth/change-password', {
      token,
      body: {
        currentPassword: 'student123',
        newPassword: 'newStudentPass123',
        confirmPassword: 'newStudentPass123',
      },
    });
    assert.equal(res.status, 200);
    assert.equal(res.body.code, 'AUTH_CHANGE_PASSWORD_SUCCESS');

    // Mật khẩu cũ không còn đăng nhập được
    const oldLogin = await api('POST', '/api/auth/login', {
      body: { email: 'student@example.com', password: 'student123' },
    });
    assert.equal(oldLogin.status, 401);

    // Mật khẩu mới đăng nhập thành công
    const newLogin = await api('POST', '/api/auth/login', {
      body: { email: 'student@example.com', password: 'newStudentPass123' },
    });
    assert.equal(newLogin.status, 200);
  });
});
