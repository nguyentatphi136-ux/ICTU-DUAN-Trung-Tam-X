// Kho lưu trữ dữ liệu hồ sơ học viên và nghiệp vụ chuyển đổi Lead (Story S3-03 & S3-04)
const { ROLES } = require('../constants/roles');
const {
  DEFAULT_STUDENT_STATUS,
  PHONE_MSG,
  STUDENT_STATUSES,
  isValidEmail,
  isValidPhone,
  isValidStudentStatus,
  loginOf,
  pad,
  seq,
} = require('../constants/students');
const { addUser, findUserById, users } = require('./users');
const { addLeadCareLog, findLeadById, leads } = require('./leads');
const { LEAD_STATUS } = require('../constants/leads');
const { classSize, findClass, totalSessions } = require('./classes');

// Danh sách học viên mẫu khởi tạo ban đầu đồng bộ với frontend (data/students.ts)
const SEED_STUDENTS = [
  {
    code: 'HV260142',
    name: 'DO TUAN KIET',
    phone: '0971 846 320',
    email: 'kiet.dt@gmail.com',
    program: 'Lập trình web Full-stack',
    status: 'Mới nhập học',
    enrolledAt: '2026-10-05',
    counselor: 'TRAN THU HA',
    source: 'Biểu mẫu web',
    leadId: 5,
    notes: [],
    history: [],
  },
  {
    code: 'HV260140',
    name: 'LE MINH KHOI',
    phone: '0909 518 264',
    email: 'khoi.lm@gmail.com',
    program: 'Lập trình web Full-stack',
    status: 'Mới nhập học',
    enrolledAt: '2026-10-03',
    notes: [],
    history: [],
  },
  {
    code: 'HV260139',
    name: 'PHAM NGOC HAN',
    phone: '0933 704 186',
    email: 'han.pn@gmail.com',
    program: 'Lập trình web Full-stack',
    status: 'Mới nhập học',
    enrolledAt: '2026-10-02',
    notes: [],
    history: [],
  },
  {
    code: 'HV260137',
    name: 'VU KHANH LINH',
    phone: '0982 660 715',
    email: 'linh.vk@gmail.com',
    program: 'Lập trình web Full-stack',
    status: 'Mới nhập học',
    enrolledAt: '2026-09-30',
    notes: [],
    history: [],
  },
  {
    code: 'HV260136',
    name: 'HOANG GIA BAO',
    phone: '0915 337 902',
    email: 'bao.hg@gmail.com',
    program: 'Lập trình web Full-stack',
    status: 'Mới nhập học',
    enrolledAt: '2026-09-29',
    notes: [],
    history: [],
  },
  {
    code: 'HV260131',
    name: 'NGUYEN THU TRANG',
    phone: '0976 220 148',
    email: 'trang.nt@gmail.com',
    program: 'Lập trình web Full-stack',
    status: 'Đang học',
    enrolledAt: '2026-09-25',
    cls: 'WEB-K16',
    notes: [],
    history: [],
  },
  {
    code: 'HV260128',
    name: 'TRAN DUC HUY',
    phone: '0904 612 775',
    email: 'huy.td@gmail.com',
    program: 'Lập trình web Full-stack',
    status: 'Đang học',
    enrolledAt: '2026-09-22',
    cls: 'WEB-K16',
    notes: [],
    history: [],
  },
  {
    code: 'HV260125',
    name: 'BUI THANH TAM',
    phone: '0968 245 019',
    email: 'tam.bt@gmail.com',
    program: 'Lập trình web Full-stack',
    status: 'Mới nhập học',
    enrolledAt: '2026-09-20',
    notes: [],
    history: [],
  },
  {
    code: 'HV260101',
    name: 'NGUYEN MINH ANH',
    phone: '0912 345 678',
    email: 'minhanh.ng@gmail.com',
    program: 'Lập trình web Full-stack',
    status: 'Đang học',
    enrolledAt: '2026-08-12',
    cls: 'WEB-K15',
    birth: '2004-03-15',
    address: '12 Nguyễn Trãi, Thanh Xuân, Hà Nội',
    counselor: 'TRAN THU HA',
    source: 'Biểu mẫu web',
    entryScore: '6,5 / 10',
    level: 'Cơ bản',
    background: 'Sinh viên năm 3 ngành Kinh tế, đã tự học HTML và CSS.',
    leadId: 1,
    notes: [
      { at: '2026-09-22', by: 'LE HOANG NAM', text: 'Xin nghỉ hai buổi vì lịch thi ở trường, đã báo giảng viên.' },
      { at: '2026-08-12', by: 'TRAN THU HA', text: 'Muốn học lớp tối 2-4-6.' },
    ],
    history: [
      { cls: 'GIT-K03', program: 'Git và làm việc nhóm', period: '06/2026 – 07/2026', result: 'Hoàn thành · 8,2' },
      { cls: 'WEB-K12', program: 'Lập trình web Full-stack', period: '02/2026 – 04/2026', result: 'Bảo lưu' },
      { cls: 'TIN-K20', program: 'Tin học văn phòng', period: '09/2025 – 11/2025', result: 'Hoàn thành · 9,0' },
    ],
  },
  {
    code: 'HV260102',
    name: 'HOANG YEN NHI',
    phone: '0934 118 205',
    email: 'nhi.hy@gmail.com',
    program: 'Lập trình web Full-stack',
    status: 'Đang học',
    enrolledAt: '2026-08-12',
    cls: 'WEB-K15',
    notes: [],
    history: [],
  },
  {
    code: 'HV260088',
    name: 'TRAN GIA HUY',
    phone: '0988 402 117',
    email: 'huy.tg@gmail.com',
    program: 'Phân tích dữ liệu',
    status: 'Đang học',
    enrolledAt: '2026-07-20',
    cls: 'DATA-K07',
    notes: [],
    history: [],
  },
  {
    code: 'HV260075',
    name: 'LE THANH TAM',
    phone: '0906 731 554',
    email: 'tam.lt@gmail.com',
    program: 'Thiết kế UI/UX',
    status: 'Bảo lưu',
    enrolledAt: '2026-07-02',
    cls: 'UIUX-K09',
    notes: [],
    history: [],
  },
  {
    code: 'HV250311',
    name: 'PHAM QUOC VIET',
    phone: '0979 266 840',
    email: 'viet.pq@gmail.com',
    program: 'Lập trình Java Backend',
    status: 'Đã tốt nghiệp',
    enrolledAt: '2025-11-15',
    cls: 'JAVA-K11',
    notes: [],
    history: [],
  },
  {
    code: 'HV250298',
    name: 'BUI QUANG KHAI',
    phone: '0915 090 433',
    email: 'khai.bq@gmail.com',
    program: 'Kiểm thử phần mềm',
    status: 'Thôi học',
    enrolledAt: '2025-11-03',
    cls: 'TEST-K05',
    notes: [],
    history: [],
  },
  {
    code: 'HV250260',
    name: 'NGO KHANH LINH',
    phone: '0961 577 208',
    email: 'linh.nk@gmail.com',
    program: 'Thiết kế UI/UX',
    status: 'Đã tốt nghiệp',
    enrolledAt: '2025-09-18',
    cls: 'UIUX-K08',
    notes: [],
    history: [],
  },
];

const students = [];
const activationEmailLogs = [];

/**
 * Khởi tạo dữ liệu mẫu học viên ban đầu
 */
function seedSampleStudents() {
  students.length = 0;
  activationEmailLogs.length = 0;

  for (const s of SEED_STUDENTS) {
    const sequenceNumber = seq(s.code);
    students.push({
      ...s,
      login: loginOf(sequenceNumber),
      notes: [...(s.notes || [])],
      history: [...(s.history || [])],
    });
  }

  // Sắp xếp mã học viên giảm dần theo chuẩn TMS
  students.sort((a, b) => b.code.localeCompare(a.code));
}

// Khởi chạy seed ban đầu
seedSampleStudents();

/**
 * Sinh mã học viên tự động kế tiếp của năm hiện tại (2026 -> HV26xxxx)
 * Tiêu chí nghiệm thu 2 của Story S3-03: "Hệ thống sinh mã học viên tự động"
 */
function nextStudentCode(yearPrefix = '26') {
  const prefix = `HV${yearPrefix}`;
  const numbers = students
    .filter((s) => s.code.startsWith(prefix))
    .map((s) => seq(s.code));

  const max = numbers.length > 0 ? Math.max(0, ...numbers) : 0;
  const nextNum = max + 1;
  return `${prefix}${pad(nextNum, 4)}`;
}

/**
 * Tìm học viên theo mã HV kèm thông tin lớp đang học (nếu có)
 */
function findStudentByCode(code, { enrichClass = true } = {}) {
  if (!code || typeof code !== 'string') return null;
  const upper = code.trim().toUpperCase();
  const student = students.find((s) => s.code.toUpperCase() === upper);
  if (!student) return null;

  if (enrichClass) {
    const clone = { ...student, notes: [...student.notes], history: [...student.history] };
    if (student.cls) {
      const k = findClass(student.cls);
      if (k) {
        clone.currentClass = {
          code: k.code,
          program: k.program,
          start: k.start,
          max: k.max,
          mode: k.mode,
          status: k.status,
          done: k.done,
          slot: k.slot,
          room: k.room || '',
          totalSessions: totalSessions(k),
          classSize: classSize(k.code, students),
          subjects: k.subjects,
        };
      }
    }
    return clone;
  }
  return student;
}

/**
 * Tìm học viên theo ID lead gốc
 */
function findStudentByLeadId(leadId) {
  return students.find((s) => String(s.leadId) === String(leadId)) || null;
}

/**
 * Lấy danh sách học viên kèm tìm kiếm, bộ lọc và thống kê (Story S3-04)
 */
function getStudents({ search = '', status = '', program = '', cls = '', page = 1, limit = 20 } = {}) {
  let list = students;

  if (search && search.trim()) {
    const q = search.trim().toLowerCase();
    const cleanPhone = q.replace(/[\s.-]/g, '');
    list = list.filter((s) => {
      const matchCode = s.code.toLowerCase().includes(q);
      const matchName = s.name.toLowerCase().includes(q);
      const matchEmail = (s.email || '').toLowerCase().includes(q);
      const matchLogin = (s.login || '').toLowerCase().includes(q);
      const cleanStudentPhone = (s.phone || '').replace(/[\s.-]/g, '');
      const matchPhone = cleanPhone.length > 2 && cleanStudentPhone.includes(cleanPhone);
      return matchCode || matchName || matchEmail || matchLogin || matchPhone;
    });
  }

  if (status && status.trim()) {
    list = list.filter((s) => s.status.toLowerCase() === status.trim().toLowerCase());
  }

  if (program && program.trim()) {
    list = list.filter((s) => (s.program || '').toLowerCase().includes(program.trim().toLowerCase()));
  }

  if (cls && cls.trim()) {
    list = list.filter((s) => (s.cls || '').toLowerCase() === cls.trim().toLowerCase());
  }

  const total = list.length;
  const pageNum = Math.max(1, Number(page) || 1);
  const limitNum = Math.max(1, Number(limit) || 20);
  const totalPages = Math.ceil(total / limitNum) || 1;
  const offset = (pageNum - 1) * limitNum;
  const paginated = list.slice(offset, offset + limitNum);

  const studyingCount = students.filter((s) => s.status === 'Đang học').length;
  const totalAll = students.length;
  const from = total === 0 ? 0 : offset + 1;
  const to = Math.min(offset + limitNum, total);

  return {
    total,
    page: pageNum,
    limit: limitNum,
    totalPages,
    from,
    to,
    studyingCount,
    totalAll,
    items: paginated,
    students: paginated,
    data: paginated,
  };
}

/**
 * Cập nhật thông tin hồ sơ học viên (Story S3-04 / IDTTX-212 / Subtask IDTTX-241)
 * Cho phép xem và sửa thông tin cá nhân, trình độ đầu vào và thêm ghi chú
 */
function updateStudentProfile(code, payload = {}, user = {}) {
  const student = students.find((s) => s.code.toUpperCase() === (code || '').trim().toUpperCase());
  if (!student) {
    const err = new Error('Không tìm thấy học viên');
    err.code = 'STUDENT_NOT_FOUND';
    err.statusCode = 404;
    throw err;
  }

  // 1. Kiểm tra validation số điện thoại nếu có gửi lên
  if (payload.phone !== undefined) {
    if (!isValidPhone(payload.phone)) {
      const err = new Error(PHONE_MSG);
      err.code = 'INVALID_PHONE';
      err.statusCode = 400;
      throw err;
    }
    student.phone = payload.phone.trim();
  }

  // 2. Kiểm tra validation email nếu có gửi lên
  if (payload.email !== undefined && payload.email !== '') {
    if (!isValidEmail(payload.email)) {
      const err = new Error('Email liên hệ không hợp lệ');
      err.code = 'INVALID_EMAIL';
      err.statusCode = 400;
      throw err;
    }
    student.email = payload.email.trim();
  }

  // 3. Cập nhật các trường thông tin cá nhân
  if (payload.birth !== undefined) {
    student.birth = payload.birth ? String(payload.birth).trim() : '';
  }
  if (payload.address !== undefined) {
    student.address = String(payload.address || '').trim();
  }

  // 4. Cập nhật trình độ đầu vào
  if (payload.entryScore !== undefined) {
    student.entryScore = String(payload.entryScore || '').trim();
  }
  if (payload.level !== undefined) {
    student.level = String(payload.level || '').trim();
  }
  if (payload.background !== undefined) {
    student.background = String(payload.background || '').trim();
  }

  // 5. Cập nhật trạng thái nếu hợp lệ
  if (payload.status !== undefined && payload.status.trim()) {
    if (isValidStudentStatus(payload.status)) {
      student.status = payload.status.trim();
    }
  }

  // 6. Tự động thêm ghi chú nếu có gửi kèm trong Drawer chỉnh sửa (S3-04 Figma)
  const noteText = payload.note || payload.text;
  if (noteText && String(noteText).trim()) {
    const todayStr = new Date().toISOString().slice(0, 10);
    student.notes.unshift({
      at: todayStr,
      by: user.name || 'Quản lý đào tạo',
      text: String(noteText).trim(),
    });
  }

  student.updatedAt = new Date().toISOString();
  return findStudentByCode(code);
}

/**
 * Thêm ghi chú vào hồ sơ học viên
 */
function addStudentNote(code, { text, by = 'Tư vấn viên' }) {
  const student = students.find((s) => s.code.toUpperCase() === (code || '').trim().toUpperCase());
  if (!student) {
    const err = new Error('Không tìm thấy học viên');
    err.code = 'STUDENT_NOT_FOUND';
    err.statusCode = 404;
    throw err;
  }

  const noteEntry = {
    at: new Date().toISOString().slice(0, 10),
    by: by || 'Quản lý',
    text: (text || '').trim(),
  };

  student.notes.unshift(noteEntry);
  return noteEntry;
}

/**
 * NGHIỆP VỤ CHÍNH STORY S3-03 (IDTTX-211 / Subtask IDTTX-240):
 * Chuyển một lead đã chốt thành hồ sơ học viên và tự động sinh tài khoản, gửi email
 * 
 * Tiêu chí nghiệm thu:
 * 1. Dữ liệu lead được chuyển sang hồ sơ học viên, không phải nhập lại
 * 2. Hệ thống sinh mã học viên tự động (HV26xxxx)
 * 3. Tài khoản đăng nhập cho học viên được tạo và gửi email (S25200212xxx@tms.vn)
 * 4. Lead chuyển sang trạng thái Đã nhập học và không sửa được nữa
 */
function convertLeadToStudent({ leadId, sendTo = null, counselorId = null }) {
  const lead = findLeadById(leadId);
  if (!lead) {
    const err = new Error('Không tìm thấy Lead để chuyển đổi');
    err.code = 'LEAD_NOT_FOUND';
    err.statusCode = 404;
    throw err;
  }

  // 1. Kiểm tra điều kiện: Chỉ lead ở cột "Chốt" (hoặc WON) mới được chuyển đổi
  const isWon = lead.stage === 'Chốt' || lead.status === LEAD_STATUS.WON;
  if (!isWon) {
    const err = new Error('Chỉ lead đã chốt đăng ký mới có thể chuyển thành học viên');
    err.code = 'LEAD_NOT_WON';
    err.statusCode = 400;
    throw err;
  }

  // 2. Kiểm tra tính toàn vẹn: Không được chuyển đổi lại lead đã nhập học
  if (lead.isConverted || lead.student || lead.studentCode || lead.isLocked) {
    const existingCode = lead.student || lead.studentCode || 'đã cấp';
    const err = new Error(`Lead này đã được chuyển thành học viên ${existingCode} trước đó`);
    err.code = 'LEAD_ALREADY_CONVERTED';
    err.statusCode = 409;
    throw err;
  }

  // 3. Kiểm tra email nhận thông tin kích hoạt
  const targetEmail = (sendTo || lead.email || '').trim();
  if (!isValidEmail(targetEmail)) {
    const err = new Error('Nhập email hợp lệ để gửi thông tin đăng nhập');
    err.code = 'INVALID_EMAIL';
    err.statusCode = 400;
    throw err;
  }

  // 4. Sinh mã học viên tự động (HV26xxxx)
  const studentCode = nextStudentCode('26');
  const sequenceNumber = seq(studentCode);

  // 5. Sinh tài khoản đăng nhập (S25200212xxx@tms.vn)
  const loginEmail = loginOf(sequenceNumber);

  // 6. Tạo tài khoản người dùng với vai trò Học viên (Student)
  const counselor = counselorId ? findUserById(counselorId) : null;
  const authorName = counselor ? counselor.name : lead.assignedCounselorName || 'Tư vấn viên';

  const newUser = addUser({
    email: loginEmail,
    name: lead.fullName,
    phone: lead.phone,
    roles: [ROLES.STUDENT],
    password: 'Student@123',
    status: 'active',
  });

  // 7. Tạo hồ sơ học viên đầy đủ kế thừa từ thông tin Lead
  const todayStr = new Date().toISOString().slice(0, 10);
  const newStudent = {
    code: studentCode,
    name: lead.fullName,
    phone: lead.phone,
    email: targetEmail,
    login: loginEmail,
    program: lead.course || 'Lập trình web Full-stack',
    status: DEFAULT_STUDENT_STATUS,
    enrolledAt: todayStr,
    cls: '',
    birth: '',
    address: '',
    counselor: authorName,
    source: lead.sourceLabel || lead.source || 'Biểu mẫu web',
    notes: [],
    history: [],
    leadId: Number(lead.id),
    userId: Number(newUser.id),
    createdAt: new Date().toISOString(),
  };

  students.unshift(newStudent);

  // 8. Tự động ghi nhật ký và timeline cho Lead (trước khi khóa)
  const now = new Date().toISOString();
  const logMsg = `Đã chuyển thành học viên ${studentCode} (${loginEmail}). Email kích hoạt gửi tới ${targetEmail}`;

  lead.timeline.unshift({
    id: String(Date.now()),
    time: now,
    type: 'STATUS_CHANGE',
    title: `Chuyển thành học viên ${studentCode}`,
    content: logMsg,
    author: authorName,
  });

  addLeadCareLog(lead.id, {
    kind: 'Đổi trạng thái',
    text: logMsg,
    by: authorName,
    at: now,
  });

  // 9. Cập nhật và khóa Lead: "Lead chuyển sang trạng thái Đã nhập học và không sửa được nữa"
  lead.student = studentCode;
  lead.studentCode = studentCode;
  lead.isConverted = true;
  lead.isLocked = true;
  lead.convertedAt = now;
  lead.subStatus = 'Đã nhập học';
  lead.updatedAt = now;
  lead.lastInteractionAt = now;

  // 10. Ghi log mô phỏng gửi email kích hoạt
  const emailLog = {
    studentCode,
    recipientEmail: targetEmail,
    loginUsername: loginEmail,
    tempPassword: 'Student@123',
    status: 'SENT',
    sentAt: now,
  };
  activationEmailLogs.push(emailLog);

  return {
    success: true,
    message: `Chuyển đổi thành công lead ${lead.fullName} thành học viên ${studentCode}`,
    data: newStudent,
    student: newStudent,
    user: {
      id: newUser.id,
      email: newUser.email,
      name: newUser.name,
      roles: newUser.roles,
    },
    lead: {
      id: lead.id,
      fullName: lead.fullName,
      student: lead.student,
      isConverted: lead.isConverted,
      isLocked: lead.isLocked,
      subStatus: lead.subStatus,
    },
    emailSentTo: targetEmail,
  };
}

module.exports = {
  activationEmailLogs,
  addStudentNote,
  convertLeadToStudent,
  findStudentByCode,
  findStudentByLeadId,
  getStudents,
  nextStudentCode,
  seedSampleStudents,
  students,
  updateStudentProfile,
};
