// Bài tập và bài nộp cho S5-05 trở đi. Khi tích hợp: GET/POST/PUT /assignments, POST /assignments/:id/assign,
// POST /assignments/:id/submissions (tạo phiên bản mới), PUT /submissions/:id/grade.
import { addDays } from './schedule';
import { TODAY } from './funnel';
import { persisted } from './store';
import { STUDENTS } from './students';

export type FileRef = { name: string; size: number };
/** Tiêu chí chấm (S6-02): tên, trọng số phần trăm, mô tả mức đạt. */
export type Criterion = { name: string; weight: number; hint?: string };

export type Assignment = {
  id: number;
  cls: string;
  subject: string;
  /** Buổi học gắn với bài tập (mã buổi trong thời khoá biểu). */
  session?: string;
  title: string;
  /** Mô tả có định dạng (Markdown rút gọn: **đậm**, _nghiêng_, # tiêu đề, - danh sách, `mã`, [liên kết](url)). */
  description: string;
  /** Hạn nộp yyyy-mm-ddThh:mm. */
  due: string;
  max: number;
  files: FileRef[];
  status: 'Nháp' | 'Đã giao';
  savedAt: string;
  assignedAt?: string;
  by: string;
  /** S5-09: Chặn là hết hạn khoá nộp; Cho nộp là vẫn nộp được nhưng gắn nhãn Nộp muộn. */
  late: 'Chặn' | 'Cho nộp';
  /** S6-02: chấm theo rubric nếu có. */
  rubric?: Criterion[];
};

export type Version = { no: number; at: string; files: FileRef[]; repo?: string; note?: string };
export type GradeLog = { at: string; by: string; text: string };
export type Grade = {
  score: number;
  comment: string;
  by: string;
  at: string;
  rubric?: Record<string, number>;
  /** S6-01: điểm chỉ hiện cho học viên sau khi công bố. Dữ liệu cũ không có trường này là đã công bố. */
  published?: boolean;
  publishedAt?: string;
  /** S6-01: nhật ký chấm và sửa điểm. */
  log?: GradeLog[];
};
export type SubState = 'Đã nộp' | 'Cần làm lại' | 'Đã chấm';
export type Submission = {
  assignment: number;
  student: string;
  versions: Version[];
  state: SubState;
  grade?: Grade;
  /** S6-03: yêu cầu làm lại kèm nhận xét và hạn nộp lại. */
  redo?: { note: string; due: string; by: string; at: string };
  /** S6-03: mọi lần yêu cầu làm lại, cũ nhất ở đầu. */
  redos?: { note: string; due: string; by: string; at: string }[];
  /** S5-08: giảng viên mở lại bài đã chấm để học viên nộp lại. */
  reopened?: boolean;
};

const at = (days: number, time = '23:59') => addDays(TODAY, days) + 'T' + time;
const md = (s: string) => s.trim().replace(/^ +/gm, '');

type Seed = Omit<Assignment, 'id' | 'cls' | 'subject' | 'files' | 'status' | 'savedAt' | 'by' | 'late' | 'max'> & Partial<Assignment>;
const SEED: Seed[] = [
  { title: 'Bài 8: Mảng và vòng lặp', description: 'Viết các hàm xử lý mảng theo đề bài đính kèm.', due: at(-14) },
  { title: 'Bài 9: Biểu mẫu đăng ký có kiểm tra dữ liệu', description: 'Dựng biểu mẫu đăng ký, kiểm tra **email**, **số điện thoại** và mật khẩu trước khi gửi.', due: at(-9) },
  { title: 'Bài 10: Đối tượng', description: 'Mô hình hoá giỏ hàng bằng đối tượng và phương thức.', due: at(-8) },
  { title: 'Bài 11: Hàm và phạm vi biến', description: 'Viết các hàm xử lý mảng và đối tượng theo đề bài đính kèm, có kiểm thử cho từng hàm.', due: at(-6), files: [{ name: 'de-bai-11.pdf', size: 153600 }] },
  { title: 'Bài 12: Sự kiện và DOM', description: 'Làm danh sách việc cần làm, thêm và xoá bằng sự kiện click.', due: at(-3) },
  { title: 'Bài 13: Thao tác với mảng và đối tượng', description: 'Viết các hàm xử lý mảng và đối tượng theo đề bài đính kèm, có kiểm thử cho từng hàm.', due: at(-1), files: [{ name: 'de-bai-13.pdf', size: 153600 }] },
  {
    title: 'Bài 14: Gọi API danh sách sản phẩm',
    description: md(`
      Dùng \`fetch\` gọi API danh sách sản phẩm và hiển thị dạng lưới thẻ.

      - Có trạng thái đang tải
      - Báo lỗi khi API hỏng
      - Nộp liên kết repo và tệp nén mã nguồn`),
    due: at(0),
    files: [{ name: 'de-bai-14.pdf', size: 184320 }],
  },
  { title: 'Bài 15: Xử lý lỗi và trạng thái tải', description: 'Bổ sung xử lý lỗi mạng và hiển thị trạng thái tải cho bài 14.', due: at(3) },
  {
    title: 'Bài tập cuối môn JavaScript: ứng dụng ghi chú',
    description: md(`
      # Yêu cầu
      Xây dựng ứng dụng ghi chú chạy trên trình duyệt, lưu dữ liệu bằng API mẫu của lớp.

      - Thêm, sửa, xoá ghi chú
      - Tìm kiếm theo tiêu đề
      - Hiện trạng thái đang tải và thông báo lỗi khi gọi API

      Nộp liên kết repo GitHub và tệp nén mã nguồn.`),
    due: at(11),
    rubric: [
      { name: 'Đủ chức năng theo yêu cầu', weight: 40, hint: 'Làm đúng và đủ các chức năng trong đề bài' },
      { name: 'Chất lượng mã nguồn', weight: 30, hint: 'Đặt tên rõ, chia hàm hợp lý, không lặp mã' },
      { name: 'Giao diện và trải nghiệm', weight: 20, hint: 'Bố cục gọn, có trạng thái tải và báo lỗi' },
      { name: 'Báo cáo và trình bày', weight: 10, hint: 'Báo cáo nêu rõ cách làm và hạn chế' },
    ],
    files: [
      { name: 'de-bai-chi-tiet.pdf', size: 253952 },
      { name: 'api-mau.postman_collection.json', size: 12288 },
    ],
  },
  {
    title: 'Bài tập thực hành 1: Thiết kế lược đồ ERD & Tạo bảng CSDL',
    description: md(`
      # Yêu cầu thực hành Cơ sở dữ liệu (DB-01)
      Dựa trên đặc tả hệ thống quản lý bán hàng:
      - Vẽ sơ đồ quan hệ thực thể (ERD) hoàn chỉnh
      - Viết script SQL \`create_tables.sql\` tạo các bảng kèm khoá chính, khoá ngoại
      - Chèn ít nhất 5 bản ghi mẫu vào mỗi bảng

      Nộp file script \`.sql\` và ảnh chụp sơ đồ quan hệ.`),
    due: at(4),
    subject: 'DB-01',
    files: [{ name: 'de-bai-erd-db01.pdf', size: 215040 }],
  },
  {
    title: 'Bài tập thực hành 2 (Nháp): Truy vấn SQL nâng cao với JOIN & GROUP BY',
    description: md(`
      # Yêu cầu
      Viết 10 câu truy vấn phức hợp lấy dữ liệu báo cáo bán hàng:
      - Doanh thu theo tháng
      - Top 5 khách hàng mua nhiều nhất
      - Thống kê tồn kho theo danh mục sản phẩm`),
    due: at(10),
    subject: 'DB-01',
    status: 'Nháp' as const,
    files: [{ name: 'du-lieu-mau.sql', size: 45056 }],
  },
];

export const ASSIGNMENTS: Assignment[] = persisted(
  'assignments',
  SEED.map((s, i) => ({
    id: i + 1,
    cls: 'WEB-K15',
    subject: s.subject ?? 'WEB-02',
    files: [],
    status: (s.status ?? 'Đã giao') as const,
    savedAt: addDays(s.due.slice(0, 10), -7) + 'T14:02',
    assignedAt: s.status === 'Nháp' ? undefined : addDays(s.due.slice(0, 10), -7) + 'T14:05',
    by: 'VU DUC LONG',
    late: i === 8 || i === 4 ? ('Cho nộp' as const) : ('Chặn' as const),
    max: 10,
    ...s,
  })),
);

const hash = (s: string) => [...s].reduce((h, c) => (h * 31 + c.charCodeAt(0)) >>> 0, 11);
const minus = (iso: string, hours: number) => {
  const d = new Date(iso + ':00');
  d.setMinutes(d.getMinutes() - Math.round(hours * 60));
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}T${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
};
const COMMENTS = ['Làm đủ yêu cầu, đặt tên biến rõ.', 'Đúng yêu cầu, cần thêm kiểm thử cho trường hợp biên.', 'Code gọn, nên tách hàm nhỏ hơn.', 'Còn thiếu xử lý lỗi, xem lại phần gọi API.'];

// Bài nộp mẫu của lớp WEB-K15: phần lớn nộp đúng hạn, vài bài nộp muộn hoặc chưa nộp; bài đã qua hạn lâu thì đã chấm.
function seedSubs(): Submission[] {
  const out: Submission[] = [];
  const roster = STUDENTS.filter((s) => s.cls === 'WEB-K15');
  for (const a of ASSIGNMENTS) {
    const daysLeft = (Date.parse(a.due.slice(0, 10)) - Date.parse(TODAY)) / 86400000;
    for (const st of roster) {
      const h = hash(st.code + a.id) % 100;
      if (st.code === 'HV260101') continue; // bài của học viên mẫu viết riêng bên dưới
      const submitted = daysLeft < 0 ? h < 82 : daysLeft === 0 ? h < 45 : h < 20;
      if (!submitted) continue;
      const late = a.late === 'Cho nộp' && daysLeft < 0 && h < 10;
      const time = late ? minus(a.due, -(2 + (h % 9))) : minus(a.due, 3 + (h % 40));
      const versions: Version[] = [{ no: 1, at: time, files: [{ name: `bai-${a.id}-${st.code.toLowerCase()}.zip`, size: 40000 + h * 900 }], repo: h % 3 ? `https://github.com/${st.name.split(' ').pop()!.toLowerCase()}${h}/bai-${a.id}` : undefined, note: 'Bản đầu tiên.' }];
      if (h % 5 === 0) versions.unshift({ no: 2, at: minus(time, -0.5), files: [{ name: `bai-${a.id}-v2.zip`, size: 52000 }], note: 'Sửa theo góp ý.' });
      const graded = daysLeft <= -5 || (daysLeft < 0 && h % 3 === 0);
      out.push({
        assignment: a.id,
        student: st.code,
        versions,
        state: graded ? 'Đã chấm' : 'Đã nộp',
        grade: graded ? { score: Math.round((6 + (h % 40) / 10) * 2) / 2, comment: COMMENTS[h % COMMENTS.length], by: a.by, at: a.due.slice(0, 10) === TODAY ? a.due : addDays(a.due.slice(0, 10), 2) + 'T21:00' } : undefined,
      });
    }
  }
  // Học viên mẫu NGUYEN MINH ANH (HV260101), khớp các màn hình S5-06 đến S6-04.
  const v = (no: number, at: string, name: string, size: number, note: string, repo?: string): Version => ({ no, at, files: [{ name, size }], repo, note });
  out.push(
    { assignment: 1, student: 'HV260101', versions: [v(1, minus(at(-14), 26), 'bai-8.zip', 61000, 'Bản đầu tiên.')], state: 'Đã chấm', grade: { score: 7.5, comment: 'Đúng yêu cầu, cần thêm kiểm thử cho trường hợp biên.', by: 'VU DUC LONG', at: at(-12, '20:00') } },
    { assignment: 2, student: 'HV260101', versions: [v(1, minus(at(-9), 4), 'bai-9.zip', 72000, 'Bản đầu tiên.')], state: 'Cần làm lại', redo: { note: 'Phần kiểm tra số điện thoại chưa đúng với số bắt đầu bằng 03. Sửa và nộp lại.', due: at(2), by: 'VU DUC LONG', at: at(-7, '19:30') } },
    { assignment: 3, student: 'HV260101', versions: [v(1, minus(at(-8), 6), 'bai-10.zip', 58000, 'Bản đầu tiên.')], state: 'Đã chấm', grade: { score: 9, comment: 'Mô hình rõ ràng, phương thức đặt tên tốt.', by: 'VU DUC LONG', at: at(-6, '21:10') } },
    { assignment: 4, student: 'HV260101', versions: [v(2, minus(at(-6), 2.9), 'bai-11-v2.zip', 63488, 'Bổ sung ví dụ closure.'), v(1, minus(at(-6), 28.5), 'bai-11-v1.zip', 59392, 'Bản đầu tiên.')], state: 'Đã chấm', grade: { score: 8.5, comment: 'Làm đủ yêu cầu, đặt tên biến rõ. Cần thêm kiểm thử cho trường hợp mảng rỗng.', by: 'VU DUC LONG', at: at(-4, '20:30') } },
    { assignment: 5, student: 'HV260101', versions: [v(1, minus(at(-3), -1.5), 'bai-12.zip', 70000, 'Bản đầu tiên.')], state: 'Đã nộp' },
    { assignment: 6, student: 'HV260101', versions: [v(2, minus(at(-1), 3.75), 'bai-13-v2.zip', 86016, 'Sửa hàm groupBy theo góp ý trên lớp.', 'https://github.com/minhanh-ng/bai-13'), v(1, minus(at(-1), 25.3), 'bai-13-v1.zip', 81920, 'Bản đầu tiên.')], state: 'Đã nộp' },
  );
  return out;
}

export const SUBMISSIONS: Submission[] = persisted('submissions', seedSubs());

/** Điểm đã công bố cho học viên xem chưa (S6-01, S6-04). */
export const isPublished = (g?: Grade) => !!g && g.published !== false;

export const subOf = (assignment: number, student: string) => SUBMISSIONS.find((s) => s.assignment === assignment && s.student === student);
export const findAssignment = (id: number) => ASSIGNMENTS.find((a) => a.id === id);

/** Số giờ nộp trễ của một phiên bản so với hạn (0 nếu đúng hạn), làm tròn lên. */
export const lateHours = (a: Assignment, v: Version) => Math.max(0, Math.ceil((Date.parse(v.at + ':00') - Date.parse(a.due + ':00')) / 3600000));

export type MyState = 'Chưa nộp' | SubState;
/** Trạng thái học viên nhìn thấy: bài đã chấm nhưng chưa công bố điểm vẫn là Đã nộp (S6-01). */
export function stateOf(a: Assignment, student: string): MyState {
  const s = subOf(a.id, student);
  if (!s) return 'Chưa nộp';
  return s.state === 'Đã chấm' && !isPublished(s.grade) ? 'Đã nộp' : s.state;
}

/** "1,8 MB", "312 KB" */
export const fileSize = (n: number) => (n >= 1048576 ? (n / 1048576).toFixed(1).replace('.', ',') + ' MB' : Math.max(1, Math.round(n / 1024)) + ' KB');
/** "29/10/2026 21:40" */
export const vdt = (iso: string) => `${iso.slice(8, 10)}/${iso.slice(5, 7)}/${iso.slice(0, 4)} ${iso.slice(11, 16)}`;
/** Điểm kiểu Việt Nam: 8,5 */
export const score = (n: number) => String(n).replace('.', ',');
