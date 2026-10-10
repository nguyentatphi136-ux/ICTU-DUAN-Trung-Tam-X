import { useEffect, useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import { Icon, type IconName } from './Icon';
import { pageNumbers } from './paging';

export { pageNumbers, paginate } from './paging';

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
    <div className="overlay modal-overlay" onClick={onClose}>
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

export const PAGE_SIZES = [10, 20, 50];

/** Trang hiện tại và số dòng mỗi trang; đổi số dòng thì về trang 1. */
export function usePaging(initialSize = 20) {
  const [page, setPage] = useState(1);
  const [size, setSizeState] = useState(initialSize);
  const setSize = (s: number) => {
    setSizeState(s);
    setPage(1);
  };
  return { page, setPage, size, setSize };
}


/** Chân bảng: "Hiển thị a–b trong n …", số dòng mỗi trang, nút Trước/Sau và các số trang. */
export function Pager({ page, pages, onPage, size, onSize, children }: { page: number; pages: number; onPage: (p: number) => void; size?: number; onSize?: (s: number) => void; children: ReactNode }) {
  const nums = pageNumbers(page, pages);
  return (
    <div className="pager">
      <span>{children}</span>
      <div>
        {onSize && (
          <label className="pager-size">
            Mỗi trang
            <select value={size} onChange={(e) => onSize(Number(e.target.value))} aria-label="Số dòng mỗi trang">
              {PAGE_SIZES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </label>
        )}
        <button type="button" className="step" disabled={page <= 1} onClick={() => onPage(page - 1)}>
          Trước
        </button>
        {nums.map((n, i) =>
          n ? (
            <button key={i} type="button" className={n === page ? 'on' : undefined} aria-current={n === page ? 'page' : undefined} onClick={() => onPage(n)}>
              {n}
            </button>
          ) : (
            <button key={i} type="button" disabled>
              …
            </button>
          ),
        )}
        <button type="button" className="step" disabled={page >= pages} onClick={() => onPage(page + 1)}>
          Sau
        </button>
      </div>
    </div>
  );
}

/**
 * Hộp xác nhận thay cho window.confirm. Mặc định là chuyển vào thùng rác (khôi phục được);
 * permanent=true là xoá vĩnh viễn, ghi rõ không hoàn tác được.
 */
export function ConfirmDialog({ title, children, confirmLabel, permanent, onConfirm, onClose }: { title: string; children: ReactNode; confirmLabel?: string; permanent?: boolean; onConfirm: () => void; onClose: () => void }) {
  return (
    <Modal onClose={onClose} label={title}>
      <div className="modal-head">
        <span className="danger-ic">
          <Icon name="trash" />
        </span>
        <div>
          <h3>{title}</h3>
          <p>{permanent ? 'Thao tác này không hoàn tác được.' : 'Bạn khôi phục được trong Thùng rác.'}</p>
        </div>
      </div>
      <div className="confirm-body">{children}</div>
      <div className="acts">
        <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className="btn danger" autoFocus onClick={onConfirm}>
          {confirmLabel ?? (permanent ? 'Xoá vĩnh viễn' : 'Chuyển vào thùng rác')}
        </button>
      </div>
    </Modal>
  );
}

export type MenuAction = { label: string; onClick?: () => void; disabled?: string | boolean; danger?: boolean };

/** Nút ba chấm cuối dòng, mở danh sách thao tác. disabled là chuỗi thì hiện làm lý do bên dưới. */
export function RowMenu({ actions }: { actions: MenuAction[] }) {
  const [open, setOpen] = useState(false);
  const [up, setUp] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  // Dòng cuối bảng: menu mở xuống sẽ bị khung cuộn của bảng cắt và đẩy ra thanh cuộn, nên mở lên trên khi phía dưới không đủ chỗ.
  useLayoutEffect(() => {
    const menu = ref.current?.querySelector<HTMLElement>('.menu');
    if (!open || !menu) return setUp(false);
    let box = ref.current!.parentElement;
    while (box && getComputedStyle(box).overflowY === 'visible') box = box.parentElement;
    const bound = box ? box.getBoundingClientRect() : { top: 0, bottom: innerHeight };
    const btn = ref.current!.getBoundingClientRect();
    const h = menu.offsetHeight + 4;
    setUp(btn.bottom + h > Math.min(bound.bottom, innerHeight) && btn.top - h >= Math.max(bound.top, 0));
  }, [open]);
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
        <div className={up ? 'menu up' : 'menu'} role="menu">
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

