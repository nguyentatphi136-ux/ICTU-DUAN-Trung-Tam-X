const assert = require('node:assert/strict');
const { after, before, beforeEach, describe, test } = require('node:test');
const app = require('../src/app');
const { LEAD_SOURCE, LEAD_STATUS } = require('../src/constants/leads');
const { clearLeads, getAllLeads } = require('../src/data/leads');
const { consultationLimiter } = require('../src/routes/consultation.routes');

let server;
let baseUrl;

async function postConsultation(body, { endpoint = '/api/public/consultation-requests', headers = {} } = {}) {
  const reqHeaders = {
    'Content-Type': 'application/json',
    ...headers,
  };

  const response = await fetch(`${baseUrl}${endpoint}`, {
    method: 'POST',
    headers: reqHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const responseBody = await response.json();
  return { status: response.status, body: responseBody, headers: response.headers };
}

before(async () => {
  server = app.listen(0);
  await new Promise((resolve) => server.once('listening', resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

after(() => new Promise((resolve) => server.close(resolve)));

beforeEach(() => {
  clearLeads();
  consultationLimiter.reset();
});

describe('S2-08 ΓÇô EP-03: ─É─âng k├╜ t╞░ vß║Ñn c├┤ng khai (Public Consultation Requests)', () => {
  test('TC01: Gß╗¡i ─æß║ºy ─æß╗º th├┤ng tin hß╗úp lß╗ç -> tß║ío Lead th├ánh c├┤ng vß╗¢i trß║íng th├íi Mß╗¢i (201 Created)', async () => {
    const payload = {
      fullName: 'Nguyß╗àn V─ân A',
      phone: '0912345678',
      email: 'nguyenvana@gmail.com',
      course: 'Lß║¡p tr├¼nh Web Front-end',
      message: 'T├┤i muß╗æn ─æ╞░ß╗úc t╞░ vß║Ñn lß╗Ö tr├¼nh hß╗ìc cho ng╞░ß╗¥i mß╗¢i bß║»t ─æß║ºu',
      preferredTime: 'Buß╗òi tß╗æi c├íc ng├áy trong tuß║ºn',
    };

    const res = await postConsultation(payload);
    assert.equal(res.status, 201);
    assert.equal(res.body.success, true);
    assert.match(res.body.message, /Cß║úm ╞ín bß║ín ─æ├ú ─æ─âng k├╜ t╞░ vß║Ñn/);
    assert.ok(res.body.data);
    assert.equal(res.body.data.fullName, 'Nguyß╗àn V─ân A');
    assert.equal(res.body.data.phone, '0912345678');
    assert.equal(res.body.data.email, 'nguyenvana@gmail.com');
    assert.equal(res.body.data.course, 'Lß║¡p tr├¼nh Web Front-end');
    assert.equal(res.body.data.status, LEAD_STATUS.NEW);
    assert.ok(res.body.data.createdAt);

    // Kiß╗âm tra trong data store
    const storedLeads = getAllLeads();
    assert.equal(storedLeads.length, 1);
    assert.equal(storedLeads[0].fullName, 'Nguyß╗àn V─ân A');
    assert.equal(storedLeads[0].source, LEAD_SOURCE.WEBSITE);
    assert.equal(storedLeads[0].status, LEAD_STATUS.NEW);
  });

  test('TC02: Gß╗¡i chß╗ë vß╗¢i th├┤ng tin tß╗æi thiß╗âu bß║»t buß╗Öc (Hß╗ì t├¬n + Sß╗æ ─æiß╗çn thoß║íi) -> th├ánh c├┤ng (201 Created)', async () => {
    const payload = {
      fullName: 'Trß║ºn Thß╗ï B',
      phone: '0987654321',
    };

    const res = await postConsultation(payload);
    assert.equal(res.status, 201);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.fullName, 'Trß║ºn Thß╗ï B');
    assert.equal(res.body.data.phone, '0987654321');
    assert.equal(res.body.data.email, null);
    assert.equal(res.body.data.status, LEAD_STATUS.NEW);
  });

  test('TC03: Hß╗ù trß╗ú t├¬n tr╞░ß╗¥ng dß║íng snake_case (full_name) v├á alias /api/consultation-requests', async () => {
    const payload = {
      full_name: 'L├¬ Ho├áng C',
      phone: '+84905123456',
      notes: 'Quan t├óm kh├│a IELTS',
    };

    const res = await postConsultation(payload, { endpoint: '/api/consultation-requests' });
    assert.equal(res.status, 201);
    assert.equal(res.body.success, true);
    assert.equal(res.body.data.fullName, 'L├¬ Ho├áng C');
  });

  test('TC04: Tß╗½ chß╗æi khi thiß║┐u hß╗ì v├á t├¬n hoß║╖c hß╗ì t├¬n chß╗ë chß╗⌐a khoß║úng trß║»ng (400 Bad Request)', async () => {
    const res1 = await postConsultation({ phone: '0912345678' });
    assert.equal(res1.status, 400);
    assert.equal(res1.body.success, false);
    assert.equal(res1.body.code, 'VALIDATION_ERROR');
    assert.ok(res1.body.errors.some((err) => err.includes('hß╗ì v├á t├¬n')));

    const res2 = await postConsultation({ fullName: '   ', phone: '0912345678' });
    assert.equal(res2.status, 400);
    assert.ok(res2.body.errors.some((err) => err.includes('hß╗ì v├á t├¬n')));
  });

  test('TC05: Tß╗½ chß╗æi khi thiß║┐u sß╗æ ─æiß╗çn thoß║íi hoß║╖c sß╗æ ─æiß╗çn thoß║íi chß╗ë chß╗⌐a khoß║úng trß║»ng (400 Bad Request)', async () => {
    const res1 = await postConsultation({ fullName: 'Phß║ím V─ân D' });
    assert.equal(res1.status, 400);
    assert.equal(res1.body.success, false);
    assert.equal(res1.body.code, 'VALIDATION_ERROR');
    assert.ok(res1.body.errors.some((err) => err.includes('sß╗æ ─æiß╗çn thoß║íi')));

    const res2 = await postConsultation({ fullName: 'Phß║ím V─ân D', phone: '   ' });
    assert.equal(res2.status, 400);
    assert.ok(res2.body.errors.some((err) => err.includes('sß╗æ ─æiß╗çn thoß║íi')));
  });

  test('TC06: Tß╗½ chß╗æi sß╗æ ─æiß╗çn thoß║íi sai ─æß╗ïnh dß║íng (chß╗⌐a chß╗» c├íi, kh├┤ng ─æß╗º sß╗æ, sai tiß╗ün tß╗æ) (400 Bad Request)', async () => {
    const invalidPhones = ['abc1234567', '09123', '012345678912345678901', '1234567890'];

    for (const phone of invalidPhones) {
      const res = await postConsultation({ fullName: 'Ng╞░ß╗¥i D├╣ng Test', phone });
      assert.equal(res.status, 400, `Phone ${phone} should be rejected`);
      assert.ok(res.body.errors.some((err) => err.includes('Sß╗æ ─æiß╗çn thoß║íi')));
    }
  });

  test('TC07: Tß╗½ chß╗æi email sai ─æß╗ïnh dß║íng nß║┐u ng╞░ß╗¥i d├╣ng c├│ nhß║¡p email (400 Bad Request)', async () => {
    const invalidEmails = ['invalid-email', 'abc@', '@domain.com', 'test@domain'];

    for (const email of invalidEmails) {
      const res = await postConsultation({
        fullName: 'Ng╞░ß╗¥i D├╣ng Test',
        phone: '0912345678',
        email,
      });
      assert.equal(res.status, 400, `Email ${email} should be rejected`);
      assert.ok(res.body.errors.some((err) => err.includes('Email')));
    }
  });

  test('TC08: Kh├┤ng y├¬u cß║ºu x├íc thß╗▒c hoß║╖c ─æ─âng nhß║¡p (Hoß║ít ─æß╗Öng b├¼nh th╞░ß╗¥ng kh├┤ng c├│ Authorization header)', async () => {
    const res = await postConsultation(
      { fullName: 'Kh├ích V├úng Lai', phone: '0912345678' },
      { headers: { Authorization: '' } }
    );
    assert.equal(res.status, 201);
    assert.equal(res.body.success, true);
  });

  test('TC09: Chß╗æng Mass Assignment - Kh├┤ng cho ph├⌐p kh├ích tß╗▒ chß╗ë ─æß╗ïnh status, assignedCounselorId hoß║╖c id', async () => {
    const payload = {
      id: '9999',
      fullName: 'Hacker Thß╗¡ Nghiß╗çm',
      phone: '0912345678',
      status: 'WON', // Cß╗æ t├¼nh ─æß╗òi sang trß║íng th├íi Chß╗æt
      assignedCounselorId: 1, // Cß╗æ t├¼nh tß╗▒ g├ín nh├ón vi├¬n
      convertedStudentId: 5,
      rejectReason: 'Kh├┤ng c├│',
    };

    const res = await postConsultation(payload);
    assert.equal(res.status, 201);

    // Dß╗» liß╗çu tß║ío ra phß║úi ─æ╞░ß╗úc bß║úo vß╗ç
    const storedLead = getAllLeads()[0];
    assert.notEqual(storedLead.id, '9999');
    assert.equal(storedLead.status, LEAD_STATUS.NEW);
    assert.equal(storedLead.assignedCounselorId, null);
    assert.equal(storedLead.convertedStudentId, null);
    assert.equal(storedLead.rejectReason, null);
  });

  test('TC10: L├ám sß║ích nß╗Öi dung (Sanitization) - Loß║íi bß╗Å c├íc thß║╗ ─æß╗Öc hß║íi XSS trong hß╗ì t├¬n v├á nß╗Öi dung', async () => {
    const payload = {
      fullName: '<script>alert("XSS")</script>Nguyß╗àn V─ân E<b> bold</b>',
      phone: '0912345678',
      message: 'T├┤i muß╗æn t╞░ vß║Ñn <iframe src="evil.com"></iframe> ngay!',
    };

    const res = await postConsultation(payload);
    assert.equal(res.status, 201);
    assert.equal(res.body.data.fullName, 'Nguyß╗àn V─ân E bold');

    const storedLead = getAllLeads()[0];
    assert.equal(storedLead.fullName, 'Nguyß╗àn V─ân E bold');
    assert.equal(storedLead.notes, 'T├┤i muß╗æn t╞░ vß║Ñn  ngay!');
    assert.ok(!storedLead.notes.includes('<iframe'));
    assert.ok(!storedLead.fullName.includes('<script'));
  });

  test('TC11: Chß╗æng gß╗¡i tr├╣ng lß║╖p nhiß╗üu lß║ºn trong thß╗¥i gian ngß║»n (Debounce double-click)', async () => {
    const payload = {
      fullName: 'Kh├ích H├áng Nhanh Tay',
      phone: '0933112233',
      message: 'Bß║Ñm gß╗¡i li├¬n tiß║┐p 2 lß║ºn',
    };

    // Lß║ºn 1: Th├ánh c├┤ng
    const res1 = await postConsultation(payload);
    assert.equal(res1.status, 201);

    // Lß║ºn 2 (ngay lß║¡p tß╗⌐c trong v├▓ng 10 gi├óy vß╗¢i c├╣ng sß╗æ ─æiß╗çn thoß║íi): Bß╗ï tß╗½ chß╗æi ─æß╗â chß╗æng tß║ío 2 bß║ún ghi tr├╣ng
    const res2 = await postConsultation(payload);
    assert.equal(res2.status, 409);
    assert.equal(res2.body.code, 'DUPLICATE_SUBMISSION');
    assert.match(res2.body.message, /kh├┤ng bß║Ñm gß╗¡i li├¬n tß╗Ñc/);

    // Chß╗ë c├│ ─æ├║ng 1 bß║ún ghi ─æ╞░ß╗úc tß║ío trong CSDL/bß╗Ö nhß╗¢
    const stored = getAllLeads().filter((l) => l.phone === '0933112233');
    assert.equal(stored.length, 1);
  });

  test('TC12: Chß╗æng spam bß║▒ng Rate Limiter (Qu├í 5 requests tß╗½ c├╣ng 1 IP trong 1 ph├║t -> 429)', async () => {
    // Gß╗¡i 5 request hß╗úp lß╗ç li├¬n tiß║┐p vß╗¢i c├íc sß╗æ ─æiß╗çn thoß║íi kh├íc nhau tß╗½ c├╣ng 1 IP
    for (let i = 1; i <= 5; i++) {
      const res = await postConsultation({
        fullName: `Kh├ích Thß╗¡ Nghiß╗çm ${i}`,
        phone: `091234560${i}`,
      });
      assert.equal(res.status, 201, `Request ${i} should succeed`);
    }

    // Request thß╗⌐ 6: Phß║úi bß╗ï chß║╖n bß╗ƒi Rate Limiter
    const blockedRes = await postConsultation({
      fullName: 'Kh├ích Thß╗¡ Nghiß╗çm 6',
      phone: '0912345606',
    });

    assert.equal(blockedRes.status, 429);
    assert.equal(blockedRes.body.success, false);
    assert.equal(blockedRes.body.code, 'RATE_LIMIT_EXCEEDED');
    assert.match(blockedRes.body.message, /qu├í nhiß╗üu y├¬u cß║ºu/);
  });
});
