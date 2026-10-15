import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { toAccount, type Account, type ServerUser } from '../pages/S1-01-dang-nhap/mockAuth';
import { api, isOffline } from './api';

// Người đang đăng nhập và vai trò đang dùng (S1-01, S1-02, S1-06).
// Lưu trong sessionStorage để tải lại trang không mất phiên; khi tải trang hỏi lại GET /api/auth/me,
// máy chủ báo hết phiên thì đăng xuất, máy chủ chưa bật thì giữ nguyên (chế độ dữ liệu mẫu).
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

  useEffect(() => {
    if (!read()) return;
    api<ServerUser>('GET', '/auth/me')
      .then((me) => {
        const a = toAccount(me);
        setUser((u) => {
          // Vai trò đổi ở máy chủ có hiệu lực ngay; giữ vai trò đang dùng nếu vẫn còn.
          const next = { ...a, active: u && a.roles.includes(u.active) ? u.active : a.roles[0] };
          sessionStorage.setItem(KEY, JSON.stringify(next));
          return next;
        });
      })
      .catch((e) => {
        if (!isOffline(e)) save(null);
      });
  }, []);

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
          if (user) api('POST', '/auth/logout').catch(() => {});
          save(null);
        },
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
