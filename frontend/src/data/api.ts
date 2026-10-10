// Gọi backend Java khi có biến môi trường VITE_API_URL (ví dụ http://localhost:8080).
// Vite chuyển tiếp /api sang địa chỉ đó (vite.config.js) nên cookie phiên dùng chung một origin.
// Không đặt VITE_API_URL thì các trang chạy bằng dữ liệu mẫu trong data/*.ts như trước.

export const apiEnabled = !!import.meta.env.VITE_API_URL;

/** Lỗi máy chủ trả về: code là mã nghiệp vụ, ví dụ PHONE_DUPLICATE, CURRICULUM_CHANGED. */
export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message);
  }
}

type Envelope<T> = { success?: boolean; data?: T; message?: string; error?: { code?: string; message?: string } };

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const isForm = init.body instanceof FormData;
  let res: Response;
  try {
    res = await fetch(path, {
      credentials: 'include',
      ...init,
      headers: { Accept: 'application/json', ...(init.body && !isForm ? { 'Content-Type': 'application/json' } : {}), ...init.headers },
    });
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.');
  }
  const body: Envelope<T> | null = await res.json().catch(() => null);
  if (!res.ok || body?.success === false) {
    throw new ApiError(res.status, body?.error?.code ?? 'HTTP_' + res.status, body?.error?.message ?? 'Máy chủ báo lỗi (' + res.status + ').');
  }
  return body?.data as T;
}

export const json = (method: string, data: unknown): RequestInit => ({ method, body: JSON.stringify(data) });
