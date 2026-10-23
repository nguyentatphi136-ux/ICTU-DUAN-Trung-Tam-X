import { useState } from 'react';
import { Link } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Box } from '../../components/ui';
import { ASSIGNMENTS, type Criterion } from '../../data/assignments';
import { RUBRIC_SCALE, RUBRICS } from '../../data/rubrics';
import { saveAll } from '../../data/store';
import { MODULE } from '../S5-05-bai-tap/AssignmentPages';
import './rubric.css';

const blank = (): Criterion => ({ name: '', weight: 0, hint: '' });

// S6-02. Tạo mẫu rubric: các tiêu chí kèm trọng số, tổng trọng số phải đúng 100%. Mẫu lưu lại để gắn vào nhiều bài tập.
export function RubricPage() {
  const toast = useToast();
  const [, setTick] = useState(0);
  const [name, setName] = useState('');
  const [rows, setRows] = useState<Criterion[]>([blank(), blank()]);
  const [error, setError] = useState('');
  const total = rows.reduce((n, c) => n + (Number(c.weight) || 0), 0);
  const set = (i: number, patch: Partial<Criterion>) => setRows(rows.map((c, j) => (j === i ? { ...c, ...patch } : c)));
  const usedBy = (n: string) => ASSIGNMENTS.filter((a) => a.rubric && RUBRICS.find((r) => r.name === n)?.criteria.every((c, i) => a.rubric![i]?.name === c.name)).length;

  function save() {
    const list = rows.filter((c) => c.name.trim() || c.weight);
    if (!name.trim()) return setError('Nhập tên mẫu');
    if (RUBRICS.some((r) => r.name.toLowerCase() === name.trim().toLowerCase())) return setError('Đã có mẫu cùng tên');
    if (list.length < 2) return setError('Rubric cần ít nhất hai tiêu chí');
    if (list.some((c) => !c.name.trim() || !(c.weight > 0))) return setError('Mỗi tiêu chí cần tên và trọng số lớn hơn 0');
    if (total !== 100) return setError(`Tổng trọng số đang là ${total}%, cần đúng 100% mới lưu được`);
    RUBRICS.unshift({ id: Math.max(0, ...RUBRICS.map((r) => r.id)) + 1, name: name.trim(), criteria: list.map((c) => ({ name: c.name.trim(), weight: Number(c.weight), hint: c.hint?.trim() || undefined })) });
    saveAll();
    toast('Đã lưu mẫu rubric', name.trim());
    setName('');
    setRows([blank(), blank()]);
    setError('');
    setTick((t) => t + 1);
  }

  return (
    <AppLayout crumb={[MODULE, 'Mẫu rubric']} module={MODULE}>
      <Link to="/bai-tap" className="back">
        <Icon name="back" size={16} />
        Bài tập
      </Link>
      <div className="h">
        <div>
          <h2>Tạo mẫu rubric</h2>
          <p>Mẫu lưu lại để dùng cho nhiều bài tập</p>
        </div>
      </div>
      <div className="rb-grid">
        <section className="panel sp-card">
          <div className="rb-row2">
            <div className="field">
              <label htmlFor="rb-name">Tên mẫu</label>
              <div className="inp">
                <input id="rb-name" value={name} placeholder="Ví dụ: Bài tập lập trình web" onChange={(e) => setName(e.target.value)} />
              </div>
            </div>
            <div className="field">
              <label htmlFor="rb-scale">Thang điểm mỗi tiêu chí</label>
              <div className="inp">
                <input id="rb-scale" value={`0 đến ${RUBRIC_SCALE}`} disabled />
              </div>
            </div>
          </div>
          <div className="rb-head">
            <span className="lbl">Tiêu chí</span>
            <span className="lbl">Trọng số</span>
          </div>
          <ol className="rb-list">
            {rows.map((c, i) => (
              <li key={i}>
                <span className="rb-no">{i + 1}</span>
                <div className="rb-fields">
                  <input aria-label={`Tên tiêu chí ${i + 1}`} placeholder="Tên tiêu chí" value={c.name} onChange={(e) => set(i, { name: e.target.value })} />
                  <input aria-label={`Mô tả tiêu chí ${i + 1}`} className="rb-hint" placeholder="Mô tả cách chấm tiêu chí này" value={c.hint ?? ''} onChange={(e) => set(i, { hint: e.target.value })} />
                </div>
                <span className="rb-w">
                  <input aria-label={`Trọng số tiêu chí ${i + 1}`} type="number" min={0} max={100} value={c.weight || ''} placeholder="0" onChange={(e) => set(i, { weight: Number(e.target.value) })} />%
                </span>
                <button type="button" className="icon-btn" aria-label={`Bỏ tiêu chí ${i + 1}`} disabled={rows.length <= 1} onClick={() => setRows(rows.filter((_, j) => j !== i))}>
                  <Icon name="x" size={14} />
                </button>
              </li>
            ))}
          </ol>
          <button type="button" className="link" onClick={() => setRows([...rows, blank()])}>
            + Thêm tiêu chí
          </button>
          <div className={'rb-total' + (total === 100 ? ' ok' : ' bad')}>
            <span>{total === 100 ? 'Tổng trọng số đủ 100%' : `Tổng trọng số đang là ${total}%, cần đúng 100% mới lưu được`}</span>
            <b>{total} / 100%</b>
          </div>
          {error && <p className="err">{error}</p>}
          <div className="acts">
            <button type="button" className="btn" style={{ width: 90 }} onClick={() => (setRows([blank(), blank()]), setName(''), setError(''))}>
              Huỷ
            </button>
            <button type="button" className="btn primary" style={{ width: 120 }} onClick={save}>
              Lưu mẫu
            </button>
          </div>
        </section>
        <aside>
          <section className="panel sp-card">
            <h3>Mẫu đã lưu</h3>
            <ul className="rb-saved">
              {RUBRICS.map((r) => {
                const n = usedBy(r.name);
                return (
                  <li key={r.id}>
                    <b>{r.name}</b>
                    <small>
                      {r.criteria.length} tiêu chí · {n ? `dùng cho ${n} bài tập` : 'chưa dùng'}
                    </small>
                    <button
                      type="button"
                      className="link"
                      onClick={() => {
                        setName(r.name + ' (bản sao)');
                        setRows(r.criteria.map((c) => ({ ...c })));
                        setError('');
                      }}
                    >
                      Dùng làm bản sao
                    </button>
                  </li>
                );
              })}
            </ul>
          </section>
          <Box icon="edit" tone="info" title="Gắn rubric khi tạo bài tập">
            Ở form tạo bài tập, chọn một mẫu trong ô Rubric. Học viên thấy điểm từng tiêu chí sau khi công bố.
          </Box>
        </aside>
      </div>
    </AppLayout>
  );
}
