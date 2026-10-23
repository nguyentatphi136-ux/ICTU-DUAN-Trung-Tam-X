import { useState, type ReactNode } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { MODULE, TrainingTabs, useCanEditTraining } from '../../components/TrainingTabs';
import { Box, Filter } from '../../components/ui';
import { audit } from '../../data/audit';
import { useAuth } from '../../data/auth';
import { CLASSES, findClass, isOpen } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { CLASS_GRADES, configOf, num, programConfig, PROGRAM_GRADES, SOURCES, totalWeight, validateConfig, type Comp, type GradeConfig, type Source } from '../../data/grades';
import { ROLES } from '../../data/permissions';
import { saveAll } from '../../data/store';
import { PROGRAMS } from '../../data/training';
import './gradecfg.css';

const clone = (c: GradeConfig): GradeConfig => JSON.parse(JSON.stringify(c));
const at = () => TODAY + 'T' + new Date().toTimeString().slice(0, 5);
const overrideNote = (c: GradeConfig, p: GradeConfig) => {
  const diff = c.comps.filter((x) => p.comps.find((y) => y.key === x.key)?.weight !== x.weight).map((x) => `${x.name} ${x.weight}%`);
  if (c.pass !== p.pass) diff.push(`Điểm đạt ${num(c.pass)}`);
  if (c.fail !== p.fail) diff.push(`Điểm liệt ${num(c.fail)}`);
  return diff.join(', ') || 'Giống chương trình';
};

/** Bảng thành phần và ngưỡng, dùng cho cả cấp chương trình và cấp lớp. base là cấu hình chương trình khi đang ghi đè ở cấp lớp. */
function ConfigEditor({ initial, base, canEdit, side, note, onSave }: { initial: GradeConfig; base?: GradeConfig; canEdit: boolean; side?: ReactNode; note?: string; onSave: (c: GradeConfig) => void }) {
  const [c, setC] = useState(() => clone(initial));
  const [error, setError] = useState('');
  const t = totalWeight(c);
  const setComp = (i: number, patch: Partial<Comp>) => setC({ ...c, comps: c.comps.map((x, j) => (j === i ? { ...x, ...patch } : x)) });
  const decimal = (s: string) => Number(s.replace(',', '.'));

  function save() {
    const e = validateConfig(c);
    if (e) return setError(e);
    setError('');
    onSave({ ...c, comps: c.comps.map((x) => ({ ...x, name: x.name.trim() })), failOn: c.failOn.filter((k) => c.comps.some((x) => x.key === k)) });
  }

  return (
    <div className="gc-grid">
      <section className="panel gc-main">
        <div className="gc-head">
          <h3>Thành phần điểm và trọng số</h3>
          {base && <span className="gc-badge">Ghi đè ở cấp lớp</span>}
        </div>
        {c.comps.map((x, i) => {
          const orig = base?.comps.find((y) => y.key === x.key);
          return (
            <div className="gc-row" key={x.key}>
              <div>
                {canEdit ? <input className="gc-name" aria-label="Tên thành phần" value={x.name} onChange={(e) => setComp(i, { name: e.target.value })} /> : <b>{x.name}</b>}
                <span className="hint">
                  Nguồn:{' '}
                  {canEdit ? (
                    <select aria-label={`Nguồn điểm ${x.name}`} value={x.source} onChange={(e) => setComp(i, { source: e.target.value as Source })}>
                      {(Object.keys(SOURCES) as Source[]).map((s) => (
                        <option key={s} value={s}>
                          {SOURCES[s]}
                        </option>
                      ))}
                    </select>
                  ) : (
                    SOURCES[x.source]
                  )}
                </span>
              </div>
              {base && (!orig || orig.weight !== x.weight) && <span className="gc-orig">{orig ? `chương trình: ${orig.weight}%` : 'không có ở chương trình'}</span>}
              <div className="inp gc-w">
                <input aria-label={`Trọng số ${x.name}`} inputMode="numeric" disabled={!canEdit} value={x.weight || ''} onChange={(e) => setComp(i, { weight: Number(e.target.value.replace(/\D/g, '')) || 0 })} />
                <span>%</span>
              </div>
              {canEdit && (
                <button type="button" className="icon-btn" aria-label={`Bỏ ${x.name}`} disabled={c.comps.length < 2} onClick={() => setC({ ...c, comps: c.comps.filter((_, j) => j !== i), failOn: c.failOn.filter((k) => k !== x.key) })}>
                  <Icon name="x" size={16} />
                </button>
              )}
            </div>
          );
        })}
        {canEdit && (
          <button type="button" className="link gc-add" onClick={() => setC({ ...c, comps: [...c.comps, { key: 'c' + Date.now().toString(36), name: '', weight: Math.max(0, 100 - t), source: 'Nhập tay' }] })}>
            + Thêm thành phần
          </button>
        )}
        <div className={'fe-sum ' + (t === 100 ? 'ok' : 'bad')}>
          <span>
            <Icon name={t === 100 ? 'check' : 'alert'} size={16} />
            {t === 100 ? 'Tổng trọng số bằng 100%' : `Tổng trọng số đang là ${t}%, cần đúng 100%`}
          </span>
          <b>{t} / 100%</b>
        </div>
        {note && <p className="hint gc-note">{note}</p>}
        {error && <p className="err gc-note">{error}</p>}
        {canEdit && (
          <div className="acts gc-acts">
            <button type="button" className="btn" style={{ width: 90 }} onClick={() => (setC(clone(initial)), setError(''))}>
              Huỷ
            </button>
            <button type="button" className="btn primary" onClick={save}>
              Lưu cấu hình
            </button>
          </div>
        )}
      </section>
      <aside className="gc-side">
        <section className="panel fe-pad">
          <h3>Ngưỡng của môn</h3>
          <div className="grid2">
            <div className="field">
              <label htmlFor="gc-pass">Điểm đạt</label>
              <div className="inp gc-w">
                <input id="gc-pass" inputMode="decimal" disabled={!canEdit} defaultValue={num(c.pass)} onBlur={(e) => setC({ ...c, pass: decimal(e.target.value) })} />
                <span>/ 10</span>
              </div>
            </div>
            <div className="field">
              <label htmlFor="gc-fail">Điểm liệt</label>
              <div className="inp gc-w">
                <input id="gc-fail" inputMode="decimal" disabled={!canEdit} defaultValue={num(c.fail)} onBlur={(e) => setC({ ...c, fail: decimal(e.target.value) })} />
              </div>
            </div>
          </div>
          <p className="hint">
            Đạt môn khi điểm tổng kết từ {num(c.pass)} trở lên và không có thành phần bắt buộc nào dưới điểm liệt.
          </p>
          <h4 className="gc-sub">Thành phần xét điểm liệt</h4>
          {c.comps.map((x) => (
            <label key={x.key} className="gc-check">
              <input type="checkbox" disabled={!canEdit} checked={c.failOn.includes(x.key)} onChange={(e) => setC({ ...c, failOn: e.target.checked ? [...c.failOn, x.key] : c.failOn.filter((k) => k !== x.key) })} />
              {x.name || 'Thành phần mới'}
            </label>
          ))}
        </section>
        {side}
      </aside>
    </div>
  );
}

// S8-01. Cách tính điểm tổng kết ở cấp chương trình: thành phần, trọng số (tổng 100%), điểm đạt, điểm liệt; danh sách lớp đang ghi đè.
export function ProgramGradePage() {
  const { user } = useAuth();
  const toast = useToast();
  const canEdit = useCanEditTraining();
  const programs = PROGRAMS.filter((p) => p.active && PROGRAM_GRADES[p.name]);
  const [name, setName] = useState(programs[0]?.name ?? '');
  const [ver, setVer] = useState(0);
  const classes = CLASSES.filter((k) => k.program === name && k.status !== 'Huỷ');
  const over = classes.filter((k) => CLASS_GRADES[k.code]);

  return (
    <AppLayout crumb={[MODULE, 'Cách tính điểm']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Cách tính điểm tổng kết</h2>
          <p>Chương trình {name} · áp dụng cho mọi lớp trừ lớp có ghi đè</p>
        </div>
      </div>
      <TrainingTabs />
      <div className="bar">
        <Filter label="Chương trình" value={name} options={programs.map((p) => p.name)} onChange={(v) => v && setName(v)} width={320} />
      </div>
      <ConfigEditor
        key={name + ver}
        initial={programConfig(name)}
        canEdit={canEdit}
        onSave={(c) => {
          PROGRAM_GRADES[name] = c;
          audit({ at: at(), by: user!.name, role: ROLES[user!.active], entity: 'Điểm', action: 'Sửa cách tính điểm', target: `Chương trình ${name}`, changes: [{ field: 'Thành phần', before: '', after: c.comps.map((x) => `${x.name} ${x.weight}%`).join(', ') }] });
          saveAll();
          setVer(ver + 1);
          toast('Đã lưu cách tính điểm', `${name} · ${classes.length - over.length} lớp dùng cấu hình này`);
        }}
        side={
          <section className="panel fe-pad">
            <h3>Lớp đang ghi đè</h3>
            {over.map((k) => (
              <div key={k.code} className="gc-cls">
                <Link to={`/lop-hoc/${k.code}/cach-tinh-diem`} className="link">
                  {k.code}
                </Link>
                <span className="hint">{overrideNote(CLASS_GRADES[k.code], programConfig(name))}</span>
              </div>
            ))}
            {!over.length && <p className="hint">Chưa có lớp nào ghi đè.</p>}
            <p className="hint" style={{ marginTop: 12 }}>
              Các lớp khác dùng cấu hình này:{' '}
              {classes
                .filter((k) => !CLASS_GRADES[k.code])
                .map((k, i) => (
                  <span key={k.code}>
                    {i ? ', ' : ''}
                    <Link to={`/lop-hoc/${k.code}/cach-tinh-diem`} className="link gc-inline">
                      {k.code}
                    </Link>
                  </span>
                ))}
              .
            </p>
          </section>
        }
      />
    </AppLayout>
  );
}

// S8-01. Ghi đè cách tính điểm ở cấp lớp. Dòng khác chương trình ghi giá trị gốc bên cạnh. Môn đã chốt điểm không bị tính lại.
export function ClassGradePage() {
  const { cls = '' } = useParams();
  const { user } = useAuth();
  const toast = useToast();
  const nav = useNavigate();
  const canEdit = useCanEditTraining() && !!findClass(cls) && isOpen(findClass(cls)!);
  const [ver, setVer] = useState(0);
  const k = findClass(cls);
  if (!k) return <AppLayout crumb={['Lớp học & thời khoá biểu']} module="Lớp học & thời khoá biểu"><p className="empty">Không tìm thấy lớp {cls}.</p></AppLayout>;
  const program = programConfig(k.program);
  const own = CLASS_GRADES[cls];
  const log = (action: string, after: string) => audit({ at: at(), by: user!.name, role: ROLES[user!.active], entity: 'Điểm', action, target: `Lớp ${cls}`, changes: [{ field: 'Cách tính điểm', before: own ? 'Ghi đè' : 'Theo chương trình', after }] });

  return (
    <AppLayout crumb={['Lớp học & thời khoá biểu', k.code, 'Cách tính điểm']} module="Lớp học & thời khoá biểu">
      <Link to={`/lop-hoc/${k.code}`} className="back">
        <Icon name="back" size={16} />
        Lớp {k.code}
      </Link>
      <div className="h">
        <div>
          <h2>Cách tính điểm của lớp {k.code}</h2>
          <p>{own ? `Đang ghi đè cấu hình của chương trình ${k.program}` : `Đang dùng cấu hình của chương trình ${k.program}. Lưu để tạo cấu hình riêng cho lớp.`}</p>
        </div>
        <Link to="/dao-tao/cach-tinh-diem" className="btn">
          Về cấu hình chương trình
        </Link>
      </div>
      <ConfigEditor
        key={ver}
        initial={configOf(cls)}
        base={program}
        canEdit={canEdit}
        note={`Thay đổi chỉ áp dụng cho lớp ${k.code}. Môn đã chốt điểm không bị tính lại.`}
        onSave={(c) => {
          CLASS_GRADES[cls] = c;
          log('Ghi đè cách tính điểm', c.comps.map((x) => `${x.name} ${x.weight}%`).join(', '));
          saveAll();
          setVer(ver + 1);
          toast('Đã lưu cách tính điểm của lớp', k.code);
        }}
        side={
          <>
            <Box icon="file" tone="info" title="Thứ tự áp dụng">
              Lớp có ghi đè thì dùng cấu hình của lớp. Lớp không ghi đè dùng cấu hình của chương trình.
            </Box>
            {own && canEdit && (
              <button
                type="button"
                className="btn gc-reset"
                onClick={() => {
                  delete CLASS_GRADES[cls];
                  log('Bỏ ghi đè cách tính điểm', 'Theo chương trình');
                  saveAll();
                  toast('Đã bỏ ghi đè', `Lớp ${k.code} dùng lại cấu hình chương trình`);
                  nav('/dao-tao/cach-tinh-diem');
                }}
              >
                Bỏ ghi đè, dùng cấu hình chương trình
              </button>
            )}
          </>
        }
      />
    </AppLayout>
  );
}
