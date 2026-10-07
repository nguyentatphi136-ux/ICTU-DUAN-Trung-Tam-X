const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const app = require('../src/app');
const { ALL_ROLES, PERMISSIONS, ROLE_PERMISSIONS, hasPermission } = require('../src/constants/permissions');

let server;
let baseUrl;

const tokens = {
  admin: null,
  instructor: null,
  accountant: null,
  student: null,
};

async function login(email, password) {
  const response = await fetch(`${baseUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  return response.json();
}

async function requestApi(method, path, { token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  let data = null;
  try {
    data = await response.json();
  } catch {}

  return { status: response.status, body: data };
}

test.before(async () => {
  server = http.createServer(app);
  await new Promise((resolve) => server.listen(0, resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;

  tokens.admin = (await login('admin@example.com', 'admin123')).token;
  tokens.instructor = (await login('instructor@example.com', 'instructor123')).token;
  tokens.accountant = (await login('accountant@example.com', 'accountant123')).token;
  tokens.student = (await login('student@example.com', 'student123')).token;
});

test.after(async () => {
  if (server) {
    await new Promise((resolve) => server.close(resolve));
  }
});

test.describe('IDTTX-20: Khai báo 8 vai trò nghiệp vụ & ma trận quyền', () => {
  test('Hệ thống định nghĩa đầy đủ 8 vai trò nghiệp vụ', () => {
    const expectedRoles = [
      'Admin',
      'TrainingManager',
      'Admissions',
      'Instructor',
      'TeachingAssistant',
      'Accountant',
      'Student',
      'Guest',
    ];
    assert.deepEqual(Object.values(ALL_ROLES), expectedRoles);
    assert.equal(Object.keys(ROLE_PERMISSIONS).length, 8);
  });

  test('Ma trận phân quyền: Giảng viên có quyền sửa điểm nhưng KHÔNG có quyền sửa học phí', () => {
    assert.equal(hasPermission([ALL_ROLES.INSTRUCTOR], PERMISSIONS.GRADE_EDIT), true);
    assert.equal(hasPermission([ALL_ROLES.INSTRUCTOR], PERMISSIONS.TUITION_EDIT), false);
  });

  test('Ma trận phân quyền: Kế toán có quyền sửa học phí nhưng KHÔNG có quyền sửa điểm', () => {
    assert.equal(hasPermission([ALL_ROLES.ACCOUNTANT], PERMISSIONS.TUITION_EDIT), true);
    assert.equal(hasPermission([ALL_ROLES.ACCOUNTANT], PERMISSIONS.GRADE_EDIT), false);
    assert.equal(hasPermission([ALL_ROLES.ACCOUNTANT], PERMISSIONS.GRADE_VIEW), true);
  });

  test('Ma trận phân quyền: Học viên không có quyền sửa điểm và sửa học phí', () => {
    assert.equal(hasPermission([ALL_ROLES.STUDENT], PERMISSIONS.GRADE_EDIT), false);
    assert.equal(hasPermission([ALL_ROLES.STUDENT], PERMISSIONS.TUITION_EDIT), false);
  });

  test('Ma trận phân quyền: Admin có toàn bộ quyền hệ thống', () => {
    assert.equal(hasPermission([ALL_ROLES.ADMIN], PERMISSIONS.GRADE_EDIT), true);
    assert.equal(hasPermission([ALL_ROLES.ADMIN], PERMISSIONS.TUITION_EDIT), true);
  });
});

test.describe('IDTTX-20: Giảng viên sửa được điểm - Kế toán KHÔNG sửa được điểm', () => {
  test('Giảng viên (Instructor) cập nhật điểm số thành công (HTTP 200)', async () => {
    const res = await requestApi('PUT', '/api/grades/3', {
      token: tokens.instructor,
      body: { componentName: 'Giữa kỳ', score: 9.5 },
    });

    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(res.body.message, 'Cập nhật điểm số thành công');
    assert.equal(res.body.grade.score, 9.5);
  });

  test('Kế toán (Accountant) thử sửa điểm -> BỊ TỪ CHỐI (HTTP 403) với thông báo tiếng Việt', async () => {
    const res = await requestApi('PUT', '/api/grades/3', {
      token: tokens.accountant,
      body: { componentName: 'Giữa kỳ', score: 10.0 },
    });

    assert.equal(res.status, 403);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'FORBIDDEN');
    assert.equal(res.body.message, 'Kế toán không có quyền chỉnh sửa điểm số học viên');
  });

  test('Học viên (Student) thử sửa điểm -> BỊ TỪ CHỐI (HTTP 403) với thông báo tiếng Việt', async () => {
    const res = await requestApi('PUT', '/api/grades/3', {
      token: tokens.student,
      body: { componentName: 'Giữa kỳ', score: 10.0 },
    });

    assert.equal(res.status, 403);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'FORBIDDEN');
    assert.equal(res.body.message, 'Bạn không có quyền chỉnh sửa điểm số học viên');
  });
});

test.describe('IDTTX-20: Kế toán sửa được học phí - Giảng viên KHÔNG sửa được học phí', () => {
  test('Kế toán (Accountant) cập nhật học phí thành công (HTTP 200)', async () => {
    const res = await requestApi('PUT', '/api/tuition/3', {
      token: tokens.accountant,
      body: { paidAmount: 15000000, status: 'paid' },
    });

    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(res.body.message, 'Cập nhật thông tin học phí thành công');
    assert.equal(res.body.tuition.paidAmount, 15000000);
    assert.equal(res.body.tuition.status, 'paid');
  });

  test('Giảng viên (Instructor) thử sửa học phí -> BỊ TỪ CHỐI (HTTP 403) với thông báo tiếng Việt', async () => {
    const res = await requestApi('PUT', '/api/tuition/3', {
      token: tokens.instructor,
      body: { paidAmount: 5000000 },
    });

    assert.equal(res.status, 403);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'FORBIDDEN');
    assert.equal(res.body.message, 'Giảng viên không có quyền chỉnh sửa thông tin học phí');
  });

  test('Học viên (Student) thử sửa học phí -> BỊ TỪ CHỐI (HTTP 403) với thông báo tiếng Việt', async () => {
    const res = await requestApi('PUT', '/api/tuition/3', {
      token: tokens.student,
      body: { paidAmount: 0 },
    });

    assert.equal(res.status, 403);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'FORBIDDEN');
    assert.equal(res.body.message, 'Bạn không có quyền chỉnh sửa thông tin học phí');
  });
});

test.describe('IDTTX-20: Quản trị hệ thống (Admin) có toàn quyền sửa điểm & học phí', () => {
  test('Admin sửa được điểm số học viên', async () => {
    const res = await requestApi('PUT', '/api/grades/3', {
      token: tokens.admin,
      body: { componentName: 'Cuối kỳ', score: 9.0 },
    });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
  });

  test('Admin sửa được học phí học viên', async () => {
    const res = await requestApi('PUT', '/api/tuition/3', {
      token: tokens.admin,
      body: { paidAmount: 12000000, status: 'partially_paid' },
    });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
  });
});

test.describe('IDTTX-20: Mặc định từ chối ở tầng máy chủ & Thông báo tiếng Việt rõ ràng', () => {
  test('Chưa đăng nhập (thiếu token) -> Bị từ chối HTTP 401 với thông báo tiếng Việt rõ ràng', async () => {
    const resGrade = await requestApi('GET', '/api/grades/3');
    assert.equal(resGrade.status, 401);
    assert.equal(resGrade.body.message, 'Vui lòng đăng nhập để tiếp tục');

    const resTuition = await requestApi('PUT', '/api/tuition/3', { body: { paidAmount: 1000 } });
    assert.equal(resTuition.status, 401);
    assert.equal(resTuition.body.message, 'Vui lòng đăng nhập để tiếp tục');
  });

  test('Token không hợp lệ / giả mạo -> Bị từ chối HTTP 401 với thông báo tiếng Việt rõ ràng', async () => {
    const res = await requestApi('GET', '/api/grades/3', { token: 'invalid-fake-token' });
    assert.equal(res.status, 401);
    assert.equal(res.body.message, 'Phiên đăng nhập không hợp lệ hoặc đã hết hạn');
  });

  test('Gọi API không tồn tại -> Trả về HTTP 404 thông báo tiếng Việt rõ ràng thay vì stacktrace', async () => {
    const res = await requestApi('GET', '/api/unknown-endpoint');
    assert.equal(res.status, 404);
    assert.equal(res.body.message, 'Không tìm thấy API');
  });
});
