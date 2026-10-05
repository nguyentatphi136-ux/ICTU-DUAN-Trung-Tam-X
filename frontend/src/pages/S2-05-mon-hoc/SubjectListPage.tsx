import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Box, Drawer, Filter, Pager, paginate, RowMenu, Search } from '../../components/ui';
import { CURRICULUM, PROGRAMS, programsUsing, SUBJECTS, type Subject } from '../../data/training';
import { MODULE, TrainingTabs, useCanEditTraining } from '../S2-04-chuong-trinh/ProgramListPage';

// S2-05. Môn học dùng lại được ở nhiều chương trình (cột Dùng trong). Môn đã có lớp học thì không xoá được.
export function SubjectListPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const canEdit = useCanEditTraining();
  const [items, setItems] = useState(SUBJECTS);
  const [q, setQ] = useState('');
  const [program, setProgram] = useState('');
  const [page, setPage] = useState(1);
  const [editing, setEditing] = useState<Subject | 'new' | null>(null);

  const filtered = useMemo(() => {
    const k = q.trim().toLowerCase();
    const inProgram = PROGRAMS.find((p) => p.name === program);
    return items.filter(
      (s) => (!k || s.code.toLowerCase().includes(k) || s.name.toLowerCase().includes(k)) && (!inProgram || CURRICULUM[inProgram.code]?.some((c) => c.code === s.code)),
    );
  }, [items, q, program]);
  const view = paginate(filtered, page);

  const commit = (list: Subject[]) => {
    SUBJECTS.splice(0, SUBJECTS.length, ...list);
    setItems(list);
  };

  return (
    <AppLayout crumb={[MODULE, 'Môn học']} module={MODULE}>
      <div className="h">
        <div>
          <h2>{MODULE}</h2>
        </div>
        {canEdit && (
          <button type="button" className="btn primary" style={{ width: 170 }} onClick={() => setEditing('new')}>
            <Icon name="plus" size={16} />
            Thêm môn học
          </button>
        )}
      </div>
      <TrainingTabs />
      <div className="bar">
        <Search placeholder="Tìm theo mã hoặc tên môn" value={q} onChange={(v) => (setQ(v), setPage(1))} />
        <Filter label="Chương trình" value={program} options={PROGRAMS.map((p) => p.name)} onChange={(v) => (setProgram(v), setPage(1))} width={260} />
      </div>
      <div className="panel">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Mã</th>
                <th>Tên môn học</th>
                <th>Số buổi</th>
                <th>Trọng số</th>
                <th>Dùng trong</th>
                <th>Lớp đã mở</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {view.items.map((s) => (
                <tr key={s.code}>
                  <td className="strong">{s.code}</td>
                  <td>{s.name}</td>
                  <td>{s.sessions}</td>
                  <td>{s.weight}</td>
                  <td>{programsUsing(s.code).length} chương trình</td>
                  <td>{s.openedClasses}</td>
                  <td style={{ width: 56 }}>
                    <RowMenu
                      actions={[
                        { label: 'Xem buổi học', onClick: () => navigate('/dao-tao/mon-hoc/' + s.code) },
                        ...(canEdit
                          ? [
                              { label: 'Sửa môn học', onClick: () => setEditing(s) },
                              {
                                label: 'Xoá',
                                danger: true,
                                disabled: s.openedClasses ? `Môn đã có ${s.openedClasses} lớp học nên không xoá được` : false,
                                onClick: () => {
                                  if (!window.confirm(`Xoá môn ${s.name}?`)) return;
                                  commit(items.filter((x) => x.code !== s.code));
                                  toast('Đã xoá môn học', s.name);
                                },
                              },
                            ]
                          : []),
                      ]}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Pager page={view.page} pages={view.pages} onPage={setPage}>
          Hiển thị {view.from}–{view.to} trong {filtered.length} môn học
        </Pager>
      </div>

      {editing && (
        <SubjectDrawer
          subject={editing === 'new' ? null : editing}
          items={items}
          onClose={() => setEditing(null)}
          onSave={(s) => {
            commit(editing === 'new' ? [...items, s] : items.map((x) => (x.code === editing.code ? s : x)));
            toast('Đã lưu môn học', s.name);
            setEditing(null);
          }}
        />
      )}
    </AppLayout>
  );
}

function SubjectDrawer({ subject, items, onClose, onSave }: { subject: Subject | null; items: Subject[]; onClose: () => void; onSave: (s: Subject) => void }) {
  const [f, setF] = useState({
    code: subject?.code ?? '',
    name: subject?.name ?? '',
    sessions: String(subject?.sessions ?? ''),
    weight: String(subject?.weight ?? 1),
    outcome: subject?.outcome ?? '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const used = subject ? programsUsing(subject.code) : [];
  const set = (k: keyof typeof f) => (e: { target: { value: string } }) => {
    setF((x) => ({ ...x, [k]: e.target.value }));
    setErrors((x) => ({ ...x, [k]: '' }));
  };

  function save() {
    const code = f.code.trim().toUpperCase();
    const dup = items.find((s) => s.code === code && s.code !== subject?.code);
    const e: Record<string, string> = {};
    if (!code) e.code = 'Nhập mã môn';
    else if (dup) e.code = `Mã ${code} đã dùng cho môn ${dup.name}`;
    if (!f.name.trim()) e.name = 'Nhập tên môn học';
    if (!(Number(f.sessions) > 0)) e.sessions = 'Nhập số buổi lớn hơn 0';
    if (!(Number(f.weight) > 0)) e.weight = 'Trọng số phải lớn hơn 0';
    setErrors(e);
    if (Object.keys(e).length) return;
    onSave({ code, name: f.name.trim(), sessions: Number(f.sessions), weight: Number(f.weight), outcome: f.outcome.trim(), openedClasses: subject?.openedClasses ?? 0 });
  }

  const input = (k: 'code' | 'name' | 'sessions' | 'weight', label: string, hint?: string) => (
    <div className="field">
      <label htmlFor={'sj-' + k}>{label}</label>
      <div className={'inp' + (errors[k] ? ' bad' : '')}>
        <input id={'sj-' + k} value={f[k]} inputMode={k === 'sessions' || k === 'weight' ? 'numeric' : undefined} onChange={set(k)} />
      </div>
      {errors[k] ? <p className="err">{errors[k]}</p> : hint && <p className="hint">{hint}</p>}
    </div>
  );

  return (
    <Drawer
      title={subject ? 'Sửa môn học' : 'Thêm môn học'}
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn" style={{ width: 100 }} onClick={onClose}>
            Huỷ
          </button>
          <button type="button" className="btn primary" style={{ width: 150 }} onClick={save}>
            Lưu môn học
          </button>
        </>
      }
    >
      <div className="grid2" style={{ gridTemplateColumns: '150px 1fr' }}>
        {input('code', 'Mã môn')}
        {input('name', 'Tên môn học')}
      </div>
      <div className="grid2">
        {input('sessions', 'Số buổi')}
        {input('weight', 'Trọng số', 'Dùng khi tính điểm toàn khoá.')}
      </div>
      <div className="field">
        <label htmlFor="sj-outcome">Mô tả chuẩn đầu ra</label>
        <div className="inp">
          <textarea id="sj-outcome" rows={4} style={{ height: 110 }} value={f.outcome} onChange={set('outcome')} />
        </div>
      </div>
      {used.length > 0 && (
        <Box icon="book" title={`Đang dùng trong ${used.length} chương trình`}>
          {used.map((p) => p.name).join(', ')}
        </Box>
      )}
      {!!subject?.openedClasses && (
        <Box icon="lock" tone="warn" title="Không xoá được môn này">
          Môn đã có {subject.openedClasses} lớp học. Bạn vẫn sửa được tên và mô tả.
        </Box>
      )}
    </Drawer>
  );
}
