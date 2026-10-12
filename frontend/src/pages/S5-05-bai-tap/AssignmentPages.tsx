import { useRef, useState } from 'react';
import { Link, Navigate, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Markdown } from '../../components/Markdown';
import { useToast } from '../../components/Toast';
import { Box, Filter, Modal, Pill } from '../../components/ui';
import { ASSIGNMENTS, fileSize, findAssignment, SUBMISSIONS, vdt, type Assignment, type FileRef } from '../../data/assignments';
import { useAuth } from '../../data/auth';
import { classesOf, classSize, CLASSES, findClass, isOpen } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { notify } from '../../data/notifications';
import { dm, sessionsOf, WEEKDAYS, dow } from '../../data/schedule';
import { saveAll } from '../../data/store';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import '../S3-04-ho-so-hoc-vien/student.css';
import './assignment.css';

export const MODULE = 'Bài tập & chấm điểm';
const now = () => TODAY + 'T' + new Date().toTimeString().slice(0, 5);

/** Lớp người này được giao bài: lớp mình dạy (giảng viên, trợ giảng) hoặc mọi lớp đang chạy (Quản lý đào tạo). */
export function teachable(role: number, name: string) {
  return (role === 5 || role === 6 ? CLASSES : classesOf(name)).filter(isOpen);
}

// S5-05. Danh sách bài tập của các lớp mình dạy: bản nháp và bài đã giao, số bài đã nộp.
export function AssignmentListPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const role = user?.active ?? -1;
  const [cls, setCls] = useState('');
  if (role === 0) return <Navigate to="/bai-tap-cua-toi" replace />;
  const mine = teachable(role, user!.name);
  const codes = new Set(mine.map((k) => k.code));
  const list = ASSIGNMENTS.filter((a) => codes.has(a.cls) && (!cls || a.cls === cls)).sort((a, b) => b.due.localeCompare(a.due));

  return (
    <AppLayout crumb={[MODULE]} module={MODULE}>
      <div className="h">
        <div>
          <h2>Bài tập</h2>
          <p>
            {list.filter((a) => a.status === 'Đã giao').length} bài đã giao · {list.filter((a) => a.status === 'Nháp').length} bản nháp
          </p>
        </div>
        {mine.length > 0 && (
          <Link to={'/bai-tap/moi?lop=' + (cls || mine[0].code)} className="btn primary">
            <Icon name="plus" size={16} />
            Tạo bài tập
          </Link>
        )}
      </div>
      <div className="bar">
        <Filter label="Lớp" value={cls} options={mine.map((k) => k.code)} onChange={setCls} />
      </div>
      <div className="panel">
        <div className="scroll">
          <table className="rows">
            <thead>
              <tr>
                <th>Bài tập</th>
                <th>Lớp · môn</th>
                <th>Hạn nộp</th>
                <th>Nộp muộn</th>
                <th>Đã nộp</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {list.map((a) => (
                <tr key={a.id} tabIndex={0} onClick={() => navigate(a.status === 'Nháp' ? `/bai-tap/${a.id}/sua` : `/bai-tap/${a.id}`)} onKeyDown={(e) => e.key === 'Enter' && navigate(a.status === 'Nháp' ? `/bai-tap/${a.id}/sua` : `/bai-tap/${a.id}`)}>
                  <td className="strong">{a.title}</td>
                  <td>
                    {a.cls} · {a.subject}
                  </td>
                  <td className={a.due.slice(0, 10) === TODAY ? 'bad-t' : undefined}>{vdt(a.due)}</td>
                  <td>{a.late === 'Chặn' ? 'Chặn' : 'Cho nộp, có nhãn'}</td>
                  <td>{a.status === 'Nháp' ? '–' : `${SUBMISSIONS.filter((s) => s.assignment === a.id).length}/${classSize(a.cls)}`}</td>
                  <td>{a.status === 'Nháp' ? <Pill>Bản nháp</Pill> : <Pill tone="ok">Đã giao</Pill>}</td>
                </tr>
              ))}
              {!list.length && (
                <tr>
                  <td colSpan={6} className="empty">
                    Chưa có bài tập. Bấm Tạo bài tập để giao bài cho lớp.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </AppLayout>
  );
}

type Draft = Pick<Assignment, 'cls' | 'subject' | 'session' | 'title' | 'description' | 'due' | 'max' | 'files' | 'late'>;

// S5-05. Tạo bài tập: tiêu đề, mô tả có định dạng, tệp đính kèm, môn và buổi học, hạn nộp (không được ở quá khứ), điểm tối đa.
// Lưu nháp trước, giao cho lớp sau; khi giao có bước xác nhận và toàn bộ học viên của lớp nhận thông báo.
export function AssignmentEditorPage() {
  const { id } = useParams();
  const [params] = useSearchParams();
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const existing = id ? findAssignment(Number(id)) : undefined;
  const cls0 = existing?.cls ?? params.get('lop') ?? '';
  const k0 = findClass(cls0);
  const [d, setD] = useState<Draft>(() =>
    existing
      ? { ...existing, files: [...existing.files] }
      : { cls: cls0, subject: k0?.subjects[0]?.code ?? '', title: '', description: '', due: '', max: 10, files: [], late: 'Chặn' },
  );
  const [savedAt, setSavedAt] = useState(existing?.savedAt);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [confirm, setConfirm] = useState(false);
  const [preview, setPreview] = useState(false);
  const ta = useRef<HTMLTextAreaElement>(null);
  const role = user?.active ?? -1;
  const k = findClass(d.cls);
  if (!k) return <ErrorPage code="404" />;
  if (!teachable(role, user!.name).includes(k)) return <ErrorPage code="403" />;
  const assigned = existing?.status === 'Đã giao';
  const sessions = sessionsOf(k.code).filter((s) => s.subject === d.subject && !s.cancelled);
  const set = (patch: Partial<Draft>) => setD({ ...d, ...patch });

  function validate(full: boolean) {
    const e: Record<string, string> = {};
    if (!d.title.trim()) e.title = 'Nhập tiêu đề bài tập';
    if (full && !d.description.trim()) e.description = 'Nhập mô tả yêu cầu';
    if (full && !d.due) e.due = 'Chọn hạn nộp';
    if (d.due && d.due < now()) e.due = 'Hạn nộp không được ở quá khứ';
    if (!(d.max > 0 && d.max <= 100)) e.max = 'Điểm tối đa từ 1 đến 100';
    setErrors(e);
    return !Object.keys(e).length;
  }

  function persist(status: Assignment['status']) {
    const at = now();
    const base = { ...d, title: d.title.trim(), description: d.description.trim(), savedAt: at, by: existing?.by ?? user!.name };
    if (existing) Object.assign(existing, base, status === 'Đã giao' && !existing.assignedAt ? { status, assignedAt: at } : {});
    else ASSIGNMENTS.unshift({ ...base, id: Math.max(0, ...ASSIGNMENTS.map((a) => a.id)) + 1, status, assignedAt: status === 'Đã giao' ? at : undefined });
    setSavedAt(at);
    saveAll();
    return existing ?? ASSIGNMENTS[0];
  }

  function draft() {
    if (!validate(false)) return;
    const a = persist(existing?.status ?? 'Nháp');
    toast(assigned ? 'Đã lưu thay đổi' : 'Đã lưu nháp', a.title);
    if (!existing) navigate(`/bai-tap/${a.id}/sua`, { replace: true });
  }

  function assign() {
    const a = persist('Đã giao');
    notify({ at: now(), kind: 'Bài tập', title: 'Bài tập mới: ' + a.title, body: `Lớp ${a.cls} · hạn nộp ${vdt(a.due)} · điểm tối đa ${a.max}.`, link: '/bai-tap-cua-toi/' + a.id, to: { cls: a.cls } });
    saveAll();
    toast('Đã giao bài cho lớp ' + a.cls, `${classSize(a.cls)} học viên đã nhận thông báo.`);
    navigate('/bai-tap');
  }

  /** Bọc phần đang chọn trong ô mô tả bằng ký hiệu định dạng. */
  function wrap(before: string, after = before, block = false) {
    const el = ta.current;
    if (!el) return;
    const { selectionStart: a, selectionEnd: b, value } = el;
    const sel = value.slice(a, b) || (block ? 'nội dung' : 'chữ');
    const text = block ? sel.split('\n').map((l, i) => before.replace('1.', `${i + 1}.`) + l).join('\n') : before + sel + after;
    set({ description: value.slice(0, a) + text + value.slice(b) });
    requestAnimationFrame(() => {
      el.focus();
      el.setSelectionRange(a, a + text.length);
    });
  }

  const tools: [string, () => void, string][] = [
    ['B', () => wrap('**'), 'Đậm'],
    ['I', () => wrap('_'), 'Nghiêng'],
    ['U', () => wrap('__'), 'Gạch chân'],
    ['H1', () => wrap('# ', '', true), 'Tiêu đề lớn'],
    ['H2', () => wrap('## ', '', true), 'Tiêu đề nhỏ'],
    ['• Danh sách', () => wrap('- ', '', true), 'Danh sách'],
    ['1. Số', () => wrap('1. ', '', true), 'Danh sách đánh số'],
    ['<> Mã', () => wrap('`'), 'Mã'],
    ['Liên kết', () => wrap('[', '](https://)'), 'Liên kết'],
  ];

  return (
    <AppLayout crumb={[MODULE, existing ? 'Sửa bài tập' : 'Tạo bài tập']} module={MODULE}>
      <Link to="/bai-tap" className="back">
        <Icon name="back" size={16} />
        Bài tập
      </Link>
      <div className="h">
        <div>
          <h2>
            {existing ? 'Sửa bài tập' : 'Tạo bài tập'}{' '}
            {savedAt && <Pill tone={assigned ? 'ok' : undefined}>{assigned ? 'Đã giao' : `Bản nháp · đã lưu ${savedAt.slice(11, 16)}`}</Pill>}
          </h2>
          <p>
            Lớp {k.code} · {k.program}
          </p>
        </div>
      </div>
      <div className="as-grid">
        <section className="panel sp-card">
          <div className="field">
            <label htmlFor="as-title">Tiêu đề</label>
            <div className={'inp' + (errors.title ? ' bad' : '')}>
              <input id="as-title" value={d.title} placeholder="Ví dụ: Bài 16: Phân trang danh sách" onChange={(e) => set({ title: e.target.value })} />
            </div>
            {errors.title && <p className="err">{errors.title}</p>}
          </div>
          <div className="field">
            <div className="as-desc-h">
              <span className="lbl">Mô tả</span>
              <button type="button" className="link" onClick={() => setPreview(!preview)}>
                {preview ? 'Sửa' : 'Xem trước'}
              </button>
            </div>
            <div className={'as-editor' + (errors.description ? ' bad' : '')}>
              <div className="as-tools" role="toolbar" aria-label="Định dạng">
                {tools.map(([label, run, title]) => (
                  <button key={label} type="button" title={title} disabled={preview} onClick={run}>
                    {label}
                  </button>
                ))}
              </div>
              {preview ? (
                <div className="as-preview">{d.description.trim() ? <Markdown text={d.description} /> : <p className="hint">Chưa có mô tả.</p>}</div>
              ) : (
                <textarea ref={ta} aria-label="Mô tả" rows={10} value={d.description} placeholder="Yêu cầu, tiêu chí, cách nộp bài." onChange={(e) => set({ description: e.target.value })} />
              )}
            </div>
            {errors.description && <p className="err">{errors.description}</p>}
          </div>
          <span className="lbl">Tệp đính kèm</span>
          <ul className="as-files">
            {d.files.map((f: FileRef, i) => (
              <li key={i}>
                <Icon name="file" size={16} />
                <span>{f.name}</span>
                <small>{fileSize(f.size)}</small>
                <button type="button" className="icon-btn" aria-label={'Bỏ ' + f.name} onClick={() => set({ files: d.files.filter((_, j) => j !== i) })}>
                  <Icon name="x" size={14} />
                </button>
              </li>
            ))}
          </ul>
          <label className="as-add">
            <Icon name="plus" size={14} />
            Thêm tệp đính kèm
            <input type="file" multiple onChange={(e) => set({ files: [...d.files, ...[...(e.target.files ?? [])].map((f) => ({ name: f.name, size: f.size }))] })} />
          </label>
        </section>
        <aside className="panel sp-card">
          <div className="field">
            <label htmlFor="as-subject">Môn học</label>
            <div className="inp">
              <select id="as-subject" value={d.subject} onChange={(e) => set({ subject: e.target.value, session: undefined })}>
                {k.subjects.map((s) => (
                  <option key={s.code} value={s.code}>
                    {s.code} · {s.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="field">
            <label htmlFor="as-session">Buổi học</label>
            <div className="inp">
              <select id="as-session" value={d.session ?? ''} onChange={(e) => set({ session: e.target.value || undefined })}>
                <option value="">Không gắn buổi cụ thể</option>
                {sessions.map((s) => (
                  <option key={s.id} value={s.id}>
                    Buổi {s.no} · {s.topic} ({WEEKDAYS[dow(s.date)]} {dm(s.date)})
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="grid2">
            <div className="field">
              <label htmlFor="as-due">Hạn nộp</label>
              <div className={'inp' + (errors.due ? ' bad' : '')}>
                <input id="as-due" type="datetime-local" min={now()} value={d.due} onChange={(e) => set({ due: e.target.value })} />
              </div>
              {errors.due && <p className="err">{errors.due}</p>}
            </div>
            <div className="field">
              <label htmlFor="as-max">Điểm tối đa</label>
              <div className={'inp' + (errors.max ? ' bad' : '')}>
                <input id="as-max" type="number" min={1} max={100} value={d.max} onChange={(e) => set({ max: Number(e.target.value) })} />
              </div>
              {errors.max && <p className="err">{errors.max}</p>}
            </div>
          </div>
          {/* S5-09 */}
          <fieldset className="check-list as-late">
            <legend>Nộp muộn</legend>
            <label>
              <input type="radio" name="late" checked={d.late === 'Chặn'} onChange={() => set({ late: 'Chặn' })} />
              <span>
                Chặn nộp muộn
                <small>Hết hạn là khoá nộp bài</small>
              </span>
            </label>
            <label>
              <input type="radio" name="late" checked={d.late === 'Cho nộp'} onChange={() => set({ late: 'Cho nộp' })} />
              <span>
                Cho nộp, đánh dấu Nộp muộn
                <small>Bài nộp sau hạn hiện nhãn và số giờ trễ</small>
              </span>
            </label>
          </fieldset>
          <p className="hint">
            {assigned ? 'Bài đã giao vẫn sửa được mô tả và hạn nộp, không quay về bản nháp được.' : `Khi giao, ${classSize(k.code)} học viên của lớp ${k.code} sẽ nhận thông báo.`}
          </p>
          <div className="as-acts">
            <button type="button" className="btn" onClick={draft}>
              {assigned ? 'Lưu thay đổi' : 'Lưu nháp'}
            </button>
            {!assigned && (
              <button type="button" className="btn primary" onClick={() => validate(true) && setConfirm(true)}>
                Giao cho lớp
              </button>
            )}
          </div>
        </aside>
      </div>
      {confirm && (
        <Modal onClose={() => setConfirm(false)} label={'Giao bài tập cho lớp ' + k.code}>
          <h3 style={{ marginBottom: 4 }}>Giao bài tập cho lớp {k.code}?</h3>
          <p className="hint" style={{ fontSize: 14, marginBottom: 16 }}>
            Sau khi giao, học viên thấy bài tập ngay và bắt đầu nộp được.
          </p>
          <dl className="sp-facts as-sum">
            <div className="wide">
              <dt>Bài tập</dt>
              <dd>{d.title}</dd>
            </div>
            <div>
              <dt>Hạn nộp</dt>
              <dd>{vdt(d.due)}</dd>
            </div>
            <div>
              <dt>Điểm tối đa</dt>
              <dd>{d.max}</dd>
            </div>
            <div>
              <dt>Nộp muộn</dt>
              <dd>{d.late === 'Chặn' ? 'Chặn sau hạn' : 'Cho nộp, có nhãn'}</dd>
            </div>
          </dl>
          <Box icon="bell" tone="info" title={`${classSize(k.code)} học viên của lớp sẽ nhận thông báo`}>
            Bài đã giao vẫn sửa được mô tả và hạn nộp, không quay về bản nháp được.
          </Box>
          <div className="acts">
            <button type="button" className="btn" style={{ width: 100 }} onClick={() => setConfirm(false)}>
              Để sau
            </button>
            <button type="button" className="btn primary" style={{ width: 120 }} onClick={assign}>
              Giao bài
            </button>
          </div>
        </Modal>
      )}
    </AppLayout>
  );
}
