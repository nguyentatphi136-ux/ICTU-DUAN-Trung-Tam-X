import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { KIND_ICON, useInbox } from '../../components/NotificationBell';
import { Filter, Pill } from '../../components/ui';
import { ago, isRead, markRead } from '../../data/inbox';
import type { Notice } from '../../data/notifications';
import './inbox.css';

const KINDS: Notice['kind'][] = ['Bài tập', 'Điểm', 'Lịch học', 'Điểm danh', 'Học phí', 'Thông báo lớp', 'Khảo sát'];
const PAGE = 10;

// S6-10. Danh sách thông báo đầy đủ: lọc chưa đọc và theo loại, đánh dấu đã đọc từng thông báo hoặc tất cả.
// Bấm vào thông báo mở đúng màn hình liên quan. Cuộn xuống cuối danh sách tự tải thêm thông báo cũ hơn.
export function NotificationsPage() {
  const { user, list, unread, refresh } = useInbox();
  const navigate = useNavigate();
  const [only, setOnly] = useState(false);
  const [kind, setKind] = useState('');
  const [shown, setShown] = useState(PAGE);
  const end = useRef<HTMLDivElement>(null);
  const filtered = list.filter((n) => (!only || !isRead(n, user!)) && (!kind || n.kind === kind));
  const more = shown < filtered.length;

  useEffect(() => {
    if (!more || !end.current) return;
    const io = new IntersectionObserver((e) => e[0].isIntersecting && setShown((s) => s + PAGE));
    io.observe(end.current);
    return () => io.disconnect();
  }, [more, filtered.length]);
  if (!user) return null;

  return (
    <AppLayout crumb={['Thông báo']}>
      <div className="h">
        <div>
          <h2>Thông báo</h2>
          <p>{unread.length} chưa đọc</p>
        </div>
        <button
          type="button"
          className="btn"
          disabled={!unread.length}
          onClick={() => {
            markRead(unread, user);
            refresh();
          }}
        >
          Đánh dấu tất cả đã đọc
        </button>
      </div>
      <div className="bar">
        <div className="seg ib-tabs" role="tablist">
          <button type="button" role="tab" aria-selected={!only} className={!only ? 'on' : undefined} onClick={() => setOnly(false)}>
            Tất cả
          </button>
          <button type="button" role="tab" aria-selected={only} className={only ? 'on' : undefined} onClick={() => setOnly(true)}>
            Chưa đọc ({unread.length})
          </button>
        </div>
        <span style={{ flex: 1 }} />
        <Filter label="Loại" value={kind} options={KINDS} onChange={setKind} />
      </div>
      <div className="panel ib-list">
        {filtered.slice(0, shown).map((n) => {
          const read = isRead(n, user);
          return (
            <div key={n.id} className={'ib-item' + (read ? '' : ' new')}>
              <span className="bell-ic">
                <Icon name={KIND_ICON[n.kind]} size={16} />
              </span>
              <button
                type="button"
                className="ib-main"
                onClick={() => {
                  markRead([n], user);
                  if (n.link) navigate(n.link);
                  else refresh();
                }}
              >
                <b>{n.title}</b>
                <span>{n.body}</span>
              </button>
              <Pill tone={read ? undefined : 'info'}>{n.kind}</Pill>
              <small>{ago(n.at)}</small>
              {!read ? (
                <button
                  type="button"
                  className="link"
                  onClick={() => {
                    markRead([n], user);
                    refresh();
                  }}
                >
                  Đánh dấu đã đọc
                </button>
              ) : (
                <span className="ib-done">Đã đọc</span>
              )}
            </div>
          );
        })}
        {!filtered.length && <p className="empty ib-empty">{only ? 'Bạn đã đọc hết thông báo.' : 'Chưa có thông báo nào.'}</p>}
        <div ref={end} className="ib-more">
          {more ? 'Đang tải thêm thông báo cũ hơn...' : filtered.length > PAGE ? 'Đã hiện hết thông báo.' : ''}
        </div>
      </div>
    </AppLayout>
  );
}
