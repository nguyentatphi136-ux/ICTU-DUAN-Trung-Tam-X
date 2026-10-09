const assert = require('node:assert/strict');
const { after, before, beforeEach, describe, test } = require('node:test');
const app = require('../src/app');
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
  tokens.manager = (await login('manager@example.com', 'manager123')).token; // TrainingManager
  tokens.admissions1 = (await login('admissions@example.com', 'admissions123')).token; // id: 5 (L├¬ Thß╗ï Thu H├á)
  tokens.admissions2 = (await login('tuvan@example.com', 'tuvan123')).token; // id: 6 (Ho├áng V─ân T╞░)
  tokens.instructor = (await login('instructor@example.com', 'instructor123')).token;
  tokens.student = (await login('student@example.com', 'student123')).token;
});

after(() => new Promise((resolve) => server.close(resolve)));

beforeEach(() => {
  seedSampleLeads();
});

describe('S2-11 (IDTTX-166 / Subtask IDTTX-198): T├¼m kiß║┐m v├á lß╗ìc lead theo nhiß╗üu ─æiß╗üu kiß╗çn', () => {
  test('1. T├¼m nhanh theo t├¬n kh├ích h├áng (case-insensitive)', async () => {
    const res = await api('GET', '/api/leads?search=Minh Anh', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.length >= 1, true);
    assert.match(res.body.data[0].fullName, /Minh Anh/i);
  });

  test('2. T├¼m nhanh theo sß╗æ ─æiß╗çn thoß║íi (hß╗ù trß╗ú partial match ─æß║ºu sß╗æ)', async () => {
    const res = await api('GET', '/api/leads?search=0988', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.length >= 1, true);
    assert.match(res.body.data[0].phone, /0988/);
  });

  test('3. Lß╗ìc theo trß║íng th├íi (status: chuß║⌐n enum v├á alias tiß║┐ng Viß╗çt)', async () => {
    // Chuß║⌐n enum
    const resEnum = await api('GET', '/api/leads?status=CONSULTING', { token: tokens.manager });
    assert.equal(resEnum.status, 200);
    for (const lead of resEnum.body.data) {
      assert.equal(lead.status, 'CONSULTING');
    }

    // Alias frontend / tiß║┐ng Viß╗çt
    const resAlias = await api('GET', '/api/leads?status=counseling', { token: tokens.manager });
    assert.equal(resAlias.status, 200);
    for (const lead of resAlias.body.data) {
      assert.equal(lead.status, 'CONSULTING');
    }
  });

  test('4. Lß╗ìc theo nguß╗ôn tiß║┐p cß║¡n (source)', async () => {
    const res = await api('GET', '/api/leads?source=FACEBOOK', { token: tokens.manager });
    assert.equal(res.status, 200);
    for (const lead of res.body.data) {
      assert.equal(lead.source, 'FACEBOOK');
    }
  });

  test('5. Lß╗ìc theo ng╞░ß╗¥i phß╗Ñ tr├ích (counselorId) v├á lead ch╞░a ph├ón c├┤ng', async () => {
    // Lß╗ìc lead cß╗ºa t╞░ vß║Ñn vi├¬n 5
    const res = await api('GET', '/api/leads?counselorId=5', { token: tokens.manager });
    assert.equal(res.status, 200);
    for (const lead of res.body.data) {
      assert.equal(String(lead.assignedCounselorId), '5');
    }

    // Lß╗ìc lead ch╞░a ai phß╗Ñ tr├ích (unassigned)
    const resUnassigned = await api('GET', '/api/leads?counselorId=unassigned', { token: tokens.manager });
    assert.equal(resUnassigned.status, 200);
    for (const lead of resUnassigned.body.data) {
      assert.equal(lead.assignedCounselorId, null);
    }
  });

  test('6. Lß╗ìc theo khoß║úng thß╗¥i gian "th├íng tr╞░ß╗¢c" (datePreset=last-month) ─æß╗â t├¼m lß║íi cuß╗Öc trao ─æß╗òi', async () => {
    const res = await api('GET', '/api/leads?datePreset=last-month', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    // Dß╗» liß╗çu mß║½u khß╗ƒi tß║ío c├│ lead tß║ío tß╗½ th├íng tr╞░ß╗¢c
    assert.equal(res.body.data.length >= 2, true);
  });

  test('7. Lß╗ìc theo khoß║úng ng├áy cß╗Ñ thß╗â (createdFrom, createdTo)', async () => {
    const now = new Date();
    const todayStr = now.toISOString().split('T')[0];
    const res = await api('GET', `/api/leads?createdFrom=${todayStr}&createdTo=${todayStr}`, {
      token: tokens.manager,
    });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    for (const lead of res.body.data) {
      assert.equal(lead.createdAt.startsWith(todayStr), true);
    }
  });

  test('8. Ghi nhß║¡t k├╜ t╞░╞íng t├íc / cuß╗Öc gß╗ìi v├áo hß╗ô s╞í lead', async () => {
    const interactionRes = await api('POST', '/api/leads/1/interactions', {
      token: tokens.admissions1,
      body: {
        type: 'CALL',
        title: 'Cuß╗Öc gß╗ìi t╞░ vß║Ñn hß╗ìc ph├¡ th├íng tr╞░ß╗¢c',
        content: 'Kh├ích h├áng hß╗Åi vß╗ü ch├¡nh s├ích ─æ├│ng hß╗ìc ph├¡ chia ─æß╗út.',
        durationSeconds: 240,
      },
    });

    assert.equal(interactionRes.status, 201);
    assert.equal(interactionRes.body.success, true);
    assert.equal(interactionRes.body.data.title, 'Cuß╗Öc gß╗ìi t╞░ vß║Ñn hß╗ìc ph├¡ th├íng tr╞░ß╗¢c');

    // Kiß╗âm tra d├▓ng thß╗¥i gian (timeline) cß╗ºa lead c├│ cuß╗Öc trao ─æß╗òi
    const detailRes = await api('GET', '/api/leads/1', { token: tokens.admissions1 });
    assert.equal(detailRes.status, 200);
    const topTimeline = detailRes.body.data.timeline[0];
    assert.equal(topTimeline.title, 'Cuß╗Öc gß╗ìi t╞░ vß║Ñn hß╗ìc ph├¡ th├íng tr╞░ß╗¢c');
    assert.equal(topTimeline.content, 'Kh├ích h├áng hß╗Åi vß╗ü ch├¡nh s├ích ─æ├│ng hß╗ìc ph├¡ chia ─æß╗út.');
  });

  test('9. Lß║Ñy thß╗æng k├¬ phß╗àu tuyß╗ân sinh (GET /api/leads/stats)', async () => {
    const res = await api('GET', '/api/leads/stats', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(typeof res.body.data.total, 'number');
    assert.equal(typeof res.body.data.new, 'number');
    assert.equal(typeof res.body.data.consulting, 'number');
  });

  test('10. Hß╗ù trß╗ú ph├ón trang v├á sß║»p xß║┐p dß╗» liß╗çu (page, limit, sortBy, sortOrder)', async () => {
    const res = await api('GET', '/api/leads?page=1&limit=2&sortBy=fullName&sortOrder=asc', {
      token: tokens.manager,
    });
    assert.equal(res.status, 200);
    assert.equal(res.body.data.length, 2);
    assert.equal(res.body.pagination.page, 1);
    assert.equal(res.body.pagination.limit, 2);
    assert.equal(res.body.pagination.total >= 5, true);
  });
});
