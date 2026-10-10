import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Icon, type IconName } from './Icon';

// Các mảnh giao diện dùng lại ở nhiều trang: ngăn bên, hộp thoại, nhãn trạng thái, phân trang, menu dòng, hộp thông tin.

export function Drawer({ title, onClose, children, footer }: { title: string; onClose: () => void; children: ReactNode; footer?: ReactNode }) {
  useEscape(onClose);
  return (
    <>
      <div className="overlay" onClick={onClose} />
      <aside className="drawer" role="dialog" aria-modal="true" aria-label={title}>
        <header>
          <h3>{title}</h3>
          <button type="button" aria-label="Đóng" onClick={onClose}>
            <Icon name="x" size={20} />
          </button>
        </header>
        <div className="body">{children}</div>
        {footer && <footer>{footer}</footer>}
      </aside>
    </>
  );
}

export function Modal({ onClose, children, label, wide }: { onClose: () => void; children: ReactNode; label: string; wide?: boolean }) {
  useEscape(onClose);
  return (
    <div className="overlay" onClick={onClose}>
      <div className={'modal' + (wide ? ' wide' : '')} role="dialog" aria-modal="true" aria-label={label} onClick={(e) => e.stopPropagation()}>
        {children}
      </div>
    </div>
  );
}

function useEscape(onClose: () => void) {
  useEffect(() => {
    const f = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', f);
    return () => window.removeEventListener('keydown', f);
  }, [onClose]);
}

/** Nhãn trạng thái. tone: ok (xanh lá), wait (vàng), off (đỏ), info (xanh dương), mặc định xám. */
export function Pill({ tone, children }: { tone?: 'ok' | 'wait' | 'off' | 'info'; children: ReactNode }) {
  return <span className={'pill' + (tone ? ' ' + tone : '')}>{children}</span>;
}

/** Hộp thông tin có icon. tone mặc định nền xám nhạt. */
export function Box({ icon, tone, title, children }: { icon: IconName; tone?: 'ok' | 'warn' | 'bad' | 'info'; title: ReactNode; children?: ReactNode }) {
  return (
    <div className={'box' + (tone ? ' ' + tone : '')}>
      <Icon name={icon} size={20} />
      <div>
        <b>{title}</b>
        {children}
      </div>
    </div>
  );
}

/** Chân bảng: "Hiển thị a–b trong n …" và các nút trang. */
export function Pager({ page, pages, onPage, children }: { page: number; pages: number; onPage: (p: number) => void; children: ReactNode }) {
  const nums = pages <= 5 ? [...Array(pages)].map((_, i) => i + 1) : [1, 2, 3, 0, pages];
  return (
    <div className="pager">
      <span>{children}</span>
      <div>
        {nums.map((n, i) =>
          n ? (
            <button key={i} type="button" className={n === page ? 'on' : undefined} onClick={() => onPage(n)}>
              {n}
            </button>
          ) : (
            <button key={i} type="button" disabled>
              …
            </button>
          ),
        )}
      </div>
    </div>
  );
}

export type MenuAction = { label: string; onClick?: () => void; disabled?: string | boolean; danger?: boolean };

/** Nút ba chấm cuối dòng, mở danh sách thao tác. disabled là chuỗi thì hiện làm lý do bên dưới. */
export function RowMenu({ actions }: { actions: MenuAction[] }) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return;
    const f = (e: MouseEvent) => !ref.current?.contains(e.target as Node) && setOpen(false);
    document.addEventListener('mousedown', f);
    return () => document.removeEventListener('mousedown', f);
  }, [open]);
  return (
    <div className="rowmenu" ref={ref}>
      <button type="button" className="icon-btn" aria-label="Thao tác" aria-expanded={open} onClick={() => setOpen((o) => !o)}>
        <Icon name="dots" />
      </button>
      {open && (
        <div className="menu" role="menu">
          {actions.map((a) => (
            <button
              key={a.label}
              type="button"
              role="menuitem"
              disabled={!!a.disabled}
              className={a.danger ? 'danger' : undefined}
              onClick={() => {
                setOpen(false);
                a.onClick?.();
              }}
            >
              {a.label}
              {typeof a.disabled === 'string' && <small>{a.disabled}</small>}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

/** Ô chọn có nhãn đứng trước giá trị, ví dụ "Vai trò: Tất cả". */
export function Filter({ label, value, options, onChange, width }: { label: string; value: string; options: string[]; onChange: (v: string) => void; width?: number }) {
  return (
    <label className="filter" style={width ? { width } : undefined}>
      <span>
        {label}: {value || 'Tất cả'}
      </span>
      <Icon name="down" />
      <select value={value} onChange={(e) => onChange(e.target.value)} aria-label={label}>
        <option value="">Tất cả</option>
        {options.map((o) => (
          <option key={o}>{o}</option>
        ))}
      </select>
    </label>
  );
}

export function Search({ placeholder, value, onChange, width }: { placeholder: string; value: string; onChange: (v: string) => void; width?: number }) {
  return (
    <div className="inp lead search" style={width ? { width } : undefined}>
      <Icon name="search" />
      <input type="search" placeholder={placeholder} value={value} onChange={(e) => onChange(e.target.value)} aria-label={placeholder} />
    </div>
  );
}

/** Chữ cái đầu của tên gọi (chữ cuối họ tên), dùng cho ảnh đại diện tròn. */
export const initial = (name: string) => (name.trim().split(/\s+/).pop() ?? '?')[0];

/** Chia trang danh sách phía máy khách. Khi có API: dùng ?page=&size= của máy chủ. */
export function paginate<T>(rows: T[], page: number, size = 20) {
  const pages = Math.max(1, Math.ceil(rows.length / size));
  const p = Math.min(page, pages);
  const from = (p - 1) * size;
  return { page: p, pages, items: rows.slice(from, from + size), from: rows.length ? from + 1 : 0, to: Math.min(from + size, rows.length) };
}
