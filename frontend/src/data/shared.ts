// Dữ liệu dùng chung giữa các menu khi có backend. Trước đây mỗi trang tự tải phần mình cần nên trang chưa có API riêng
// (lịch, điểm danh, học phí, báo cáo, dashboard...) đọc dữ liệu mẫu hoặc ảnh chụp của trang mở trước đó, và số liệu
// giữa các menu, giữa các vai trò lệch nhau. AppLayout gọi loadShared một lần cho mỗi người đăng nhập và vai trò đang
// dùng, nên mọi menu của vai trò đó đọc cùng một bản dữ liệu máy chủ. Phần vai trò không có quyền xem thì giữ nguyên.
// Tải xong thì App dựng lại trang đang mở (sharedVersion), vì trang tính danh sách từ các mảng này trước khi AppLayout tải xong.
import { CLASSES } from './classes';
import { LEADS } from './leads';
import { SESSIONS } from './schedule';
import { loadUsers } from './s1api';
import { loadTraining, searchLeads } from './s2api';
import { loadClasses, loadFunnel, loadMyClasses, loadRooms, loadStaff, loadStudents } from './s3api';

async function loadLeads() {
  const out = [];
  for (let page = 1; ; page++) {
    const res = await searchLeads({ q: '', status: '', source: '', owner: '', from: '', to: '', page, size: 200 });
    out.push(...res.items);
    if (out.length >= res.total || !res.items.length) break;
  }
  LEADS.splice(0, LEADS.length, ...out);
}

/** Lớp theo quyền: có CLASS_READ thì mọi lớp (trang "lớp của tôi" tự lọc), không có thì chỉ lớp mình dạy. */
export const loadClassScope = (codes: string[] | undefined, me: string) =>
  codes?.includes('CLASS_READ') ? Promise.all([loadClasses(), loadStaff(), loadRooms()]) : loadMyClasses(me);

/** codes: mã quyền máy chủ của vai trò đang dùng. Lỗi từng phần bỏ qua để trang vẫn mở, trang đó tự báo khi tải lại. */
export async function loadShared(codes: string[], me: string) {
  const has = (...c: string[]) => c.some((x) => codes.includes(x));
  const tasks: Promise<unknown>[] = [];
  if (has('PROGRAM_READ', 'PROGRAM_MANAGE')) tasks.push(loadTraining());
  if (has('STUDENT_READ')) tasks.push(loadStudents());
  // Lịch buổi học (S4) chưa có API, lưu ở trình duyệt: bỏ buổi của lớp không còn trên máy chủ (lớp mẫu, lớp đã xoá).
  const pruneSessions = () => SESSIONS.splice(0, SESSIONS.length, ...SESSIONS.filter((x) => CLASSES.some((k) => k.code === x.cls)));
  if (has('CLASS_READ')) tasks.push(loadClassScope(codes, me).then(pruneSessions));
  if (has('LEAD_READ', 'LEAD_MANAGE')) tasks.push(loadFunnel(), loadLeads());
  if (has('USER_READ')) tasks.push(loadUsers());
  await Promise.all(tasks.map((t) => t.catch(() => undefined)));
}

/** Bản đã tải cho người dùng, vai trò và quyền nào (key). Tải lại trang (F5) thì tải lại từ máy chủ. */
let current = { key: '', done: false };
let version = 0;
const listeners = new Set<() => void>();
export const subscribeShared = (f: () => void) => (listeners.add(f), () => void listeners.delete(f));
export const sharedVersion = () => version;
export const sharedReady = (key: string) => current.key === key && current.done;

export function ensureShared(key: string, codes: string[], me: string) {
  if (current.key === key) return;
  const next = { key, done: false };
  current = next;
  loadShared(codes, me).then(() => {
    next.done = true;
    if (current !== next) return;
    version++;
    listeners.forEach((f) => f());
  });
}
