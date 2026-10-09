import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react';

type ToastState = { title: string; sub?: string } | null;
type ShowToast = (title: string, sub?: string) => void;

const ToastContext = createContext<ShowToast>(() => {});

// Thông báo nổi góc trên phải, tự ẩn sau 4,5 giây.
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<ToastState>(null);
  const timer = useRef<number | undefined>(undefined);

  const show = useCallback<ShowToast>((title, sub) => {
    setToast({ title, sub });
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => setToast(null), 4500);
  }, []);

  return (
    <ToastContext.Provider value={show}>
      {children}
      {toast && (
        <div className="toast" role="status">
          <b>{toast.title}</b>
          {toast.sub && <span>{toast.sub}</span>}
        </div>
      )}
    </ToastContext.Provider>
  );
}

export const useToast = () => useContext(ToastContext);
