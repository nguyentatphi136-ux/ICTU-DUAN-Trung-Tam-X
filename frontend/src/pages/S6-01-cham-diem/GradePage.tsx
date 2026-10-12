import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Pill } from '../../components/ui';
import { fileSize, findAssignment, isPublished, lateHours, score, vdt, type Assignment, type Submission } from '../../data/assignments';
import { audit } from '../../data/audit';
import { useAuth } from '../../data/auth';
import { findClass } from '../../data/classes';
import { notify } from '../../data/notifications';
import { ROLES } from '../../data/permissions';
import { saveAll } from '../../data/store';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import { MODULE, teachable } from '../S5-05-bai-tap/AssignmentPages';
import { nowIso } from '../S5-06-bai-tap-cua-toi/MyAssignmentsPage';
import { rowsOf } from '../S5-10-bai-nop/SubmissionsPage';
import './grade.css';

/** "8,5" hoặc "8.5" thành số; rỗng hoặc sai dạng là NaN. */
export const parseScore = (s: string) => (/^\d+([.,]\d)?$/.test(s.trim()) ? Number(s.trim().replace(',', '.')) : NaN);

/** Công bố điểm: học viên mới thấy điểm, nhận thông báo. */
export function publish(a: Assignment, sub: Submission, by: string, name: string) {
  const g = sub.grade!;
  g.published = true;
  g.publishedAt = nowIso();
  (g.log ??= []).unshift({ at: g.publishedAt, by, text: `Công bố điểm ${score(g.score)}.` });
  notify({ at: g.publishedAt, kind: 'Điểm', title: 'Đã có điểm ' + a.title.split(':')[0], body: `Điểm của bạn: ${score(g.score)} / ${a.max}.`, link: '/bai-tap-cua-toi/' + a.id, to: { users: [name] } });
}

// S6-01. Màn hình chấm: xem trước tệp nộp, mở liên kết repo, nhập điểm 0 đến điểm tối đa kèm nhận xét, chuyển nhanh sang bài
// kế tiếp. Điểm chỉ hiện cho học viên sau khi bấm Công bố. Mỗi lần sửa điểm ghi nhật ký kèm người sửa và thời điểm.
export function GradePage() {
  const { id = '', student = '' } = useParams();
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const a = findAssignment(Number(id));
  const rows = a ? rowsOf(a).filter((r) => r.sub) : [];
  const idx = rows.findIndex((r) => r.st.code === student);
  const r = rows[idx];
  const g = r?.sub?.grade;
  const [value, setValue] = useState(g ? score(g.score) : '');
  const [comment, setComment] = useState(g?.comment ?? '');
  const [reason, setReason] = useState('');
  const [file, setFile] = useState(0);
  const [error, setError] = useState('');
  const k = findClass(a?.cls);
  if (!a || !k || !r) return <ErrorPage code="404" />;
  if (!teachable(user?.active ?? -1, user!.name).includes(k)) return <ErrorPage code="403" />;
  const sub = r.sub!;
  const v = sub.versions[0];
  const late = lateHours(a, v);
  const n = parseScore(value);
  const changed = !!g && n !== g.score;
  const go = (j: number) => rows[j] && navigate(`/bai-tap/${a.id}/cham/${rows[j].st.code}`);

  /** Lưu điểm nháp; trả về false nếu dữ liệu chưa hợp lệ. */
  function save(): boolean {
    if (!(n >= 0 && n <= a!.max)) return setError(`Nhập số từ 0 đến ${a!.max}, cho phép một chữ số thập phân.`), false;
    if (!comment.trim()) return setError('Viết nhận xét để học viên biết cần cải thiện gì.'), false;
    if (changed && isPublished(g) && !reason.trim()) return setError('Điểm đã công bố, ghi lý do sửa điểm.'), false;
    const at = nowIso();
    const by = user!.name;
    if (!g) {
      sub.grade = { score: n, comment: comment.trim(), by, at, published: false, log: [{ at, by, text: `Chấm lần đầu: ${score(n)}.` }] };
      audit({ at, by, role: ROLES[user!.active], entity: 'Điểm', action: 'Chấm điểm', target: `${r!.st.code} · ${a!.cls} · ${a!.title.split(':')[0]}`, changes: [{ field: 'Điểm', before: '–', after: score(n) }] });
    } else if (changed || comment.trim() !== g.comment) {
      if (changed) {
        (g.log ??= []).unshift({ at, by, text: `Điểm ${score(g.score)} → ${score(n)}.${reason.trim() ? ' Lý do: ' + reason.trim() + '.' : ''}` });
        audit({ at, by, role: ROLES[user!.active], entity: 'Điểm', action: 'Sửa điểm', target: `${r!.st.code} · ${a!.cls} · ${a!.title.split(':')[0]}`, changes: [{ field: 'Điểm', before: score(g.score), after: score(n) }], note: reason.trim() || undefined });
      }
      Object.assign(g, { score: n, comment: comment.trim(), by, at });
    }
    sub.state = 'Đã chấm';
    setError('');
    setReason('');
    saveAll();
    return true;
  }

  return (
    <AppLayout crumb={[MODULE, a.title.split(':')[0], r.st.name]} module={MODULE}>
      <Link to={'/bai-tap/' + a.id} className="back">
        <Icon name="back" size={16} />
        Danh sách bài nộp
      </Link>
      <div className="h">
        <div>
          <h2>{r.st.name}</h2>
          <p>
            {r.st.code} · Bản {v.no} · nộp {vdt(v.at)} · {late ? `trễ ${late} giờ` : 'đúng hạn'}
          </p>
        </div>
        <div className="gr-nav">
          <button type="button" className="icon-btn" aria-label="Bài trước" disabled={idx <= 0} onClick={() => go(idx - 1)}>
            <Icon name="back" size={16} />
          </button>
          <b>
            Bài {idx + 1} / {rows.length}
          </b>
          <button type="button" className="icon-btn" aria-label="Bài kế tiếp" disabled={idx >= rows.length - 1} onClick={() => go(idx + 1)}>
            <Icon name="go" size={16} />
          </button>
        </div>
      </div>
      <div className="gr-grid">
        <section className="panel gr-view">
          <div className="gr-tabs" role="tablist">
            {v.files.map((f, i) => (
              <button key={f.name} type="button" role="tab" aria-selected={file === i} className={file === i ? 'on' : undefined} onClick={() => setFile(i)}>
                <Icon name="file" size={14} />
                {f.name}
              </button>
            ))}
            {v.repo && (
              <a href={v.repo} target="_blank" rel="noreferrer" className="gr-repo">
                <Icon name="go" size={14} />
                Repo
              </a>
            )}
          </div>
          {v.files[file] ? (
            <div className="gr-doc">
              <Icon name="file" size={40} />
              <b>{v.files[file].name}</b>
              <span>
                {fileSize(v.files[file].size)} · {/\.pdf$/i.test(v.files[file].name) ? 'xem trước ngay trong trình duyệt khi có máy chủ tệp' : 'tệp nén, tải về để chạy thử'}
              </span>
              <button type="button" className="btn" disabled title="Chưa có máy chủ lưu tệp trong bản demo">
                <Icon name="dl" size={16} />
                Tải về
              </button>
            </div>
          ) : (
            <div className="gr-doc">
              <span>Học viên chỉ nộp liên kết repo.</span>
            </div>
          )}
          {v.repo && (
            <a className="gr-link" href={v.repo} target="_blank" rel="noreferrer">
              {v.repo} · mở trong tab mới
            </a>
          )}
          {v.note && <p className="hint gr-note">Ghi chú của học viên: {v.note}</p>}
        </section>

        <section className="panel sp-card gr-form">
          <div className="sb-head">
            <h3>Chấm điểm</h3>
            {g && isPublished(g) ? <Pill tone="ok">Đã công bố</Pill> : <Pill>Chưa công bố</Pill>}
          </div>
          <div className="field">
            <label htmlFor="gr-score">Điểm</label>
            <div className="gr-score">
              <div className={'inp' + (error && !(n >= 0 && n <= a.max) ? ' bad' : '')}>
                <input id="gr-score" inputMode="decimal" value={value} placeholder="0" onChange={(e) => setValue(e.target.value)} />
              </div>
              <span>/ {a.max}</span>
            </div>
            <p className="hint">Nhập số từ 0 đến {a.max}, cho phép một chữ số thập phân.</p>
          </div>
          <div className="field">
            <label htmlFor="gr-comment">Nhận xét</label>
            <div className="inp">
              <textarea id="gr-comment" rows={4} value={comment} onChange={(e) => setComment(e.target.value)} />
            </div>
          </div>
          {changed && (
            <div className="field">
              <label htmlFor="gr-reason">Lý do sửa điểm {isPublished(g) ? '' : '(không bắt buộc)'}</label>
              <div className="inp">
                <input id="gr-reason" value={reason} placeholder="Ví dụ: chấm lại phần tìm kiếm" onChange={(e) => setReason(e.target.value)} />
              </div>
            </div>
          )}
          {g?.log?.length ? (
            <div className="gr-log">
              <span className="lbl">Nhật ký sửa điểm</span>
              <ul>
                {g.log.map((l, i) => (
                  <li key={i}>
                    <b>
                      {vdt(l.at)} · {l.by}
                    </b>
                    <span>{l.text}</span>
                  </li>
                ))}
              </ul>
            </div>
          ) : null}
          {error && <p className="err">{error}</p>}
          <p className="hint">Điểm chỉ hiện cho học viên sau khi bạn bấm Công bố.</p>
          <div className="gr-acts">
            <button
              type="button"
              className="btn"
              onClick={() => {
                if (!save()) return;
                publish(a, sub, user!.name, r.st.name);
                saveAll();
                toast('Đã công bố điểm', `${r.st.name}: ${score(sub.grade!.score)}`);
              }}
            >
              Công bố điểm bài này
            </button>
            <button
              type="button"
              className="btn primary"
              onClick={() => {
                if (!save()) return;
                toast('Đã lưu điểm nháp', r.st.name);
                if (idx < rows.length - 1) go(idx + 1);
              }}
            >
              {idx < rows.length - 1 ? 'Lưu nháp và sang bài kế tiếp' : 'Lưu nháp'}
            </button>
          </div>
        </section>
      </div>
    </AppLayout>
  );
}
