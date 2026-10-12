import { useState } from 'react';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Box, Modal, Pill } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { CLASSES, isOpen } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { viDate } from '../../data/leads';
import { notify } from '../../data/notifications';
import { saveAll } from '../../data/store';
import { STUDENTS } from '../../data/students';
import { DRAFTS, isOpenRun, notAnswered, QUESTIONS, RUNS, runState, type Answer, type SurveyRun } from '../../data/surveys';
import './survey.css';

export const MODULE = 'Khảo sát chất lượng';
const empty = (): Answer => ({ scores: {}, texts: {} });

// S8-05. Khảo sát chất lượng: học viên trả lời, Quản lý đào tạo khai báo bộ câu hỏi và mở theo lớp.
export function SurveyPage() {
  const { user } = useAuth();
  if (user?.active === 0) return <MySurveys />;
  if (user?.active === 5 || user?.active === 6) return <SurveyAdmin />;
  return (
    <AppLayout crumb={[MODULE]} module={MODULE}>
      <p className="empty">Giảng viên xem kết quả tổng hợp khảo sát của các lớp mình dạy.</p>
    </AppLayout>
  );
}

function MySurveys() {
  const { user } = useAuth();
  const toast = useToast();
  const me = STUDENTS.find((s) => s.login === user?.email);
  const mine = RUNS.filter((r) => r.cls === me?.cls);
  const [sel, setSel] = useState(() => mine.find((r) => isOpenRun(r) && !r.responded.includes(me?.code ?? ''))?.id ?? mine[0]?.id);
  const r = mine.find((x) => x.id === sel);
  const key = `${r?.id}|${me?.code}`;
  const [ans, setAns] = useState<Answer>(() => DRAFTS[key] ?? empty());
  const [tried, setTried] = useState(false);
  const [, setTick] = useState(0);
  if (!me) return <AppLayout crumb={[MODULE]} module={MODULE}><p className="empty">Không tìm thấy hồ sơ học viên.</p></AppLayout>;
  const scale = r?.questions.filter((q) => q.kind === 'scale') ?? [];
  const left = scale.filter((q) => !ans.scores[q.id]).length;
  const done = !!r && r.responded.includes(me.code);
  const answered = r ? r.questions.filter((q) => (q.kind === 'scale' ? ans.scores[q.id] : ans.texts[q.id]?.trim())).length : 0;
  const choose = (id: number) => {
    setSel(id);
    setAns(DRAFTS[`${id}|${me.code}`] ?? empty());
    setTried(false);
  };

  function submit() {
    if (!r) return;
    setTried(true);
    if (left) return;
    // Lưu câu trả lời và danh sách người đã trả lời tách nhau để giữ ẩn danh.
    r.answers.push({ scores: { ...ans.scores }, texts: Object.fromEntries(Object.entries(ans.texts).filter(([, v]) => v.trim())) });
    r.responded.push(me!.code);
    delete DRAFTS[key];
    saveAll();
    setTick((t) => t + 1);
    toast('Đã gửi khảo sát', 'Cảm ơn bạn đã góp ý');
  }

  return (
    <AppLayout crumb={[MODULE]} module={MODULE}>
      <div className="sv-grid">
        <section>
          {r ? (
            <>
              <div className="h">
                <div>
                  <h2>
                    {r.title} lớp {r.cls}
                  </h2>
                  <p>Hạn trả lời {viDate(r.due)} · khoảng 3 phút · mỗi học viên trả lời một lần</p>
                </div>
              </div>
              <Box icon="lock" tone="info" title="Câu trả lời của bạn ẩn danh với giảng viên">
                Giảng viên chỉ thấy kết quả tổng hợp. Quản lý đào tạo biết bạn đã trả lời hay chưa để nhắc, không gắn tên bạn với nội dung.
              </Box>
              {done ? (
                <div className="panel fe-pad sv-done">
                  <Icon name="check" size={20} />
                  Bạn đã gửi khảo sát này. Khảo sát đã gửi không sửa hay gửi lại được.
                </div>
              ) : !isOpenRun(r) ? (
                <div className="panel fe-pad">Khảo sát đã đóng ngày {viDate(r.due)}.</div>
              ) : (
                <div className="sv-form">
                  <p className="hint">
                    Câu {answered} / {r.questions.length}
                  </p>
                  {r.questions.map((q, i) => (
                    <div key={q.id} className={'panel sv-q' + (tried && q.kind === 'scale' && !ans.scores[q.id] ? ' bad' : '')}>
                      <b>
                        {i + 1}. {q.text}
                      </b>
                      {q.kind === 'scale' ? (
                        <>
                          <div className="sv-scale" role="radiogroup" aria-label={q.text}>
                            {[1, 2, 3, 4, 5].map((n) => (
                              <button key={n} type="button" role="radio" aria-checked={ans.scores[q.id] === n} className={ans.scores[q.id] === n ? 'on' : undefined} onClick={() => setAns({ ...ans, scores: { ...ans.scores, [q.id]: n } })}>
                                {n}
                              </button>
                            ))}
                          </div>
                          <p className="sv-legend">
                            <span>1 = Rất không hài lòng</span>
                            <span>5 = Rất hài lòng</span>
                          </p>
                        </>
                      ) : (
                        <textarea className="inp" rows={3} aria-label={q.text} placeholder="Viết câu trả lời của bạn (không bắt buộc)" value={ans.texts[q.id] ?? ''} onChange={(e) => setAns({ ...ans, texts: { ...ans.texts, [q.id]: e.target.value } })} />
                      )}
                    </div>
                  ))}
                  <div className="sv-foot">
                    {tried && left > 0 && <span className="err">Còn {left} câu thang điểm chưa trả lời</span>}
                    <button
                      type="button"
                      className="btn"
                      onClick={() => {
                        DRAFTS[key] = ans;
                        saveAll();
                        toast('Đã lưu nháp', 'Bạn có thể quay lại trả lời tiếp trước hạn');
                      }}
                    >
                      Lưu nháp
                    </button>
                    <button type="button" className="btn primary" onClick={submit}>
                      Gửi
                    </button>
                  </div>
                </div>
              )}
            </>
          ) : (
            <p className="empty">Lớp của bạn chưa có khảo sát nào.</p>
          )}
        </section>
        <aside className="panel fe-pad sv-mine">
          <h3>Khảo sát của tôi</h3>
          {mine.map((x) => {
            const sent = x.responded.includes(me.code);
            return (
              <button key={x.id} type="button" className={x.id === sel ? 'on' : undefined} onClick={() => choose(x.id)}>
                <span>
                  <b>
                    {x.cls} {x.title.replace('Khảo sát ', '')}
                  </b>
                  <small>{sent ? 'Đã gửi' : `Hạn ${viDate(x.due).slice(0, 5)}`}</small>
                </span>
                <Pill tone={sent ? 'ok' : isOpenRun(x) ? 'wait' : undefined}>{sent ? 'Đã trả lời' : isOpenRun(x) ? (DRAFTS[`${x.id}|${me.code}`] ? 'Đang làm' : 'Chưa trả lời') : 'Đã đóng'}</Pill>
              </button>
            );
          })}
          <p className="hint">Khảo sát đã gửi không sửa hay gửi lại được.</p>
        </aside>
      </div>
    </AppLayout>
  );
}

function SurveyAdmin() {
  const toast = useToast();
  const [qs, setQs] = useState(() => QUESTIONS.map((q) => ({ ...q })));
  const [sel, setSel] = useState(RUNS.find(isOpenRun)?.id ?? RUNS[0]?.id);
  const [opening, setOpening] = useState(false);
  const [, setTick] = useState(0);
  const r = RUNS.find((x) => x.id === sel);
  const missing = r ? notAnswered(r) : [];
  const dirty = JSON.stringify(qs) !== JSON.stringify(QUESTIONS);
  const add = (kind: 'scale' | 'text') => setQs([...qs, { id: 'q' + Date.now().toString(36), text: '', kind }]);

  function remind(x: SurveyRun) {
    const to = notAnswered(x);
    notify({ at: TODAY + 'T' + new Date().toTimeString().slice(0, 5), kind: 'Khảo sát', title: `Nhắc trả lời ${x.title.toLowerCase()} lớp ${x.cls}`, body: `Hạn ${viDate(x.due)}, khoảng 3 phút.`, link: '/khao-sat', to: { users: to.map((s) => s.name) } });
    toast(`Đã nhắc ${to.length} học viên`, `${x.title} · ${x.cls}`);
  }

  return (
    <AppLayout crumb={[MODULE]} module={MODULE}>
      <div className="h">
        <div>
          <h2>Khảo sát chất lượng</h2>
          <p>Bộ câu hỏi dùng chung, mở riêng cho từng lớp</p>
        </div>
      </div>
      <div className="sv-admin">
        <section className="panel">
          <div className="fe-head">
            <h3>Bộ câu hỏi</h3>
            <span className="hint">{qs.length} câu</span>
          </div>
          {qs.map((q, i) => (
            <div key={q.id} className="sv-qrow">
              <span className="hint">{i + 1}</span>
              <div>
                <input aria-label={`Câu ${i + 1}`} value={q.text} placeholder="Nội dung câu hỏi" onChange={(e) => setQs(qs.map((x) => (x.id === q.id ? { ...x, text: e.target.value } : x)))} />
                <span className={'sv-kind' + (q.kind === 'text' ? ' text' : '')}>{q.kind === 'scale' ? 'Thang điểm 1–5' : 'Tự luận'}</span>
              </div>
              <button type="button" className="icon-btn" aria-label={`Xoá câu ${i + 1}`} onClick={() => setQs(qs.filter((x) => x.id !== q.id))}>
                <Icon name="x" size={16} />
              </button>
            </div>
          ))}
          <div className="sv-adds">
            <button type="button" className="link" onClick={() => add('scale')}>
              + Thêm câu thang điểm
            </button>
            <button type="button" className="link" onClick={() => add('text')}>
              + Thêm câu tự luận
            </button>
          </div>
          <div className="fe-pad">
            <Box icon="file" tone="info" title="Sửa bộ câu hỏi không ảnh hưởng đợt đã mở">
              Lớp đã mở khảo sát giữ nguyên câu hỏi tại thời điểm mở.
            </Box>
            <div className="acts" style={{ marginTop: 12 }}>
              <button type="button" className="btn" disabled={!dirty} onClick={() => setQs(QUESTIONS.map((q) => ({ ...q })))}>
                Huỷ
              </button>
              <button
                type="button"
                className="btn primary"
                disabled={!dirty}
                onClick={() => {
                  if (qs.some((q) => !q.text.trim())) return toast('Chưa lưu', 'Câu hỏi nào cũng cần nội dung');
                  if (!qs.some((q) => q.kind === 'scale')) return toast('Chưa lưu', 'Cần ít nhất một câu thang điểm');
                  QUESTIONS.splice(0, QUESTIONS.length, ...qs.map((q) => ({ ...q, text: q.text.trim() })));
                  saveAll();
                  setTick((t) => t + 1);
                  toast('Đã lưu bộ câu hỏi', 'Áp dụng cho các lớp mở từ bây giờ');
                }}
              >
                Lưu bộ câu hỏi
              </button>
            </div>
          </div>
        </section>
        <aside className="sv-side">
          <section className="panel">
            <div className="fe-head">
              <h3>Mở cho lớp</h3>
              <button type="button" className="btn" onClick={() => setOpening(true)}>
                <Icon name="plus" size={16} />
                Mở thêm
              </button>
            </div>
            {RUNS.map((x) => {
              const left = notAnswered(x).length;
              return (
                <div key={x.id} className={'sv-run' + (x.id === sel ? ' on' : '')} role="button" tabIndex={0} onClick={() => setSel(x.id)} onKeyDown={(e) => e.key === 'Enter' && setSel(x.id)}>
                  <div>
                    <b>{x.cls}</b> <span className="hint">· {x.title}</span>
                    <small className="hint">Hạn {viDate(x.due)}</small>
                    <small>
                      {x.responded.length} / {x.size} đã trả lời
                    </small>
                  </div>
                  <div className="sv-run-r">
                    <Pill tone={isOpenRun(x) ? 'ok' : undefined}>{runState(x)}</Pill>
                    {isOpenRun(x) && left > 0 && (
                      <button type="button" className="link" onClick={(e) => (e.stopPropagation(), remind(x))}>
                        Nhắc {left} người
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </section>
          {r && (
            <section className="panel fe-pad">
              <h3>
                {r.cls} · chưa trả lời ({missing.length})
              </h3>
              {missing.slice(0, 6).map((s) => (
                <p key={s.code} className="sv-who">
                  <span>{s.name}</span>
                  <span className="hint">Chưa trả lời</span>
                </p>
              ))}
              {missing.length > 6 && <p className="hint">và {missing.length - 6} học viên khác</p>}
              {!missing.length && <p className="hint">Mọi học viên đã trả lời.</p>}
              <p className="hint sv-note">
                <Icon name="eyeoff" size={14} /> Chỉ biết ai đã trả lời, không xem được nội dung theo tên.
              </p>
            </section>
          )}
        </aside>
      </div>
      {opening && (
        <OpenDialog
          onClose={() => setOpening(false)}
          onOpen={(cls, title, due) => {
            const id = Math.max(0, ...RUNS.map((x) => x.id)) + 1;
            const size = STUDENTS.filter((s) => s.cls === cls && s.status !== 'Thôi học').length;
            RUNS.unshift({ id, cls, title, questions: QUESTIONS.map((q) => ({ ...q })), opened: TODAY, due, size, responded: [], answers: [] });
            notify({ at: TODAY + 'T' + new Date().toTimeString().slice(0, 5), kind: 'Khảo sát', title: `${title} lớp ${cls}`, body: `Hạn trả lời ${viDate(due)}. Câu trả lời ẩn danh với giảng viên.`, link: '/khao-sat', to: { cls } });
            saveAll();
            setSel(id);
            setOpening(false);
            toast('Đã mở khảo sát', `${cls} · hạn ${viDate(due)}`);
          }}
        />
      )}
    </AppLayout>
  );
}

function OpenDialog({ onClose, onOpen }: { onClose: () => void; onOpen: (cls: string, title: string, due: string) => void }) {
  const free = CLASSES.filter((k) => isOpen(k) && !RUNS.some((r) => r.cls === k.code && isOpenRun(r)));
  const [cls, setCls] = useState(free[0]?.code ?? '');
  const [title, setTitle] = useState('Khảo sát giữa khoá');
  const [due, setDue] = useState(() => new Date(Date.parse(TODAY) + 14 * 864e5).toISOString().slice(0, 10));
  const [error, setError] = useState('');
  return (
    <Modal onClose={onClose} label="Mở khảo sát cho lớp">
      <h3 style={{ marginBottom: 16 }}>Mở khảo sát cho lớp</h3>
      <div className="field">
        <label htmlFor="sv-cls">Lớp</label>
        <div className="inp">
          <select id="sv-cls" value={cls} onChange={(e) => setCls(e.target.value)}>
            {free.map((k) => (
              <option key={k.code}>{k.code}</option>
            ))}
          </select>
        </div>
        {!free.length && <p className="hint">Lớp nào đang học cũng đã có khảo sát đang mở.</p>}
      </div>
      <div className="grid2">
        <div className="field">
          <label htmlFor="sv-title">Tên đợt</label>
          <div className="inp">
            <input id="sv-title" value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label htmlFor="sv-due">Hạn trả lời</label>
          <div className="inp">
            <input id="sv-due" type="date" min={TODAY} value={due} onChange={(e) => setDue(e.target.value)} />
          </div>
        </div>
      </div>
      <p className="hint" style={{ marginBottom: 12 }}>
        Lớp nhận {QUESTIONS.length} câu hỏi của bộ hiện tại; học viên nhận thông báo trong ứng dụng.
      </p>
      {error && <p className="err" style={{ marginBottom: 12 }}>{error}</p>}
      <div className="acts">
        <button type="button" className="btn" style={{ width: 90 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className="btn primary" onClick={() => (!cls ? setError('Chọn lớp') : !title.trim() ? setError('Nhập tên đợt') : !due || due < TODAY ? setError('Hạn trả lời phải từ hôm nay') : onOpen(cls, title.trim(), due))}>
          Mở khảo sát
        </button>
      </div>
    </Modal>
  );
}
