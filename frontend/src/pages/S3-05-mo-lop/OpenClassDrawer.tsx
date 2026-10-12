import { useState } from 'react';
import { Drawer } from '../../components/ui';
import { CLASSES, PROGRAM_SUBJECTS, STUDY_MODES, totalSessions, type Klass, type StudyMode } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { PROGRAMS } from '../../data/training';
import './open.css';

type Props = { onClose: () => void; onSave: (k: Klass) => void };

// S3-05. Mở lớp mới từ một chương trình: mã lớp duy nhất, ngày khai giảng dự kiến, sĩ số tối đa, hình thức học.
// Danh sách môn được sao chép từ chương trình lúc mở lớp; lớp mới luôn bắt đầu ở Chuẩn bị.
export function OpenClassDrawer({ onClose, onSave }: Props) {
  const programs = PROGRAMS.filter((p) => p.active && PROGRAM_SUBJECTS[p.name]);
  const [code, setCode] = useState('');
  const [program, setProgram] = useState(programs[0]?.name ?? '');
  const [start, setStart] = useState('');
  const [max, setMax] = useState('30');
  const [mode, setMode] = useState<StudyMode>('Trực tiếp');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const subjects = PROGRAM_SUBJECTS[program]?.() ?? [];
  const sessions = subjects.reduce((n, s) => n + s.sessions, 0);

  function save() {
    const c = code.trim().toUpperCase();
    const e: Record<string, string> = {};
    const taken = CLASSES.find((k) => k.code === c);
    if (!c) e.code = 'Nhập mã lớp, ví dụ WEB-K17';
    else if (!/^[A-Z0-9]+(-[A-Z0-9]+)*$/.test(c)) e.code = 'Mã lớp chỉ gồm chữ không dấu, số và dấu gạch ngang';
    else if (taken) e.code = `Mã ${c} đã dùng cho một lớp ${taken.status.toLowerCase()}`;
    if (!start) e.start = 'Chọn ngày khai giảng dự kiến';
    else if (start < TODAY) e.start = 'Ngày khai giảng phải từ hôm nay trở đi';
    const n = Number(max);
    if (!Number.isInteger(n) || n < 1 || n > 200) e.max = 'Sĩ số tối đa từ 1 đến 200';
    setErrors(e);
    if (Object.keys(e).length) return;
    onSave({ code: c, program, start, max: n, mode, status: 'Chuẩn bị', done: 0, slot: 'Chưa xếp lịch', subjects });
  }

  const err = (k: string) => errors[k] && <p className="err">{errors[k]}</p>;

  return (
    <Drawer
      title="Mở lớp mới"
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
            Huỷ
          </button>
          <button type="button" className="btn primary" style={{ width: 120 }} onClick={save}>
            Mở lớp
          </button>
        </>
      }
    >
      <div className="grid2">
        <div className="field">
          <label htmlFor="oc-code">Mã lớp</label>
          <div className={'inp' + (errors.code ? ' bad' : '')}>
            <input id="oc-code" value={code} placeholder="WEB-K17" onChange={(e) => setCode(e.target.value)} />
          </div>
          {err('code')}
        </div>
        <div className="field">
          <label htmlFor="oc-program">Chương trình đào tạo</label>
          <div className="inp">
            <select id="oc-program" value={program} onChange={(e) => setProgram(e.target.value)}>
              {programs.map((p) => (
                <option key={p.code}>{p.name}</option>
              ))}
            </select>
          </div>
        </div>
        <div className="field">
          <label htmlFor="oc-start">Ngày khai giảng dự kiến</label>
          <div className={'inp' + (errors.start ? ' bad' : '')}>
            <input id="oc-start" type="date" min={TODAY} value={start} onChange={(e) => setStart(e.target.value)} />
          </div>
          {err('start')}
        </div>
        <div className="field">
          <label htmlFor="oc-max">Sĩ số tối đa</label>
          <div className={'inp' + (errors.max ? ' bad' : '')}>
            <input id="oc-max" type="number" min={1} max={200} value={max} onChange={(e) => setMax(e.target.value)} />
          </div>
          {err('max')}
        </div>
      </div>
      <div className="field">
        <span className="lbl">Hình thức học</span>
        <div className="seg oc-mode" role="radiogroup" aria-label="Hình thức học">
          {STUDY_MODES.map((m) => (
            <button key={m} type="button" role="radio" aria-checked={mode === m} className={mode === m ? 'on' : undefined} onClick={() => setMode(m)}>
              {m}
            </button>
          ))}
        </div>
      </div>
      <div className="field">
        <span className="lbl">Vòng đời lớp</span>
        <ol className="oc-life">
          <li className="on">Chuẩn bị</li>
          <li>Đang học</li>
          <li>Kết thúc</li>
          <li className="or">hoặc</li>
          <li className="off">Huỷ</li>
        </ol>
        <p className="hint">Lớp mới luôn bắt đầu ở Chuẩn bị.</p>
      </div>
      <div className="oc-subj-h">
        <span className="lbl">Môn học sao chép từ chương trình</span>
        <span className="hint">
          {subjects.length} môn · {sessions} buổi
        </span>
      </div>
      <div className="panel">
        <table className="oc-subj">
          <thead>
            <tr>
              <th>#</th>
              <th>Mã</th>
              <th>Môn học</th>
              <th>Số buổi</th>
            </tr>
          </thead>
          <tbody>
            {subjects.map((s, i) => (
              <tr key={s.code}>
                <td>{i + 1}</td>
                <td className="strong">{s.code}</td>
                <td className="strong">{s.name}</td>
                <td>{s.sessions}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="hint" style={{ marginTop: 10 }}>
        Sửa chương trình sau này không làm đổi danh sách môn của lớp đã mở.
      </p>
    </Drawer>
  );
}

/** Bước kế tiếp trong vòng đời Chuẩn bị → Đang học → Kết thúc, kèm lý do nếu chưa chuyển được. */
export function nextStep(k: Klass): { label: string; to: Klass['status']; blocked?: string } | null {
  if (k.status === 'Chuẩn bị') {
    const missing = k.subjects.filter((s) => !s.teacher).length;
    return { label: 'Bắt đầu học', to: 'Đang học', blocked: missing ? `${missing} môn chưa có giảng viên chính` : undefined };
  }
  if (k.status === 'Đang học') return { label: 'Kết thúc lớp', to: 'Kết thúc', blocked: k.done < totalSessions(k) ? 'Còn buổi chưa học' : undefined };
  return null;
}
