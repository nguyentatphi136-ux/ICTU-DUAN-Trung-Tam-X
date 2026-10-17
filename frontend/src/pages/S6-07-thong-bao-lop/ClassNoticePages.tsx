import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Markdown } from '../../components/Markdown';
import { useToast } from '../../components/Toast';
import { Pill } from '../../components/ui';
import { vdt } from '../../data/assignments';
import { useAuth } from '../../data/auth';
import { audienceOf, CLASS_NOTICES, type ClassNotice } from '../../data/classNotices';
import { notify } from '../../data/notifications';
import { saveAll } from '../../data/store';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import { teachable } from '../S5-05-bai-tap/AssignmentPages';
import { nowIso } from '../S5-06-bai-tap-cua-toi/MyAssignmentsPage';
import { MODULE } from '../S6-05-hoc-lieu/MaterialsPage';
import './notice.css';

/** Gửi thông báo: sinh một thông báo trong ứng dụng cho mỗi lớp (S6-09 hiển thị). Email gửi bất đồng bộ ở máy chủ. */
function send(n: ClassNotice) {
  n.status = 'Đã gửi';
  n.at = nowIso();
  if (n.inApp) for (const cls of n.classes) notify({ at: n.at, kind: 'Thông báo lớp', title: n.title, body: n.body.split('\n').find((l) => l.trim() && !/^chào/i.test(l.trim())) ?? n.body, link: '/thong-bao', to: { cls }, ref: 'cn' + n.id });
}

// S6-07. Danh sách thông báo đã gửi kèm số học viên đã đọc; xem chi tiết theo lớp, danh sách chưa đọc và gửi lại.
export function NoticeListPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const toast = useToast();
  const [tab, setTab] = useState<'unread' | 'read'>('unread');
  const role = user?.active ?? -1;
  const mine = new Set(teachable(role, user!.name).map((k) => k.code));
  const list = CLASS_NOTICES.filter((n) => role === 5 || role === 6 || n.classes.some((c) => mine.has(c))).sort((a, b) => b.at.localeCompare(a.at));
  if (role === 0) return <ErrorPage code="403" />;
  const open = list.find((n) => String(n.id) === id) ?? list[0];
  const to = open ? audienceOf(open.classes) : [];
  const read = to.filter((s) => open!.readBy.includes(s.code));
  const unread = to.filter((s) => !open!.readBy.includes(s.code));

  return (
    <AppLayout crumb={[MODULE, 'Thông báo lớp']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Thông báo đã gửi</h2>
          <p>{list.filter((n) => n.status === 'Đã gửi').length} thông báo</p>
        </div>
        <Link to="/thong-bao-lop/soan" className="btn primary">
          <Icon name="plus" size={16} />
          Soạn thông báo
        </Link>
      </div>
      <div className="cn-grid">
        <div className="panel">
          <table className="rows">
            <thead>
              <tr>
                <th>Thông báo</th>
                <th>Gửi tới</th>
                <th>Đã đọc</th>
              </tr>
            </thead>
            <tbody>
              {list.map((n) => {
                const all = audienceOf(n.classes);
                return (
                  <tr key={n.id} className={open?.id === n.id ? 'on' : undefined}>
                    <td>
                      <Link to={'/thong-bao-lop/' + n.id} className="strong">
                        {n.title}
                      </Link>
                      <small className="lv-sub">
                        {n.by} · {vdt(n.at).slice(0, 5)} {n.at.slice(11, 16)}
                      </small>
                    </td>
                    <td>{n.classes.length > 1 ? `${n.classes.length} lớp` : n.classes[0]}</td>
                    <td>{n.status === 'Nháp' ? <Pill>Bản nháp</Pill> : `${all.filter((s) => n.readBy.includes(s.code)).length} / ${all.length} đã đọc`}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        {open && open.status === 'Đã gửi' && (
          <section className="panel sp-card">
            <h3>{open.title}</h3>
            <p className="hint">
              {read.length} trên {to.length} học viên đã đọc · {to.length ? Math.round((100 * read.length) / to.length) : 0}%
            </p>
            <ul className="cn-classes">
              {open.classes.map((c) => {
                const all = to.filter((s) => s.cls === c);
                const r = all.filter((s) => open.readBy.includes(s.code)).length;
                return (
                  <li key={c}>
                    <b>{c}</b>
                    <i style={{ ['--p' as string]: `${all.length ? (100 * r) / all.length : 0}%` }} />
                    <span>
                      {r} / {all.length}
                    </span>
                  </li>
                );
              })}
            </ul>
            <div className="seg cn-tabs" role="tablist">
              <button type="button" role="tab" aria-selected={tab === 'unread'} className={tab === 'unread' ? 'on' : undefined} onClick={() => setTab('unread')}>
                Chưa đọc ({unread.length})
              </button>
              <button type="button" role="tab" aria-selected={tab === 'read'} className={tab === 'read' ? 'on' : undefined} onClick={() => setTab('read')}>
                Đã đọc ({read.length})
              </button>
            </div>
            <ul className="cn-people">
              {(tab === 'unread' ? unread : read).slice(0, 8).map((s) => (
                <li key={s.code}>
                  <b>{s.name}</b>
                  <small>{s.cls}</small>
                </li>
              ))}
            </ul>
            {(tab === 'unread' ? unread : read).length > 8 && <p className="hint">và {(tab === 'unread' ? unread : read).length - 8} học viên khác</p>}
            {unread.length > 0 && (
              <button
                type="button"
                className="btn"
                style={{ width: '100%', marginTop: 12 }}
                onClick={() => {
                  notify({ at: nowIso(), kind: 'Thông báo lớp', title: 'Nhắc lại: ' + open.title, body: 'Bạn chưa đọc thông báo này.', link: '/thong-bao', to: { users: unread.map((s) => s.name) }, ref: 'cn' + open.id });
                  saveAll();
                  toast(`Đã gửi lại cho ${unread.length} người chưa đọc`);
                }}
              >
                Gửi lại cho người chưa đọc
              </button>
            )}
          </section>
        )}
      </div>
    </AppLayout>
  );
}

// S6-07. Soạn thông báo có tiêu đề và nội dung, chọn một hoặc nhiều lớp; hiện trong ứng dụng và gửi kèm email.
export function ComposeNoticePage() {
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const role = user?.active ?? -1;
  const classes = teachable(role, user!.name);
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [to, setTo] = useState<string[]>(classes[0] ? [classes[0].code] : []);
  const [inApp, setInApp] = useState(true);
  const [email, setEmail] = useState(true);
  const [adding, setAdding] = useState(false);
  const [error, setError] = useState('');
  if (role === 0) return <ErrorPage code="403" />;
  const people = audienceOf(to);
  const sample = people[0];

  function save(status: ClassNotice['status']) {
    if (!title.trim()) return setError('Nhập tiêu đề thông báo');
    if (status === 'Đã gửi' && !body.trim()) return setError('Nhập nội dung thông báo');
    if (!to.length) return setError('Chọn ít nhất một lớp');
    if (status === 'Đã gửi' && !inApp && !email) return setError('Chọn ít nhất một cách gửi');
    const n: ClassNotice = { id: Math.max(0, ...CLASS_NOTICES.map((x) => x.id)) + 1, title: title.trim(), body: body.trim(), classes: to, inApp, email, by: user!.name, at: nowIso(), status, readBy: [] };
    if (status === 'Đã gửi') send(n);
    CLASS_NOTICES.unshift(n);
    saveAll();
    toast(status === 'Đã gửi' ? `Đã gửi thông báo tới ${people.length} học viên` : 'Đã lưu nháp', n.title);
    navigate('/thong-bao-lop/' + n.id);
  }

  return (
    <AppLayout crumb={[MODULE, 'Soạn thông báo']} module={MODULE}>
      <Link to="/thong-bao-lop" className="back">
        <Icon name="back" size={16} />
        Thông báo đã gửi
      </Link>
      <div className="h">
        <div>
          <h2>Soạn thông báo tới lớp</h2>
          <p>Thông báo hiện trong ứng dụng và gửi kèm email</p>
        </div>
      </div>
      <div className="cn-grid">
        <section className="panel sp-card">
          <div className="field">
            <label htmlFor="cn-title">Tiêu đề</label>
            <div className="inp">
              <input id="cn-title" value={title} placeholder="Ví dụ: Nghỉ học ngày 20/11" onChange={(e) => setTitle(e.target.value)} />
            </div>
          </div>
          <div className="field">
            <label htmlFor="cn-body">Nội dung</label>
            <div className="inp">
              <textarea id="cn-body" rows={8} value={body} placeholder={'Chào các bạn,\n\n...'} onChange={(e) => setBody(e.target.value)} />
            </div>
            <p className="hint">Hỗ trợ **đậm**, _nghiêng_, danh sách bắt đầu bằng dấu gạch ngang và [liên kết](https://...).</p>
          </div>
          <span className="lbl">Gửi tới</span>
          <div className="chips">
            {to.map((c) => (
              <span key={c} className="chip">
                {c} · {audienceOf([c]).length} học viên
                <button type="button" aria-label={'Bỏ ' + c} onClick={() => setTo(to.filter((x) => x !== c))}>
                  <Icon name="x" />
                </button>
              </span>
            ))}
            <button type="button" className="chip-add" onClick={() => setAdding(!adding)}>
              <Icon name="plus" />
              Thêm lớp
            </button>
            {adding && (
              <div className="dropdown">
                {classes
                  .filter((k) => !to.includes(k.code))
                  .map((k) => (
                    <button
                      key={k.code}
                      type="button"
                      onClick={() => {
                        setTo([...to, k.code]);
                        setAdding(false);
                      }}
                    >
                      {k.code} · {k.program}
                    </button>
                  ))}
              </div>
            )}
          </div>
          <p className="hint" style={{ margin: '8px 0 12px' }}>
            {people.length} học viên ở {to.length} lớp. Giảng viên và trợ giảng của các lớp này cũng nhận được.
          </p>
          <div className="cn-ways">
            <label className="check-box">
              <input type="checkbox" checked={inApp} onChange={(e) => setInApp(e.target.checked)} />
              Hiện trong ứng dụng
            </label>
            <label className="check-box">
              <input type="checkbox" checked={email} onChange={(e) => setEmail(e.target.checked)} />
              Gửi kèm email
            </label>
          </div>
          {error && <p className="err">{error}</p>}
          <div className="acts">
            <button type="button" className="btn" onClick={() => save('Nháp')}>
              Lưu nháp
            </button>
            <button type="button" className="btn primary" onClick={() => save('Đã gửi')}>
              Gửi thông báo
            </button>
          </div>
        </section>
        <aside className="panel sp-card">
          <h3>Xem trước email</h3>
          <div className="cn-mail">
            <div className="cn-mail-head">TMS.</div>
            <div className="cn-mail-body">
              <b>{title || 'Tiêu đề thông báo'}</b>
              {sample && <p>Chào {sample.name},</p>}
              {body.trim() ? <Markdown text={body.replace(/^chào[^\n]*\n/i, '')} /> : <p className="hint">Nội dung thông báo sẽ hiện ở đây.</p>}
              <span className="btn primary">Xem trong TMS</span>
            </div>
            {sample && <small>Bạn nhận email này vì đang học lớp {sample.cls}.</small>}
          </div>
        </aside>
      </div>
    </AppLayout>
  );
}
