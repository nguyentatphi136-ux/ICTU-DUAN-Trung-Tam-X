const assert = require('node:assert/strict');
const { after, before, describe, test } = require('node:test');
const express = require('express');
const app = require('../src/app');

/**
 * Kiểm thử tính năng: S1-01 AC3 - Khóa tạm 15 phút sau 5 lần sai liên tiếp
 */
let server;
let baseUrl;

async function postLogin(email, password) {
  const response = await fetch(`${baseUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  return { status: response.status, body: await response.json() };
}

before(async () => {
  server = app.listen(0);
  await new Promise((resolve) => server.once('listening', resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

after(() => new Promise((resolve) => server.close(resolve)));

describe('S1-01 AC3: Khóa tạm 15 phút sau 5 lần sai liên tiếp', () => {
  const targetEmail = 'lockout_test@example.com';

  test('Lần 1-4: Nhập sai mật khẩu trả về HTTP 401 kèm số lần thử còn lại', async () => {
    for (let i = 1; i <= 4; i++) {
      const res = await postLogin(targetEmail, 'WrongPassword');
      assert.equal(res.status, 401);
      assert.equal(res.body.success, false);
      assert.equal(res.body.remainingAttempts, 5 - i);
    }
  });

  test('Lần 5: Nhập sai mật khẩu liên tiếp lần thứ 5 -> Khóa tạm 15 phút (HTTP 423)', async () => {
    const res = await postLogin(targetEmail, 'WrongPassword');
    assert.equal(res.status, 423);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'ACCOUNT_TEMPORARILY_LOCKED');
    assert.ok(res.body.message.includes('15 phút'));
    assert.equal(res.body.remainingMinutes, 15);
  });

  test('Trong thời gian 15 phút bị khóa: Tiếp tục thử (kể cả mật khẩu đúng) đều bị từ chối 423', async () => {
    const res = await postLogin(targetEmail, 'Admin@123');
    assert.equal(res.status, 423);
    assert.equal(res.body.code, 'ACCOUNT_TEMPORARILY_LOCKED');
    assert.ok(res.body.message.includes('15 phút'));
  });

  test('Đăng nhập đúng khi chưa đạt 5 lần sai -> Đăng nhập thành công và reset bộ đếm', async () => {
    const freshUser = 'instructor@example.com';
    // Thử sai 2 lần
    await postLogin(freshUser, 'Wrong1');
    await postLogin(freshUser, 'Wrong2');

    // Lần 3 đăng nhập đúng mật khẩu
    const okRes = await postLogin(freshUser, 'instructor123');
    assert.equal(okRes.status, 200);
    assert.equal(okRes.body.success, true);

    // Thử sai lại lần nữa -> đếm lại từ 1 (còn 4 lần thử)
    const afterRes = await postLogin(freshUser, 'WrongAgain');
    assert.equal(afterRes.status, 401);
    assert.equal(afterRes.body.remainingAttempts, 4);
  });
});
