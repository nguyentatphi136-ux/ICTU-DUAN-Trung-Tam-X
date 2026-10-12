// Lớp học cho S3-04 trở đi. Khi tích hợp: GET/POST /classes, PATCH /classes/:code (trạng thái, phân công, phòng),
// POST /classes/:code/enrollments. Danh sách môn của lớp là bản sao lúc mở lớp, sửa chương trình sau này không ảnh hưởng.
import { persisted } from './store';
import { STUDENTS } from './students';

export type ClassStatus = 'Chuẩn bị' | 'Đang học' | 'Kết thúc' | 'Huỷ';
export type StudyMode = 'Trực tiếp' | 'Trực tuyến' | 'Kết hợp';
export type ClassSubject = { code: string; name: string; sessions: number; teacher?: string; tas: string[] };

export type Klass = {
  code: string;
  program: string;
  start: string;
  max: number;
  mode: StudyMode;
  status: ClassStatus;
  /** Số buổi đã học. */
  done: number;
  /** Khung giờ, ví dụ "Tối 2-4-6". */
  slot: string;
  room?: string;
  subjects: ClassSubject[];
};

export const CLASS_STATUSES: ClassStatus[] = ['Chuẩn bị', 'Đang học', 'Kết thúc', 'Huỷ'];
export const STUDY_MODES: StudyMode[] = ['Trực tiếp', 'Trực tuyến', 'Kết hợp'];

export const classTone = (s: ClassStatus): 'ok' | 'info' | 'off' | undefined =>
  s === 'Đang học' ? 'ok' : s === 'Chuẩn bị' ? 'info' : s === 'Huỷ' ? 'off' : undefined;

/** Người dạy được phân công. role là chỉ số trong ROLES: 2 Giảng viên, 1 Trợ giảng. */
export type Staff = { name: string; role: 1 | 2; email: string };
export const STAFF: Staff[] = [
  { name: 'VU DUC LONG', role: 2, email: 'IST25200212045@tms.vn' },
  { name: 'LE HOANG NAM', role: 2, email: 'TM25200212002@tms.vn' },
  { name: 'NGO THI THU', role: 2, email: 'IST25200212031@tms.vn' },
  { name: 'DANG MINH SON', role: 2, email: 'IST25200212027@tms.vn' },
  { name: 'PHAM THI VY', role: 2, email: 'IST25200212019@tms.vn' },
  { name: 'PHAM GIA HUY', role: 1, email: 'TA25200212007@tms.vn' },
  { name: 'NGO BAO TU', role: 1, email: 'TA25200212011@tms.vn' },
  { name: 'TRAN MINH THU', role: 1, email: 'TA25200212015@tms.vn' },
];

const S = (code: string, name: string, sessions: number, teacher?: string, tas: string[] = []): ClassSubject => ({ code, name, sessions, teacher, tas });

const web = (t?: string) => [
  S('GIT-01', 'Git và làm việc nhóm', 4, t),
  S('WEB-01', 'HTML & CSS nền tảng', 12, t),
  S('WEB-02', 'JavaScript cơ bản', 14, t),
  S('DB-01', 'Cơ sở dữ liệu', 10, t),
  S('WEB-03', 'React', 12, t),
  S('BE-01', 'Node.js và API', 12, t),
];
const data = (t?: string) => [S('GIT-01', 'Git và làm việc nhóm', 4, t), S('DB-01', 'Cơ sở dữ liệu', 10, t), S('PY-01', 'Python cho phân tích', 14, t), S('STAT-01', 'Thống kê ứng dụng', 10, t), S('VIS-01', 'Trực quan hoá dữ liệu', 10, t)];
const uiux = (t?: string) => [S('UX-01', 'Nguyên lý thiết kế', 8, t), S('UX-02', 'Nghiên cứu người dùng', 8, t), S('UX-03', 'Thiết kế với Figma', 12, t), S('UX-04', 'Prototype và kiểm thử', 8, t), S('GIT-01', 'Git và làm việc nhóm', 4, t)];
const java = (t?: string) => [S('GIT-01', 'Git và làm việc nhóm', 4, t), S('JV-01', 'Java cơ bản', 14, t), S('DB-01', 'Cơ sở dữ liệu', 10, t), S('JV-02', 'Spring Boot', 16, t), S('JV-03', 'Kiểm thử và triển khai', 12, t)];
const test = (t?: string) => [S('TEST-01', 'Kiểm thử cơ bản', 8, t), S('TEST-02', 'Kiểm thử tự động', 12, t), S('TEST-03', 'Kiểm thử API', 8, t), S('GIT-01', 'Git và làm việc nhóm', 4, t)];

/** Môn theo chương trình khi mở lớp mới (S3-05). Khi tích hợp: GET /training/programs/:code/subjects. */
export const PROGRAM_SUBJECTS: Record<string, () => ClassSubject[]> = {
  'Lập trình web Full-stack': () => web(),
  'Phân tích dữ liệu': () => data(),
  'Thiết kế UI/UX': () => uiux(),
  'Lập trình Java Backend': () => java(),
  'Kiểm thử phần mềm': () => test(),
};

const k16 = web();
k16[0] = S('GIT-01', 'Git và làm việc nhóm', 4, 'LE HOANG NAM', ['PHAM GIA HUY']);
k16[1] = S('WEB-01', 'HTML & CSS nền tảng', 12, 'NGO THI THU', ['NGO BAO TU']);
k16[2] = S('WEB-02', 'JavaScript cơ bản', 14, 'NGO THI THU');
k16[4] = S('WEB-03', 'React', 12, 'DANG MINH SON');

export const CLASSES: Klass[] = persisted('classes', [
  { code: 'WEB-K15', program: 'Lập trình web Full-stack', start: '2026-08-12', max: 30, mode: 'Trực tiếp', status: 'Đang học', done: 22, slot: 'Tối 2-4-6', room: 'P.401', subjects: web('VU DUC LONG').map((s, i) => (i === 0 ? { ...s, tas: ['PHAM GIA HUY'] } : s)) },
  { code: 'DATA-K07', program: 'Phân tích dữ liệu', start: '2026-07-20', max: 25, mode: 'Kết hợp', status: 'Đang học', done: 30, slot: 'Tối 3-6', room: 'Zoom 01', subjects: data('LE HOANG NAM') },
  { code: 'UIUX-K09', program: 'Thiết kế UI/UX', start: '2026-07-02', max: 20, mode: 'Trực tiếp', status: 'Đang học', done: 26, slot: 'Tối 3-5', room: 'P.302', subjects: uiux('NGO THI THU') },
  { code: 'JAVA-K12', program: 'Lập trình Java Backend', start: '2026-10-15', max: 30, mode: 'Trực tiếp', status: 'Chuẩn bị', done: 0, slot: 'Sáng 5-7', room: 'Lab 1', subjects: java('DANG MINH SON') },
  { code: 'WEB-K16', program: 'Lập trình web Full-stack', start: '2026-10-20', max: 30, mode: 'Kết hợp', status: 'Chuẩn bị', done: 0, slot: 'Chưa xếp lịch', room: 'Meet WEB', subjects: k16 },
  { code: 'TEST-K06', program: 'Kiểm thử phần mềm', start: '2026-11-02', max: 25, mode: 'Trực tuyến', status: 'Chuẩn bị', done: 0, slot: 'Tối 2-4-6', subjects: test('PHAM THI VY') },
  { code: 'JAVA-K11', program: 'Lập trình Java Backend', start: '2026-03-10', max: 30, mode: 'Trực tiếp', status: 'Kết thúc', done: 56, slot: 'Tối 3-5-7', room: 'Lab 1', subjects: java('DANG MINH SON') },
  { code: 'TEST-K05', program: 'Kiểm thử phần mềm', start: '2026-05-05', max: 25, mode: 'Trực tuyến', status: 'Huỷ', done: 4, slot: 'Tối 3-5-7', subjects: test('PHAM THI VY') },
  { code: 'WEB-K14', program: 'Lập trình web Full-stack', start: '2026-03-02', max: 30, mode: 'Trực tiếp', status: 'Kết thúc', done: 64, slot: 'Tối 2-4-6', room: 'P.301', subjects: web('VU DUC LONG') },
  { code: 'DATA-K06', program: 'Phân tích dữ liệu', start: '2026-02-16', max: 25, mode: 'Kết hợp', status: 'Kết thúc', done: 48, slot: 'Tối 3-5-7', subjects: data('LE HOANG NAM') },
  { code: 'UIUX-K08', program: 'Thiết kế UI/UX', start: '2025-09-18', max: 20, mode: 'Trực tiếp', status: 'Kết thúc', done: 40, slot: 'Chiều 2-4-6', subjects: uiux('NGO THI THU') },
  { code: 'WEB-K12', program: 'Lập trình web Full-stack', start: '2026-02-03', max: 30, mode: 'Trực tiếp', status: 'Kết thúc', done: 64, slot: 'Tối 2-4-6', subjects: web('VU DUC LONG') },
  { code: 'GIT-K03', program: 'Git và làm việc nhóm', start: '2026-06-08', max: 40, mode: 'Trực tuyến', status: 'Kết thúc', done: 4, slot: 'Tối 7', subjects: [S('GIT-01', 'Git và làm việc nhóm', 4, 'DANG MINH SON')] },
  { code: 'JAVA-K10', program: 'Lập trình Java Backend', start: '2025-10-06', max: 30, mode: 'Trực tiếp', status: 'Kết thúc', done: 56, slot: 'Tối 2-4-6', subjects: java('DANG MINH SON') },
]);

export const findClass = (code?: string) => CLASSES.find((c) => c.code === code);
export const totalSessions = (k: Klass) => k.subjects.reduce((n, s) => n + s.sessions, 0);
/** Sĩ số hiện tại: học viên đang gắn với lớp. */
export const classSize = (code: string) => STUDENTS.filter((s) => s.cls === code).length;
/** Giảng viên hiện ở cột Giảng viên: người dạy chính của môn đầu tiên đã phân công. */
export const mainTeacher = (k: Klass) => k.subjects.find((s) => s.teacher)?.teacher;
export const missingTeachers = (k: Klass) => k.subjects.filter((s) => !s.teacher).length;
/** Lớp còn chạy (dùng để đếm số lớp đang dạy của giảng viên, sức chứa phòng...). */
export const isOpen = (k: Klass) => k.status === 'Chuẩn bị' || k.status === 'Đang học';
/** Các lớp có người này dạy chính hoặc trợ giảng. */
export const classesOf = (name: string) => CLASSES.filter((k) => k.subjects.some((s) => s.teacher === name || s.tas.includes(name)));

/** Lần ghi danh (S3-06): ai, vào lớp nào, ngày nào, người thực hiện. Khi tích hợp: GET /classes/:code/enrollments. */
export type Enrollment = { student: string; cls: string; at: string; by: string };
export const ENROLLMENTS: Enrollment[] = persisted('enrollments', []);
