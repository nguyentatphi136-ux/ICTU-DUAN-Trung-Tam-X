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

describe('S2-10 (IDTTX-165 / Subtask IDTTX-195): Ph├ón c├┤ng lead cho t╞░ vß║Ñn vi├¬n', () => {
  test('Quß║ún l├╜ ─æ├áo tß║ío (TrainingManager) c├│ thß╗â ph├ón c├┤ng mß╗Öt hoß║╖c nhiß╗üu lead c├╣ng l├║c', async () => {
    // Ph├ón c├┤ng ─æß╗ông thß╗¥i lead 3 v├á lead 4 cho t╞░ vß║Ñn vi├¬n 5 (L├¬ Thß╗ï Thu H├á)
    const assignRes = await api('POST', '/api/leads/assign', {
      token: tokens.manager,
      body: {
        leadIds: ['3', '4'],
        counselorId: '5',
        note: 'Giao ch─âm s├│c kh├ích h├áng mß╗¢i tß╗½ chiß║┐n dß╗ïch tuyß╗ân sinh',
      },
    });

    assert.equal(assignRes.status, 200);
    assert.equal(assignRes.body.success, true);
    assert.equal(assignRes.body.data.updatedCount, 2);

    // Kiß╗âm tra chi tiß║┐t lead 3 sau khi ─æ╞░ß╗úc ph├ón c├┤ng
    const lead3Res = await api('GET', '/api/leads/3', { token: tokens.manager });
    assert.equal(lead3Res.status, 200);
    assert.equal(lead3Res.body.data.assignedCounselorId, '5');
    assert.equal(lead3Res.body.data.assignedCounselorName, 'L├¬ Thß╗ï Thu H├á (T╞░ vß║Ñn vi├¬n)');

    // Kiß╗âm tra chi tiß║┐t lead 4 sau khi ─æ╞░ß╗úc ph├ón c├┤ng
    const lead4Res = await api('GET', '/api/leads/4', { token: tokens.manager });
    assert.equal(lead4Res.status, 200);
    assert.equal(lead4Res.body.data.assignedCounselorId, '5');
  });

  test('Ghi nhß║¡n ─æß║ºy ─æß╗º lß╗ïch sß╗¡ chuyß╗ân giao lead (lead_assignments)', async () => {
    // Ph├ón c├┤ng lead 3
    await api('POST', '/api/leads/assign', {
      token: tokens.manager,
      body: {
        leadIds: ['3'],
        counselorId: '5',
        note: 'Ph├ón c├┤ng ─æß╗út 1',
      },
    });

    // Chuyß╗ân giao tiß║┐p lead 3 tß╗½ t╞░ vß║Ñn vi├¬n 5 sang t╞░ vß║Ñn vi├¬n 6
    await api('POST', '/api/leads/assign', {
      token: tokens.manager,
      body: {
        leadIds: ['3'],
        counselorId: '6',
        note: 'Chuyß╗ân giao do t╞░ vß║Ñn vi├¬n 5 bß║¡n',
      },
    });

    // Xem lß╗ïch sß╗¡ chuyß╗ân giao
    const historyRes = await api('GET', '/api/leads/3/assignments', { token: tokens.manager });
    assert.equal(historyRes.status, 200);
    assert.equal(historyRes.body.data.length >= 2, true);

    const latest = historyRes.body.data[0];
    assert.equal(latest.previousCounselorId, '5');
    assert.equal(latest.newCounselorId, '6');
    assert.equal(latest.assignedBy, '4'); // ID cß╗ºa TrainingManager
    assert.equal(latest.note, 'Chuyß╗ân giao do t╞░ vß║Ñn vi├¬n 5 bß║¡n');
  });

  test('T╞░ vß║Ñn vi├¬n chß╗ë nh├¼n thß║Ñy lead ─æ╞░ß╗úc giao cho m├¼nh (S2-10)', async () => {
    // T╞░ vß║Ñn vi├¬n 1 (id: 5) gß╗ìi API danh s├ích lead -> hß╗ç thß╗æng tß╗▒ ─æß╗Öng lß╗ìc chß╗ë trß║ú vß╗ü lead cß╗ºa ch├¡nh hß╗ì
    const res = await api('GET', '/api/leads', { token: tokens.admissions1 });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);

    // Mß╗ìi lead trß║ú vß╗ü phß║úi thuß╗Öc vß╗ü counselorId = 5
    for (const lead of res.body.data) {
      assert.equal(String(lead.assignedCounselorId), '5');
    }

    // T╞░ vß║Ñn vi├¬n thß╗¡ truy cß║¡p trß╗▒c tiß║┐p ID lead cß╗ºa t╞░ vß║Ñn vi├¬n kh├íc (lead 5 ─æ╞░ß╗úc giao cho t╞░ vß║Ñn vi├¬n 6)
    const forbiddenRes = await api('GET', '/api/leads/5', { token: tokens.admissions1 });
    assert.equal(forbiddenRes.status, 403);
    assert.equal(forbiddenRes.body.code, 'FORBIDDEN');
  });

  test('Chß╗ë Quß║ún l├╜ ─æ├áo tß║ío hoß║╖c Admin mß╗¢i ─æ╞░ß╗úc quyß╗ün ph├ón c├┤ng lead', async () => {
    // Giß║úng vi├¬n thß╗¡ ph├ón c├┤ng -> 403
    const resInst = await api('POST', '/api/leads/assign', {
      token: tokens.instructor,
      body: { leadIds: ['3'], counselorId: '5' },
    });
    assert.equal(resInst.status, 403);

    // Hß╗ìc vi├¬n thß╗¡ ph├ón c├┤ng -> 403
    const resStudent = await api('POST', '/api/leads/assign', {
      token: tokens.student,
      body: { leadIds: ['3'], counselorId: '5' },
    });
    assert.equal(resStudent.status, 403);

    // T╞░ vß║Ñn vi├¬n kh├┤ng thß╗â tß╗▒ ph├ón c├┤ng cho m├¼nh hoß║╖c ng╞░ß╗¥i kh├íc -> 403
    const resAdv = await api('POST', '/api/leads/assign', {
      token: tokens.admissions1,
      body: { leadIds: ['3'], counselorId: '5' },
    });
    assert.equal(resAdv.status, 403);

    // Admin c├│ quyß╗ün ph├ón c├┤ng hß╗úp lß╗ç -> 200
    const resAdmin = await api('POST', '/api/leads/assign', {
      token: tokens.admin,
      body: { leadIds: ['3'], counselorId: '6', note: 'Admin ph├ón bß╗ò' },
    });
    assert.equal(resAdmin.status, 200);
    assert.equal(resAdmin.body.success, true);
  });

  test('Lß║Ñy danh s├ích t╞░ vß║Ñn vi├¬n phß╗Ñc vß╗Ñ dropdown ph├ón c├┤ng (GET /api/leads/counselors)', async () => {
    const res = await api('GET', '/api/leads/counselors', { token: tokens.manager });
    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(Array.isArray(res.body.data), true);
    assert.equal(res.body.data.length >= 2, true);
  });

  test('B├ío lß╗ùi hß╗úp lß╗ç khi thiß║┐u th├┤ng tin hoß║╖c t╞░ vß║Ñn vi├¬n kh├┤ng tß╗ôn tß║íi', async () => {
    // Kh├┤ng c├│ leadIds
    const resEmpty = await api('POST', '/api/leads/assign', {
      token: tokens.manager,
      body: { leadIds: [], counselorId: '5' },
    });
    assert.equal(resEmpty.status, 400);

    // counselorId kh├┤ng tß╗ôn tß║íi
    const resNotFound = await api('POST', '/api/leads/assign', {
      token: tokens.manager,
      body: { leadIds: ['3'], counselorId: '9999' },
    });
    assert.equal(resNotFound.status, 404);
  });
});
