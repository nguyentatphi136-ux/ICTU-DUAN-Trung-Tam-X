import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react';

type ToastAction = { label: string; onClick: () => void };
type ToastState = { title: string; sub?: string; action?: ToastAction } | null;
type ShowToast = (title: string, sub?: string, action?: ToastAction) => void;

const ToastContext = createContext<ShowToast>(() => {});

// Thông báo nổi góc trên phải, tự ẩn sau 4,5 giây (6 giây nếu có nút như "Hoàn tác").
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<ToastState>(null);
  const timer = useRef<number | undefined>(undefined);

  const show = useCallback<ShowToast>((title, sub, action) => {
    setToast({ title, sub, action });
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => setToast(null), action ? 6000 : 4500);
  }, []);

  return (
    <ToastContext.Provider value={show}>
      {children}
      {toast && (
        <div className="toast" role="status">
          <b>{toast.title}</b>
          {toast.sub && <span>{toast.sub}</span>}
          {toast.action && (
            <button
              type="button"
              className="toast-act"
              onClick={() => {
                toast.action!.onClick();
                setToast(null);
              }}
            >
              {toast.action.label}
            </button>
          )}
        </div>
      )}
    </ToastContext.Provider>
  );
}

export const useToast = () => useContext(ToastContext);
