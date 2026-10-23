// Điểm danh cho S4-07 trở đi. Khi tích hợp: GET/PUT /sessions/:id/attendance, GET /sessions/:id/attendance/logs.
// Buổi đã qua chưa có bản ghi thì sinh sẵn một bảng điểm danh mẫu cố định (giống dữ liệu đã lưu từ trước).
import { apiEnabled } from './api';
import { CLASSES, findClass, isOpen, totalSessions } from './classes';
import { TODAY } from './funnel';
import { notify } from './notifications';
import { approvedFor } from './leaves';
import { isPast, SESSIONS, type ClassSession } from './schedule';
import { persisted } from './store';
import { rosterOn } from './students';

export type Mark = 'Có mặt' | 'Đi muộn' | 'Vắng có phép' | 'Vắng không phép';
export const MARKS: Mark[] = ['Có mặt', 'Đi muộn', 'Vắng có phép', 'Vắng không phép'];
export const SHORT: Record<Mark, string> = { 'Có mặt': 'Có mặt', 'Đi muộn': 'Muộn', 'Vắng có phép': 'Có phép', 'Vắng không phép': 'Vắng' };
export const markTone = (m?: Mark): 'ok' | 'wait' | 'info' | 'off' | undefined =>
  m === 'Có mặt' ? 'ok' : m === 'Đi muộn' ? 'wait' : m === 'Vắng có phép' ? 'info' : m === 'Vắng không phép' ? 'off' : undefined;

export type Sheet = { savedAt: string; savedBy: string; marks: Record<string, Mark>; notes: Record<string, string> };
export type AttLog = { session: string; at: string; by: string; role: string; text: string };

export const SHEETS: Record<string, Sheet> = persisted('attendance', {}, true);
export const ATT_LOGS: AttLog[] = persisted('attendance-logs', [], true);

/** Băm cố định để dữ liệu mẫu không đổi giữa các lần tải trang. */
const hash = (s: string) => [...s].reduce((h, c) => (h * 31 + c.charCodeAt(0)) >>> 0, 7);

function sample(s: ClassSession): Sheet {
  const marks: Record<string, Mark> = {};
  const notes: Record<string, string> = {};
  for (const st of rosterOn(s.cls, s.date)) {
    const h = hash(st.code + s.id) % 100;
    // Vài học viên vắng nhiều hơn để minh hoạ cảnh báo chuyên cần (S5-02): mã kết thúc bằng 17 vượt ngưỡng, bằng 7 gần ngưỡng.
    const [un, ex] = st.code.endsWith('17') ? [45, 70] : st.code.endsWith('7') ? [18, 30] : [3, 6];
    marks[st.code] = h < un ? 'Vắng không phép' : h < ex ? 'Vắng có phép' : h < ex + 4 ? 'Đi muộn' : 'Có mặt';
    if (marks[st.code] === 'Đi muộn') notes[st.code] = 'Đến ' + s.start.slice(0, 3) + String(10 + (h % 30)).padStart(2, '0');
  }
  for (const code of approvedFor(s.id)) if (marks[code]) [marks[code], notes[code]] = ['Vắng có phép', 'Đơn xin phép đã duyệt'];
  const [hh, mm] = s.start.split(':').map(Number);
  const t = hh * 60 + mm + 42;
  return { savedAt: `${s.date}T${String(Math.floor(t / 60)).padStart(2, '0')}:${String(t % 60).padStart(2, '0')}`, savedBy: s.teacher ?? 'LE HOANG NAM', marks, notes };
}

const cache = new Map<string, Sheet>();

/** Bảng điểm danh của buổi, hoặc undefined nếu chưa điểm danh. */
export function sheetOf(s: ClassSession): Sheet | undefined {
  if (SHEETS[s.id]) return SHEETS[s.id];
  // Có backend: buổi chưa có bảng trên máy chủ là chưa điểm danh, không sinh bảng mẫu.
  if (apiEnabled || s.cancelled || !isPast(s)) return undefined;
  if (!cache.has(s.id)) cache.set(s.id, sample(s));
  return cache.get(s.id);
}

export function countMarks(sh?: Sheet) {
  const c: Record<Mark, number> = { 'Có mặt': 0, 'Đi muộn': 0, 'Vắng có phép': 0, 'Vắng không phép': 0 };
  for (const m of Object.values(sh?.marks ?? {})) c[m]++;
  return c;
}

export const summary = (sh?: Sheet) => {
  const c = countMarks(sh);
  return MARKS.filter((m) => c[m])
    .map((m) => `${c[m]} ${m.toLowerCase()}`)
    .join(' · ');
};

/** Buổi giảng viên điểm danh được: buổi hôm nay. Buổi khác cần quyền Quản lý đào tạo. */
export const isToday = (s: ClassSession) => s.date === TODAY;

/** Ngưỡng vắng mặc định: 20% tổng số buổi của lớp (S5-01, S5-02). */
export const DEFAULT_THRESHOLD = 20;
/** Ngưỡng đã chỉnh theo tên chương trình (S5-02); không có là dùng mặc định. */
export const THRESHOLDS: Record<string, number> = persisted('absence-thresholds', {});
export const thresholdOf = (program: string) => THRESHOLDS[program] ?? DEFAULT_THRESHOLD;
/** Người nhận thêm khi cờ bật. Giảng viên và Quản lý đào tạo luôn nhận. */
export const FLAG_TO: { counselor: boolean } = persisted('flag-to', { counselor: false });
/** Cờ đã bật: khoá "lớp:mã học viên" (vượt ngưỡng) hoặc "gần:lớp:mã" (còn 3 buổi), giá trị là ngày bật. */
export const FLAGS: Record<string, string> = persisted('absence-flags', {});

/** Bật cờ cho học viên vừa vượt hoặc sắp chạm ngưỡng ở lớp này và gửi thông báo. Trả về số cờ vượt ngưỡng mới bật. */
export function flagCheck(cls: string): number {
  const k = findClass(cls);
  if (!k) return 0;
  const total = totalSessions(k);
  const staff = [...new Set(k.subjects.map((s) => s.teacher).filter((x): x is string => !!x))];
  let n = 0;
  for (const st of rosterOn(cls, TODAY)) {
    const a = statOf(st.code, cls, k.program, total);
    const to = { users: [...staff, 'LE HOANG NAM', ...(FLAG_TO.counselor && st.counselor ? [st.counselor] : [])] };
    if (a.over && !FLAGS[`${cls}:${st.code}`]) {
      FLAGS[`${cls}:${st.code}`] = TODAY;
      n++;
      notify({ at: TODAY + 'T08:00', kind: 'Điểm danh', title: `${st.name} vượt ngưỡng vắng ở lớp ${cls}`, body: `Đã vắng ${a.absent} buổi, ngưỡng là ${a.max} trên ${total} buổi. Nên liên hệ sớm.`, link: '/hoc-vien/' + st.code, to });
    } else if (!a.over && a.left <= 3 && !FLAGS[`gần:${cls}:${st.code}`]) {
      FLAGS[`gần:${cls}:${st.code}`] = TODAY;
      notify({ at: TODAY + 'T08:00', kind: 'Điểm danh', title: `${st.name} sắp chạm ngưỡng vắng`, body: `Đã vắng ${a.absent} buổi, còn được vắng ${a.left} buổi nữa.`, link: '/chuyen-can/' + cls, to });
    }
  }
  return n;
}

export const flagAll = () => CLASSES.filter(isOpen).reduce((n, k) => n + flagCheck(k.code), 0);

export type AttStat = { held: number; late: number; excused: number; unexcused: number; absent: number; rate: number; max: number; left: number; over: boolean };

/** Chuyên cần của một học viên trong một lớp: tính trên các buổi đã điểm danh. Đi muộn vẫn tính là có mặt. */
export function statOf(code: string, cls: string, program: string, total: number): AttStat {
  let held = 0;
  let late = 0;
  let excused = 0;
  let unexcused = 0;
  for (const s of heldSessions(cls)) {
    const m = sheetOf(s)?.marks[code];
    if (!m) continue;
    held++;
    if (m === 'Đi muộn') late++;
    if (m === 'Vắng có phép') excused++;
    if (m === 'Vắng không phép') unexcused++;
  }
  const absent = excused + unexcused;
  const max = Math.floor((total * thresholdOf(program)) / 100);
  return { held, late, excused, unexcused, absent, rate: held ? Math.round((100 * (held - absent)) / held) : 100, max, left: max - absent, over: absent > max };
}

/** Các buổi đã diễn ra (không tính buổi huỷ) của một lớp. */
export const heldSessions = (cls: string) => SESSIONS.filter((s) => s.cls === cls && !s.cancelled && s.date <= TODAY).sort((a, b) => (a.date + a.start).localeCompare(b.date + b.start));

// Lần đầu mở ứng dụng: bật cờ cho dữ liệu mẫu để thấy thông báo minh hoạ.
if (!Object.keys(FLAGS).length) flagAll();
