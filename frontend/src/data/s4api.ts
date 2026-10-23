// Gọi API backend cho các trang S4 (thời khoá biểu, điểm danh, xin phép, tình trạng học) và đổi dữ liệu máy chủ sang
// kiểu các trang đang dùng. Chỉ dùng khi apiEnabled. Dữ liệu tải về được ghi vào chính SESSIONS, HOLIDAYS, SHEETS,
// ATT_LOGS, LEAVES, STUDENTS để các trang đọc chung một nguồn như ở chế độ dữ liệu mẫu.
// Buổi học trên máy chủ có id số; trên giao diện id là chuỗi của số đó (đường dẫn /diem-danh/123).
import { api, json } from './api';
import { ATT_LOGS, SHEETS, type Mark, type Sheet } from './attendance';
import { STAFF } from './classes';
import { LEAVES, type Leave, type LeaveStatus } from './leaves';
import { ROOMS } from './rooms';
import { HOLIDAYS, iso, SESSIONS, type ClassSession, type Holiday, type Pattern, type Skipped } from './schedule';
import { findStudent, STUDENTS, type StatusChange, type StatusChangeType } from './students';

const enc = encodeURIComponent;

/** Một buổi như máy chủ trả (ClassSessionResponse). */
type ApiSession = {
  id: number; classId: number; classCode: string; classSubjectId?: number; subjectCode?: string; instructorId?: number;
  instructorName?: string; classroomId?: number; roomCode?: string; sessionDate: string; startTime: string; endTime: string;
  status: string; sessionNumber: number; seq: number; topic?: string; cancelReason?: string;
};

/** Bản gốc máy chủ của từng buổi, cần id giảng viên, phòng, môn khi sửa hoặc xếp buổi bù (S4-03). */
const RAW = new Map<string, ApiSession>();

const toSession = (s: ApiSession): ClassSession => {
  RAW.set(String(s.id), s);
  return {
    id: String(s.id),
    cls: s.classCode,
    no: s.seq,
    date: s.sessionDate,
    start: s.startTime,
    end: s.endTime,
    subject: s.subjectCode ?? '',
    lesson: s.sessionNumber,
    topic: s.topic ?? `Bài ${s.sessionNumber}`,
    teacher: s.instructorName,
    room: s.roomCode,
    cancelled: s.status === 'CANCELLED' ? s.cancelReason || 'Đã huỷ' : undefined,
  };
};

/** Thay buổi cùng id, thêm buổi mới. */
function upsert(list: ApiSession[]) {
  const out = list.map(toSession);
  for (const s of out) {
    const i = SESSIONS.findIndex((x) => x.id === s.id);
    if (i >= 0) SESSIONS[i] = s;
    else SESSIONS.push(s);
  }
  return out;
}

/** Bỏ buổi của lớp cls (hoặc mọi lớp) trong [from, to] trước khi nạp lại, để buổi đã xoá trên máy chủ không còn hiện. */
function dropRange(from: string, to: string, cls?: string) {
  const keep = SESSIONS.filter((s) => s.date < from || s.date > to || (cls && s.cls !== cls));
  SESSIONS.splice(0, SESSIONS.length, ...keep);
}

// ===== S4-04 lịch tuần, tháng; S4-01, S4-03 dùng lại =====

export async function loadSessions(from: string, to: string, cls?: string) {
  const list = await api<ApiSession[]>(`/api/schedules/range?startDate=${from}&endDate=${to}${cls ? '&classCode=' + enc(cls) : ''}`);
  dropRange(from, to, cls);
  return upsert(list);
}

/** Lịch của một lớp: 6 tháng trước tới 7 tháng sau hôm nay. ponytail: lớp dài hơn 13 tháng thì cần tải theo từng đoạn. */
export const loadClassSessions = (cls: string, today: string) => {
  const d = new Date(today + 'T00:00:00');
  const at = (months: number) => iso(new Date(d.getFullYear(), d.getMonth() + months, d.getDate()));
  return loadSessions(at(-6), at(7), cls);
};

// ===== S4-01 sinh thời khoá biểu =====

type ApiHoliday = { id: number; name: string; from: string; to: string };

export async function loadHolidays() {
  const list = await api<ApiHoliday[]>('/api/v1/holidays');
  HOLIDAYS.splice(0, HOLIDAYS.length, ...list.map((h): Holiday => ({ from: h.from, to: h.to === h.from ? undefined : h.to, name: h.name })));
}

export const addHoliday = (h: Holiday) => api('/api/v1/holidays', json('POST', { name: h.name, from: h.from, to: h.to ?? h.from }));

type ApiPlanned = { id: string; cls: string; no: number; date: string; start: string; end: string; subject: string; lesson: number; topic: string; teacher?: string; room?: string };

/** Xem trước lịch máy chủ sinh: lấy lớp, môn, giảng viên, giáo trình, ngày nghỉ lễ trong CSDL. */
export async function previewSchedule(cls: string, p: Pattern): Promise<{ sessions: ClassSession[]; skipped: Skipped[] }> {
  const res = await api<{ sessions: ApiPlanned[]; skipped: Skipped[] }>(`/api/v1/classes/${enc(cls)}/schedule/preview`, json('POST', p));
  return { sessions: res.sessions.map((s) => ({ ...s, teacher: s.teacher === 'Chưa phân công' ? undefined : s.teacher })), skipped: res.skipped };
}

export const saveSchedule = (cls: string, p: Pattern) => api<{ sessionsCount: number }>(`/api/v1/classes/${enc(cls)}/schedule`, json('POST', p));

// ===== S4-03 sửa, dời, huỷ buổi =====

const idOfTeacher = (name?: string) => STAFF.find((p) => p.name === name)?.id;
const idOfRoom = (name?: string) => ROOMS.find((r) => r.name === name)?.id;

/** Sửa hoặc dời buổi. Chỉ gửi giảng viên, phòng khi đổi; máy chủ giữ nguyên trường không gửi và kiểm tra trùng lịch. */
export async function editSession(s: ClassSession, f: { date: string; start: string; end: string; room: string; teacher: string; reason: string }, override: boolean) {
  const raw = RAW.get(s.id);
  const body: Record<string, unknown> = { sessionDate: f.date, startTime: f.start, endTime: f.end, reason: f.reason };
  if (f.teacher && f.teacher !== s.teacher) body.instructorId = idOfTeacher(f.teacher);
  if (f.room && f.room !== s.room) body.classroomId = idOfRoom(f.room);
  if (override) Object.assign(body, { overrideConflict: true, overrideReason: f.reason });
  const res = await api<{ session?: { id: number } }>(`/api/schedules/${s.id}`, json('PATCH', body));
  await loadSessions(f.date < s.date ? f.date : s.date, f.date > s.date ? f.date : s.date, raw?.classCode);
  return res;
}

/** Huỷ buổi; makeup là ngày học bù (buổi bù cùng môn, giờ, phòng, giảng viên). */
export async function cancelSession(s: ClassSession, reason: string, makeup?: string) {
  await api(`/api/schedules/${s.id}/cancel`, json('POST', { reason }));
  const raw = RAW.get(s.id);
  if (makeup && raw) {
    await api('/api/schedules', json('POST', {
      classId: raw.classId, classSubjectId: raw.classSubjectId, instructorId: raw.instructorId, classroomId: raw.classroomId,
      sessionNumber: raw.sessionNumber, sessionDate: makeup, startTime: s.start, endTime: s.end,
    }));
  }
  await loadSessions(s.date, makeup && makeup > s.date ? makeup : s.date, s.cls);
}

// ===== S4-05 lịch của tôi =====

/** Lớp đang học của học viên đăng nhập, các lớp có buổi của tôi (giảng viên, trợ giảng); điền khi tải lịch của tôi. */
export const MY: { cls?: string; program?: string; classes: string[] } = { classes: [] };

export async function loadMySchedule() {
  const res = await api<{ cls?: string; program?: string; sessions: ApiSession[] }>('/api/me/schedule');
  const sessions = upsert(res.sessions);
  Object.assign(MY, { cls: res.cls, program: res.program, classes: [...new Set(sessions.map((s) => s.cls))] });
  return sessions;
}

// ===== S4-07 điểm danh =====

export const MARK_CODES: Record<Mark, string> = { 'Có mặt': 'PRESENT', 'Đi muộn': 'LATE', 'Vắng có phép': 'EXCUSED_ABSENCE', 'Vắng không phép': 'UNEXCUSED_ABSENCE' };
const MARK_OF = Object.fromEntries(Object.entries(MARK_CODES).map(([k, v]) => [v, k])) as Record<string, Mark>;

type ApiSheet = { savedAt: string; savedBy?: string; marks: Record<string, string>; notes: Record<string, string> };
const toSheet = (s: ApiSheet): Sheet => ({
  savedAt: s.savedAt,
  savedBy: s.savedBy ?? '',
  marks: Object.fromEntries(Object.entries(s.marks).map(([code, m]) => [code, MARK_OF[m]])),
  notes: s.notes,
});

function setSheet(id: string, sheet: ApiSheet | null) {
  if (sheet) SHEETS[id] = toSheet(sheet);
  else delete SHEETS[id];
}

/** Id các buổi người đăng nhập được điểm danh, theo lần tải gần nhất của trang Điểm danh. */
export const ATT_SESSIONS = new Set<string>();

/** Buổi mình dạy (quản lý: mọi buổi) trong [from, to] và bảng điểm danh đã lưu. */
export async function loadAttendanceSessions(from: string, to: string) {
  const rows = await api<{ session: ApiSession; sheet: ApiSheet | null }[]>(`/api/attendance/sessions?from=${from}&to=${to}`);
  ATT_SESSIONS.clear();
  for (const r of rows) {
    ATT_SESSIONS.add(String(r.session.id));
    upsert([r.session]);
    setSheet(String(r.session.id), r.sheet);
  }
}

/** Danh sách học viên theo ngày học, học viên có đơn đã duyệt và quyền sửa của từng buổi đã tải. */
export const ROSTERS: Record<string, { code: string; name: string }[]> = {};
export const APPROVED: Record<string, string[]> = {};
export const CAN_EDIT: Record<string, boolean> = {};

type ApiSheetPage = {
  session: ApiSession; roster: { code: string; name: string }[]; sheet: ApiSheet | null; approved: string[]; canEdit: boolean;
  logs: { at: string; by: string; role: string; text: string }[];
};

function applySheetPage(p: ApiSheetPage) {
  const [s] = upsert([p.session]);
  ROSTERS[s.id] = p.roster.map((r) => ({ code: r.code, name: r.name }));
  APPROVED[s.id] = p.approved;
  CAN_EDIT[s.id] = p.canEdit;
  setSheet(s.id, p.sheet);
  const others = ATT_LOGS.filter((l) => l.session !== s.id);
  ATT_LOGS.splice(0, ATT_LOGS.length, ...p.logs.map((l) => ({ session: s.id, ...l })), ...others);
}

export async function loadAttendance(id: string) {
  applySheetPage(await api<ApiSheetPage>(`/api/sessions/${enc(id)}/attendance`));
}

export async function saveAttendance(id: string, marks: Record<string, Mark>, notes: Record<string, string>, reason: string) {
  const body = { marks: Object.fromEntries(Object.entries(marks).map(([c, m]) => [c, MARK_CODES[m]])), notes, reason };
  applySheetPage(await api<ApiSheetPage>(`/api/sessions/${enc(id)}/attendance`, json('PUT', body)));
}

// ===== S4-08 đơn xin phép =====

const LEAVE_STATUS: Record<string, LeaveStatus> = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối' };
type ApiLeave = {
  id: number; session: ApiSession; studentCode: string; studentName: string; reason: string; fileName?: string; sentAt: string;
  status: string; decidedBy?: string; decidedAt?: string; note?: string; excused: number; unexcused: number;
};

/** Số buổi vắng có phép, không phép của học viên trong lớp của đơn, theo id đơn. */
export const LEAVE_COUNTS: Record<number, [number, number]> = {};

function toLeave(l: ApiLeave): Leave {
  const [s] = upsert([l.session]);
  LEAVE_COUNTS[l.id] = [l.excused, l.unexcused];
  // Trang duyệt tra tên học viên trong STUDENTS; người duyệt có thể chưa tải danh sách học viên.
  if (!findStudent(l.studentCode)) {
    STUDENTS.push({ code: l.studentCode, name: l.studentName, phone: '', email: '', login: '', program: '', status: 'Đang học', enrolledAt: '', cls: s.cls, notes: [], history: [] });
  }
  return {
    id: l.id,
    student: l.studentCode,
    session: s.id,
    reason: l.reason,
    file: l.fileName ? { name: l.fileName, size: 0 } : undefined,
    sentAt: l.sentAt,
    status: LEAVE_STATUS[l.status] ?? 'Chờ duyệt',
    decidedBy: l.decidedBy,
    decidedAt: l.decidedAt,
    note: l.note,
  };
}

/** mine: đơn của học viên đăng nhập; không thì đơn của các lớp mình dạy (quản lý: tất cả). */
export async function loadLeaves(mine: boolean) {
  const list = await api<ApiLeave[]>('/api/leave-requests' + (mine ? '?scope=mine' : ''));
  LEAVES.splice(0, LEAVES.length, ...list.map(toLeave));
}

export const sendLeave = (session: string, reason: string, fileName?: string) =>
  api<ApiLeave>('/api/leave-requests', json('POST', { sessionId: session, reason, fileName })).then((l) => LEAVES.unshift(toLeave(l)));

export async function decideLeave(id: number, ok: boolean, note: string) {
  const l = await api<ApiLeave>(`/api/leave-requests/${id}`, json('PATCH', { decision: ok ? 'APPROVED' : 'REJECTED', note }));
  const i = LEAVES.findIndex((x) => x.id === id);
  if (i >= 0) LEAVES[i] = toLeave(l);
}

// ===== S4-06 bảo lưu, chuyển lớp, thôi học =====

type ApiChange = { type: StatusChangeType; at: string; reason: string; by: string; from?: string; to?: string; recorded: string };
const toChanges = (list: ApiChange[]): StatusChange[] => list.map((c) => ({ ...c, from: c.from ?? undefined, to: c.to ?? undefined }));

export async function loadStatusChanges(code: string) {
  const res = await api<{ changes: ApiChange[] }>('/api/student-status/' + enc(code));
  const st = findStudent(code);
  if (st) st.changes = toChanges(res.changes);
}

export async function changeStatus(code: string, c: { type: StatusChangeType; at: string; reason: string; note: string; to?: string }) {
  const res = await api<{ changes: ApiChange[] }>('/api/student-status/' + enc(code), json('POST', c));
  const st = findStudent(code);
  if (st) st.changes = toChanges(res.changes);
}
