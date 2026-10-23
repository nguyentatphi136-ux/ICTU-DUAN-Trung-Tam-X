// Danh mục lớp học và thông tin lớp phục vụ quản lý hồ sơ học viên (Story S3-04 trở đi)
const S = (code, name, sessions, teacher, tas = []) => ({ code, name, sessions, teacher, tas });

const web = (t) => [
  S('GIT-01', 'Git và làm việc nhóm', 4, t),
  S('WEB-01', 'HTML & CSS nền tảng', 12, t),
  S('WEB-02', 'JavaScript cơ bản', 14, t),
  S('DB-01', 'Cơ sở dữ liệu', 10, t),
  S('WEB-03', 'React', 12, t),
  S('BE-01', 'Node.js và API', 12, t),
];

const data = (t) => [
  S('GIT-01', 'Git và làm việc nhóm', 4, t),
  S('DB-01', 'Cơ sở dữ liệu', 10, t),
  S('PY-01', 'Python cho phân tích', 14, t),
  S('STAT-01', 'Thống kê ứng dụng', 10, t),
  S('VIS-01', 'Trực quan hoá dữ liệu', 10, t),
];

const uiux = (t) => [
  S('UX-01', 'Nguyên lý thiết kế', 8, t),
  S('UX-02', 'Nghiên cứu người dùng', 8, t),
  S('UX-03', 'Thiết kế với Figma', 12, t),
  S('UX-04', 'Prototype và kiểm thử', 8, t),
  S('GIT-01', 'Git và làm việc nhóm', 4, t),
];

const java = (t) => [
  S('GIT-01', 'Git và làm việc nhóm', 4, t),
  S('JV-01', 'Java cơ bản', 14, t),
  S('DB-01', 'Cơ sở dữ liệu', 10, t),
  S('JV-02', 'Spring Boot', 16, t),
  S('JV-03', 'Kiểm thử và triển khai', 12, t),
];

const test = (t) => [
  S('TEST-01', 'Kiểm thử cơ bản', 8, t),
  S('TEST-02', 'Kiểm thử tự động', 12, t),
  S('TEST-03', 'Kiểm thử API', 8, t),
  S('GIT-01', 'Git và làm việc nhóm', 4, t),
];

const k16 = web();
k16[0] = S('GIT-01', 'Git và làm việc nhóm', 4, 'VU DUC LONG', ['PHAM GIA HUY']);
k16[1] = S('WEB-01', 'HTML & CSS nền tảng', 12, 'VU DUC LONG', ['NGO BAO TU']);
k16[2] = S('WEB-02', 'JavaScript cơ bản', 14, 'VU DUC LONG');
k16[4] = S('WEB-03', 'React', 12, 'DANG MINH SON');

const SEED_CLASSES = [
  {
    code: 'WEB-K15',
    program: 'Lập trình web Full-stack',
    start: '2026-08-12',
    max: 30,
    mode: 'Trực tiếp',
    status: 'Đang học',
    done: 22,
    slot: 'Tối 2-4-6',
    room: 'P.301',
    subjects: web('VU DUC LONG').map((s, i) => (i === 0 ? { ...s, tas: ['PHAM GIA HUY'] } : s)),
  },
  {
    code: 'DATA-K07',
    program: 'Phân tích dữ liệu',
    start: '2026-07-20',
    max: 25,
    mode: 'Kết hợp',
    status: 'Đang học',
    done: 30,
    slot: 'Tối 3-5-7',
    room: 'P.301',
    subjects: data('LE HOANG NAM'),
  },
  {
    code: 'UIUX-K09',
    program: 'Thiết kế UI/UX',
    start: '2026-07-02',
    max: 20,
    mode: 'Trực tiếp',
    status: 'Đang học',
    done: 26,
    slot: 'Chiều 2-4-6',
    room: 'P.302',
    subjects: uiux('NGO THI THU'),
  },
  {
    code: 'JAVA-K12',
    program: 'Lập trình Java Backend',
    start: '2026-10-15',
    max: 30,
    mode: 'Trực tiếp',
    status: 'Chuẩn bị',
    done: 0,
    slot: 'Tối 2-4-6',
    room: 'Lab 1',
    subjects: java(),
  },
  {
    code: 'WEB-K16',
    program: 'Lập trình web Full-stack',
    start: '2026-10-20',
    max: 30,
    mode: 'Kết hợp',
    status: 'Chuẩn bị',
    done: 0,
    slot: 'Tối 3-5-7',
    room: 'Meet WEB',
    subjects: k16,
  },
  {
    code: 'TEST-K06',
    program: 'Kiểm thử phần mềm',
    start: '2026-11-02',
    max: 25,
    mode: 'Trực tuyến',
    status: 'Chuẩn bị',
    done: 0,
    slot: 'Tối 2-4-6',
    subjects: test('PHAM THI VY'),
  },
  {
    code: 'JAVA-K11',
    program: 'Lập trình Java Backend',
    start: '2026-03-10',
    max: 30,
    mode: 'Trực tiếp',
    status: 'Kết thúc',
    done: 56,
    slot: 'Tối 3-5-7',
    room: 'Lab 1',
    subjects: java('DANG MINH SON'),
  },
  {
    code: 'TEST-K05',
    program: 'Kiểm thử phần mềm',
    start: '2026-05-05',
    max: 25,
    mode: 'Trực tuyến',
    status: 'Huỷ',
    done: 4,
    slot: 'Tối 3-5-7',
    subjects: test('PHAM THI VY'),
  },
  {
    code: 'WEB-K14',
    program: 'Lập trình web Full-stack',
    start: '2026-03-02',
    max: 30,
    mode: 'Trực tiếp',
    status: 'Kết thúc',
    done: 64,
    slot: 'Tối 2-4-6',
    room: 'P.301',
    subjects: web('VU DUC LONG'),
  },
  {
    code: 'DATA-K06',
    program: 'Phân tích dữ liệu',
    start: '2026-02-16',
    max: 25,
    mode: 'Kết hợp',
    status: 'Kết thúc',
    done: 48,
    slot: 'Tối 3-5-7',
    subjects: data('LE HOANG NAM'),
  },
  {
    code: 'UIUX-K08',
    program: 'Thiết kế UI/UX',
    start: '2025-09-18',
    max: 20,
    mode: 'Trực tiếp',
    status: 'Kết thúc',
    done: 40,
    slot: 'Chiều 2-4-6',
    subjects: uiux('NGO THI THU'),
  },
  {
    code: 'WEB-K12',
    program: 'Lập trình web Full-stack',
    start: '2026-02-03',
    max: 30,
    mode: 'Trực tiếp',
    status: 'Kết thúc',
    done: 64,
    slot: 'Tối 2-4-6',
    subjects: web('VU DUC LONG'),
  },
  {
    code: 'GIT-K03',
    program: 'Git và làm việc nhóm',
    start: '2026-06-08',
    max: 40,
    mode: 'Trực tuyến',
    status: 'Kết thúc',
    done: 4,
    slot: 'Tối 7',
    subjects: [S('GIT-01', 'Git và làm việc nhóm', 4, 'DANG MINH SON')],
  },
  {
    code: 'JAVA-K10',
    program: 'Lập trình Java Backend',
    start: '2025-10-06',
    max: 30,
    mode: 'Trực tiếp',
    status: 'Kết thúc',
    done: 56,
    slot: 'Tối 2-4-6',
    subjects: java('DANG MINH SON'),
  },
];

const classes = [];

function seedSampleClasses() {
  classes.length = 0;
  SEED_CLASSES.forEach((c) => {
    classes.push(JSON.parse(JSON.stringify(c)));
  });
}

seedSampleClasses();

/**
 * Tìm lớp học theo mã lớp
 */
function findClass(code) {
  if (!code) return null;
  const normalized = String(code).trim().toUpperCase();
  return classes.find((c) => c.code.toUpperCase() === normalized) || null;
}

/**
 * Tính tổng số buổi học trong một lớp
 */
function totalSessions(k) {
  if (!k || !Array.isArray(k.subjects)) return 0;
  return k.subjects.reduce((sum, s) => sum + (Number(s.sessions) || 0), 0);
}

/**
 * Đếm sĩ số học viên đang theo học của lớp
 */
function classSize(classCode, studentsList = []) {
  if (!classCode) return 0;
  const normalized = String(classCode).trim().toUpperCase();
  return studentsList.filter((s) => s.cls && s.cls.toUpperCase() === normalized).length;
}

module.exports = {
  classes,
  classSize,
  findClass,
  seedSampleClasses,
  totalSessions,
};
