// Gọi API backend cho các trang S2 (docs/S2-api.md) và đổi dữ liệu máy chủ sang kiểu mà các trang đang dùng.
// Chỉ dùng khi apiEnabled. Dữ liệu tải về được ghi vào chính các mảng PROGRAMS, SUBJECTS, CURRICULUM để trang khác
// đọc chung một nguồn như ở chế độ dữ liệu mẫu.
import { api, json } from './api';
import type { Lead, LeadStatus } from './leads';
import { CURRICULUM, PROGRAMS, SUBJECTS, type CurriculumItem, type Lesson, type Program, type Subject } from './training';

const enc = encodeURIComponent;
const replace = <T>(list: T[], items: T[]) => list.splice(0, list.length, ...items);
type Page<T> = { items: T[]; total: number };

// ===== S2-04 chương trình, S2-05 môn học, S2-06 lộ trình =====

type ApiProgram = { id: number; code: string; name: string; description?: string; duration: number; standardTuition: number; status: string; runningClasses: number };
type ApiSubject = Omit<Subject, 'openedClasses'> & { id: number; openedClasses: number; outcome?: string };
type ApiCurriculum = { code: string; prereq: string[] };

const toProgram = (p: ApiProgram): Program => ({
  id: p.id,
  code: p.code,
  name: p.name,
  description: p.description ?? '',
  hours: p.duration,
  fee: Number(p.standardTuition),
  runningClasses: p.runningClasses,
  active: p.status === 'ACTIVE',
});
const toCurriculum = (list: ApiCurriculum[]): CurriculumItem[] => list.map((c) => ({ code: c.code, prereq: c.prereq ?? [] }));

/** Tải chương trình, môn học và lộ trình của từng chương trình. */
export async function loadTraining() {
  const [programs, subjects] = await Promise.all([
    api<Page<ApiProgram>>('/api/training-programs?size=500'),
    api<Page<ApiSubject>>('/api/subjects'),
  ]);
  replace(PROGRAMS, programs.items.map(toProgram));
  replace(SUBJECTS, subjects.items.map((s) => ({ id: s.id, code: s.code, name: s.name, sessions: s.sessions, weight: Number(s.weight), outcome: s.outcome ?? '', openedClasses: s.openedClasses })));
  const lists = await Promise.all(PROGRAMS.map((p) => api<ApiCurriculum[]>(`/api/training-programs/${enc(p.code)}/subjects`)));
  for (const key of Object.keys(CURRICULUM)) delete CURRICULUM[key];
  PROGRAMS.forEach((p, i) => (CURRICULUM[p.code] = toCurriculum(lists[i])));
}

export function saveProgram(p: Program) {
  const body = { code: p.code, name: p.name, description: p.description, duration: p.hours, standardTuition: p.fee, status: p.active ? 'ACTIVE' : 'INACTIVE' };
  return p.id ? api(`/api/training-programs/${p.id}`, json('PUT', body)) : api('/api/training-programs', json('POST', body));
}
export const trashProgram = (id: number) => api(`/api/training-programs/${id}`, { method: 'DELETE' });

/** oldCode: mã đang lưu trên máy chủ khi sửa (sửa được cả mã); không có là thêm mới. */
export function saveSubject(s: Subject, programCodes: string[], oldCode?: string) {
  const body = { code: s.code, name: s.name, sessions: s.sessions, weight: s.weight, outcome: s.outcome, programCodes };
  return oldCode ? api(`/api/subjects/${enc(oldCode)}`, json('PUT', body)) : api('/api/subjects', json('POST', body));
}
export const trashSubject = (code: string) => api(`/api/subjects/${enc(code)}`, { method: 'DELETE' });

const curriculumPath = (program: string) => `/api/training-programs/${enc(program)}/subjects`;
const curriculum = (p: Promise<ApiCurriculum[]>) => p.then(toCurriculum);
export const addToCurriculum = (program: string, subjectCode: string) => curriculum(api(curriculumPath(program), json('POST', { subjectCode })));
export const removeFromCurriculum = (program: string, subject: string) => curriculum(api(`${curriculumPath(program)}/${enc(subject)}`, { method: 'DELETE' }));
export const reorderCurriculum = (program: string, subjectCodes: string[]) => curriculum(api(curriculumPath(program), json('PUT', { subjectCodes })));
export const setPrereq = (program: string, subject: string, prereq: string[]) =>
  curriculum(api(`${curriculumPath(program)}/${enc(subject)}/prerequisites`, json('PUT', { prereq })));

// ===== S2-07 buổi học =====

const lessonsPath = (subject: string) => `/api/subjects/${enc(subject)}/lessons`;
const lessons = (p: Promise<{ topic: string; goal: string }[]>): Promise<Lesson[]> => p.then((list) => list.map((l) => ({ topic: l.topic, goal: l.goal })));
export const loadLessons = (subject: string) => lessons(api(lessonsPath(subject)));
export const addLesson = (subject: string, l: Lesson) => lessons(api(lessonsPath(subject), json('POST', l)));
/** index tính từ 0 như trên giao diện; máy chủ đánh số buổi từ 1. */
export const updateLesson = (subject: string, index: number, l: Lesson) => lessons(api(`${lessonsPath(subject)}/${index + 1}`, json('PUT', l)));
export const deleteLesson = (subject: string, index: number) => lessons(api(`${lessonsPath(subject)}/${index + 1}`, { method: 'DELETE' }));
export const cloneLessons = (subject: string, from: string) =>
  lessons(api(`${lessonsPath(subject)}/clone`, json('POST', { fromSubjectCode: from, overwrite: true })));

// ===== S2-08 đến S2-11 lead =====

export const LEAD_SOURCE_CODES: Record<string, string> = { 'Biểu mẫu web': 'WEBSITE', Facebook: 'FACEBOOK', Zalo: 'ZALO', 'Giới thiệu': 'REFERRAL' };
export const LEAD_STATUS_CODES: Record<LeadStatus, string> = {
  Mới: 'NEW',
  'Đang chăm sóc': 'CONSULTING',
  'Hẹn gọi lại': 'CONTACTED',
  'Đã nhập học': 'WON',
  'Không quan tâm': 'REJECTED',
};

type ApiLead = {
  id: number; name: string; phone: string; email?: string; sourceLabel: string; programCode?: string; programName?: string;
  statusLabel: string; ownerId?: number; ownerName?: string; note?: string; createdAt: string;
};
const toLead = (l: ApiLead): Lead => ({
  id: l.id,
  name: l.name,
  phone: l.phone,
  email: l.email,
  source: l.sourceLabel,
  program: l.programName ?? '',
  status: l.statusLabel as LeadStatus,
  owner: l.ownerName ?? '',
  ownerId: l.ownerId,
  createdAt: l.createdAt.slice(0, 10),
  note: l.note,
});

export type PublicProgram = { code: string; name: string };
/** Chương trình đang tuyển sinh; dùng được cả khi chưa đăng nhập (biểu mẫu tư vấn) và với tư vấn viên. */
export const loadPublicPrograms = () => api<PublicProgram[]>('/api/public/programs');

export type LeadQuery = { q: string; status: string; source: string; owner: string; from: string; to: string; page: number; size: number };
export async function searchLeads(f: LeadQuery): Promise<Page<Lead>> {
  const params = new URLSearchParams({ page: String(f.page), size: String(f.size) });
  if (f.q.trim()) params.set('q', f.q.trim());
  if (f.status) params.set('status', LEAD_STATUS_CODES[f.status as LeadStatus]);
  if (f.source) params.set('source', LEAD_SOURCE_CODES[f.source] ?? f.source);
  if (f.owner) params.set('owner', f.owner);
  if (f.from) params.set('from', f.from);
  if (f.to) params.set('to', f.to);
  const res = await api<Page<ApiLead>>('/api/leads?' + params);
  return { items: res.items.map(toLead), total: res.total };
}

export async function samePhoneLeads(phone: string, excludeId?: number) {
  const params = new URLSearchParams({ phone });
  if (excludeId) params.set('excludeId', String(excludeId));
  return (await api<ApiLead[]>('/api/leads/check-phone?' + params)).map(toLead);
}

/** programs: danh sách {code, name} để đổi tên chương trình trên form sang mã. */
export function saveLead(l: Lead, programs: PublicProgram[]) {
  const body = {
    name: l.name,
    phone: l.phone,
    email: l.email,
    source: LEAD_SOURCE_CODES[l.source] ?? l.source,
    programCode: programs.find((p) => p.name === l.program)?.code,
    status: LEAD_STATUS_CODES[l.status] ?? l.status,
    note: l.note,
  };
  return l.id ? api(`/api/leads/${l.id}`, json('PUT', body)) : api('/api/leads', json('POST', body));
}
export const trashLead = (id: number) => api(`/api/leads/${id}`, { method: 'DELETE' });

export type Counselor = { id: number; name: string; openLeads: number };
export const loadCounselors = () => api<Counselor[]>('/api/leads/counselors');
export const assignLeads = (leadIds: number[], counselorId: number, note: string) =>
  api<{ assigned: number }>('/api/leads/assign', json('POST', { leadIds, counselorId, note }));

export const submitConsultation = (f: { name: string; phone: string; email: string; programCode: string; message: string }) =>
  api('/api/public/consultations', json('POST', f));

// ===== Thùng rác (S2, nhận xét mentor) =====

export type ServerTrashKind = 'program' | 'subject' | 'lead' | 'user';
const TRASH_BASE: Record<ServerTrashKind, string> = {
  program: '/api/training-programs',
  subject: '/api/subjects',
  lead: '/api/leads',
  user: '/api/admin/users',
};
export type ServerTrashItem = { kind: ServerTrashKind; serverId: number; label: string; deletedAt: string; deletedBy: string };
type ApiTrash = { id: number; code?: string; name?: string; fullName?: string; email?: string; phone?: string; deletedAt: string; deletedBy?: string };

export async function loadTrash(kinds: ServerTrashKind[]): Promise<ServerTrashItem[]> {
  const lists = await Promise.all(kinds.map((k) => api<ApiTrash[]>(TRASH_BASE[k] + '/trash')));
  return lists
    .flatMap((list, i) =>
      list.map((t) => ({
        kind: kinds[i],
        serverId: t.id,
        label: t.fullName ? `${t.fullName} (${t.email})` : t.code ? `${t.name} (${t.code})` : `${t.name}${t.phone ? ' · ' + t.phone : ''}`,
        deletedAt: t.deletedAt,
        deletedBy: t.deletedBy ?? '',
      })),
    )
    .sort((a, b) => b.deletedAt.localeCompare(a.deletedAt));
}
/** Khôi phục bản ghi trong thùng rác máy chủ (cũng dùng cho nút Hoàn tác ngay sau khi xoá). */
export const restoreItem = (kind: ServerTrashKind, id: number) => api(`${TRASH_BASE[kind]}/${id}/restore`, { method: 'POST' });
export const purgeFromTrash = (t: ServerTrashItem) => api(`${TRASH_BASE[t.kind]}/${t.serverId}/purge`, { method: 'DELETE' });
