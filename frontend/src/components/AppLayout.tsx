import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Link, Navigate, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { api, apiEnabled, SESSION_EXPIRED } from '../data/api';
import { useAuth } from '../data/auth';
import { canAccess, menuFor, ROLES } from '../data/permissions';
import { ROLE_CODES } from '../data/s1api';
import { ensureShared, sharedReady } from '../data/shared';
import { ErrorPage } from '../pages/S1-07-trang-loi/ErrorPage';
import { Icon } from './Icon';
import { NotificationBell } from './NotificationBell';
import { ThemeToggle } from './ThemeToggle';
import { LoadState, useTitle } from './ui';

type Props = {
  /** Đường dẫn trên thanh trên, phần tử cuối là trang hiện tại. */
  crumb: string[];
  /** Tên module trong MODULES. Vai trò không có quyền sẽ thấy trang 403. */
  module?: string;
  children: ReactNode;
};

const slug = (s: string) => encodeURIComponent(s);
export const moduleHref = (name: string, to?: string) => to ?? '/module/' + slug(name);

// S1-06. Layout chung: sidebar trái 248px theo vai trò, thanh trên 64px có tên và vai trò, menu người dùng.
// Chưa đăng nhập thì chuyển về trang đăng nhập và quay lại đúng trang sau khi đăng nhập (S1-02).
export function AppLayout({ crumb, module, children }: Props) {
  const { user, switchRole, updateUser } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [menu, setMenu] = useState(false);
  useTitle(crumb[crumb.length - 1]);
  const menuRef = useRef<HTMLDivElement>(null);
  const [permsFailed, setPermsFailed] = useState(false);
  // Chưa tải xong quyền máy chủ (hoặc không có backend) thì codes là undefined: dùng ma trận sheet 2.
  const codes = apiEnabled && user ? user.perms?.[ROLE_CODES[user.active]] : undefined;
  // Đổi người đăng nhập, vai trò hoặc quyền thì tải lại dữ liệu dùng chung trước khi hiện trang.
  const sharedKey = user && codes ? `${user.email}|${user.active}|${codes.join()}` : '';
  useEffect(() => {
    if (sharedKey && !user?.mustChangePassword) ensureShared(sharedKey, codes!, user!.name);
  }, [sharedKey]);
  const ready = !apiEnabled || (sharedKey ? sharedReady(sharedKey) : permsFailed);

  useEffect(() => setOpen(false), [location.pathname]);
  // S1-02: phiên hết hạn phía máy chủ thì về trang đăng nhập kèm thông báo, đăng nhập lại thì quay về đúng trang này.
  useEffect(() => {
    const expired = () => navigate('/dang-nhap', { replace: true, state: { expired: true, email: user?.email, from: location.pathname } });
    window.addEventListener(SESSION_EXPIRED, expired);
    return () => window.removeEventListener(SESSION_EXPIRED, expired);
  }, [navigate, user?.email, location.pathname]);
  // Menu theo quyền máy chủ: tải lại mỗi lần mở trang, đổi quyền ở S1-05 thì lần mở trang kế tiếp menu đổi theo.
  useEffect(() => {
    if (!apiEnabled || !user || user.mustChangePassword) return;
    api<Record<string, string[]>>('/api/me/permissions')
      .then((p) => JSON.stringify(p) !== JSON.stringify(user.perms) && updateUser({ perms: p }))
      .catch(() => setPermsFailed(true));
  }, [location.pathname]);
  useEffect(() => {
    if (!menu) return;
    const f = (e: MouseEvent) => !menuRef.current?.contains(e.target as Node) && setMenu(false);
    document.addEventListener('mousedown', f);
    return () => document.removeEventListener('mousedown', f);
  }, [menu]);

  if (!user) return <Navigate to="/dang-nhap" replace state={{ from: location.pathname + location.search }} />;
  if (user.mustChangePassword) return <Navigate to="/doi-mat-khau" replace state={{ from: location.pathname + location.search }} />;
  if (module && !canAccess(user.active, module, codes)) return <ErrorPage code="403" />;

  const multi = user.roles.length > 1;
  const items = menuFor(user.active, codes);
  const logout = () => {
    // Trang đăng nhập mới xoá phiên, để chuyển trang xong trước rồi mới mất user (tránh bị chuyển hướng kiểu "chưa đăng nhập").
    navigate('/dang-nhap', { replace: true, state: { loggedOut: true } });
  };
  const pick = (role: number) => {
    setMenu(false);
    switchRole(role);
    navigate('/');
  };

  const roleList = multi && (
    <div className="role-list">
      <div className="um-label">Chuyển vai trò</div>
      {user.roles.map((r) =>
        r === user.active ? (
          <div key={r} className="role on">
            <span>
              <b>{ROLES[r]}</b>
              <small>Đang dùng</small>
            </span>
            <Icon name="check" />
          </div>
        ) : (
          <button key={r} type="button" className="role" onClick={() => pick(r)}>
            <span>
              <b>{ROLES[r]}</b>
              <small>Chuyển sang vai trò này</small>
            </span>
            <Icon name="swap" />
          </button>
        ),
      )}
    </div>
  );

  return (
    <div className={'app' + (open ? ' open' : '')}>
      {open && <div className="scrim" onClick={() => setOpen(false)} />}
      <aside className="side">
        <Link to="/" className="logo">
          <span>
            <Icon name="cap" />
          </span>
          TMS.
        </Link>
        <div className="me">
          <span className="ava" />
          <div>
            <b>{user.name}</b>
            <span>{ROLES[user.active]}</span>
          </div>
        </div>
        {multi && <div className="me-roles">{roleList}</div>}
        <nav className="nav">
          {items.map((m) => (
            <NavLink key={m.name} to={moduleHref(m.name, m.to)} end={m.to === '/'} className={({ isActive }) => (isActive || (!m.exact && (isUnder(location.pathname, m.to) || (!!module && m.name === module))) ? 'on' : undefined)}>
              <Icon name={m.icon} />
              {m.name}
            </NavLink>
          ))}
        </nav>
        <button type="button" className="out" onClick={logout}>
          <Icon name="out" />
          Đăng xuất
        </button>
      </aside>

      <div className="main">
        <header className="top">
          <button type="button" className="burger" aria-label="Mở menu" onClick={() => setOpen(true)}>
            <Icon name="menu" />
          </button>
          <Link to="/" className="logo top-logo">
            <span>
              <Icon name="cap" />
            </span>
            TMS.
          </Link>
          <div className="crumb">
            {crumb.map((c, i) => (
              <span key={c} className={i < crumb.length - 1 ? 'up' : undefined}>
                {c}
                {i < crumb.length - 1 && <i>/</i>}
              </span>
            ))}
          </div>
          <ThemeToggle />
          <NotificationBell />
          <div className="um" ref={menuRef}>
            <button type="button" className="um-btn" aria-expanded={menu} onClick={() => setMenu((m) => !m)}>
              <span className="who">
                <b>{user.name}</b>
                <span>{ROLES[user.active]}</span>
              </span>
              <span className="ava" />
              {multi && <Icon name="down" />}
            </button>
            {menu && (
              <div className="um-menu" role="menu">
                {multi && (
                  <div className="um-head">
                    <span className="ava" />
                    <div>
                      <b>{user.name}</b>
                      <span>{user.email}</span>
                    </div>
                  </div>
                )}
                {roleList}
                <div className="um-links">
                  <Link to="/ho-so">Hồ sơ cá nhân</Link>
                  <Link to="/doi-mat-khau">Đổi mật khẩu</Link>
                  <Link to="/cai-dat">Cài đặt thông báo</Link>
                  <button type="button" className="danger" onClick={logout}>
                    Đăng xuất
                    <small>Kết thúc phiên trên máy chủ ngay</small>
                  </button>
                </div>
              </div>
            )}
          </div>
        </header>
        <main className="content">{ready ? children : <LoadState loading error="" reload={() => undefined} />}</main>
      </div>
    </div>
  );
}

// Trang con (ví dụ /dao-tao/mon-hoc) vẫn tô sáng mục cha "Chương trình & môn học".
function isUnder(path: string, to?: string) {
  if (!to || to === '/') return false;
  const root = '/' + to.split('/')[1];
  return path.startsWith(root + '/');
}
