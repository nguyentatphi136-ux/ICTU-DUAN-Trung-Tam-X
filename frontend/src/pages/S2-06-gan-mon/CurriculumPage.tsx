import { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Search } from '../../components/ui';
import { CURRICULUM, PROGRAMS, SUBJECTS, type CurriculumItem } from '../../data/training';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import { MODULE, useCanEditTraining } from '../../components/TrainingTabs';
import './curriculum.css';

const subject = (code: string) => SUBJECTS.find((s) => s.code === code);
const hhmm = () => new Date().toTimeString().slice(0, 5);

// S2-06. Lộ trình môn của một chương trình: kéo thả đổi thứ tự (tự lưu), thêm bằng nút +, gỡ bằng ×.
// Môn tiên quyết chỉ chọn được từ các môn đứng trước. Khi tích hợp: PUT /training/programs/:code/subjects.
export function CurriculumPage() {
  const code = useParams().code ?? '';
  const program = PROGRAMS.find((p) => p.code === code);
  const canEdit = useCanEditTraining();
  const [items, setItems] = useState<CurriculumItem[]>(() => CURRICULUM[code] ?? []);
  const [savedAt, setSavedAt] = useState('');
  const [dragging, setDragging] = useState<number | null>(null);
  const [q, setQ] = useState('');

  if (!program) return <ErrorPage code="404" />;

  const commit = (list: CurriculumItem[]) => {
    // Môn tiên quyết phải đứng trước: bỏ những môn không còn đứng trước sau khi đổi thứ tự hoặc gỡ.
    const fixed = list.map((it, i) => ({ ...it, prereq: it.prereq.filter((p) => list.slice(0, i).some((x) => x.code === p)) }));
    CURRICULUM[code] = fixed;
    setItems(fixed);
    setSavedAt(hhmm());
  };
  const move = (from: number, to: number) => {
    if (from === to) return;
    const list = [...items];
    list.splice(to, 0, list.splice(from, 1)[0]);
    commit(list);
  };

  const total = items.reduce((n, it) => n + (subject(it.code)?.sessions ?? 0), 0);
  const k = q.trim().toLowerCase();
  const available = SUBJECTS.filter((s) => !items.some((it) => it.code === s.code) && (!k || s.code.toLowerCase().includes(k) || s.name.toLowerCase().includes(k)));

  return (
    <AppLayout crumb={[MODULE, 'Chương trình', program.name]} module={MODULE}>
      <div className="h">
        <div>
          <h2>{program.name}</h2>
          <p>{canEdit ? 'Kéo thả để đổi thứ tự học. Thứ tự được lưu tự động.' : 'Lộ trình học của chương trình.'}</p>
        </div>
        {savedAt && (
          <span className="cu-saved">
            <Icon name="check" size={16} />
            Đã lưu thứ tự lúc {savedAt}
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
            return (
              <div
                key={it.code}
                className={'cu-row' + (dragging === i ? ' drag' : '')}
                draggable={canEdit}
                onDragStart={(e) => {
                  setDragging(i);
                  e.dataTransfer.effectAllowed = 'move';
                }}
                onDragEnd={() => setDragging(null)}
                onDragOver={(e) => e.preventDefault()}
                onDrop={() => {
                  if (dragging !== null) move(dragging, i);
                  setDragging(null);
                }}
              >
                {canEdit && <Icon name="grip" />}
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
                  onChange={(prereq) => commit(items.map((x, j) => (j === i ? { ...x, prereq } : x)))}
                />
                {canEdit && (
                  <button type="button" className="icon-btn" aria-label={'Gỡ ' + (s?.name ?? it.code)} onClick={() => commit(items.filter((_, j) => j !== i))}>
                    <Icon name="x" />
                  </button>
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
                <button type="button" className="btn cu-plus" aria-label={'Thêm ' + s.name} onClick={() => commit([...items, { code: s.code, prereq: [] }])}>
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
