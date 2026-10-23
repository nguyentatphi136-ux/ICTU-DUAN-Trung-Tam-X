import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../data/auth';
import { ago, inboxOf, isRead, markRead, syncInbox } from '../data/inbox';
import { onNotice, type Notice } from '../data/notifications';
import { runFeeReminders } from '../data/feeReminders';
import { runReminders } from '../data/prefs';
import { Icon, type IconName } from './Icon';
import './bell.css';

export const KIND_ICON: Record<Notice['kind'], IconName> = { 'Lịch học': 'cal', 'Bài tập': 'edit', Điểm: 'award', 'Điểm danh': 'check', 'Học phí': 'wallet', 'Thông báo lớp': 'chat', 'Khảo sát': 'chat' };

/** Thông báo của người dùng, tự cập nhật khi có thông báo mới sinh ra ở nơi khác (S6-09). */
export function useInbox() {
  const { user } = useAuth();
  const [, setTick] = useState(0);
  useEffect(() => onNotice(() => setTick((t) => t + 1)), []);
  // Có backend: tải thông báo khi mở trang và mỗi phút một lần.
  useEffect(() => {
    if (!user) return;
    void syncInbox(user);
    const timer = setInterval(() => void syncInbox(user), 60_000);
    return () => clearInterval(timer);
    // Chỉ tải lại khi đổi người đăng nhập, không theo từng lần đối tượng user được tạo lại.
  }, [user?.email, user?.name]);
  const list = user ? inboxOf(user) : [];
  return { user, list, unread: user ? list.filter((n) => !isRead(n, user)) : [], refresh: () => setTick((t) => t + 1) };
}

// S6-09. Chuông thông báo trên thanh trên: số thông báo chưa đọc, danh sách thả xuống mới nhất, đánh dấu tất cả đã đọc.
// Bấm một thông báo mở đúng màn hình liên quan (S6-10).
export function NotificationBell() {
  const { user, list, unread, refresh } = useInbox();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    runReminders();
    runFeeReminders();
  }, []);
  useEffect(() => {
    if (!open) return;
    const f = (e: MouseEvent) => !ref.current?.contains(e.target as Node) && setOpen(false);
    document.addEventListener('mousedown', f);
    return () => document.removeEventListener('mousedown', f);
  }, [open]);
  if (!user) return null;

  const go = (n: Notice) => {
    markRead([n], user);
    setOpen(false);
    refresh();
    if (n.link) navigate(n.link);
  };

  return (
    <div className="bell" ref={ref}>
      <button type="button" className="bell-btn" aria-label={`Thông báo, ${unread.length} chưa đọc`} aria-expanded={open} onClick={() => setOpen((o) => !o)}>
        <Icon name="bell" />
        {unread.length > 0 && <span className="bell-n">{unread.length > 99 ? '99+' : unread.length}</span>}
      </button>
      {open && (
        <div className="bell-menu" role="menu">
          <header>
            <b>Thông báo</b>
            {unread.length > 0 && <span className="pill off">{unread.length} chưa đọc</span>}
            <button
              type="button"
              className="link"
              disabled={!unread.length}
              onClick={() => {
                markRead(unread, user);
                refresh();
              }}
            >
              Đánh dấu tất cả đã đọc
            </button>
          </header>
          <ul>
            {list.slice(0, 6).map((n) => (
              <li key={n.id}>
                <button type="button" role="menuitem" className={isRead(n, user) ? undefined : 'new'} onClick={() => go(n)}>
                  <span className="bell-ic">
                    <Icon name={KIND_ICON[n.kind]} size={16} />
                  </span>
                  <span>
                    <b>{n.title}</b>
                    <small>{ago(n.at)}</small>
                  </span>
                </button>
              </li>
            ))}
            {!list.length && <li className="bell-empty">Bạn chưa có thông báo nào.</li>}
          </ul>
          <Link to="/thong-bao" className="bell-all" onClick={() => setOpen(false)}>
            Xem tất cả thông báo
          </Link>
        </div>
      )}
    </div>
  );
}

