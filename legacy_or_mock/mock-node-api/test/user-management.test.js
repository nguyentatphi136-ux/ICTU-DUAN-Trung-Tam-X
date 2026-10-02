const assert = require('node:assert/strict');
const { after, before, describe, test } = require('node:test');
const app = require('../src/app');

describe('Chức năng S1-08: Quản trị tạo, sửa, tìm kiếm tài khoản người dùng', () => {
  let server;
  let baseUrl;
  let adminToken;
  let nonAdminToken;

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

    const studentLogin = await api('POST', '/api/auth/login', {
      body: { email: 'student@example.com', password: 'student123' },
    });
    assert.equal(studentLogin.status, 200);
    nonAdminToken = studentLogin.body.token;
  });

  after(() => new Promise((resolve) => server.close(resolve)));

  test('1. [AC1] Tạo tài khoản mới thành công kèm mật khẩu tạm & gửi email kích hoạt', async () => {
    const res = await api('POST', '/admin/users', {
      token: adminToken,
      body: {
        email: 'nhanvien.moi@edumanager.vn',
        fullName: 'Nguyễn Văn Nhân Sự Mới',
        phone: '0981112233',
        roles: ['Instructor'],
      },
    });
    assert.equal(res.status, 201);
    assert.equal(res.body.success, true);
    assert.ok(res.body.tempPassword);
    assert.equal(res.body.user.email, 'nhanvien.moi@edumanager.vn');
    assert.equal(res.body.user.name, 'Nguyễn Văn Nhân Sự Mới');
  });

  test('2. [AC2] Chặn trùng email khi tạo tài khoản (HTTP 409 EMAIL_ALREADY_EXISTS)', async () => {
    const res = await api('POST', '/admin/users', {
      token: adminToken,
      body: {
        email: 'nhanvien.moi@edumanager.vn', // Email vừa tạo ở trên
        fullName: 'Người Trùng Email',
        phone: '0989999999',
      },
    });
    assert.equal(res.status, 409);
    assert.equal(res.body.code, 'EMAIL_ALREADY_EXISTS');
    assert.ok(res.body.message.includes('đã tồn tại'));
  });

  test('3. [AC3] Sửa thông tin tài khoản (Họ tên, SĐT)', async () => {
    // Sửa thông tin người dùng vừa tạo (id tìm kiếm theo email)
    const listRes = await api('GET', '/admin/users?q=nhanvien.moi', { token: adminToken });
    assert.equal(listRes.status, 200);
    const createdUser = listRes.body.items.find((u) => u.email === 'nhanvien.moi@edumanager.vn');
    assert.ok(createdUser);

    const updateRes = await api('PUT', `/admin/users/${createdUser.id}`, {
      token: adminToken,
      body: {
        name: 'Nguyễn Văn Nhân Sự Mới (Đã Cập Nhật)',
        phone: '0919998877',
      },
    });
    assert.equal(updateRes.status, 200);
    assert.equal(updateRes.body.user.name, 'Nguyễn Văn Nhân Sự Mới (Đã Cập Nhật)');
    assert.equal(updateRes.body.user.phone, '0919998877');
  });

  test('4. [AC3] Tìm kiếm tài khoản theo Họ tên, Email, Số điện thoại', async () => {
    // Tìm theo tên
    const searchName = await api('GET', '/admin/users?q=Nhân Sự Mới', { token: adminToken });
    assert.equal(searchName.status, 200);
    assert.ok(searchName.body.items.length >= 1);
    assert.ok(searchName.body.items.some((u) => u.email === 'nhanvien.moi@edumanager.vn'));

    // Tìm theo email
    const searchEmail = await api('GET', '/admin/users?q=nhanvien.moi@edumanager.vn', { token: adminToken });
    assert.equal(searchEmail.status, 200);
    assert.equal(searchEmail.body.items.length, 1);

    // Tìm theo SĐT
    const searchPhone = await api('GET', '/admin/users?q=0919998877', { token: adminToken });
    assert.equal(searchPhone.status, 200);
    assert.equal(searchPhone.body.items.length, 1);
  });

  test('5. [AC3] Lọc danh sách theo vai trò và trạng thái', async () => {
    // Lọc theo vai trò Instructor
    const filterRole = await api('GET', '/admin/users?role=Instructor', { token: adminToken });
    assert.equal(filterRole.status, 200);
    assert.ok(filterRole.body.items.every((u) => u.roles.some((r) => r.toLowerCase() === 'instructor')));

    // Lọc theo trạng thái active
    const filterStatus = await api('GET', '/admin/users?status=active', { token: adminToken });
    assert.equal(filterStatus.status, 200);
    assert.ok(filterStatus.body.items.every((u) => u.status === 'active'));
  });

  test('6. [AC4] Phân trang danh sách tài khoản (mặc định 20 dòng / tùy chỉnh limit)', async () => {
    const defaultPage = await api('GET', '/admin/users', { token: adminToken });
    assert.equal(defaultPage.status, 200);
    assert.equal(defaultPage.body.pageSize, 20);
    assert.equal(defaultPage.body.page, 1);
    assert.ok(defaultPage.body.items.length <= 20);

    // Kiểm tra phân trang với limit = 2
    const p1 = await api('GET', '/admin/users?page=1&limit=2', { token: adminToken });
    assert.equal(p1.status, 200);
    assert.equal(p1.body.pageSize, 2);
    assert.equal(p1.body.items.length, 2);

    const p2 = await api('GET', '/admin/users?page=2&limit=2', { token: adminToken });
    assert.equal(p2.status, 200);
    assert.equal(p2.body.pageSize, 2);
    assert.equal(p2.body.items.length, 2);
    // User trang 2 phải khác trang 1
    assert.notEqual(p1.body.items[0].id, p2.body.items[0].id);
  });

  test('7. Bảo mật: Người không phải Admin không được truy cập API quản lý người dùng', async () => {
    const listRes = await api('GET', '/admin/users', { token: nonAdminToken });
    assert.equal(listRes.status, 403);

    const createRes = await api('POST', '/admin/users', {
      token: nonAdminToken,
      body: { email: 'hacker@test.com', fullName: 'Hacker' },
    });
    assert.equal(createRes.status, 403);
  });
});
