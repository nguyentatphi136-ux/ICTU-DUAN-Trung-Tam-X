// Dữ liệu mẫu được lưu vào localStorage để tải lại trang không mất thay đổi (chế độ chưa có backend).
// Mỗi mảng/đối tượng đăng ký một khoá; trang vẫn sửa trực tiếp như cũ, khi rời hoặc tải lại trang thì ghi lại tất cả.

const PREFIX = 'tms.data.v1.';
const registry = new Map<string, unknown>();

/** Giá trị đã lưu của khoá này, hoặc seed nếu chưa có hay dữ liệu hỏng. */
export function persisted<T>(key: string, seed: T): T {
  let value = seed;
  try {
    const raw = localStorage.getItem(PREFIX + key);
    if (raw) value = JSON.parse(raw) as T;
  } catch {
    // Trình duyệt chặn localStorage hoặc dữ liệu hỏng: dùng dữ liệu mẫu.
  }
  registry.set(key, value);
  return value;
}

/** Ghi ngay mọi dữ liệu đã đăng ký (gọi sau thao tác quan trọng như lưu hồ sơ). */
export function saveAll() {
  for (const [key, value] of registry) {
    try {
      localStorage.setItem(PREFIX + key, JSON.stringify(value));
    } catch {
      // Hết dung lượng hoặc bị chặn: bỏ qua, dữ liệu vẫn còn trong phiên này.
    }
  }
}

if (typeof window !== 'undefined') {
  window.addEventListener('pagehide', saveAll);
  document.addEventListener('visibilitychange', () => document.visibilityState === 'hidden' && saveAll());
}
