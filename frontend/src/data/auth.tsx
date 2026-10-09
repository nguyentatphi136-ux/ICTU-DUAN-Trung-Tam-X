import { createContext, useContext, useState, type ReactNode } from 'react';
import type { Account } from '../pages/S1-01-dang-nhap/mockAuth';

// Người đang đăng nhập và vai trò đang dùng (S1-01, S1-02, S1-06).
// Lưu trong sessionStorage để tải lại trang không mất phiên. Khi có backend: thay bằng token và GET /auth/me.
export type User = Account & { active: number };

type Auth = {
  user: User | null;
  signIn: (a: Account) => void;
  signOut: () => void;
  switchRole: (role: number) => void;
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
        signOut: () => save(null),
        switchRole: (role) => user && save({ ...user, active: role }),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);

/** Tên gọi ngắn ở lời chào: chữ cuối của họ tên. */
export const shortName = (name: string) => name.trim().split(/\s+/).pop() ?? name;
