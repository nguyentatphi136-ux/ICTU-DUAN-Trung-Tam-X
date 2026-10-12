// Hồ sơ học viên cho S3-03 trở đi. Khi tích hợp: GET /students?search=&status=&program=, GET/PUT /students/:code,
// POST /admissions/leads/:id/convert (tạo hồ sơ, tài khoản và gửi email kích hoạt trong một giao dịch).
import { persisted } from './store';

export type StudentStatus = 'Mới nhập học' | 'Đang học' | 'Bảo lưu' | 'Đã tốt nghiệp' | 'Thôi học';
export type StudentNote = { at: string; by: string; text: string };
/** Một lớp đã học xong hoặc đã rời. result ví dụ "Hoàn thành · 8,2", "Bảo lưu". */
export type PastClass = { cls: string; program: string; period: string; result: string };

/** Bảo lưu, chuyển lớp, thôi học (S4-06). at là ngày hiệu lực; recorded là ngày ghi nhận. */
export type StatusChangeType = 'Bảo lưu' | 'Chuyển lớp' | 'Thôi học';
export type StatusChange = { type: StatusChangeType; at: string; reason: string; by: string; from?: string; to?: string; recorded: string };

export type Student = {
  code: string;
  name: string;
  phone: string;
  /** Email liên hệ của học viên. */
  email: string;
  /** Email đăng nhập do hệ thống cấp. */
  login: string;
  program: string;
  status: StudentStatus;
  /** Ngày nhập học, yyyy-mm-dd. */
  enrolledAt: string;
  /** Mã lớp đang học, rỗng là chưa xếp lớp. */
  cls?: string;
  birth?: string;
  address?: string;
  counselor?: string;
  source?: string;
  entryScore?: string;
  level?: string;
  background?: string;
  notes: StudentNote[];
  history: PastClass[];
  /** Lead gốc nếu học viên được chuyển từ phễu tuyển sinh (S3-03). */
  leadId?: number;
  /** Lịch sử bảo lưu, chuyển lớp, thôi học, mới nhất ở đầu (S4-06). */
  changes?: StatusChange[];
};

export const STUDENT_STATUSES: StudentStatus[] = ['Mới nhập học', 'Đang học', 'Bảo lưu', 'Đã tốt nghiệp', 'Thôi học'];

export const studentTone = (s: StudentStatus): 'ok' | 'wait' | 'off' | 'info' | undefined =>
  s === 'Mới nhập học' ? 'info' : s === 'Đang học' ? 'ok' : s === 'Bảo lưu' ? 'wait' : s === 'Thôi học' ? 'off' : undefined;

/** Số thứ tự trong mã: HV260143 là học viên thứ 143 nhập học năm 2026. */
const seq = (code: string) => Number(code.slice(4));
const pad = (n: number, w: number) => String(n).padStart(w, '0');
export const loginOf = (n: number) => `S25200212${pad(n, 3)}@tms.vn`;

type Seed = Omit<Student, 'login' | 'notes' | 'history'> & Partial<Pick<Student, 'notes' | 'history'>>;

const SEED: Seed[] = [
  { code: 'HV260142', name: 'DO TUAN KIET', phone: '0971 846 320', email: 'kiet.dt@gmail.com', program: 'Lập trình web Full-stack', status: 'Mới nhập học', enrolledAt: '2026-10-05', counselor: 'TRAN THU HA', source: 'Biểu mẫu web', leadId: 24 },
  { code: 'HV260140', name: 'LE MINH KHOI', phone: '0909 518 264', email: 'khoi.lm@gmail.com', program: 'Lập trình web Full-stack', status: 'Mới nhập học', enrolledAt: '2026-10-03' },
  { code: 'HV260139', name: 'PHAM NGOC HAN', phone: '0933 704 186', email: 'han.pn@gmail.com', program: 'Lập trình web Full-stack', status: 'Mới nhập học', enrolledAt: '2026-10-02' },
  { code: 'HV260137', name: 'VU KHANH LINH', phone: '0982 660 715', email: 'linh.vk@gmail.com', program: 'Lập trình web Full-stack', status: 'Mới nhập học', enrolledAt: '2026-09-30' },
  { code: 'HV260136', name: 'HOANG GIA BAO', phone: '0915 337 902', email: 'bao.hg@gmail.com', program: 'Lập trình web Full-stack', status: 'Mới nhập học', enrolledAt: '2026-09-29' },
  { code: 'HV260131', name: 'NGUYEN THU TRANG', phone: '0976 220 148', email: 'trang.nt@gmail.com', program: 'Lập trình web Full-stack', status: 'Đang học', enrolledAt: '2026-09-25', cls: 'WEB-K16' },
  { code: 'HV260128', name: 'TRAN DUC HUY', phone: '0904 612 775', email: 'huy.td@gmail.com', program: 'Lập trình web Full-stack', status: 'Đang học', enrolledAt: '2026-09-22', cls: 'WEB-K16' },
  { code: 'HV260125', name: 'BUI THANH TAM', phone: '0968 245 019', email: 'tam.bt@gmail.com', program: 'Lập trình web Full-stack', status: 'Mới nhập học', enrolledAt: '2026-09-20' },
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
  { code: 'HV260102', name: 'HOANG YEN NHI', phone: '0934 118 205', email: 'nhi.hy@gmail.com', program: 'Lập trình web Full-stack', status: 'Đang học', enrolledAt: '2026-08-12', cls: 'WEB-K15' },
  { code: 'HV260088', name: 'TRAN GIA HUY', phone: '0988 402 117', email: 'huy.tg@gmail.com', program: 'Phân tích dữ liệu', status: 'Đang học', enrolledAt: '2026-07-20', cls: 'DATA-K07' },
  { code: 'HV260075', name: 'LE THANH TAM', phone: '0906 731 554', email: 'tam.lt@gmail.com', program: 'Thiết kế UI/UX', status: 'Bảo lưu', enrolledAt: '2026-07-02', cls: 'UIUX-K09' },
  { code: 'HV250311', name: 'PHAM QUOC VIET', phone: '0979 266 840', email: 'viet.pq@gmail.com', program: 'Lập trình Java Backend', status: 'Đang học', enrolledAt: '2025-11-15', cls: 'JAVA-K11' },
  { code: 'HV250298', name: 'BUI QUANG KHAI', phone: '0915 090 433', email: 'khai.bq@gmail.com', program: 'Kiểm thử phần mềm', status: 'Thôi học', enrolledAt: '2025-11-03', cls: 'TEST-K05' },
  { code: 'HV250260', name: 'NGO KHANH LINH', phone: '0961 577 208', email: 'linh.nk@gmail.com', program: 'Thiết kế UI/UX', status: 'Đã tốt nghiệp', enrolledAt: '2025-09-18', cls: 'UIUX-K08' },
];

// Lớp của phần học viên sinh tự động, đủ sĩ số như bảng Lớp học trong thiết kế.
const FILL: [cls: string, program: string, count: number, status: StudentStatus, from: string][] = [
  ['WEB-K15', 'Lập trình web Full-stack', 26, 'Đang học', '2026-08-'],
  ['DATA-K07', 'Phân tích dữ liệu', 17, 'Đang học', '2026-07-'],
  ['UIUX-K09', 'Thiết kế UI/UX', 19, 'Đang học', '2026-06-'],
  ['JAVA-K12', 'Lập trình Java Backend', 12, 'Đang học', '2026-09-'],
  ['WEB-K16', 'Lập trình web Full-stack', 7, 'Đang học', '2026-09-'],
  ['TEST-K06', 'Kiểm thử phần mềm', 2, 'Đang học', '2026-09-'],
  // Lớp JAVA-K11 đã kết thúc, học viên chờ xét hoàn thành khoá (S8-04).
  ['JAVA-K11', 'Lập trình Java Backend', 23, 'Đang học', '2026-02-'],
  ['TEST-K05', 'Kiểm thử phần mềm', 5, 'Thôi học', '2026-04-'],
  ['', 'Phân tích dữ liệu', 17, 'Mới nhập học', '2026-09-'],
];

const LAST = ['AN', 'BINH', 'CHI', 'DUNG', 'GIANG', 'HANH', 'KHANH', 'LINH', 'MINH', 'NGOC', 'PHUONG', 'QUAN', 'SON', 'THAO', 'TUAN', 'VY', 'HIEU', 'LAM'];
const FIRST = ['NGUYEN', 'TRAN', 'LE', 'PHAM', 'HOANG', 'VU', 'DANG', 'BUI', 'DO', 'NGO', 'DUONG', 'LY'];
const MID = ['VAN', 'THI', 'MINH', 'THANH', 'HUU', 'NGOC', 'DUC', 'GIA'];

function generate(): Student[] {
  const used = new Set(SEED.map((s) => seq(s.code)));
  const free = [...Array(141)].map((_, i) => i + 1).filter((n) => !used.has(n));
  const out: Student[] = [];
  let k = 0;
  for (const [cls, program, count, status, month] of FILL) {
    for (let j = 0; j < count && k < free.length; j++, k++) {
      const n = free[k];
      const name = `${FIRST[n % FIRST.length]} ${MID[(n * 3) % MID.length]} ${LAST[(n * 7) % LAST.length]}`;
      out.push({
        code: 'HV26' + pad(n, 4),
        name,
        phone: `09${String(20000000 + n * 61331).slice(-8).replace(/(\d{2})(\d{3})(\d{3})/, '$1 $2 $3')}`,
        email: name.split(' ').pop()!.toLowerCase() + '.' + n + '@gmail.com',
        login: loginOf(n),
        program,
        status,
        enrolledAt: month + pad(1 + (n % 27), 2),
        cls: cls || undefined,
        notes: [],
        history: [],
      });
    }
  }
  return out;
}

export const STUDENTS: Student[] = persisted(
  'students',
  [...SEED.map((s): Student => ({ notes: [], history: [], ...s, login: loginOf(seq(s.code) % 1000) })), ...generate()].sort((a, b) => b.code.localeCompare(a.code)),
);

/** Mã học viên kế tiếp của năm 2026, ví dụ HV260143. */
export function nextStudentCode() {
  const max = Math.max(0, ...STUDENTS.filter((s) => s.code.startsWith('HV26')).map((s) => seq(s.code)));
  return 'HV26' + pad(max + 1, 4);
}

export const findStudent = (code: string) => STUDENTS.find((s) => s.code === code);

/** Danh sách lớp vào một ngày (dùng cho điểm danh): ai đã rời lớp từ ngày hiệu lực thì không còn, ai chuyển đến thì có từ ngày hiệu lực. */
export function rosterOn(cls: string, date: string) {
  return STUDENTS.filter((s) => {
    const left = s.changes?.find((c) => c.from === cls);
    if (left) return date < left.at;
    if (s.cls !== cls || s.status === 'Bảo lưu' || s.status === 'Thôi học') return false;
    const joined = s.changes?.find((c) => c.to === cls);
    return !joined || date >= joined.at;
  });
}
