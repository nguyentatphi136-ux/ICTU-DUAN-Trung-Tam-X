// Gọi API backend cho các trang S3 (docs/S3-api.md) và đổi dữ liệu máy chủ sang kiểu mà các trang đang dùng.
// Chỉ dùng khi apiEnabled. Dữ liệu tải về được ghi vào chính các mảng FUNNEL, STUDENTS, CLASSES, ROOMS, STAFF
// để các trang đọc chung một nguồn như ở chế độ dữ liệu mẫu. Mỗi trang tự tải phần mình cần khi mở.
import { api, json } from './api';
import { CLASSES, ENROLLMENTS, STAFF, type ClassStatus, type ClassSubject, type Klass, type StudyMode } from './classes';
import { FUNNEL, LOGS, type CareLog, type FunnelLead, type LogKind, type Stage } from './funnel';
import { ROOMS, type Room } from './rooms';
import { STUDENTS, type PastClass, type Student, type StudentNote, type StudentStatus } from './students';

const enc = encodeURIComponent;
const replace = <T>(list: T[], items: T[]) => list.splice(0, list.length, ...items);
const invert = <K extends string>(m: Record<K, string>) => Object.fromEntries(Object.entries(m).map(([k, v]) => [v, k])) as Record<string, K>;
type Page<T> = { items: T[]; total: number };

/** Tải hết các trang, mỗi trang 200 dòng. ponytail: giữ toàn bộ trong bộ nhớ; chuyển sang tìm kiếm phía máy chủ khi lên vài nghìn dòng. */
async function all<T>(path: string): Promise<T[]> {
  const out: T[] = [];
  for (let page = 1; ; page++) {
    const res = await api<Page<T>>(`${path}${path.includes('?') ? '&' : '?'}page=${page}&size=200`);
    out.push(...res.items);
    if (out.length >= res.total || !res.items.length) return out;
  }
}

// ===== S3-01 phễu, S3-02 nhật ký, S3-03 chuyển học viên =====

export const STAGE_CODES: Record<Stage, string> = {
  Mới: 'NEW',
  'Đã liên hệ': 'CONTACTED',
  'Đang tư vấn': 'CONSULTING',
  'Hẹn học thử': 'TRIAL_SCHEDULED',
  Chốt: 'WON',
  'Từ chối': 'REJECTED',
};
const STAGE_OF = invert(STAGE_CODES);
const LOG_CODES: Partial<Record<LogKind, string>> = { Gọi: 'CALL', Nhắn: 'MESSAGE', Gặp: 'MEETING' };

type ApiLead = {
  id: number; name: string; phone: string; email?: string; sourceLabel: string; programName?: string; status: string;
  ownerName?: string; rejectReason?: string; callbackDate?: string; studentCode?: string;
};
const toFunnel = (l: ApiLead): FunnelLead => ({
  id: l.id,
  name: l.name,
  phone: l.phone,
  email: l.email,
  program: l.programName ?? '',
  source: l.sourceLabel,
  owner: l.ownerName ?? '',
  stage: STAGE_OF[l.status] ?? 'Mới',
  callback: l.callbackDate,
  reason: l.rejectReason,
  student: l.studentCode,
});

export async function loadFunnel() {
  const res = await api<{ columns: { items: ApiLead[] }[] }>('/api/leads/funnel');
  replace(FUNNEL, res.columns.flatMap((c) => c.items.map(toFunnel)));
}

export const moveLead = (id: number, to: Stage, reason?: string) => api(`/api/leads/${id}/stage`, json('PUT', { status: STAGE_CODES[to], reason }));

/** Nhật ký chăm sóc cùng lịch sử phân công của lead, ghi vào LOGS[id]. */
export async function loadLeadLogs(id: number) {
  type ApiLog = { typeLabel: LogKind; at: string; by?: string; content: string };
  type ApiAssign = { at: string; to?: string; by?: string };
  const [logs, lead] = await Promise.all([api<ApiLog[]>(`/api/leads/${id}/logs`), api<{ assignments: ApiAssign[] }>(`/api/leads/${id}`)]);
  LOGS[id] = [
    ...logs.map((l): CareLog => ({ kind: l.typeLabel, at: l.at.slice(0, 16), by: l.by ?? '', text: l.content })),
    ...lead.assignments.map((a): CareLog => ({ kind: 'Phân công', at: a.at.slice(0, 16), by: a.by ?? '', text: 'Giao cho ' + (a.to ?? '') })),
  ];
}

export const addLeadLog = (id: number, log: CareLog, callback?: string) =>
  api(`/api/leads/${id}/logs`, json('POST', { type: LOG_CODES[log.kind], content: log.text, at: log.at, callbackDate: callback }));

export type Converted = { code: string; login: string; email: string; emailSent: boolean };
export const convertLead = (id: number, sendTo: string) => api<Converted>(`/api/leads/${id}/convert`, json('POST', { sendTo }));

// ===== S3-04 hồ sơ học viên =====

type ApiStudent = {
  code: string; name: string; phone: string; email?: string; login: string; programName?: string; status: string; enrolledAt?: string;
  currentClass?: string; birth?: string; address?: string; level?: string; notes?: string; counselor?: string; source?: string; leadId?: number;
  history?: { classCode: string; programName?: string; startDate: string; endDate?: string; status: string; classStatus: string }[];
};

/** STUDYING chưa có lớp đang chạy hiện là Mới nhập học, giống dữ liệu mẫu. */
const studentStatus = (s: ApiStudent): StudentStatus =>
  s.status === 'SUSPENDED' ? 'Bảo lưu' : s.status === 'GRADUATED' ? 'Đã tốt nghiệp' : s.status === 'DROPPED_OUT' ? 'Thôi học' : s.currentClass ? 'Đang học' : 'Mới nhập học';

// Máy chủ lưu ghi chú là một khối văn bản; mỗi dòng "yyyy-mm-dd · NGƯỜI GHI: nội dung" là một ghi chú.
const NOTE_LINE = /^(\d{4}-\d{2}-\d{2}) · (.+?): (.*)$/;
const parseNotes = (text?: string): StudentNote[] =>
  (text ?? '')
    .split('\n')
    .filter((l) => l.trim())
    .map((l) => {
      const m = NOTE_LINE.exec(l);
      return m ? { at: m[1], by: m[2], text: m[3] } : { at: '', by: '', text: l };
    });
const joinNotes = (notes: StudentNote[]) => notes.map((n) => (n.at ? `${n.at} · ${n.by}: ${n.text}` : n.text)).join('\n');

const monthYear = (iso: string) => iso.slice(5, 7) + '/' + iso.slice(0, 4);
const RESULT: Record<string, string> = { SUSPENDED: 'Bảo lưu', DROPPED: 'Thôi học', TRANSFERRED: 'Chuyển lớp', GRADUATED: 'Hoàn thành' };
const toPast = (h: NonNullable<ApiStudent['history']>[number]): PastClass => ({
  cls: h.classCode,
  program: h.programName ?? '',
  period: monthYear(h.startDate) + (h.endDate ? ' – ' + monthYear(h.endDate) : ''),
  result: RESULT[h.status] ?? (h.classStatus === 'CANCELLED' ? 'Lớp đã huỷ' : 'Hoàn thành'),
});

const toStudent = (s: ApiStudent): Student => ({
  code: s.code,
  name: s.name,
  phone: s.phone,
  email: s.email ?? '',
  login: s.login,
  program: s.programName ?? '',
  status: studentStatus(s),
  enrolledAt: s.enrolledAt ?? '',
  cls: s.currentClass,
  birth: s.birth,
  address: s.address,
  counselor: s.counselor,
  source: s.source,
  level: s.level,
  leadId: s.leadId,
  notes: parseNotes(s.notes),
  history: (s.history ?? []).map(toPast),
});

export async function loadStudents() {
  replace(STUDENTS, (await all<ApiStudent>('/api/students')).map(toStudent));
}

/** Hồ sơ đầy đủ của một học viên, thay vào STUDENTS. */
export async function loadStudent(code: string) {
  const s = toStudent(await api<ApiStudent>('/api/students/' + enc(code)));
  const i = STUDENTS.findIndex((x) => x.code === s.code);
  if (i >= 0) STUDENTS[i] = s;
  else STUDENTS.unshift(s);
  return s;
}

export const saveStudent = (s: Student) =>
  api('/api/students/' + enc(s.code), json('PUT', { name: s.name, phone: s.phone, email: s.email, birth: s.birth, address: s.address, level: s.level, notes: joinNotes(s.notes) }));

// ===== S3-05 đến S3-08 lớp học =====

export const CLASS_STATUS_CODES: Record<ClassStatus, string> = { 'Chuẩn bị': 'PLANNING', 'Đang học': 'IN_PROGRESS', 'Kết thúc': 'FINISHED', Huỷ: 'CANCELLED' };
const MODE_CODES: Record<StudyMode, string> = { 'Trực tiếp': 'OFFLINE', 'Trực tuyến': 'ONLINE', 'Kết hợp': 'HYBRID' };
const STATUS_OF = invert(CLASS_STATUS_CODES);
const MODE_OF = invert(MODE_CODES);

type Person = { id: number; name: string };
type ApiClass = {
  code: string; programName?: string; startDate: string; maxCapacity: number; enrolled: number; studyMode: string; status: string;
  room?: { code: string }; sessionsDone: number;
  subjects?: { code: string; name: string; sessions: number; instructor?: Person; assistants: Person[] }[];
  mySubjects?: { code: string; name: string; role: 'INSTRUCTOR' | 'TA' }[];
};

/** me: tên người đăng nhập, dùng cho "lớp của tôi" khi máy chủ chỉ trả các môn mình phụ trách. */
const toKlass = (k: ApiClass, me?: string): Klass => ({
  code: k.code,
  program: k.programName ?? '',
  start: k.startDate,
  max: k.maxCapacity,
  enrolled: k.enrolled,
  mode: MODE_OF[k.studyMode] ?? 'Trực tiếp',
  status: STATUS_OF[k.status] ?? 'Chuẩn bị',
  done: k.sessionsDone,
  // Khung giờ thuộc thời khoá biểu (S4), backend S3 chưa có.
  slot: 'Chưa xếp lịch',
  room: k.room?.code,
  subjects: k.subjects
    ? k.subjects.map((s): ClassSubject => ({ code: s.code, name: s.name, sessions: s.sessions, teacher: s.instructor?.name, tas: s.assistants.map((a) => a.name) }))
    : (k.mySubjects ?? []).map((s): ClassSubject => ({ code: s.code, name: s.name, sessions: 0, teacher: s.role === 'INSTRUCTOR' ? me : undefined, tas: s.role === 'TA' && me ? [me] : [] })),
});

const classDetail = (code: string) => api<ApiClass>('/api/classes/' + enc(code));

/** Mọi lớp kèm môn và người phụ trách. ponytail: một yêu cầu chi tiết cho mỗi lớp; thêm trường môn vào danh sách nếu số lớp lên hàng trăm. */
export async function loadClasses() {
  const list = await all<ApiClass>('/api/classes');
  replace(CLASSES, (await Promise.all(list.map((k) => classDetail(k.code)))).map((k) => toKlass(k)));
}

/** Một lớp, thay vào CLASSES. */
export async function loadClass(code: string) {
  const k = toKlass(await classDetail(code));
  const i = CLASSES.findIndex((x) => x.code === k.code);
  if (i >= 0) CLASSES[i] = k;
  else CLASSES.unshift(k);
}

/** Lớp của giảng viên, trợ giảng đang đăng nhập (S3-08). */
export async function loadMyClasses(me: string) {
  replace(CLASSES, (await api<ApiClass[]>('/api/classes/mine')).map((k) => toKlass(k, me)));
}

export async function loadStaff() {
  type ApiStaff = Person & { email: string; openClasses: number };
  const res = await api<{ instructors: ApiStaff[]; assistants: ApiStaff[] }>('/api/classes/staff');
  replace(STAFF, [...res.instructors.map((p) => ({ ...p, role: 2 as const })), ...res.assistants.map((p) => ({ ...p, role: 1 as const }))]);
}

export const openClass = (k: Klass, programCode: string) =>
  api('/api/classes', json('POST', { code: k.code, programCode, startDate: k.start, maxCapacity: k.max, studyMode: MODE_CODES[k.mode] }));

export const setClassStatus = (code: string, status: ClassStatus) => api(`/api/classes/${enc(code)}/status`, json('PUT', { status: CLASS_STATUS_CODES[status] }));

/** Người được chọn trên giao diện là tên; đổi sang id theo STAFF đã tải. */
export function assignSubject(code: string, subjectCode: string, teacher: string | undefined, tas: string[]) {
  const id = (name: string, role: 1 | 2) => STAFF.find((p) => p.role === role && p.name === name)?.id;
  return api(`/api/classes/${enc(code)}/subjects/${enc(subjectCode)}`, json('PUT', { instructorId: teacher ? id(teacher, 2) : null, assistantIds: tas.map((t) => id(t, 1)) }));
}

export const enrollStudents = (code: string, studentCodes: string[]) => api(`/api/classes/${enc(code)}/enrollments`, json('POST', { studentCodes }));

/** Danh sách ghi danh của một lớp, thay phần của lớp đó trong ENROLLMENTS. */
export async function loadEnrollments(code: string) {
  const list = await api<{ studentCode: string; enrolledAt: string; by?: string }[]>(`/api/classes/${enc(code)}/enrollments`);
  replace(ENROLLMENTS, [...ENROLLMENTS.filter((e) => e.cls !== code), ...list.map((e) => ({ student: e.studentCode, cls: code, at: e.enrolledAt, by: e.by ?? '' }))]);
}

// ===== S3-09 phòng học =====

type ApiRoom = { id: number; code: string; type: string; capacity?: number; location?: string; meetingUrl?: string };
const toRoom = (r: ApiRoom): Room => ({ id: r.id, name: r.code, kind: r.type === 'ONLINE' ? 'Trực tuyến' : 'Vật lý', where: r.location ?? r.meetingUrl ?? '', capacity: r.capacity });

export async function loadRooms() {
  replace(ROOMS, (await api<ApiRoom[]>('/api/rooms')).map(toRoom));
}

/** Tên phòng trên giao diện là mã phòng trên máy chủ. id có là sửa, không có là thêm. */
export function saveRoom(r: Room, id?: number) {
  const online = r.kind === 'Trực tuyến';
  const body = { code: r.name, name: r.name, type: online ? 'ONLINE' : 'PHYSICAL', capacity: r.capacity, location: online ? null : r.where, meetingUrl: online ? r.where.replace(/\s+/g, '') : null };
  return id ? api(`/api/rooms/${id}`, json('PUT', body)) : api('/api/rooms', json('POST', body));
}
export const deleteRoom = (id: number) => api(`/api/rooms/${id}`, { method: 'DELETE' });
export const setClassRoom = (code: string, roomId?: number) => api(`/api/classes/${enc(code)}/room`, json('PUT', { roomId: roomId ?? null }));
