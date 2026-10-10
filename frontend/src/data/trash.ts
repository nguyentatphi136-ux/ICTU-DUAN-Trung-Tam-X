// Thùng rác (xoá mềm) cho dữ liệu mẫu: bản ghi bị xoá được chuyển vào đây, khôi phục được hoặc xoá vĩnh viễn.
// Khi có backend: chương trình và tài khoản dùng /api/training-programs/trash, /api/admin/users/trash.
import { LEADS, type Lead } from './leads';
import { persisted } from './store';
import { CURRICULUM, LESSONS, PROGRAMS, SUBJECTS, type CurriculumItem, type Lesson, type Program, type Subject } from './training';
import { USERS, type UserRow } from './users';

export type TrashKind = 'program' | 'subject' | 'lesson' | 'lead' | 'user';

type Payload =
  | { kind: 'program'; item: Program; curriculum?: CurriculumItem[] }
  | { kind: 'subject'; item: Subject; usedIn: { program: string; index: number; prereq: string[] }[] }
  | { kind: 'lesson'; item: Lesson; subject: string; index: number }
  | { kind: 'lead'; item: Lead }
  | { kind: 'user'; item: UserRow };

/** at: vị trí trong danh sách gốc lúc bị xoá, để khôi phục về đúng chỗ. */
export type TrashEntry = Payload & { id: string; label: string; deletedAt: string; deletedBy: string; at: number };

export const TRASH: TrashEntry[] = persisted('trash', []);

export const KIND_LABEL: Record<TrashKind, string> = {
  program: 'Chương trình',
  subject: 'Môn học',
  lesson: 'Buổi học',
  lead: 'Lead',
  user: 'Tài khoản',
};

/** Chuyển một bản ghi vào thùng rác và gỡ khỏi danh sách gốc. Trả về id để hoàn tác. */
export function moveToTrash(payload: Payload, label: string, by: string): string {
  const id = payload.kind + ':' + Date.now() + ':' + Math.random().toString(36).slice(2, 6);
  let at = 0;
  switch (payload.kind) {
    case 'program':
      at = remove(PROGRAMS, (p) => p.code === payload.item.code);
      payload.curriculum = CURRICULUM[payload.item.code];
      delete CURRICULUM[payload.item.code];
      break;
    case 'subject':
      at = remove(SUBJECTS, (s) => s.code === payload.item.code);
      payload.usedIn = [];
      for (const [program, list] of Object.entries(CURRICULUM)) {
        const index = list.findIndex((c) => c.code === payload.item.code);
        if (index < 0) continue;
        payload.usedIn.push({ program, index, prereq: list[index].prereq });
        list.splice(index, 1);
        list.forEach((c) => (c.prereq = c.prereq.filter((p) => p !== payload.item.code)));
      }
      break;
    case 'lesson':
      LESSONS[payload.subject]?.splice(payload.index, 1);
      break;
    case 'lead':
      at = remove(LEADS, (l) => l.id === payload.item.id);
      break;
    case 'user':
      at = remove(USERS, (u) => u.id === payload.item.id);
      break;
  }
  TRASH.unshift({ ...payload, id, label, deletedAt: new Date().toISOString(), deletedBy: by, at } as TrashEntry);
  return id;
}

/** Đưa bản ghi về chỗ cũ. Trả về lý do nếu không khôi phục được (ví dụ mã đã bị dùng lại). */
export function restore(id: string): string | null {
  const entry = TRASH.find((t) => t.id === id);
  if (!entry) return 'Không còn trong thùng rác.';
  switch (entry.kind) {
    case 'program':
      if (PROGRAMS.some((p) => p.code === entry.item.code)) return `Mã ${entry.item.code} đã được dùng cho chương trình khác.`;
      insert(PROGRAMS, entry.at, entry.item);
      if (entry.curriculum) CURRICULUM[entry.item.code] = entry.curriculum.filter((c) => SUBJECTS.some((s) => s.code === c.code));
      break;
    case 'subject':
      if (SUBJECTS.some((s) => s.code === entry.item.code)) return `Mã ${entry.item.code} đã được dùng cho môn khác.`;
      insert(SUBJECTS, entry.at, entry.item);
      for (const u of entry.usedIn) {
        const list = CURRICULUM[u.program];
        if (list) list.splice(Math.min(u.index, list.length), 0, { code: entry.item.code, prereq: u.prereq.filter((p) => list.some((c) => c.code === p)) });
      }
      break;
    case 'lesson': {
      const subject = SUBJECTS.find((s) => s.code === entry.subject);
      const list = (LESSONS[entry.subject] ??= []);
      if (subject && list.length >= subject.sessions) return `Môn ${subject.name} đã đủ ${subject.sessions} buổi.`;
      list.splice(Math.min(entry.index, list.length), 0, entry.item);
      break;
    }
    case 'lead':
      insert(LEADS, entry.at, entry.item);
      break;
    case 'user':
      if (USERS.some((u) => u.email === entry.item.email)) return `Email ${entry.item.email} đã được dùng cho tài khoản khác.`;
      insert(USERS, entry.at, entry.item);
      break;
  }
  remove(TRASH, (t) => t.id === id);
  return null;
}

/** Xoá vĩnh viễn, không hoàn tác được. */
export function purge(id: string) {
  remove(TRASH, (t) => t.id === id);
}

/** Gỡ phần tử khớp đầu tiên, trả về vị trí của nó (0 nếu không có). */
function remove<T>(list: T[], match: (x: T) => boolean): number {
  const i = list.findIndex(match);
  if (i >= 0) list.splice(i, 1);
  return Math.max(i, 0);
}

function insert<T>(list: T[], at: number | undefined, item: T) {
  list.splice(Math.min(at ?? 0, list.length), 0, item);
}
