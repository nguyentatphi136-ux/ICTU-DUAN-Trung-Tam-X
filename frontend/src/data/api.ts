// Gọi backend Java qua /api (khi chạy npm run dev, vite chuyển sang localhost:8080; phiên giữ bằng cookie JSESSIONID).
// Máy chủ không chạy thì ném lỗi status 0 (isOffline) để trang quay về dữ liệu mẫu như trước.
export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public detail?: Record<string, unknown>,
  ) {
    super(message);
  }
}

export async function api<T = unknown>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  let res: Response;
  try {
    res = await fetch('/api' + path, { method, headers, credentials: 'include', body: body === undefined ? undefined : JSON.stringify(body) });
  } catch {
    throw new ApiError(0, 'OFFLINE', 'Không kết nối được máy chủ');
  }
  const json = await res.json().catch(() => null);
  // Proxy của vite trả 500 không có JSON khi backend chưa bật.
  if (!json) throw new ApiError(0, 'OFFLINE', 'Không kết nối được máy chủ');
  if (!res.ok || json.success === false) {
    const err = json.error ?? {};
    throw new ApiError(res.status, err.code ?? 'ERROR', err.message ?? json.message ?? 'Đã có lỗi, vui lòng thử lại', err);
  }
  return json.data as T;
}

export const isOffline = (e: unknown) => e instanceof ApiError && e.status === 0;

/** Mã vai trò ở máy chủ, cùng thứ tự với ROLES trong permissions.ts. */
export const ROLE_CODES = ['STUDENT', 'TA', 'INSTRUCTOR', 'ADMISSIONS', 'ACCOUNTANT', 'TRAINING_MANAGER', 'ADMIN'];

/** Nhận cả "TRAINING_MANAGER" lẫn "training-manager"; trả chỉ số trong ROLES, xếp quyền giảm dần, bỏ vai trò lạ. */
export const roleIndexes = (codes: string[]) =>
  codes
    .map((c) => ROLE_CODES.indexOf(c.toUpperCase().replace(/-/g, '_')))
    .filter((i) => i >= 0)
    .sort((a, b) => b - a);
