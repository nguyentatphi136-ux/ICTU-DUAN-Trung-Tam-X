const assert = require('node:assert/strict');
const { after, before, beforeEach, describe, test } = require('node:test');
const app = require('../src/app');
const { resetToDefaults } = require('../src/data/programs');

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
  return { status: response.status, body: await response.json() };
}

async function login(email, password) {
  const { status, body } = await api('POST', '/api/auth/login', { body: { email, password } });
  assert.equal(status, 200);
  return body;
}

before(async () => {
  server = app.listen(0);
  await new Promise((resolve) => server.once('listening', resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;

  tokens.admin = (await login('admin@example.com', 'admin123')).token;
  tokens.trainingManager = (await login('trainingmanager@example.com', 'trainingmanager123')).token;
  tokens.instructor = (await login('instructor@example.com', 'instructor123')).token;
  tokens.student = (await login('student@example.com', 'student123')).token;
});

after(() => new Promise((resolve) => server.close(resolve)));

beforeEach(() => {
  resetToDefaults();
});

describe('S2-06 – EP-02: Quản lý chương trình đào tạo & Môn học tiên quyết', () => {
  // =========================================================================
  // 1. CHỨC NĂNG THÊM MÔN HỌC VÀO CHƯƠNG TRÌNH
  // =========================================================================
  test('TEST 1: Thêm môn học hợp lệ vào chương trình -> Thành công (201 Created)', async () => {
    // Chương trình 1 (Frontend) hiện có môn 1, 2, 3, 5. Thêm môn 4 (UI/UX - WEB104)
    const res = await api('POST', '/api/training-programs/1/courses', {
      token: tokens.trainingManager,
      body: { courseId: 4 },
    });

    assert.equal(res.status, 201);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.subjectId, 4);
    assert.equal(res.body.data.subjectCode, 'WEB104');
    assert.equal(res.body.data.orderIndex, 5); // Tự động gán thứ tự tiếp theo

    // Kiểm tra danh sách môn học sau khi thêm
    const listRes = await api('GET', '/api/training-programs/1/courses');
    assert.equal(listRes.status, 200);
    assert.equal(listRes.body.totalCourses, 5);
  });

  test('TEST 2: Thêm môn học đã tồn tại trong chương trình -> Từ chối, không tạo duplicate (409 Conflict)', async () => {
    // Môn 1 (HTML/CSS) đã có trong chương trình 1
    const res = await api('POST', '/api/training-programs/1/courses', {
      token: tokens.trainingManager,
      body: { courseId: 1 },
    });

    assert.equal(res.status, 409);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'SUBJECT_ALREADY_IN_PROGRAM');
  });

  // =========================================================================
  // 2. CHỨC NĂNG GỠ MÔN HỌC KHỎI CHƯƠNG TRÌNH
  // =========================================================================
  test('TEST 3: Gỡ môn học đang thuộc chương trình -> Thành công (200 OK) và reorder liên tục', async () => {
    // Gỡ môn 3 (Git & GitHub, orderIndex: 3) khỏi chương trình 1
    const res = await api('DELETE', '/api/training-programs/1/courses/3', {
      token: tokens.trainingManager,
    });

    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);

    // Kiểm tra danh sách còn 3 môn và thứ tự được đánh số liên tục 1, 2, 3
    const listRes = await api('GET', '/api/training-programs/1/courses');
    assert.equal(listRes.body.totalCourses, 3);
    const orders = listRes.body.data.map((c) => c.orderIndex);
    assert.deepEqual(orders, [1, 2, 3]);
  });

  test('TEST 4: Gỡ môn học không thuộc chương trình -> Trả lỗi phù hợp (404 Not Found)', async () => {
    // Môn 6 (IELTS Foundation) không thuộc chương trình 1 (Frontend)
    const res = await api('DELETE', '/api/training-programs/1/courses/6', {
      token: tokens.trainingManager,
    });

    assert.equal(res.status, 404);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'SUBJECT_NOT_IN_PROGRAM');
  });

  test('TEST 4B: Gỡ môn học đang làm tiên quyết cho môn khác -> Từ chối (400 Bad Request)', async () => {
    // Môn 1 (HTML/CSS) đang là tiên quyết của môn 2 (JavaScript) trong chương trình 1
    const res = await api('DELETE', '/api/training-programs/1/courses/1', {
      token: tokens.trainingManager,
    });

    assert.equal(res.status, 400);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'IS_PREREQUISITE_OF_OTHERS');
    assert.match(res.body.message, /môn tiên quyết/);
  });

  // =========================================================================
  // 3. CHỨC NĂNG SẮP XẾP THỨ TỰ MÔN HỌC (REORDER)
  // =========================================================================
  test('TEST 5: Sắp xếp lại thứ tự các môn học trong chương trình -> Thứ tự được lưu chính xác (200 OK)', async () => {
    // Chương trình 4 (Data) có: 11 (data-basic, order 1), 12 (excel, order 2, prereq: 11), 13 (sql, order 3, prereq: 11), 14 (powerbi, order 4, prereq: 12)
    // Sắp xếp đổi chỗ: 11 (order 1) -> 13 (order 2) -> 12 (order 3) -> 14 (order 4)
    // Cả 12 và 13 đều chỉ phụ thuộc 11, và 14 phụ thuộc 12, nên thứ tự này hoàn toàn hợp lệ!
    const res = await api('PUT', '/api/training-programs/4/courses/order', {
      token: tokens.trainingManager,
      body: {
        courses: [
          { courseId: 11, order: 1 },
          { courseId: 13, order: 2 },
          { courseId: 12, order: 3 },
          { courseId: 14, order: 4 },
        ],
      },
    });

    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);

    const listRes = await api('GET', '/api/training-programs/4/courses');
    const actualOrder = listRes.body.data.map((c) => ({ id: c.subjectId, order: c.orderIndex }));
    assert.deepEqual(actualOrder, [
      { id: 11, order: 1 },
      { id: 13, order: 2 },
      { id: 12, order: 3 },
      { id: 14, order: 4 },
    ]);
  });

  test('TEST 6: Gửi request order có duplicate courseId -> Từ chối (400 Bad Request)', async () => {
    const res = await api('PUT', '/api/training-programs/4/courses/order', {
      token: tokens.trainingManager,
      body: {
        courses: [
          { courseId: 11, order: 1 },
          { courseId: 11, order: 2 }, // Duplicate 11
          { courseId: 12, order: 3 },
        ],
      },
    });

    assert.equal(res.status, 400);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'DUPLICATE_SUBJECT_IN_ORDER');
  });

  test('TEST 7: Cập nhật order với courseId không thuộc chương trình -> Từ chối (400 Bad Request)', async () => {
    // Môn 1 (Frontend) không thuộc chương trình 4 (Data)
    const res = await api('PUT', '/api/training-programs/4/courses/order', {
      token: tokens.trainingManager,
      body: {
        courses: [
          { courseId: 11, order: 1 },
          { courseId: 1, order: 2 }, // Môn 1 không thuộc chương trình 4
        ],
      },
    });

    assert.equal(res.status, 400);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'SUBJECT_NOT_IN_PROGRAM');
  });

  test('TEST 7B: Sắp xếp đặt môn tiên quyết đứng SAU môn học cần nó -> Từ chối (400 Bad Request)', async () => {
    // Trong CT 1: Môn 1 (HTML/CSS) là tiên quyết của môn 2 (JavaScript).
    // Nếu xếp môn 2 đứng ở order 1, còn môn 1 đứng ở order 2 -> Vi phạm lộ trình!
    const res = await api('PUT', '/api/training-programs/1/courses/order', {
      token: tokens.trainingManager,
      body: {
        courses: [
          { courseId: 2, order: 1 }, // Học JavaScript trước khi học HTML/CSS
          { courseId: 1, order: 2 },
          { courseId: 3, order: 3 },
          { courseId: 5, order: 4 },
        ],
      },
    });

    assert.equal(res.status, 400);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'PREREQUISITE_ORDER_VIOLATION');
    assert.match(res.body.message, /phải đứng trước môn/);
  });

  // =========================================================================
  // 4. CHỨC NĂNG KHAI BÁO MÔN TIÊN QUYẾT (PREREQUISITE)
  // =========================================================================
  test('TEST 8: Tạo prerequisite hợp lệ -> Thành công (200 OK)', async () => {
    // Trong CT 1: Môn 3 (Git, order 3) có thể đặt môn 1 (HTML/CSS, order 1) làm tiên quyết
    const res = await api('PUT', '/api/training-programs/1/courses/3/prerequisite', {
      token: tokens.trainingManager,
      body: { prerequisiteId: 1 },
    });

    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.prerequisiteSubjectId, 1);
  });

  test('TEST 9: Môn học làm prerequisite cho chính nó -> Từ chối (400 Bad Request)', async () => {
    const res = await api('PUT', '/api/training-programs/1/courses/3/prerequisite', {
      token: tokens.trainingManager,
      body: { prerequisiteId: 3 }, // Tự làm tiên quyết cho chính mình
    });

    assert.equal(res.status, 400);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'SELF_PREREQUISITE');
  });

  test('TEST 10: Gỡ bỏ môn tiên quyết bằng null hoặc endpoint DELETE -> Thành công (200 OK)', async () => {
    // Môn 2 đang có prereq là 1. Gỡ prereq
    const res = await api('DELETE', '/api/training-programs/1/courses/2/prerequisite', {
      token: tokens.trainingManager,
    });

    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);

    const listRes = await api('GET', '/api/training-programs/1/courses');
    const subj2 = listRes.body.data.find((c) => c.subjectId === 2);
    assert.equal(subj2.prerequisiteSubjectId, null);
  });

  test('TEST 11: Tạo vòng lặp prerequisite (Circular Dependency) -> Từ chối (400 Bad Request)', async () => {
    // CT 1: Môn 1 -> Môn 2 (2 cần 1). Môn 2 -> Môn 5 (5 cần 2).
    // Nếu cố tình đặt 1 cần 5 -> Tạo chu trình: 1 -> 5 -> 2 -> 1!
    const res = await api('PUT', '/api/training-programs/1/courses/1/prerequisite', {
      token: tokens.trainingManager,
      body: { prerequisiteId: 5 },
    });

    assert.equal(res.status, 400);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'PREREQUISITE_ORDER_INVALID'); // Thứ tự 5 > 1 hoặc CIRCULAR
  });

  // =========================================================================
  // 5. PHÂN QUYỀN (AUTHORIZATION)
  // =========================================================================
  test('TEST 12: User không có quyền Quản lý đào tạo (Student, Instructor) cố gắng sửa chương trình -> 403 Forbidden', async () => {
    // Học viên thử thêm môn học
    const studentRes = await api('POST', '/api/training-programs/1/courses', {
      token: tokens.student,
      body: { courseId: 4 },
    });
    assert.equal(studentRes.status, 403);
    assert.equal(studentRes.body.code, 'FORBIDDEN');

    // Giảng viên thử đổi thứ tự
    const instructorRes = await api('PUT', '/api/training-programs/1/courses/order', {
      token: tokens.instructor,
      body: { courses: [{ courseId: 1, order: 1 }] },
    });
    assert.equal(instructorRes.status, 403);
    assert.equal(instructorRes.body.code, 'FORBIDDEN');

    // Không có token
    const noAuthRes = await api('DELETE', '/api/training-programs/1/courses/3');
    assert.equal(noAuthRes.status, 401);
  });

  test('TEST 12B: Admin cũng có quyền quản trị chương trình đào tạo -> Thành công (201/200)', async () => {
    const res = await api('POST', '/api/training-programs/1/courses', {
      token: tokens.admin,
      body: { courseId: 4 },
    });
    assert.equal(res.status, 201);
  });

  // =========================================================================
  // 6. VALIDATION & KHÔNG TỒN TẠI
  // =========================================================================
  test('TEST 13: Program không tồn tại -> Trả lỗi phù hợp (404 Not Found)', async () => {
    const res = await api('GET', '/api/training-programs/9999/courses');
    assert.equal(res.status, 404);
    assert.equal(res.body.success, false);

    const postRes = await api('POST', '/api/training-programs/9999/courses', {
      token: tokens.trainingManager,
      body: { courseId: 1 },
    });
    assert.equal(postRes.status, 404);
  });

  test('TEST 14: Course không tồn tại -> Trả lỗi phù hợp (404 Not Found)', async () => {
    const res = await api('POST', '/api/training-programs/1/courses', {
      token: tokens.trainingManager,
      body: { courseId: 9999 },
    });
    assert.equal(res.status, 404);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'SUBJECT_NOT_FOUND');
  });
});
