const assert = require('node:assert/strict');
const { after, before, beforeEach, describe, test } = require('node:test');
const app = require('../src/app');
const { seedSampleLeads } = require('../src/data/leads');
const { seedSampleStudents, students } = require('../src/data/students');
const { seedSampleClasses } = require('../src/data/classes');
const { resetUsers } = require('../src/data/users');

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
  tokens.admissions = (await login('admissions@example.com', 'admissions123')).token;
  tokens.instructor = (await login('instructor@example.com', 'instructor123')).token;
  tokens.studentMe = (await login('s25200212101@tms.vn', 'student123')).token; // HV260101 (NGUYEN MINH ANH)
  tokens.studentOther = (await login('student@example.com', 'student123')).token;
});

after(() => new Promise((resolve) => server.close(resolve)));

beforeEach(() => {
  resetUsers();
  seedSampleLeads();
  seedSampleClasses();
  seedSampleStudents();
});

describe('S3-04 (IDTTX-212 / Subtask IDTTX-241): Quản lý hồ sơ học viên', () => {
  test('1. Tra cứu danh sách học viên theo mã, tên hoặc số điện thoại', async () => {
    // Tìm theo mã học viên
    const resCode = await api('GET', '/api/students?search=HV260101', { token: tokens.manager });
    assert.equal(resCode.status, 200);
    assert.equal(resCode.body.success, true);
    assert.ok(resCode.body.data.length >= 1);
    assert.equal(resCode.body.data[0].code, 'HV260101');

    // Tìm theo tên
    const resName = await api('GET', '/api/students?search=Minh%20Anh', { token: tokens.manager });
    assert.equal(resName.status, 200);
    assert.ok(resName.body.data.some((s) => s.name.includes('MINH ANH')));

    // Tìm theo số điện thoại (hỗ trợ nhập liền không dấu cách: 0912345678)
    const resPhone = await api('GET', '/api/students?q=0912345678', { token: tokens.manager });
    assert.equal(resPhone.status, 200);
    assert.ok(resPhone.body.data.some((s) => s.code === 'HV260101'));
  });

  test('2. Lọc học viên theo trạng thái và chương trình đào tạo', async () => {
    // Lọc trạng thái "Đang học"
    const resStatus = await api('GET', '/api/students?status=Đang%20học', { token: tokens.manager });
    assert.equal(resStatus.status, 200);
    assert.ok(resStatus.body.data.every((s) => s.status === 'Đang học'));

    // Lọc theo chương trình "Phân tích dữ liệu"
    const resProg = await api('GET', '/api/students?program=Phân%20tích%20dữ%20liệu', {
      token: tokens.manager,
    });
    assert.equal(resProg.status, 200);
    assert.ok(resProg.body.data.every((s) => s.program.includes('Phân tích dữ liệu')));
  });

  test('3. Phân trang và cung cấp thống kê số lượng học viên', async () => {
    const res = await api('GET', '/api/students?page=1&limit=5', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.page, 1);
    assert.equal(res.body.limit, 5);
    assert.equal(res.body.data.length, 5);
    assert.ok(res.body.total > 10);
    assert.ok(res.body.studyingCount > 0);
    assert.ok(res.body.totalAll >= res.body.total);
    assert.equal(res.body.from, 1);
    assert.equal(res.body.to, 5);
  });

  test('4. Xem chi tiết hồ sơ học viên kèm lớp đang học và lịch sử các lớp đã học', async () => {
    const res = await api('GET', '/api/students/HV260101', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);

    const s = res.body.data;
    assert.equal(s.code, 'HV260101');
    assert.equal(s.name, 'NGUYEN MINH ANH');
    assert.equal(s.cls, 'WEB-K15');

    // Thông tin lớp đang học được gắn kèm
    assert.ok(s.currentClass, 'Học viên phải có thông tin lớp đang học');
    assert.equal(s.currentClass.code, 'WEB-K15');
    assert.equal(s.currentClass.slot, 'Tối 2-4-6');
    assert.equal(s.currentClass.room, 'P.301');
    assert.ok(s.currentClass.totalSessions > 0);
    assert.ok(s.currentClass.classSize > 0);

    // Lịch sử các lớp đã học
    assert.ok(Array.isArray(s.history));
    assert.ok(s.history.length >= 3);
    assert.ok(s.history.some((h) => h.cls === 'GIT-K03' && h.result.includes('Hoàn thành')));

    // Ghi chú
    assert.ok(Array.isArray(s.notes));
    assert.ok(s.notes.length >= 2);
  });

  test('5. Sửa thông tin cá nhân và trình độ đầu vào (PUT /api/students/:code)', async () => {
    const updatePayload = {
      phone: '0988 777 666',
      birth: '2004-05-20',
      email: 'minhanh.new@gmail.com',
      address: 'Số 99 Cầu Giấy, Hà Nội',
      entryScore: '8,0 / 10',
      level: 'Khá',
      background: 'Đã hoàn thành khóa React cơ bản',
    };

    const res = await api('PUT', '/api/students/HV260101', {
      token: tokens.manager,
      body: updatePayload,
    });

    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);

    const updated = res.body.data;
    assert.equal(updated.phone, '0988 777 666');
    assert.equal(updated.birth, '2004-05-20');
    assert.equal(updated.email, 'minhanh.new@gmail.com');
    assert.equal(updated.address, 'Số 99 Cầu Giấy, Hà Nội');
    assert.equal(updated.entryScore, '8,0 / 10');
    assert.equal(updated.level, 'Khá');
    assert.equal(updated.background, 'Đã hoàn thành khóa React cơ bản');
  });

  test('6. Sửa hồ sơ và thêm ghi chú mới đồng thời trong cùng một thao tác', async () => {
    const payload = {
      phone: '0912 345 678',
      address: 'Hà Nội',
      note: 'Xin nghỉ thêm buổi thứ Sáu do có việc gia đình',
    };

    const res = await api('PUT', '/api/students/HV260101', {
      token: tokens.manager,
      body: payload,
    });

    assert.equal(res.status, 200);
    const updated = res.body.data;
    assert.ok(updated.notes.length >= 3);
    assert.equal(updated.notes[0].text, 'Xin nghỉ thêm buổi thứ Sáu do có việc gia đình');
    assert.equal(updated.notes[0].by, 'Phạm Thị Quản (Quản lý đào tạo)');
  });

  test('7. Validation số điện thoại: sai định dạng -> Từ chối với HTTP 400 (INVALID_PHONE)', async () => {
    const invalidPhones = ['12345', '0123456789', '09123', 'abcd0912345678'];
    for (const badPhone of invalidPhones) {
      const res = await api('PUT', '/api/students/HV260101', {
        token: tokens.manager,
        body: { phone: badPhone },
      });
      assert.equal(res.status, 400);
      assert.equal(res.body.code, 'INVALID_PHONE');
    }
  });

  test('8. Validation email: sai định dạng -> Từ chối với HTTP 400 (INVALID_EMAIL)', async () => {
    const res = await api('PUT', '/api/students/HV260101', {
      token: tokens.manager,
      body: { email: 'bad-email-format' },
    });
    assert.equal(res.status, 400);
    assert.equal(res.body.code, 'INVALID_EMAIL');
  });

  test('9. Thêm ghi chú riêng lẻ qua POST /api/students/:code/notes', async () => {
    const res = await api('POST', '/api/students/HV260101/notes', {
      token: tokens.admissions,
      body: { text: 'Học viên liên hệ hỏi về chứng chỉ tốt nghiệp' },
    });

    assert.equal(res.status, 201);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.text, 'Học viên liên hệ hỏi về chứng chỉ tốt nghiệp');
    assert.equal(res.body.data.by, 'Lê Thị Thu Hà (Tư vấn viên)');

    // Kiểm tra xem ghi chú đã xuất hiện trong hồ sơ
    const checkRes = await api('GET', '/api/students/HV260101', { token: tokens.manager });
    assert.equal(checkRes.body.data.notes[0].text, 'Học viên liên hệ hỏi về chứng chỉ tốt nghiệp');
  });

  test('10. Thêm ghi chú rỗng -> Bị từ chối với HTTP 400 (NOTE_REQUIRED)', async () => {
    const res = await api('POST', '/api/students/HV260101/notes', {
      token: tokens.admissions,
      body: { text: '   ' },
    });
    assert.equal(res.status, 400);
    assert.equal(res.body.code, 'NOTE_REQUIRED');
  });

  test('11. Phân quyền: Học viên xem hồ sơ của CHÍNH MÌNH -> Thành công HTTP 200', async () => {
    const res = await api('GET', '/api/students/HV260101', { token: tokens.studentMe });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.code, 'HV260101');
  });

  test('12. Phân quyền: Học viên cố tình xem hồ sơ của HỌC VIÊN KHÁC -> Bị từ chối HTTP 403', async () => {
    const res = await api('GET', '/api/students/HV260142', { token: tokens.studentMe });
    assert.equal(res.status, 403);
    assert.equal(res.body.code, 'FORBIDDEN_STUDENT_PROFILE');
  });

  test('13. Phân quyền: Học viên cố tình xem danh sách học viên toàn trường -> Bị từ chối HTTP 403', async () => {
    const res = await api('GET', '/api/students', { token: tokens.studentMe });
    assert.equal(res.status, 403);
    assert.equal(res.body.code, 'FORBIDDEN_STUDENT_LIST');
  });

  test('14. Phân quyền: Học viên cố tình sửa hồ sơ -> Bị từ chối HTTP 403 (FORBIDDEN)', async () => {
    const res = await api('PUT', '/api/students/HV260101', {
      token: tokens.studentMe,
      body: { address: 'Địa chỉ mới' },
    });
    assert.equal(res.status, 403);
  });

  test('15. Phân quyền: Giảng viên được xem chi tiết hồ sơ nhưng KHÔNG được sửa hồ sơ', async () => {
    // Giảng viên xem chi tiết: Thành công
    const viewRes = await api('GET', '/api/students/HV260101', { token: tokens.instructor });
    assert.equal(viewRes.status, 200);
    assert.equal(viewRes.body.data.code, 'HV260101');

    // Giảng viên cố tình sửa hồ sơ: Bị chặn 403
    const editRes = await api('PUT', '/api/students/HV260101', {
      token: tokens.instructor,
      body: { address: 'Thử sửa đổi' },
    });
    assert.equal(editRes.status, 403);
  });

  test('16. Tra cứu học viên của chính mình qua GET /api/students/me', async () => {
    const res = await api('GET', '/api/students/me', { token: tokens.studentMe });
    assert.equal(res.status, 200);
    assert.equal(res.body.data.code, 'HV260101');
    assert.equal(res.body.data.name, 'NGUYEN MINH ANH');
  });

  test('17. Tra cứu học viên không tồn tại -> Trả về HTTP 404 (STUDENT_NOT_FOUND)', async () => {
    const res = await api('GET', '/api/students/HV999999', { token: tokens.manager });
    assert.equal(res.status, 404);
    assert.equal(res.body.code, 'STUDENT_NOT_FOUND');
  });

  test('18. Tra cứu danh mục lớp học (GET /api/classes và GET /api/classes/:code)', async () => {
    const resList = await api('GET', '/api/classes', { token: tokens.manager });
    assert.equal(resList.status, 200);
    assert.ok(resList.body.data.length > 5);
    assert.ok(resList.body.data.some((c) => c.code === 'WEB-K15'));

    const resDetail = await api('GET', '/api/classes/WEB-K15', { token: tokens.manager });
    assert.equal(resDetail.status, 200);
    assert.equal(resDetail.body.data.code, 'WEB-K15');
    assert.equal(resDetail.body.data.room, 'P.301');
    assert.ok(resDetail.body.data.totalSessions > 0);
  });
});
