import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Markdown } from '../../components/Markdown';
import { useToast } from '../../components/Toast';
import { Box, Pill } from '../../components/ui';
import { fileSize, findAssignment, lateHours, SUBMISSIONS, subOf, vdt, type Assignment, type FileRef, type Version } from '../../data/assignments';
import { findClass } from '../../data/classes';
import { notify } from '../../data/notifications';
import { findSession } from '../../data/schedule';
import { saveAll } from '../../data/store';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import { VersionList } from '../S5-08-phien-ban/Versions';
import { useMe } from '../S4-05-lich-cua-toi/MySchedulePage';
import { MODULE } from '../S5-05-bai-tap/AssignmentPages';
import { dueOf, nowIso, remaining } from '../S5-06-bai-tap-cua-toi/MyAssignmentsPage';
import './submit.css';

export const MAX_FILE = 20 * 1048576;
/** Liên kết repo: https:// và trỏ tới GitHub, GitLab hoặc Bitbucket, có chủ sở hữu và tên repo. */
export const REPO_RE = /^https:\/\/(www\.)?(github\.com|gitlab\.com|bitbucket\.org)\/[\w.-]+\/[\w.-]+\/?$/;

type Picked = FileRef & { tooBig: boolean };

/** "sớm 3 giờ 45 phút" hoặc "trễ 9 giờ" so với hạn. */
export function timing(a: Assignment, v: Version) {
  const late = lateHours(a, v);
  if (late) return `Nộp muộn, trễ ${late} giờ`;
  const m = Math.round((Date.parse(a.due + ':00') - Date.parse(v.at + ':00')) / 60000);
  return `Đúng hạn, sớm ${m >= 60 ? Math.floor(m / 60) + ' giờ ' : ''}${m % 60} phút`;
}


// S5-07. Nộp bài: tải lên nhiều tệp (mỗi tệp tối đa 20MB) kèm liên kết repo; liên kết được kiểm tra định dạng trước khi lưu.
// Nộp xong hiện mốc thời gian nộp và trạng thái Đã nộp.
export function SubmitPage() {
  const { id = '' } = useParams();
  const me = useMe();
  const toast = useToast();
  const [, setTick] = useState(0);
  const [files, setFiles] = useState<Picked[]>([]);
  const [repo, setRepo] = useState('');
  const [note, setNote] = useState('');
  const [tried, setTried] = useState(false);
  const [editing, setEditing] = useState(false);
  const a = findAssignment(Number(id));
  if (!a || a.status !== 'Đã giao') return <ErrorPage code="404" />;
  if (!me || me.cls !== a.cls) return <ErrorPage code="403" />;
  const sub = subOf(a.id, me.code);
  const latest = sub?.versions[0];
  const due = dueOf(a, me.code);
  const [left, tone] = remaining(due);
  const closed = a.late === 'Chặn' && nowIso() > due;
  const s = a.session ? findSession(a.session) : undefined;
  const teacher = findClass(a.cls)?.subjects.find((x) => x.code === a.subject)?.teacher ?? a.by;
  const refresh = () => setTick((t) => t + 1);
  const canSubmit = !sub || sub.state === 'Cần làm lại' || editing;
  // S5-08: bài đã chấm khoá nộp lại, trừ khi giảng viên mở lại.
  const locked = sub?.state === 'Đã chấm' && !sub.reopened;

  const big = files.filter((f) => f.tooBig);
  const repoBad = repo.trim() !== '' && !REPO_RE.test(repo.trim());
  const errors = big.length + (repoBad ? 1 : 0) + (!files.some((f) => !f.tooBig) && !repo.trim() ? 1 : 0);

  function submit() {
    setTried(true);
    if (errors || closed) return;
    const at = nowIso();
    const v: Version = { no: (latest?.no ?? 0) + 1, at, files: files.filter((f) => !f.tooBig).map(({ name, size }) => ({ name, size })), repo: repo.trim() || undefined, note: note.trim() || undefined };
    if (sub) {
      sub.versions.unshift(v);
      sub.state = 'Đã nộp';
      sub.reopened = false;
    } else SUBMISSIONS.push({ assignment: a!.id, student: me!.code, versions: [v], state: 'Đã nộp' });
    notify({ at, kind: 'Bài tập', title: `${me!.name} đã nộp ${a!.title.split(':')[0]}`, body: `Lớp ${a!.cls} · bản ${v.no} · ${timing(a!, v)}.`, link: '/bai-tap/' + a!.id, to: { users: [teacher] } });
    saveAll();
    setFiles([]);
    setRepo('');
    setNote('');
    setTried(false);
    setEditing(false);
    refresh();
    toast('Đã nộp bài', `${a!.title.split(':')[0]} · ${vdt(at)}`);
  }

  function askReopen() {
    notify({ at: nowIso(), kind: 'Bài tập', title: `${me!.name} xin mở lại ${a!.title.split(':')[0]}`, body: `Học viên muốn nộp lại bài đã chấm của lớp ${a!.cls}.`, link: '/bai-tap/' + a!.id, to: { users: [teacher] } });
    saveAll();
    toast('Đã gửi yêu cầu tới giảng viên');
  }

  return (
    <AppLayout crumb={[MODULE, a.title.split(':')[0]]} module={MODULE}>
      <Link to="/bai-tap-cua-toi" className="back">
        <Icon name="back" size={16} />
        Bài tập của tôi
      </Link>
      <div className="sb-grid">
        <section className="panel sp-card">
          <h2 className="sb-title">{a.title}</h2>
          <p className="hint" style={{ fontSize: 14 }}>
            {a.subject}
            {s ? ` · Buổi ${s.no}` : ''} · {teacher} · Điểm tối đa {a.max}
          </p>
          <div className={'sb-due ' + (nowIso() > due || (sub && sub.state !== 'Cần làm lại') ? '' : (tone ?? ''))}>
            <Icon name="clock" size={16} />
            {nowIso() > due ? `Hạn nộp ${vdt(due)}, đã qua` : `Hạn nộp ${vdt(due)}, ${left.toLowerCase()}`}
          </div>
          <h4 className="lbl">Yêu cầu</h4>
          <Markdown text={a.description} />
          {a.files.length > 0 && (
            <>
              <h4 className="lbl" style={{ marginTop: 16 }}>
                Tệp đính kèm của giảng viên
              </h4>
              <ul className="as-files">
                {a.files.map((f) => (
                  <li key={f.name}>
                    <Icon name="file" size={16} />
                    <span>{f.name}</span>
                    <small>{fileSize(f.size)}</small>
                  </li>
                ))}
              </ul>
            </>
          )}
          <p className="hint" style={{ marginTop: 12 }}>
            {a.late === 'Chặn' ? 'Nộp muộn: hết hạn là khoá nộp bài.' : 'Nộp muộn: được nộp sau hạn nhưng bài sẽ có nhãn Nộp muộn kèm số giờ trễ.'}
          </p>
        </section>

        <section className="panel sp-card">
          <div className="sb-head">
            <h3>Bài nộp của bạn</h3>
            <Pill tone={!sub ? undefined : sub.state === 'Đã chấm' ? 'ok' : sub.state === 'Cần làm lại' ? 'off' : 'info'}>{sub?.state ?? 'Chưa nộp'}</Pill>
          </div>
          {latest && !canSubmit && (
            <Box icon="check" tone={lateHours(a, latest) ? 'warn' : 'ok'} title={`Đã nộp lúc ${latest.at.slice(11, 16)} ngày ${vdt(latest.at).slice(0, 10)}`}>
              Bản {latest.no} là bản mới nhất, giảng viên sẽ chấm bản này. {timing(a, latest)}.
            </Box>
          )}
          {sub?.redo && sub.state === 'Cần làm lại' && (
            <Box icon="refresh" tone="bad" title="Giảng viên yêu cầu làm lại">
              {sub.redo.note} Hạn nộp lại {vdt(sub.redo.due)}.
            </Box>
          )}
          {canSubmit && closed && (
            <Box icon="lock" tone="bad" title="Đã hết hạn nộp">
              Bài tập này khoá nộp sau hạn. Liên hệ giảng viên nếu có lý do chính đáng.
            </Box>
          )}
          {canSubmit && !closed && (
            <>
              <span className="lbl">Tệp bài làm</span>
              <label className="lv-drop">
                <Icon name="upload" />
                <b>Chọn tệp hoặc kéo vào đây</b>
                <small>Được chọn nhiều tệp · mỗi tệp tối đa 20MB</small>
                <input type="file" multiple onChange={(e) => setFiles([...files, ...[...(e.target.files ?? [])].map((f) => ({ name: f.name, size: f.size, tooBig: f.size > MAX_FILE }))])} />
              </label>
              <ul className="as-files">
                {files.map((f, i) => (
                  <li key={i} className={f.tooBig ? 'bad' : undefined}>
                    <Icon name="file" size={16} />
                    <span>{f.name}</span>
                    <small>
                      {fileSize(f.size)}
                      {f.tooBig ? ' · vượt 20MB' : ''}
                    </small>
                    <button type="button" className="icon-btn" aria-label={'Bỏ ' + f.name} onClick={() => setFiles(files.filter((_, j) => j !== i))}>
                      <Icon name="x" size={14} />
                    </button>
                  </li>
                ))}
              </ul>
              {big.length > 0 && <p className="err sb-err">Tệp vượt 20MB không được tải lên. Hãy nén lại hoặc gửi liên kết xem video.</p>}
              <div className="field">
                <label htmlFor="sb-repo">Liên kết repo mã nguồn</label>
                <div className={'inp lead' + (repoBad ? ' bad' : '')}>
                  <Icon name="go" />
                  <input id="sb-repo" value={repo} placeholder="https://github.com/ten-ban/ten-repo" onChange={(e) => setRepo(e.target.value)} />
                </div>
                {repoBad && <p className="err">Liên kết phải bắt đầu bằng https:// và trỏ tới GitHub, GitLab hoặc Bitbucket</p>}
              </div>
              <div className="field">
                <label htmlFor="sb-note">Ghi chú cho giảng viên (không bắt buộc)</label>
                <div className="inp">
                  <textarea id="sb-note" value={note} onChange={(e) => setNote(e.target.value)} />
                </div>
              </div>
              <div className="sb-foot">
                {tried && errors > 0 && <span className="err">{!files.length && !repo.trim() ? 'Chọn ít nhất một tệp hoặc nhập liên kết repo.' : `Sửa ${errors} lỗi phía trên để nộp được.`}</span>}
                {editing && (
                  <button type="button" className="btn" onClick={() => setEditing(false)}>
                    Huỷ
                  </button>
                )}
                <button type="button" className="btn primary" style={{ width: 130 }} onClick={submit}>
                  {sub ? 'Nộp lại' : 'Nộp bài'}
                </button>
              </div>
            </>
          )}
          {sub && !canSubmit && locked && (
            <Box icon="lock" tone="warn" title="Bài đã chấm xong nên không nộp lại được">
              Nếu cần nộp lại, hãy nhờ giảng viên mở lại bài này.
            </Box>
          )}
          {sub && !canSubmit && (
            <>
              <h4 className="vs-h">Các phiên bản đã nộp</h4>
              <VersionList a={a} sub={sub} />
              <div className="sb-foot">
                <span className="hint" style={{ flex: 1 }}>
                  {locked ? '' : `Nộp lại sẽ tạo bản ${sub.versions[0].no + 1}, ${sub.versions.length > 1 ? sub.versions.length + ' bản cũ' : 'bản cũ'} vẫn được giữ.`}
                  {!locked && nowIso() > due && a.late === 'Cho nộp' && <span className="warn-t"> Nộp lại sau hạn sẽ bị gắn nhãn Nộp muộn.</span>}
                </span>
                {locked && (
                  <button type="button" className="btn" onClick={askReopen}>
                    Xin giảng viên mở lại bài
                  </button>
                )}
                <button type="button" className="btn" disabled={locked || closed} onClick={() => setEditing(true)}>
                  Nộp lại
                </button>
              </div>
            </>
          )}
        </section>
      </div>
    </AppLayout>
  );
}

