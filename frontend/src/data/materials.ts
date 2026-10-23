// Học liệu theo buổi học (S6-05, S6-06). Khi tích hợp: GET/POST /sessions/:id/materials, DELETE /materials/:id,
// PATCH /sessions/:id { openEarly }. Máy chủ chỉ cho học viên của lớp tải tệp và chỉ khi buổi đã diễn ra hoặc được mở sớm.
import { addDays, sessionsOf } from './schedule';
import { TODAY } from './funnel';
import { persisted } from './store';

export type Material = { id: number; session: string; kind: 'Tệp' | 'Liên kết'; name: string; size?: number; url?: string; by: string; at: string };

export const MAX_MATERIAL = 50 * 1048576;

function seed(): Material[] {
  const out: Material[] = [];
  const list = sessionsOf('WEB-K15').filter((s) => !s.cancelled);
  const past = list.filter((s) => s.date < TODAY).slice(-6);
  const next = list.find((s) => s.date >= TODAY);
  let id = 1;
  for (const s of past) {
    const at = addDays(s.date, -1) + 'T21:30';
    out.push({ id: id++, session: s.id, kind: 'Tệp', name: `slide-bai-${s.lesson}.pdf`, size: 1600000 + s.lesson * 37000, by: s.teacher ?? 'VU DUC LONG', at });
    if (s.lesson % 2) out.push({ id: id++, session: s.id, kind: 'Tệp', name: `ma-mau-bai-${s.lesson}.zip`, size: 348160, by: s.teacher ?? 'VU DUC LONG', at: at.replace('30', '32') });
    if (s.lesson % 3 === 0) out.push({ id: id++, session: s.id, kind: 'Liên kết', name: 'Tài liệu tham khảo (MDN)', url: 'https://developer.mozilla.org/vi/docs/Web/JavaScript', by: 'PHAM GIA HUY', at: s.date + 'T08:05' });
  }
  if (next) {
    const atNow = TODAY + 'T08:30';
    out.push(
      { id: id++, session: next.id, kind: 'Tệp', name: `slide-bai-${next.lesson}-thiet-ke-luoc-do-erd.pdf`, size: 2450000, by: 'VU DUC LONG', at: atNow },
      { id: id++, session: next.id, kind: 'Tệp', name: 'script-mau-tao-bang-postgresql.sql', size: 18432, by: 'VU DUC LONG', at: atNow },
      { id: id++, session: next.id, kind: 'Tệp', name: 'so-do-thuc-the-quan-he-erd.png', size: 852000, by: 'VU DUC LONG', at: atNow },
      { id: id++, session: next.id, kind: 'Liên kết', name: 'Tài liệu hướng dẫn PostgreSQL (Official Docs)', url: 'https://www.postgresql.org/docs/current/', by: 'VU DUC LONG', at: atNow },
    );
  }
  return out;
}

export const MATERIALS: Material[] = persisted('materials', seed());
/** Buổi được giảng viên mở tài liệu sớm cho học viên. */
export const EARLY: Record<string, boolean> = persisted('materials-early', (() => {
  const next = sessionsOf('WEB-K15').find((s) => !s.cancelled && s.date >= TODAY);
  return next ? { [next.id]: true } : {};
})());

export const materialsOf = (session: string) => MATERIALS.filter((m) => m.session === session);
