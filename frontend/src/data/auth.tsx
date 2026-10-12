import { createContext, useContext, useState, type ReactNode } from 'react';
import { api, apiEnabled } from './api';

// Người đang đăng nhập và vai trò đang dùng (S1-01, S1-02, S1-06).
// Lưu trong sessionStorage để tải lại trang không mất phiên. Khi có backend: thay bằng token và GET /auth/me.
/** roles là chỉ số trong ROLES (src/data/permissions.ts), xếp theo quyền giảm dần: phần tử đầu là vai trò khi vừa đăng nhập. */
export type Account = { email: string; name: string; roles: number[] };
export type User = Account & { active: number };

type Auth = {
  user: User | null;
  signIn: (a: Account) => void;
  signOut: () => void;
  switchRole: (role: number) => void;
  /** Cập nhật tên hiển thị sau khi lưu hồ sơ (S2-02). */
  updateUser: (patch: Partial<Pick<User, 'name'>>) => void;
};

const KEY = 'tms.user';
const AuthContext = createContext<Auth>(null!);

function read(): User | null {
  try {
    return JSON.parse(sessionStorage.getItem(KEY) || 'null');
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(read);

  const save = (u: User | null) => {
    if (u) sessionStorage.setItem(KEY, JSON.stringify(u));
    else sessionStorage.removeItem(KEY);
    setUser(u);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        signIn: (a) => save({ ...a, active: a.roles[0] }),
        signOut: () => {
          // Báo máy chủ huỷ phiên; lỗi mạng không chặn việc đăng xuất ở trình duyệt.
          if (apiEnabled && user) api('/api/auth/logout', { method: 'POST' }).catch(() => {});
          save(null);
        },
        switchRole: (role) => user && save({ ...user, active: role }),
        updateUser: (patch) => user && save({ ...user, ...patch }),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);

/** Tên gọi ngắn ở lời chào: chữ cuối của họ tên. */
export const shortName = (name: string) => name.trim().split(/\s+/).pop() ?? name;
