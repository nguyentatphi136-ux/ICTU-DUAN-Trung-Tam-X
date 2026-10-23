import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Box, LoadState, Pager, paginate, Pill, usePaging } from '../../components/ui';
import { apiEnabled, useLoad } from '../../data/api';
import { useAuth } from '../../data/auth';
import { findClass, totalSessions, type Klass } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { viDate } from '../../data/leads';
import { capacityText, ROOMS } from '../../data/rooms';
import { clashes, clashText, dm, dow, generate, HOLIDAYS, longDate, SESSIONS, shiftOf, shortDate, SHIFTS, WEEKDAYS, WEEKDAY_NAMES, type ClassSession, type Pattern, type Shift, type Skipped } from '../../data/schedule';
import { loadClass, loadRooms } from '../../data/s3api';
import { addHoliday, loadClassSessions, loadHolidays, loadSessions, previewSchedule, saveSchedule } from '../../data/s4api';
import { saveAll } from '../../data/store';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import '../S3-04-ho-so-hoc-vien/student.css';
import { canManageClasses, MODULE } from '../S3-07-danh-sach-lop/ClassListPage';
import './generate.css';

const DAY_ORDER = [1, 2, 3, 4, 5, 6, 0];
type Step = 1 | 2 | 3;

// S4-01. Sinh thời khoá biểu theo mẫu lặp: các thứ trong tuần, ca học, ngày bắt đầu, số buổi.
// Hệ thống sinh đủ buổi, bỏ qua ngày nghỉ lễ đã khai báo, gắn mỗi buổi với một bài học, cho xem trước rồi mới lưu.
// S4-02. Khi xem trước, buổi trùng giảng viên hoặc trùng phòng với lớp khác bị đánh dấu và chặn lưu, trừ khi xác nhận ghi đè.
export function GeneratePage() {
  const { code = '' } = useParams();
  // Có backend: lớp (môn, giảng viên), ngày nghỉ lễ, phòng lấy từ máy chủ trước khi khởi tạo mẫu lặp theo lớp.
  const load = useLoad(() => Promise.all([loadClass(code), loadHolidays(), loadRooms()]), [code]);
  if (apiEnabled && (load.loading || load.error))
    return (
      <AppLayout crumb={[MODULE, code, 'Sinh thời khoá biểu']} module={MODULE}>
        <LoadState {...load} />
      </AppLayout>
    );
  return <Generate key={code} code={code} />;
}

function Generate({ code }: { code: string }) {
  const { user } = useAuth();
  const k = findClass(code);
  const [step, setStep] = useState<Step>(1);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [pattern, setPattern] = useState<Pattern>(() => ({
    days: [1, 3, 5],
    start: SHIFTS['Tối'][0],
    end: SHIFTS['Tối'][1],
    from: k && k.start > TODAY ? k.start : TODAY,
    count: k ? totalSessions(k) : 0,
    room: k?.room,
  }));
  const [result, setResult] = useState<{ sessions: ClassSession[]; skipped: Skipped[] } | null>(null);
  if (!k) return <ErrorPage code="404" />;
  if (!canManageClasses(user?.active ?? -1)) return <ErrorPage code="403" />;

  const preview = () => {
    if (!apiEnabled) {
      setResult(generate(k, pattern));
      setStep(2);
      return;
    }
    // Máy chủ sinh lịch từ giáo trình, phân công, ngày nghỉ lễ trong CSDL; tải lịch các lớp khác cùng khoảng để báo trùng.
    setBusy(true);
    setError('');
    previewSchedule(k.code, pattern)
      .then(async (r) => {
        if (r.sessions.length) await loadSessions(r.sessions[0].date, r.sessions.at(-1)!.date);
        setResult(r);
        setStep(2);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setBusy(false));
  };

  return (
    <AppLayout crumb={[MODULE, k.code, 'Sinh thời khoá biểu']} module={MODULE}>
      <Link to={'/lop-hoc/' + k.code} className="back">
        <Icon name="back" size={16} />
        Lớp {k.code}
      </Link>
      <div className="h">
        <div>
          <h2>{step === 1 ? `Sinh thời khoá biểu cho lớp ${k.code}` : `Xem trước ${result?.sessions.length ?? 0} buổi của lớp ${k.code}`}</h2>
          <p>{step === 1 ? `${k.program} · ${k.subjects.length} môn · ${totalSessions(k)} buổi` : describe(pattern, result!)}</p>
        </div>
      </div>
      <ol className="gt-steps">
        {['Khai báo mẫu', 'Xem trước và kiểm tra trùng', 'Đã lưu'].map((t, i) => (
          <li key={t} className={step === i + 1 ? 'on' : step > i + 1 ? 'done' : undefined}>
            <span>{step > i + 1 ? <Icon name="check" size={14} /> : i + 1}</span>
            {t}
          </li>
        ))}
      </ol>
      {error && (
        <Box icon="alert" tone="bad" title="Chưa sinh được lịch">
          {error}
        </Box>
      )}
      {step === 1 && <PatternForm k={k} p={pattern} onChange={setPattern} onPreview={busy ? () => undefined : preview} />}
      {step === 2 && result && <Preview k={k} p={pattern} result={result} onBack={() => setStep(1)} onSaved={() => setStep(3)} />}
      {step === 3 && result && (
        <Box icon="check" tone="ok" title={`Đã lưu ${result.sessions.length} buổi cho lớp ${k.code}`}>
          Buổi đầu {longDate(result.sessions[0].date)}, buổi cuối {longDate(result.sessions.at(-1)!.date)}.{' '}
          <Link to={'/lop-hoc/' + k.code}>Về trang lớp</Link>
        </Box>
      )}
    </AppLayout>
  );
}

const describe = (p: Pattern, r: { skipped: Skipped[]; sessions: ClassSession[] }) =>
  `${DAY_ORDER.filter((x) => p.days.includes(x))
    .map((x) => WEEKDAYS[x])
    .join(', ')} · ${p.start} – ${p.end} · từ ${viDate(r.sessions[0]?.date ?? p.from)} đến ${viDate(r.sessions.at(-1)?.date ?? p.from)}${r.skipped.length ? ` · bỏ qua ${r.skipped.length} ngày nghỉ lễ` : ''}`;

type FormProps = { k: Klass; p: Pattern; onChange: (p: Pattern) => void; onPreview: () => void };

function PatternForm({ k, p, onChange, onPreview }: FormProps) {
  const [custom, setCustom] = useState(false);
  const [holiday, setHoliday] = useState({ from: '', name: '' });
  const [, setTick] = useState(0);
  const [error, setError] = useState('');
  const set = (patch: Partial<Pattern>) => onChange({ ...p, ...patch });
  const shift = (Object.keys(SHIFTS) as Shift[]).find((s) => SHIFTS[s][0] === p.start && SHIFTS[s][1] === p.end);
  const est = p.days.length && p.count > 0 ? generate(k, p) : null;
  const first = est?.sessions[0]?.date;
  const last = est?.sessions.at(-1)?.date;
  const plain = p.days.length && first ? Math.ceil(p.count / p.days.length) : 0;
  const weeks = first && last ? Math.round((Date.parse(last) - Date.parse(first)) / 604800000) + 1 : 0;
  const missing = k.subjects.filter((s) => !s.teacher).length;

  function go() {
    if (!p.days.length) return setError('Chọn ít nhất một thứ trong tuần');
    if (!p.from) return setError('Chọn ngày bắt đầu');
    if (p.start >= p.end) return setError('Giờ kết thúc phải sau giờ bắt đầu');
    if (!Number.isInteger(p.count) || p.count < 1 || p.count > 300) return setError('Số buổi từ 1 đến 300');
    setError('');
    onPreview();
  }

  return (
    <div className="gt-grid">
      <section className="panel sp-card">
        <h3>Mẫu lặp</h3>
        <div className="field">
          <span className="lbl">Các thứ trong tuần</span>
          <div className="gt-days">
            {DAY_ORDER.map((x) => (
              <button key={x} type="button" aria-pressed={p.days.includes(x)} className={p.days.includes(x) ? 'on' : undefined} onClick={() => set({ days: p.days.includes(x) ? p.days.filter((y) => y !== x) : [...p.days, x] })}>
                {WEEKDAYS[x]}
              </button>
            ))}
          </div>
        </div>
        <div className="field">
          <span className="lbl">Ca học</span>
          <div className="gt-shifts">
            {(Object.keys(SHIFTS) as Shift[]).map((s) => (
              <button
                key={s}
                type="button"
                aria-pressed={!custom && shift === s}
                className={!custom && shift === s ? 'on' : undefined}
                onClick={() => {
                  setCustom(false);
                  set({ start: SHIFTS[s][0], end: SHIFTS[s][1] });
                }}
              >
                {s} {SHIFTS[s][0]} – {SHIFTS[s][1]}
              </button>
            ))}
            <button type="button" className={'link' + (custom ? ' on' : '')} onClick={() => setCustom(true)}>
              hoặc tự nhập
            </button>
          </div>
          {custom && (
            <div className="gt-time">
              <input type="time" aria-label="Giờ bắt đầu" value={p.start} onChange={(e) => set({ start: e.target.value })} />
              <span>–</span>
              <input type="time" aria-label="Giờ kết thúc" value={p.end} onChange={(e) => set({ end: e.target.value })} />
            </div>
          )}
        </div>
        <div className="gt-row">
          <div className="field">
            <label htmlFor="gt-from">Ngày bắt đầu</label>
            <div className="inp">
              <input id="gt-from" type="date" value={p.from} onChange={(e) => set({ from: e.target.value })} />
            </div>
            {p.from && first && (
              <p className="hint">
                {WEEKDAY_NAMES[dow(p.from)]}.{first !== p.from && ` Buổi đầu rơi vào ${WEEKDAY_NAMES[dow(first)].toLowerCase()} ${dm(first)}.`}
              </p>
            )}
          </div>
          <div className="field">
            <label htmlFor="gt-count">Số buổi</label>
            <div className="inp">
              <input id="gt-count" type="number" min={1} value={p.count || ''} onChange={(e) => set({ count: Number(e.target.value) })} />
            </div>
            <p className="hint">{p.count === totalSessions(k) ? `Bằng tổng số buổi của ${k.subjects.length} môn` : `Tổng số buổi của các môn là ${totalSessions(k)}`}</p>
          </div>
          <div className="field">
            <label htmlFor="gt-room">Phòng học mặc định</label>
            <div className="inp">
              <select id="gt-room" value={p.room ?? ''} onChange={(e) => set({ room: e.target.value || undefined })}>
                <option value="">Chưa chọn</option>
                {ROOMS.map((r) => (
                  <option key={r.name} value={r.name}>
                    {r.name} · {capacityText(r)}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>
        <span className="lbl">Gắn buổi với bài học</span>
        <div className="box gt-map">
          <Icon name="book" size={18} />
          <div>
            <b>Theo thứ tự môn trong lớp</b>
            Buổi 1 đến {k.subjects[0]?.sessions} là {k.subjects[0]?.code}
            {k.subjects[1] && `, buổi ${k.subjects[0].sessions + 1} đến ${k.subjects[0].sessions + k.subjects[1].sessions} là ${k.subjects[1].code}`}, tiếp tục đến hết {k.subjects.at(-1)?.code}. Mỗi buổi lấy đúng bài học theo thứ tự trong môn.
          </div>
        </div>
        <span className="lbl">Giảng viên</span>
        <p className={missing ? 'gt-warn' : 'hint'}>
          Lấy theo phân công của từng môn.{missing ? ` Còn ${missing} môn chưa có giảng viên, buổi của các môn này để trống.` : ''}
        </p>
        {error && <p className="err">{error}</p>}
        <div className="acts">
          <Link to={'/lop-hoc/' + k.code} className="btn" style={{ width: 100 }}>
            Huỷ
          </Link>
          <button type="button" className="btn primary" style={{ width: 140 }} onClick={go}>
            Xem trước
          </button>
        </div>
      </section>

      <aside className="gt-side">
        <section className="panel sp-card">
          <h3>Ngày nghỉ lễ đã khai báo</h3>
          <p className="hint">Hệ thống tự bỏ qua các ngày này khi sinh lịch.</p>
          <ul className="gt-hol">
            {HOLIDAYS.map((h) => (
              <li key={h.from}>
                <b>{h.to ? `${dm(h.from)} – ${viDate(h.to)}` : viDate(h.from)}</b>
                <span>{h.name}</span>
              </li>
            ))}
          </ul>
          <div className="gt-add">
            <input type="date" aria-label="Ngày nghỉ" value={holiday.from} onChange={(e) => setHoliday({ ...holiday, from: e.target.value })} />
            <input aria-label="Tên ngày nghỉ" placeholder="Tên ngày nghỉ" value={holiday.name} onChange={(e) => setHoliday({ ...holiday, name: e.target.value })} />
            <button
              type="button"
              className="link"
              disabled={!holiday.from || !holiday.name.trim()}
              onClick={() => {
                if (apiEnabled) {
                  addHoliday({ from: holiday.from, name: holiday.name.trim() })
                    .then(loadHolidays)
                    .then(() => (setHoliday({ from: '', name: '' }), setTick((t) => t + 1)), (e: Error) => setError(e.message));
                  return;
                }
                HOLIDAYS.push({ from: holiday.from, name: holiday.name.trim() });
                HOLIDAYS.sort((a, b) => a.from.localeCompare(b.from));
                saveAll();
                setHoliday({ from: '', name: '' });
                setTick((t) => t + 1);
              }}
            >
              <Icon name="plus" size={14} /> Thêm ngày nghỉ
            </button>
          </div>
        </section>
        <section className="panel sp-card">
          <h3>Dự kiến</h3>
          {first && last ? (
            <>
              <dl className="sp-facts">
                <div>
                  <dt>Buổi đầu</dt>
                  <dd>{longDate(first)}</dd>
                </div>
                <div>
                  <dt>Buổi cuối</dt>
                  <dd>{longDate(last)}</dd>
                </div>
                <div>
                  <dt>Số buổi mỗi tuần</dt>
                  <dd>{p.days.length} buổi</dd>
                </div>
                <div>
                  <dt>Ngày bị bỏ qua</dt>
                  <dd>{est!.skipped.length} ngày nghỉ lễ</dd>
                </div>
              </dl>
              {weeks > plain && <p className="hint" style={{ marginTop: 12 }}>Ngày kết thúc lùi lại {weeks - plain} tuần do nghỉ lễ.</p>}
            </>
          ) : (
            <p className="hint">Chọn thứ trong tuần và ngày bắt đầu để xem dự kiến.</p>
          )}
        </section>
      </aside>
    </div>
  );
}

type PreviewProps = { k: Klass; p: Pattern; result: { sessions: ClassSession[]; skipped: Skipped[] }; onBack: () => void; onSaved: () => void };

type Row = { s?: ClassSession; skip?: Skipped };

function Preview({ k, p, result, onBack, onSaved }: PreviewProps) {
  const toast = useToast();
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [tab, setTab] = useState<'all' | 'clash' | 'skip'>('all');
  const [override, setOverride] = useState(false);
  // Chỉ so với buổi của lớp khác còn giữ lại sau khi lưu (các buổi cũ từ hôm nay của lớp này sẽ bị thay).
  const found = new Map(result.sessions.map((s) => [s.id, clashes(s)]));
  const bad = result.sessions.filter((s) => found.get(s.id)!.length).length;
  const { page, setPage, size, setSize } = usePaging(20);
  const day = (r: Row) => r.s?.date ?? r.skip!.date;
  const all = [...result.sessions.map((s): Row => ({ s })), ...result.skipped.map((skip): Row => ({ skip }))].sort((a, b) => day(a).localeCompare(day(b)));
  const rows = tab === 'skip' ? all.filter((r) => r.skip) : tab === 'clash' ? all.filter((r) => r.s && found.get(r.s.id)!.length) : all;
  const view = paginate(rows, page, size);

  function save() {
    if (apiEnabled) {
      // Máy chủ thay lịch cũ của lớp; lớp đã có buổi học xong, huỷ hoặc dời thì từ chối và báo lý do.
      setSaving(true);
      saveSchedule(k.code, p)
        .then(() => loadClassSessions(k.code, TODAY))
        .then(() => (toast(`Đã lưu thời khoá biểu lớp ${k.code}`, `${result.sessions.length} buổi`), onSaved()))
        .catch((e: Error) => (setSaving(false), setSaveError(e.message)));
      return;
    }
    // Giữ các buổi đã qua của lớp, thay toàn bộ buổi từ hôm nay trở đi bằng lịch mới.
    const keep = SESSIONS.filter((s) => s.cls !== k.code || s.date < TODAY);
    const kept = keep.filter((s) => s.cls === k.code).length;
    SESSIONS.splice(0, SESSIONS.length, ...keep, ...result.sessions.filter((s) => s.date >= TODAY).map((s, i) => ({ ...s, no: kept + i + 1, id: `${k.code}-${kept + i + 1}` })));
    const days = [...new Set(result.sessions.map((s) => dow(s.date)))].sort((a, b) => ((a + 6) % 7) - ((b + 6) % 7));
    if (result.sessions[0]) k.slot = `${shiftOf(result.sessions[0].start)} ${days.map((x) => (x ? String(x + 1) : 'CN')).join('-')}`;
    saveAll();
    toast(`Đã lưu thời khoá biểu lớp ${k.code}`, `${result.sessions.length} buổi`);
    onSaved();
  }

  return (
    <>
      {bad > 0 && (
        <div className="box bad gt-clash">
          <Icon name="alert" size={20} />
          <div>
            <b>{bad} buổi bị trùng lịch, chưa lưu được</b>
            Sửa từng buổi bị trùng sau khi lưu, đổi mẫu lặp, hoặc xác nhận ghi đè nếu bạn chủ động để trùng.
            <label className="check-list">
              <input type="checkbox" checked={override} onChange={(e) => setOverride(e.target.checked)} />
              Tôi xác nhận ghi đè: vẫn lưu cả {bad} buổi đang trùng
            </label>
          </div>
        </div>
      )}
      <div className="seg gt-tabs" role="tablist">
        <button type="button" role="tab" aria-selected={tab === 'all'} className={tab === 'all' ? 'on' : undefined} onClick={() => (setTab('all'), setPage(1))}>
          Tất cả ({result.sessions.length})
        </button>
        <button type="button" role="tab" aria-selected={tab === 'clash'} className={tab === 'clash' ? 'on' : undefined} onClick={() => (setTab('clash'), setPage(1))}>
          Chỉ buổi trùng ({bad})
        </button>
        <button type="button" role="tab" aria-selected={tab === 'skip'} className={tab === 'skip' ? 'on' : undefined} onClick={() => (setTab('skip'), setPage(1))}>
          Ngày bị bỏ qua ({result.skipped.length})
        </button>
      </div>
      <div className="panel">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Buổi</th>
                <th>Ngày</th>
                <th>Giờ</th>
                <th>Môn · bài</th>
                <th>Chủ đề</th>
                <th>Giảng viên</th>
                <th>Phòng</th>
                <th>Kiểm tra</th>
              </tr>
            </thead>
            <tbody>
              {view.items.map((r) =>
                r.s ? (
                  <tr key={r.s.id}>
                    <td className="strong">{r.s.no}</td>
                    <td className="strong">{shortDate(r.s.date)}</td>
                    <td>
                      {r.s.start} – {r.s.end}
                    </td>
                    <td className="strong">
                      {r.s.subject} · Bài {r.s.lesson}
                    </td>
                    <td>{r.s.topic}</td>
                    <td className={r.s.teacher ? 'strong' : 'bad-t'}>{r.s.teacher ?? 'Chưa có'}</td>
                    <td>{r.s.room ?? '–'}</td>
                    <td>
                      {found.get(r.s.id)!.length ? (
                        <div className="gt-bad">
                          {found.get(r.s.id)!.map((c, i) => (
                            <div key={i}>
                              <Pill tone="off">{c.kind}</Pill>
                              <small>{clashText(c)}</small>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <Pill tone="ok">Hợp lệ</Pill>
                      )}
                    </td>
                  </tr>
                ) : (
                  <tr key={r.skip!.date} className="gt-skip">
                    <td />
                    <td>{shortDate(r.skip!.date)}</td>
                    <td />
                    <td />
                    <td colSpan={3}>Bỏ qua: {r.skip!.reason}</td>
                    <td>
                      <Pill>Nghỉ lễ</Pill>
                    </td>
                  </tr>
                ),
              )}
            </tbody>
          </table>
        </div>
        <Pager page={view.page} pages={view.pages} onPage={setPage} size={size} onSize={setSize}>
          Đang hiện {view.from}–{view.to} trong {rows.length} dòng
        </Pager>
      </div>
      <div className="acts">
        <button type="button" className="btn" onClick={onBack}>
          Sửa mẫu lặp
        </button>
        {saveError && <p className="err">{saveError}</p>}
        <button type="button" className="btn primary" disabled={(bad > 0 && !override) || saving} onClick={save}>
          Lưu thời khoá biểu
        </button>
      </div>
    </>
  );
}

