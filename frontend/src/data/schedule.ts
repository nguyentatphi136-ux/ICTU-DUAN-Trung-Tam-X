// Thời khoá biểu cho S4-01 trở đi. Khi tích hợp: POST /classes/:code/schedule/preview (sinh và kiểm tra trùng),
// POST /classes/:code/schedule (lưu), GET /sessions?from=&to=&class=&teacher=&room=, PATCH /sessions/:id.
import { CLASSES, isOpen, type Klass } from './classes';
import { TODAY } from './funnel';
import { persisted } from './store';
import { LESSONS } from './training';

export type Shift = 'Sáng' | 'Chiều' | 'Tối';
export const SHIFTS: Record<Shift, [string, string]> = { Sáng: ['07:00', '10:00'], Chiều: ['13:00', '16:00'], Tối: ['18:00', '20:30'] };

export type ClassSession = {
  /** Mã lớp và số buổi, ví dụ WEB-K15-31. */
  id: string;
  cls: string;
  /** Số thứ tự buổi trong lớp; buổi học bù vẫn có số riêng. */
  no: number;
  date: string;
  start: string;
  end: string;
  subject: string;
  /** Số thứ tự bài trong môn. */
  lesson: number;
  topic: string;
  teacher?: string;
  room?: string;
  /** Lý do huỷ; có giá trị là buổi đã huỷ. */
  cancelled?: string;
  /** Buổi học bù cho buổi đã huỷ. */
  makeup?: boolean;
  /** Ngày giờ cũ nếu buổi đã được dời, dạng "yyyy-mm-dd hh:mm". */
  movedFrom?: string;
};

export type Holiday = { from: string; to?: string; name: string };

export const HOLIDAYS: Holiday[] = persisted('holidays', [
  { from: '2026-11-20', name: 'Ngày Nhà giáo Việt Nam' },
  { from: '2027-01-01', name: 'Tết Dương lịch' },
  { from: '2027-02-05', to: '2027-02-14', name: 'Nghỉ Tết Nguyên đán' },
  { from: '2027-04-26', name: 'Giỗ Tổ Hùng Vương' },
  { from: '2027-04-30', to: '2027-05-01', name: 'Giải phóng miền Nam, Quốc tế Lao động' },
]);

/** Thứ trong tuần theo cách gọi Việt Nam; chỉ số theo Date.getDay() (0 là Chủ nhật). */
export const WEEKDAYS = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
export const WEEKDAY_NAMES = ['Chủ nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];

const d = (iso: string) => new Date(iso + 'T00:00:00');
export const iso = (x: Date) => `${x.getFullYear()}-${String(x.getMonth() + 1).padStart(2, '0')}-${String(x.getDate()).padStart(2, '0')}`;
export const addDays = (s: string, n: number) => {
  const x = d(s);
  x.setDate(x.getDate() + n);
  return iso(x);
};
export const dow = (s: string) => d(s).getDay();
/** "21/10" */
export const dm = (s: string) => s.slice(8, 10) + '/' + s.slice(5, 7);
/** "Thứ Tư 21/10/2026" */
export const longDate = (s: string) => `${WEEKDAY_NAMES[dow(s)]} ${dm(s)}/${s.slice(0, 4)}`;
/** "T4 21/10/2026" */
export const shortDate = (s: string) => `${WEEKDAYS[dow(s)]} ${dm(s)}/${s.slice(0, 4)}`;
/** Thứ Hai của tuần chứa ngày s. */
export const mondayOf = (s: string) => addDays(s, -((dow(s) + 6) % 7));

export const holidayOn = (s: string) => HOLIDAYS.find((h) => s >= h.from && s <= (h.to ?? h.from));

export const shiftOf = (start: string): Shift => (start < '12:00' ? 'Sáng' : start < '17:00' ? 'Chiều' : 'Tối');

const TOPICS: Record<string, string[]> = {
  'GIT-01': ['Làm quen Git', 'Nhánh và gộp nhánh', 'Pull request', 'Xử lý xung đột'],
  'WEB-02': ['Biến và kiểu dữ liệu', 'Điều kiện và vòng lặp', 'Hàm', 'Mảng', 'Đối tượng', 'DOM', 'Sự kiện', 'Biểu mẫu', 'Lưu trữ cục bộ', 'Bất đồng bộ', 'Promise', 'Async/await', 'Gọi API bằng fetch', 'Bài tập cuối môn'],
  'DB-01': ['Mô hình quan hệ', 'Thiết kế lược đồ', 'SELECT cơ bản', 'JOIN', 'GROUP BY', 'Truy vấn con', 'Chỉ mục', 'Giao dịch', 'Chuẩn hoá', 'Ôn tập'],
};
export const topicOf = (subject: string, n: number) => LESSONS[subject]?.[n - 1]?.topic ?? TOPICS[subject]?.[n - 1] ?? `Bài ${n}`;

export type Pattern = { days: number[]; start: string; end: string; from: string; count: number; room?: string };
export type Skipped = { date: string; reason: string };

/** Sinh đủ count buổi theo mẫu lặp, bỏ qua ngày nghỉ lễ; mỗi buổi gắn với bài kế tiếp theo thứ tự môn trong lớp. */
export function generate(k: Klass, p: Pattern): { sessions: ClassSession[]; skipped: Skipped[] } {
  const slots = k.subjects.flatMap((s) => [...Array(s.sessions)].map((_, i) => ({ s, lesson: i + 1 })));
  const sessions: ClassSession[] = [];
  const skipped: Skipped[] = [];
  let day = p.from;
  for (let guard = 0; sessions.length < p.count && guard < 2000; guard++, day = addDays(day, 1)) {
    if (!p.days.includes(dow(day))) continue;
    const h = holidayOn(day);
    if (h) {
      skipped.push({ date: day, reason: h.name });
      continue;
    }
    const slot = slots[sessions.length] ?? slots[slots.length - 1];
    sessions.push({
      id: `${k.code}-${sessions.length + 1}`,
      cls: k.code,
      no: sessions.length + 1,
      date: day,
      start: p.start,
      end: p.end,
      subject: slot.s.code,
      lesson: slot.lesson,
      topic: topicOf(slot.s.code, slot.lesson),
      teacher: slot.s.teacher,
      room: p.room,
    });
  }
  return { sessions, skipped };
}

/** "Tối 2-4-6" thành ca và các thứ (theo Date.getDay()). Không đọc được thì trả về null. */
export function parseSlot(slot: string): { shift: Shift; days: number[] } | null {
  const m = slot.match(/^(Sáng|Chiều|Tối) ([2-7CN-]+)$/);
  if (!m) return null;
  return { shift: m[1] as Shift, days: m[2].split('-').map((x) => (x === 'CN' ? 0 : Number(x) - 1)) };
}

// Lịch có sẵn của các lớp đang chạy (lớp Chuẩn bị chưa sinh lịch thì để Quản lý đào tạo dùng S4-01).
function seed(): ClassSession[] {
  const out: ClassSession[] = [];
  for (const k of CLASSES) {
    if (!isOpen(k) || k.code === 'WEB-K16' || k.code === 'TEST-K06') continue;
    const p = parseSlot(k.slot);
    if (!p) continue;
    const [start, end] = SHIFTS[p.shift];
    out.push(...generate(k, { days: p.days, start, end, from: k.start, count: k.subjects.reduce((n, s) => n + s.sessions, 0), room: k.room }).sessions);
  }
  return out;
}

export const SESSIONS: ClassSession[] = persisted('sessions', seed());

export const sessionsOf = (cls: string) => SESSIONS.filter((s) => s.cls === cls).sort((a, b) => (a.date + a.start).localeCompare(b.date + b.start));
export const findSession = (id: string) => SESSIONS.find((s) => s.id === id);
export const isPast = (s: ClassSession) => s.date < TODAY;

/** Xung đột của một buổi với buổi của lớp khác (S4-02): cùng ngày, khung giờ chồng nhau, cùng giảng viên hoặc cùng phòng. */
export type Clash = { kind: 'Trùng giảng viên' | 'Trùng phòng'; other: ClassSession };

export function clashes(s: ClassSession, pool: ClassSession[] = SESSIONS): Clash[] {
  if (s.cancelled) return [];
  const out: Clash[] = [];
  for (const o of pool) {
    if (o.cls === s.cls || o.cancelled || o.date !== s.date || o.start >= s.end || s.start >= o.end) continue;
    if (s.teacher && o.teacher === s.teacher) out.push({ kind: 'Trùng giảng viên', other: o });
    if (s.room && o.room === s.room) out.push({ kind: 'Trùng phòng', other: o });
  }
  return out;
}

export const clashText = ({ kind, other: o }: Clash) =>
  kind === 'Trùng giảng viên' ? `${o.teacher} đang dạy ${o.cls} buổi ${o.no}, ${o.start} – ${o.end}${o.room ? ', ' + o.room : ''}` : `${o.room} đang dùng cho ${o.cls} buổi ${o.no}, ${o.start} – ${o.end}`;
