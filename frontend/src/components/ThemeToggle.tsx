import { createContext, useContext, useEffect, useLayoutEffect, useState, type MouseEvent, type ReactNode } from 'react';
import { flushSync } from 'react-dom';
import { useAuth } from '../data/auth';

// Giao diện sáng/tối. Lưu theo tài khoản (tms.theme.<email>), chưa đăng nhập thì theo máy (tms.theme).
// Lần đầu theo cài đặt hệ điều hành (prefers-color-scheme).
type Theme = 'light' | 'dark';

const key = (email?: string) => 'tms.theme' + (email ? '.' + email : '');

function stored(email?: string): Theme {
  try {
    const t = (email && localStorage.getItem(key(email))) || localStorage.getItem(key());
    if (t === 'light' || t === 'dark') return t;
  } catch {}
  return matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

const ThemeContext = createContext<[Theme, (t: Theme) => void]>(null!);

export function ThemeProvider({ children }: { children: ReactNode }) {
  const email = useAuth().user?.email;
  const [theme, setTheme] = useState(() => stored(email));
  useEffect(() => setTheme(stored(email)), [email]);
  // Layout effect để thuộc tính đổi ngay trong flushSync, kịp cho View Transitions chụp trạng thái mới.
  useLayoutEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);
  return <ThemeContext.Provider value={[theme, setTheme]}>{children}</ThemeContext.Provider>;
}

export function ThemeToggle({ className = '' }: { className?: string }) {
  const [theme, setTheme] = useContext(ThemeContext);
  const email = useAuth().user?.email;
  const dark = theme === 'dark';
  const label = dark ? 'Chuyển sang giao diện sáng' : 'Chuyển sang giao diện tối';

  const flip = (e: MouseEvent<HTMLButtonElement>) => {
    const next: Theme = dark ? 'light' : 'dark';
    try {
      localStorage.setItem(key(), next);
      if (email) localStorage.setItem(key(email), next);
    } catch {}
    const apply = () => flushSync(() => setTheme(next));
    if (!('startViewTransition' in document) || matchMedia('(prefers-reduced-motion: reduce)').matches) return apply();

    // Giao diện mới lan ra theo hình tròn tâm là nút, bán kính tới góc xa nhất, 600ms ease-in-out.
    const b = e.currentTarget.getBoundingClientRect();
    const x = b.left + b.width / 2;
    const y = b.top + b.height / 2;
    const r = Math.hypot(Math.max(x, innerWidth - x), Math.max(y, innerHeight - y));
    const opt = { duration: 600, easing: 'ease-in-out' };
    document.startViewTransition(apply).ready.then(() => {
      document.documentElement.animate({ clipPath: [`circle(0 at ${x}px ${y}px)`, `circle(${r}px at ${x}px ${y}px)`] }, { ...opt, pseudoElement: '::view-transition-new(root)' });
      const ring = document.body.appendChild(document.createElement('div'));
      ring.className = 'theme-ring';
      ring.style.left = x + 'px';
      ring.style.top = y + 'px';
      // Mép vòng tròn mờ dần ở 80ms cuối.
      ring.animate([{ width: 0, height: 0, opacity: 1 }, { opacity: 1, offset: 520 / 600 }, { width: 2 * r + 'px', height: 2 * r + 'px', opacity: 0 }], opt).finished.then(() => ring.remove());
    });
  };

  return (
    <button type="button" className={'theme-btn ' + className} aria-pressed={dark} aria-label={label} title={label} onClick={flip}>
      <svg className="sun" viewBox="0 0 24 24" stroke="url(#theme-sun)" aria-hidden="true">
        <defs>
          <linearGradient id="theme-sun" gradientUnits="userSpaceOnUse" x1="2" y1="2" x2="22" y2="22">
            <stop stopColor="#D4145A" />
            <stop offset="1" stopColor="#FBB03B" />
          </linearGradient>
        </defs>
        <circle cx="12" cy="12" r="4" />
        <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
      </svg>
      <svg className="moon" viewBox="0 0 24 24" stroke="url(#theme-moon)" aria-hidden="true">
        <defs>
          <linearGradient id="theme-moon" gradientUnits="userSpaceOnUse" x1="3" y1="3" x2="21" y2="21">
            <stop stopColor="#0575E6" />
            <stop offset="1" stopColor="#00F260" />
          </linearGradient>
        </defs>
        <path d="M20 14.5A8.5 8.5 0 0 1 9.5 4 8.5 8.5 0 1 0 20 14.5z" />
      </svg>
    </button>
  );
}
