// Gọi API quản trị tài khoản (S1-08, S1-09, S1-10) và đổi dữ liệu máy chủ sang kiểu UserRow mà trang Tài khoản dùng.
// Chỉ dùng khi apiEnabled. Danh sách tải về được ghi vào chính mảng USERS như ở chế độ dữ liệu mẫu.
import { api, json } from './api';
import { USERS, type Status, type UserRow } from './users';

/** Mã vai trò của máy chủ, theo đúng thứ tự ROLES (permissions.ts). */
export const ROLE_CODES = ['STUDENT', 'TA', 'INSTRUCTOR', 'ADMISSIONS', 'ACCOUNTANT', 'TRAINING_MANAGER', 'ADMIN'];
const STATUS: Record<string, Status> = { ACTIVE: 'Hoạt động', PENDING: 'Chờ kích hoạt', LOCKED: 'Đã khoá' };

type ApiUser = { id: number; email: string; fullName: string; phone?: string; status?: string; lockedReason?: string | null; roles?: string[] };
type ApiDetail = ApiUser & { userCode?: string; displayName?: string; dateOfBirth?: string; hometown?: string; address?: string; gender?: string; createdAt?: string };
export type AssignedClass = { classCode: string; className: string };

const toUser = (u: ApiUser): UserRow => ({
  id: u.id,
  name: u.fullName,
  email: u.email,
  phone: u.phone ?? '',
  roles: (u.roles ?? []).map((r) => ROLE_CODES.indexOf(r)).filter((i) => i >= 0),
  status: STATUS[u.status ?? 'ACTIVE'] ?? 'Hoạt động',
  lockReason: u.lockedReason ?? undefined,
});
const codes = (roles: number[]) => roles.map((r) => ROLE_CODES[r]);
const sameRoles = (a: number[], b: number[]) => [...a].sort().join() === [...b].sort().join();

/** ponytail: tải hết tài khoản rồi lọc ở trình duyệt (khoảng 300 người dùng theo sheet 1); lọc phía máy chủ khi lớn hơn nhiều. */
export async function loadUsers() {
  const out: UserRow[] = [];
  for (let page = 1; ; page++) {
    const res = await api<{ items: ApiUser[]; totalPages: number }>(`/api/admin/users?page=${page}&pageSize=200`);
    out.push(...res.items.map(toUser));
    if (page >= res.totalPages || !res.items.length) break;
  }
  USERS.splice(0, USERS.length, ...out);
}

/** S1-08: thông tin chi tiết một tài khoản (họ tên viết thường, ngày sinh, quê quán, địa chỉ...). */
export async function loadUserDetail(id: number): Promise<UserRow> {
  const d = await api<ApiDetail>(`/api/admin/users/${id}`);
  return {
    ...toUser(d),
    displayName: d.displayName || d.fullName,
    birth: d.dateOfBirth || undefined,
    hometown: d.hometown || undefined,
    address: d.address || undefined,
    code: d.userCode,
    gender: d.gender,
    createdAt: d.createdAt,
  };
}

/** Họ tên gửi lên như người nhập; máy chủ lưu bản IN HOA không dấu và giữ bản gốc ở displayName. */
const details = (u: UserRow) => ({ fullName: u.displayName ?? u.name, phone: u.phone, dateOfBirth: u.birth ?? '', hometown: u.hometown ?? '', address: u.address ?? '' });

/** S1-08: máy chủ tạo mật khẩu tạm <tên đăng nhập>@<ngày sinh ddMMyy>, gửi email kích hoạt; trả lại mật khẩu tạm. */
export const createUser = (u: UserRow) =>
  api<{ tempPassword?: string }>('/api/admin/users', json('POST', { ...details(u), email: u.email, roles: codes(u.roles) }));

/** S1-08 sửa thông tin (họ tên, số điện thoại, ngày sinh, quê quán, địa chỉ); S1-09 thay bộ vai trò khi có thay đổi. */
export async function saveUser(before: UserRow, u: UserRow) {
  await api(`/api/admin/users/${u.id}`, json('PUT', details(u)));
  if (!sameRoles(u.roles, before.roles)) await api(`/api/admin/users/${u.id}/roles`, json('PUT', { roleCodes: codes(u.roles) }));
}

/** S1-10: khoá (bắt buộc lý do, thu hồi phiên) hoặc mở khoá. Trả về các lớp người đó đang phụ trách cần bàn giao. */
export const setLocked = (id: number, locked: boolean, reason: string) =>
  api<{ assignedClasses?: AssignedClass[] }>(`/api/admin/users/${id}/status`, json('PUT', { status: locked ? 'LOCKED' : 'ACTIVE', lockedReason: reason }));

export const trashUser = (id: number) => api(`/api/admin/users/${id}`, { method: 'DELETE' });

// ===== S1-05 phân quyền: mỗi vai trò một bộ mã quyền, sửa trực tiếp trên máy chủ =====

export type ServerRole = { roleCode: string; roleName: string; permissionCodes: string[] };
export type ServerPermission = { permissionCode: string; permissionName: string; module: string; description?: string };

/** Quyền cốt lõi của Quản trị, máy chủ không cho thu hồi (PermissionDAO.ADMIN_CORE_PERMISSIONS). */
export const ADMIN_CORE = ['ROLE_PERMISSION_READ', 'ROLE_PERMISSION_UPDATE', 'USER_READ', 'USER_ROLE_ASSIGN', 'MENU_VIEW'];

export const loadRoles = () => api<ServerRole[]>('/api/admin/roles');
export const loadPermissions = () => api<ServerPermission[]>('/api/admin/permissions');
export const saveRolePermissions = (roleCode: string, codes: string[]) =>
  api(`/api/admin/roles/${encodeURIComponent(roleCode)}/permissions`, json('PUT', { permissionCodes: codes }));
