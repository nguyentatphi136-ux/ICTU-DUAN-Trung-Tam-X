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

describe('S2-06 ΓÇô EP-02: Quß║ún l├╜ ch╞░╞íng tr├¼nh ─æ├áo tß║ío & M├┤n hß╗ìc ti├¬n quyß║┐t', () => {
  // =========================================================================
  // 1. CHß╗¿C N─éNG TH├èM M├öN Hß╗îC V├ÇO CH╞»╞áNG TR├îNH
  // =========================================================================
  test('TEST 1: Th├¬m m├┤n hß╗ìc hß╗úp lß╗ç v├áo ch╞░╞íng tr├¼nh -> Th├ánh c├┤ng (201 Created)', async () => {
    // Ch╞░╞íng tr├¼nh 1 (Frontend) hiß╗çn c├│ m├┤n 1, 2, 3, 5. Th├¬m m├┤n 4 (UI/UX - WEB104)
    const res = await api('POST', '/api/training-programs/1/courses', {
      token: tokens.trainingManager,
      body: { courseId: 4 },
    });

    assert.equal(res.status, 201);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.subjectId, 4);
    assert.equal(res.body.data.subjectCode, 'WEB104');
    assert.equal(res.body.data.orderIndex, 5); // Tß╗▒ ─æß╗Öng g├ín thß╗⌐ tß╗▒ tiß║┐p theo

    // Kiß╗âm tra danh s├ích m├┤n hß╗ìc sau khi th├¬m
    const listRes = await api('GET', '/api/training-programs/1/courses');
    assert.equal(listRes.status, 200);
    assert.equal(listRes.body.totalCourses, 5);
  });

  test('TEST 2: Th├¬m m├┤n hß╗ìc ─æ├ú tß╗ôn tß║íi trong ch╞░╞íng tr├¼nh -> Tß╗½ chß╗æi, kh├┤ng tß║ío duplicate (409 Conflict)', async () => {
    // M├┤n 1 (HTML/CSS) ─æ├ú c├│ trong ch╞░╞íng tr├¼nh 1
    const res = await api('POST', '/api/training-programs/1/courses', {
      token: tokens.trainingManager,
      body: { courseId: 1 },
    });

    assert.equal(res.status, 409);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'SUBJECT_ALREADY_IN_PROGRAM');
  });

  // =========================================================================
  // 2. CHß╗¿C N─éNG Gß╗á M├öN Hß╗îC KHß╗ÄI CH╞»╞áNG TR├îNH
  // =========================================================================
  test('TEST 3: Gß╗í m├┤n hß╗ìc ─æang thuß╗Öc ch╞░╞íng tr├¼nh -> Th├ánh c├┤ng (200 OK) v├á reorder li├¬n tß╗Ñc', async () => {
    // Gß╗í m├┤n 3 (Git & GitHub, orderIndex: 3) khß╗Åi ch╞░╞íng tr├¼nh 1
    const res = await api('DELETE', '/api/training-programs/1/courses/3', {
      token: tokens.trainingManager,
    });

    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);

    // Kiß╗âm tra danh s├ích c├▓n 3 m├┤n v├á thß╗⌐ tß╗▒ ─æ╞░ß╗úc ─æ├ính sß╗æ li├¬n tß╗Ñc 1, 2, 3
    const listRes = await api('GET', '/api/training-programs/1/courses');
    assert.equal(listRes.body.totalCourses, 3);
    const orders = listRes.body.data.map((c) => c.orderIndex);
    assert.deepEqual(orders, [1, 2, 3]);
  });

  test('TEST 4: Gß╗í m├┤n hß╗ìc kh├┤ng thuß╗Öc ch╞░╞íng tr├¼nh -> Trß║ú lß╗ùi ph├╣ hß╗úp (404 Not Found)', async () => {
    // M├┤n 6 (IELTS Foundation) kh├┤ng thuß╗Öc ch╞░╞íng tr├¼nh 1 (Frontend)
    const res = await api('DELETE', '/api/training-programs/1/courses/6', {
      token: tokens.trainingManager,
    });

    assert.equal(res.status, 404);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'SUBJECT_NOT_IN_PROGRAM');
  });

  test('TEST 4B: Gß╗í m├┤n hß╗ìc ─æang l├ám ti├¬n quyß║┐t cho m├┤n kh├íc -> Tß╗½ chß╗æi (400 Bad Request)', async () => {
    // M├┤n 1 (HTML/CSS) ─æang l├á ti├¬n quyß║┐t cß╗ºa m├┤n 2 (JavaScript) trong ch╞░╞íng tr├¼nh 1
    const res = await api('DELETE', '/api/training-programs/1/courses/1', {
      token: tokens.trainingManager,
    });

    assert.equal(res.status, 400);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'IS_PREREQUISITE_OF_OTHERS');
    assert.match(res.body.message, /m├┤n ti├¬n quyß║┐t/);
  });

  // =========================================================================
  // 3. CHß╗¿C N─éNG Sß║«P Xß║╛P THß╗¿ Tß╗░ M├öN Hß╗îC (REORDER)
  // =========================================================================
  test('TEST 5: Sß║»p xß║┐p lß║íi thß╗⌐ tß╗▒ c├íc m├┤n hß╗ìc trong ch╞░╞íng tr├¼nh -> Thß╗⌐ tß╗▒ ─æ╞░ß╗úc l╞░u ch├¡nh x├íc (200 OK)', async () => {
    // Ch╞░╞íng tr├¼nh 4 (Data) c├│: 11 (data-basic, order 1), 12 (excel, order 2, prereq: 11), 13 (sql, order 3, prereq: 11), 14 (powerbi, order 4, prereq: 12)
    // Sß║»p xß║┐p ─æß╗òi chß╗ù: 11 (order 1) -> 13 (order 2) -> 12 (order 3) -> 14 (order 4)
    // Cß║ú 12 v├á 13 ─æß╗üu chß╗ë phß╗Ñ thuß╗Öc 11, v├á 14 phß╗Ñ thuß╗Öc 12, n├¬n thß╗⌐ tß╗▒ n├áy ho├án to├án hß╗úp lß╗ç!
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

  test('TEST 6: Gß╗¡i request order c├│ duplicate courseId -> Tß╗½ chß╗æi (400 Bad Request)', async () => {
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

  test('TEST 7: Cß║¡p nhß║¡t order vß╗¢i courseId kh├┤ng thuß╗Öc ch╞░╞íng tr├¼nh -> Tß╗½ chß╗æi (400 Bad Request)', async () => {
    // M├┤n 1 (Frontend) kh├┤ng thuß╗Öc ch╞░╞íng tr├¼nh 4 (Data)
    const res = await api('PUT', '/api/training-programs/4/courses/order', {
      token: tokens.trainingManager,
      body: {
        courses: [
          { courseId: 11, order: 1 },
          { courseId: 1, order: 2 }, // M├┤n 1 kh├┤ng thuß╗Öc ch╞░╞íng tr├¼nh 4
        ],
      },
    });

    assert.equal(res.status, 400);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'SUBJECT_NOT_IN_PROGRAM');
  });

  test('TEST 7B: Sß║»p xß║┐p ─æß║╖t m├┤n ti├¬n quyß║┐t ─æß╗⌐ng SAU m├┤n hß╗ìc cß║ºn n├│ -> Tß╗½ chß╗æi (400 Bad Request)', async () => {
    // Trong CT 1: M├┤n 1 (HTML/CSS) l├á ti├¬n quyß║┐t cß╗ºa m├┤n 2 (JavaScript).
    // Nß║┐u xß║┐p m├┤n 2 ─æß╗⌐ng ß╗ƒ order 1, c├▓n m├┤n 1 ─æß╗⌐ng ß╗ƒ order 2 -> Vi phß║ím lß╗Ö tr├¼nh!
    const res = await api('PUT', '/api/training-programs/1/courses/order', {
      token: tokens.trainingManager,
      body: {
        courses: [
          { courseId: 2, order: 1 }, // Hß╗ìc JavaScript tr╞░ß╗¢c khi hß╗ìc HTML/CSS
          { courseId: 1, order: 2 },
          { courseId: 3, order: 3 },
          { courseId: 5, order: 4 },
        ],
      },
    });

    assert.equal(res.status, 400);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'PREREQUISITE_ORDER_VIOLATION');
    assert.match(res.body.message, /phß║úi ─æß╗⌐ng tr╞░ß╗¢c m├┤n/);
  });

  // =========================================================================
  // 4. CHß╗¿C N─éNG KHAI B├üO M├öN TI├èN QUYß║╛T (PREREQUISITE)
  // =========================================================================
  test('TEST 8: Tß║ío prerequisite hß╗úp lß╗ç -> Th├ánh c├┤ng (200 OK)', async () => {
    // Trong CT 1: M├┤n 3 (Git, order 3) c├│ thß╗â ─æß║╖t m├┤n 1 (HTML/CSS, order 1) l├ám ti├¬n quyß║┐t
    const res = await api('PUT', '/api/training-programs/1/courses/3/prerequisite', {
      token: tokens.trainingManager,
      body: { prerequisiteId: 1 },
    });

    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.prerequisiteSubjectId, 1);
  });

  test('TEST 9: M├┤n hß╗ìc l├ám prerequisite cho ch├¡nh n├│ -> Tß╗½ chß╗æi (400 Bad Request)', async () => {
    const res = await api('PUT', '/api/training-programs/1/courses/3/prerequisite', {
      token: tokens.trainingManager,
      body: { prerequisiteId: 3 }, // Tß╗▒ l├ám ti├¬n quyß║┐t cho ch├¡nh m├¼nh
    });

    assert.equal(res.status, 400);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'SELF_PREREQUISITE');
  });

  test('TEST 10: Gß╗í bß╗Å m├┤n ti├¬n quyß║┐t bß║▒ng null hoß║╖c endpoint DELETE -> Th├ánh c├┤ng (200 OK)', async () => {
    // M├┤n 2 ─æang c├│ prereq l├á 1. Gß╗í prereq
    const res = await api('DELETE', '/api/training-programs/1/courses/2/prerequisite', {
      token: tokens.trainingManager,
    });

    assert.equal(res.status, 200);
    assert.equal(res.body.success, true);

    const listRes = await api('GET', '/api/training-programs/1/courses');
    const subj2 = listRes.body.data.find((c) => c.subjectId === 2);
    assert.equal(subj2.prerequisiteSubjectId, null);
  });

  test('TEST 11: Tß║ío v├▓ng lß║╖p prerequisite (Circular Dependency) -> Tß╗½ chß╗æi (400 Bad Request)', async () => {
    // CT 1: M├┤n 1 -> M├┤n 2 (2 cß║ºn 1). M├┤n 2 -> M├┤n 5 (5 cß║ºn 2).
    // Nß║┐u cß╗æ t├¼nh ─æß║╖t 1 cß║ºn 5 -> Tß║ío chu tr├¼nh: 1 -> 5 -> 2 -> 1!
    const res = await api('PUT', '/api/training-programs/1/courses/1/prerequisite', {
      token: tokens.trainingManager,
      body: { prerequisiteId: 5 },
    });

    assert.equal(res.status, 400);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'PREREQUISITE_ORDER_INVALID'); // Thß╗⌐ tß╗▒ 5 > 1 hoß║╖c CIRCULAR
  });

  // =========================================================================
  // 5. PH├éN QUYß╗ÇN (AUTHORIZATION)
  // =========================================================================
  test('TEST 12: User kh├┤ng c├│ quyß╗ün Quß║ún l├╜ ─æ├áo tß║ío (Student, Instructor) cß╗æ gß║»ng sß╗¡a ch╞░╞íng tr├¼nh -> 403 Forbidden', async () => {
    // Hß╗ìc vi├¬n thß╗¡ th├¬m m├┤n hß╗ìc
    const studentRes = await api('POST', '/api/training-programs/1/courses', {
      token: tokens.student,
      body: { courseId: 4 },
    });
    assert.equal(studentRes.status, 403);
    assert.equal(studentRes.body.code, 'FORBIDDEN');

    // Giß║úng vi├¬n thß╗¡ ─æß╗òi thß╗⌐ tß╗▒
    const instructorRes = await api('PUT', '/api/training-programs/1/courses/order', {
      token: tokens.instructor,
      body: { courses: [{ courseId: 1, order: 1 }] },
    });
    assert.equal(instructorRes.status, 403);
    assert.equal(instructorRes.body.code, 'FORBIDDEN');

    // Kh├┤ng c├│ token
    const noAuthRes = await api('DELETE', '/api/training-programs/1/courses/3');
    assert.equal(noAuthRes.status, 401);
  });

  test('TEST 12B: Admin c┼⌐ng c├│ quyß╗ün quß║ún trß╗ï ch╞░╞íng tr├¼nh ─æ├áo tß║ío -> Th├ánh c├┤ng (201/200)', async () => {
    const res = await api('POST', '/api/training-programs/1/courses', {
      token: tokens.admin,
      body: { courseId: 4 },
    });
    assert.equal(res.status, 201);
  });

  // =========================================================================
  // 6. VALIDATION & KH├öNG Tß╗ÆN Tß║áI
  // =========================================================================
  test('TEST 13: Program kh├┤ng tß╗ôn tß║íi -> Trß║ú lß╗ùi ph├╣ hß╗úp (404 Not Found)', async () => {
    const res = await api('GET', '/api/training-programs/9999/courses');
    assert.equal(res.status, 404);
    assert.equal(res.body.success, false);

    const postRes = await api('POST', '/api/training-programs/9999/courses', {
      token: tokens.trainingManager,
      body: { courseId: 1 },
    });
    assert.equal(postRes.status, 404);
  });

  test('TEST 14: Course kh├┤ng tß╗ôn tß║íi -> Trß║ú lß╗ùi ph├╣ hß╗úp (404 Not Found)', async () => {
    const res = await api('POST', '/api/training-programs/1/courses', {
      token: tokens.trainingManager,
      body: { courseId: 9999 },
    });
    assert.equal(res.status, 404);
    assert.equal(res.body.success, false);
    assert.equal(res.body.code, 'SUBJECT_NOT_FOUND');
  });
});
