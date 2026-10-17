import { useState } from 'react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Filter } from '../../components/ui';
import { fileSize, vdt } from '../../data/assignments';
import { useAuth } from '../../data/auth';
import { findClass } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { EARLY, MATERIALS, materialsOf, MAX_MATERIAL } from '../../data/materials';
import { addDays, dm, longDate, sessionsOf } from '../../data/schedule';
import { saveAll } from '../../data/store';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import { teachable } from '../S5-05-bai-tap/AssignmentPages';
import { nowIso } from '../S5-06-bai-tap-cua-toi/MyAssignmentsPage';
import './materials.css';

export const MODULE = 'Học liệu & thông báo lớp';

// S6-05. Giảng viên tải học liệu cho từng buổi học: nhiều tệp (tối đa 50MB mỗi tệp) và liên kết ngoài. Mỗi tài liệu ghi người
// tải lên và thời điểm. Chỉ học viên của lớp đó tải được; tài liệu hiện với học viên khi buổi đã diễn ra hoặc được mở sớm.
export function MaterialsPage() {
  const { cls = '' } = useParams();
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const role = user?.active ?? -1;
  const mine = user ? teachable(role, user.name) : [];
  const [sel, setSel] = useState('');
  const [url, setUrl] = useState('');
  const [label, setLabel] = useState('');
  const [error, setError] = useState('');
  const [, setTick] = useState(0);
  if (role === 0) return <Navigate to="/hoc-lieu-cua-toi" replace />;
  if (!cls) return mine[0] ? <Navigate to={'/hoc-lieu/' + mine[0].code} replace /> : <ErrorPage code="403" />;
  const k = findClass(cls);
  if (!k) return <ErrorPage code="404" />;
  if (!mine.includes(k)) return <ErrorPage code="403" />;

  const sessions = sessionsOf(k.code).filter((s) => !s.cancelled);
  const recent = sessions.filter((s) => s.date >= addDays(TODAY, -21) && s.date <= addDays(TODAY, 14));
  const current = sessions.find((s) => s.id === sel) ?? recent.find((s) => s.date >= TODAY) ?? recent.at(-1);
  const subjects = [...new Set(recent.map((s) => s.subject))];
  const refresh = () => (saveAll(), setTick((t) => t + 1));
  const add = (m: Omit<(typeof MATERIALS)[number], 'id' | 'session' | 'by' | 'at'>) =>
    MATERIALS.push({ ...m, id: Math.max(0, ...MATERIALS.map((x) => x.id)) + 1, session: current!.id, by: user!.name, at: nowIso() });

  function addLink() {
    const u = url.trim();
    if (!/^https?:\/\/\S+\.\S+/.test(u)) return setError('Liên kết phải bắt đầu bằng http:// hoặc https://');
    add({ kind: 'Liên kết', name: label.trim() || u.replace(/^https?:\/\//, ''), url: u });
    setUrl('');
    setLabel('');
    setError('');
    refresh();
    toast('Đã thêm liên kết');
  }

  return (
    <AppLayout crumb={[MODULE, 'Học liệu ' + k.code]} module={MODULE}>
      <div className="h">
        <div>
          <h2>Học liệu lớp {k.code}</h2>
          <p>Chỉ học viên của lớp {k.code} tải được tài liệu</p>
        </div>
        {/* S6-08 */}
        {(role === 5 || role === 6) && (
          <Link to="/nhac-lich" className="btn">
            Nhắc lịch học
          </Link>
        )}
        {/* S6-07 */}
        <Link to="/thong-bao-lop" className="btn">
          Thông báo lớp
        </Link>
        {mine.length > 1 && <Filter label="Lớp" value={k.code} options={mine.map((c) => c.code)} onChange={(v) => v && navigate('/hoc-lieu/' + v)} />}
      </div>
      <div className="ml-grid">
        <nav className="panel ml-sessions" aria-label="Buổi học">
          {subjects.map((sub) => (
            <section key={sub}>
              <h4>
                {sub} · {k.subjects.find((x) => x.code === sub)?.name}
              </h4>
              {recent
                .filter((s) => s.subject === sub)
                .map((s) => {
                  const n = materialsOf(s.id).length;
                  return (
                    <button key={s.id} type="button" className={current?.id === s.id ? 'on' : undefined} onClick={() => setSel(s.id)}>
                      <small>
                        Buổi {s.no} · {dm(s.date)}
                      </small>
                      <b>
                        Bài {s.lesson}: {s.topic}
                      </b>
                      <em>{n ? `${n} tài liệu${EARLY[s.id] && s.date > TODAY ? ' · mở sớm' : ''}` : 'Chưa có'}</em>
                    </button>
                  );
                })}
            </section>
          ))}
        </nav>
        {current && (
          <section className="panel sp-card">
            <h3 style={{ marginBottom: 2 }}>
              Buổi {current.no} · Bài {current.lesson}: {current.topic}
            </h3>
            <p className="hint" style={{ marginBottom: 14 }}>
              {longDate(current.date)} · {current.date <= TODAY ? 'đã diễn ra · học viên đã xem được' : EARLY[current.id] ? 'chưa diễn ra · đang mở sớm cho học viên' : 'chưa diễn ra · học viên chưa thấy tài liệu'}
            </p>
            <label className="lv-drop">
              <Icon name="upload" />
              <b>Chọn tệp hoặc kéo vào đây</b>
              <small>Chọn được nhiều tệp · mỗi tệp tối đa 50MB</small>
              <input
                type="file"
                multiple
                onChange={(e) => {
                  const files = [...(e.target.files ?? [])];
                  const big = files.filter((f) => f.size > MAX_MATERIAL);
                  files.filter((f) => f.size <= MAX_MATERIAL).forEach((f) => add({ kind: 'Tệp', name: f.name, size: f.size }));
                  setError(big.length ? `${big.map((f) => f.name).join(', ')} vượt 50MB nên không được tải lên.` : '');
                  refresh();
                  if (files.length > big.length) toast(`Đã tải lên ${files.length - big.length} tệp`);
                  e.target.value = '';
                }}
              />
            </label>
            <div className="ml-link">
              <input aria-label="Liên kết ngoài" placeholder="https://developer.mozilla.org/..." value={url} onChange={(e) => setUrl(e.target.value)} />
              <input aria-label="Tên hiển thị" placeholder="Tên hiển thị" value={label} onChange={(e) => setLabel(e.target.value)} />
              <button type="button" className="btn" onClick={addLink}>
                Thêm
              </button>
            </div>
            {error && <p className="err">{error}</p>}
            <div className="panel" style={{ marginTop: 12 }}>
              <table>
                <thead>
                  <tr>
                    <th>Tài liệu</th>
                    <th>Loại</th>
                    <th>Người tải lên</th>
                    <th>Thời điểm</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {materialsOf(current.id).map((m) => (
                    <tr key={m.id}>
                      <td className="strong">
                        {m.url ? (
                          <a href={m.url} target="_blank" rel="noreferrer">
                            {m.name}
                          </a>
                        ) : (
                          m.name
                        )}
                      </td>
                      <td>{m.kind === 'Tệp' ? `Tệp · ${fileSize(m.size ?? 0)}` : 'Liên kết'}</td>
                      <td>{m.by}</td>
                      <td>{vdt(m.at).slice(0, 5) + ' ' + m.at.slice(11, 16)}</td>
                      <td>
                        <button
                          type="button"
                          className="link danger"
                          onClick={() => {
                            MATERIALS.splice(MATERIALS.indexOf(m), 1);
                            refresh();
                            toast('Đã gỡ ' + m.name);
                          }}
                        >
                          Gỡ
                        </button>
                      </td>
                    </tr>
                  ))}
                  {!materialsOf(current.id).length && (
                    <tr>
                      <td colSpan={5} className="empty">
                        Buổi này chưa có tài liệu.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            <label className="check-box ml-early">
              <input
                type="checkbox"
                disabled={current.date <= TODAY}
                checked={current.date <= TODAY || !!EARLY[current.id]}
                onChange={(e) => {
                  EARLY[current.id] = e.target.checked;
                  refresh();
                }}
              />
              <span>
                Mở sớm cho học viên trước khi buổi học diễn ra
                <small>{current.date <= TODAY ? 'Buổi này đã diễn ra nên tài liệu đang hiện với học viên.' : 'Học viên thấy tài liệu ngay, không phải chờ tới ngày học.'}</small>
              </span>
            </label>
          </section>
        )}
      </div>
    </AppLayout>
  );
}

