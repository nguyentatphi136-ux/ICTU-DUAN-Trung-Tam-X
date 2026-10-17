import { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { LoadState, Search } from '../../components/ui';
import { ApiError, apiEnabled, useLoad } from '../../data/api';
import { addToCurriculum, loadTraining, removeFromCurriculum, reorderCurriculum, setPrereq } from '../../data/s2api';
import { saveAll } from '../../data/store';
import { CURRICULUM, PROGRAMS, SUBJECTS, type CurriculumItem } from '../../data/training';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import { MODULE, useCanEditTraining } from '../../components/TrainingTabs';
import './curriculum.css';

const subject = (code: string) => SUBJECTS.find((s) => s.code === code);
const hhmm = () => new Date().toTimeString().slice(0, 5);

type SaveState = { kind: 'saving' | 'saved' | 'error'; text: string } | null;

// S2-06. Lộ trình môn của một chương trình: kéo thả hoặc bấm mũi tên lên/xuống để đổi thứ tự (dùng được cả
// bàn phím và màn cảm ứng), thêm bằng nút +, gỡ bằng ×. Môn tiên quyết chỉ chọn được từ các môn đứng trước.
// Có backend: mỗi thay đổi (thứ tự, thêm, gỡ, môn tiên quyết) gửi lên /api/training-programs/:code/subjects,
// chỉ báo "Đã lưu" khi máy chủ nhận; máy chủ từ chối thì trả về như cũ. Chưa có backend: lưu vào dữ liệu mẫu.
export function CurriculumPage() {
  const code = useParams().code ?? '';
  const program = PROGRAMS.find((p) => p.code === code);
  const canEdit = useCanEditTraining();
  const toast = useToast();
  const [items, setItems] = useState<CurriculumItem[]>(() => CURRICULUM[code] ?? []);
  const [save, setSave] = useState<SaveState>(null);
  const [dragging, setDragging] = useState<number | null>(null);
  const [over, setOver] = useState<number | null>(null);
  const [q, setQ] = useState('');
  const load = useLoad(async () => {
    await loadTraining();
    setItems(CURRICULUM[code] ?? []);
  }, [code]);

  if (apiEnabled && (load.loading || load.error))
    return (
      <AppLayout crumb={[MODULE, 'Chương trình']} module={MODULE}>
        <LoadState {...load} />
      </AppLayout>
    );
  if (!program) return <ErrorPage code="404" />;

  /** send: lệnh gửi lên máy chủ khi có backend, trả về lộ trình mới do máy chủ tính. */
  const commit = (list: CurriculumItem[], send: () => Promise<CurriculumItem[]>) => {
    const prev = items;
    // Môn tiên quyết phải đứng trước: bỏ những môn không còn đứng trước sau khi đổi thứ tự hoặc gỡ.
    const fixed = list.map((it, i) => ({ ...it, prereq: it.prereq.filter((p) => list.slice(0, i).some((x) => x.code === p)) }));
    CURRICULUM[code] = fixed;
    setItems(fixed);
    saveAll();
    if (!apiEnabled) return setSave({ kind: 'saved', text: 'Đã lưu lúc ' + hhmm() });
    setSave({ kind: 'saving', text: 'Đang lưu…' });
    send()
      .then((server) => {
        CURRICULUM[code] = server;
        setItems(server);
        setSave({ kind: 'saved', text: 'Đã lưu lúc ' + hhmm() });
      })
      .catch((e: ApiError) => {
        CURRICULUM[code] = prev;
        setItems(prev);
        setSave({ kind: 'error', text: 'Chưa lưu được: ' + e.message });
      });
  };
  const move = (from: number, to: number) => {
    if (from === to || to < 0 || to >= items.length) return;
    const list = [...items];
    list.splice(to, 0, list.splice(from, 1)[0]);
    commit(list, () => reorderCurriculum(code, list.map((x) => x.code)));
    toast('Đã đổi thứ tự môn học', `${subject(list[to].code)?.name ?? list[to].code} chuyển sang vị trí thứ ${to + 1}`);
  };
  const endDrag = () => {
    setDragging(null);
    setOver(null);
  };

  const total = items.reduce((n, it) => n + (subject(it.code)?.sessions ?? 0), 0);
  const k = q.trim().toLowerCase();
  const available = SUBJECTS.filter((s) => !items.some((it) => it.code === s.code) && (!k || s.code.toLowerCase().includes(k) || s.name.toLowerCase().includes(k)));

  return (
    <AppLayout crumb={[MODULE, 'Chương trình', program.name]} module={MODULE}>
      <div className="h">
        <div>
          <h2>{program.name}</h2>
          <p>{canEdit ? 'Kéo thả hoặc bấm mũi tên để đổi thứ tự học. Thứ tự được lưu tự động.' : 'Lộ trình học của chương trình.'}</p>
        </div>
        {save && (
          <span className={'cu-saved ' + save.kind} role="status">
            <Icon name={save.kind === 'error' ? 'alert' : save.kind === 'saving' ? 'clock' : 'check'} size={16} />
            {save.text}
          </span>
        )}
      </div>
      <div className="cu">
        <section className="panel cu-list">
          <div className="cu-head">
            <b>Lộ trình học</b>
            <span>
              {items.length} môn · {total} buổi
            </span>
            <small>Môn tiên quyết</small>
          </div>
          {items.map((it, i) => {
            const s = subject(it.code);
            // Đường kẻ chỉ chỗ thả: kéo xuống thì môn rơi dưới hàng đích, kéo lên thì rơi trên.
            const drop = dragging !== null && over === i && dragging !== i ? (dragging < i ? ' drop-after' : ' drop-before') : '';
            return (
              <div
                key={it.code}
                className={'cu-row' + (dragging === i ? ' drag' : '') + drop}
                draggable={canEdit}
                onDragStart={(e) => {
                  setDragging(i);
                  e.dataTransfer.effectAllowed = 'move';
                }}
                onDragEnd={endDrag}
                onDragOver={(e) => {
                  e.preventDefault();
                  if (over !== i) setOver(i);
                }}
                onDrop={() => {
                  if (dragging !== null) move(dragging, i);
                  endDrag();
                }}
              >
                {canEdit && (
                  <span className="cu-grip" title="Kéo thả để đổi vị trí môn học">
                    <Icon name="grip" size={16} />
                  </span>
                )}
                <span className="cu-num">{i + 1}</span>
                <div className="cu-name">
                  <b>{s?.name ?? it.code}</b>
                  <span>
                    {it.code} · {s?.sessions ?? 0} buổi
                  </span>
                </div>
                <Prereq
                  value={it.prereq}
                  options={items.slice(0, i).map((x) => x.code)}
                  disabled={!canEdit}
                  onChange={(prereq) => commit(items.map((x, j) => (j === i ? { ...x, prereq } : x)), () => setPrereq(code, it.code, prereq))}
                />
                {canEdit && (
                  <span className="cu-moves">
                    <button type="button" className="icon-btn up" aria-label={'Đưa ' + (s?.name ?? it.code) + ' lên trên'} disabled={i === 0} onClick={() => move(i, i - 1)}>
                      <Icon name="down" />
                    </button>
                    <button type="button" className="icon-btn" aria-label={'Đưa ' + (s?.name ?? it.code) + ' xuống dưới'} disabled={i === items.length - 1} onClick={() => move(i, i + 1)}>
                      <Icon name="down" />
                    </button>
                    <button type="button" className="icon-btn" aria-label={'Gỡ ' + (s?.name ?? it.code)} onClick={() => commit(items.filter((_, j) => j !== i), () => removeFromCurriculum(code, it.code))}>
                      <Icon name="x" />
                    </button>
                  </span>
                )}
              </div>
            );
          })}
          {!items.length && <p className="hint" style={{ padding: 24 }}>Chưa có môn nào. Thêm môn ở cột bên phải.</p>}
        </section>

        {canEdit && (
          <section className="panel cu-add">
            <b>Thêm môn vào chương trình</b>
            <Search placeholder="Tìm môn học" value={q} onChange={setQ} />
            {available.map((s) => (
              <div className="cu-opt" key={s.code}>
                <div>
                  <b>{s.name}</b>
                  <span>
                    {s.code} · {s.sessions} buổi
                  </span>
                </div>
                <button type="button" className="btn cu-plus" aria-label={'Thêm ' + s.name} onClick={() => commit([...items, { code: s.code, prereq: [] }], () => addToCurriculum(code, s.code))}>
                  <Icon name="plus" />
                </button>
              </div>
            ))}
          </section>
        )}
      </div>
    </AppLayout>
  );
}

/** Ô chọn nhiều môn tiên quyết, chỉ gồm các môn đứng trước. */
function Prereq({ value, options, disabled, onChange }: { value: string[]; options: string[]; disabled: boolean; onChange: (v: string[]) => void }) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return;
    const f = (e: MouseEvent) => !ref.current?.contains(e.target as Node) && setOpen(false);
    document.addEventListener('mousedown', f);
    return () => document.removeEventListener('mousedown', f);
  }, [open]);
  return (
    <div className="cu-pre" ref={ref}>
      <button type="button" className={'filter' + (value.length ? '' : ' empty')} disabled={disabled || !options.length} onClick={() => setOpen((o) => !o)}>
        <span>{value.length ? value.join(', ') : 'Không có'}</span>
        <Icon name="down" />
      </button>
      {open && (
        <div className="dropdown">
          {options.map((o) => (
            <label key={o} className="cu-check">
              <input type="checkbox" checked={value.includes(o)} onChange={(e) => onChange(e.target.checked ? [...value, o] : value.filter((x) => x !== o))} />
              {o} · {subject(o)?.name}
            </label>
          ))}
        </div>
      )}
    </div>
  );
}
