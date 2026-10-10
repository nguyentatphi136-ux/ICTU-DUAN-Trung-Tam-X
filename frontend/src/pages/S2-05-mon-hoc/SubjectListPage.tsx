import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Box, ConfirmDialog, Drawer, Filter, Pager, paginate, RowMenu, Search, usePaging } from '../../components/ui';
import { useAuth } from '../../data/auth';
import { CURRICULUM, LESSONS, PROGRAMS, programsUsing, SUBJECTS, type Subject } from '../../data/training';
import { moveToTrash, restore } from '../../data/trash';
import { MODULE, TrainingTabs, useCanEditTraining } from '../../components/TrainingTabs';

// S2-05. Môn học dùng lại được ở nhiều chương trình: cột Dùng trong ghi tên chương trình, gán chương trình ngay trong form.
// Môn đã có lớp học thì không có nút xoá. Xoá là chuyển vào thùng rác, gỡ môn khỏi các lộ trình (khôi phục thì gắn lại).
export function SubjectListPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const canEdit = useCanEditTraining();
  const [items, setItems] = useState(SUBJECTS);
  const [q, setQ] = useState('');
  const [program, setProgram] = useState('');
  const { page, setPage, size, setSize } = usePaging();
  const [editing, setEditing] = useState<Subject | 'new' | null>(null);
  const [deleting, setDeleting] = useState<Subject | null>(null);
  const { user } = useAuth();

  const filtered = useMemo(() => {
    const k = q.trim().toLowerCase();
    const inProgram = PROGRAMS.find((p) => p.name === program);
    return items.filter(
      (s) => (!k || s.code.toLowerCase().includes(k) || s.name.toLowerCase().includes(k)) && (!inProgram || CURRICULUM[inProgram.code]?.some((c) => c.code === s.code)),
    );
  }, [items, q, program]);
  const view = paginate(filtered, page, size);

  const commit = (list: Subject[]) => {
    SUBJECTS.splice(0, SUBJECTS.length, ...list);
    setItems(list);
  };
  const trash = (s: Subject) => {
    const id = moveToTrash({ kind: 'subject', item: s, usedIn: [] }, s.name, user?.name ?? '');
    setItems([...SUBJECTS]);
    setDeleting(null);
    toast('Đã chuyển vào thùng rác', s.name, {
      label: 'Hoàn tác',
      onClick: () => {
        restore(id);
        setItems([...SUBJECTS]);
      },
    });
  };

  return (
    <AppLayout crumb={[MODULE, 'Môn học']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Môn học</h2>
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
                  <td>
                    <ProgramChips code={s.code} />
                  </td>
                  <td>{s.openedClasses}</td>
                  <td style={{ width: 56 }}>
                    <RowMenu
                      actions={[
                        { label: 'Xem buổi học', onClick: () => navigate('/dao-tao/mon-hoc/' + s.code) },
                        ...(canEdit
                          ? [
                              { label: 'Sửa môn học', onClick: () => setEditing(s) },
                              // Đã có lớp học thì không có mục xoá; lý do ghi trong ngăn Sửa (hộp "Không xoá được môn này").
                              ...(s.openedClasses ? [] : [{ label: 'Chuyển vào thùng rác', danger: true, onClick: () => setDeleting(s) }]),
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
        <Pager page={view.page} pages={view.pages} onPage={setPage} size={size} onSize={setSize}>
          Hiển thị {view.from}–{view.to} trong {filtered.length} môn học
        </Pager>
      </div>

      {deleting && (
        <ConfirmDialog title="Chuyển môn học vào thùng rác?" onClose={() => setDeleting(null)} onConfirm={() => trash(deleting)}>
          Môn <b>{deleting.name}</b> ({deleting.code}) sẽ ẩn khỏi danh sách
          {programsUsing(deleting.code).length > 0 && <> và được gỡ khỏi lộ trình {programsUsing(deleting.code).map((p) => p.name).join(', ')}</>}.
        </ConfirmDialog>
      )}
      {editing && (
        <SubjectDrawer
          subject={editing === 'new' ? null : editing}
          items={items}
          onClose={() => setEditing(null)}
          onSave={(s, programs) => {
            if (editing !== 'new' && editing.code !== s.code) renameSubject(editing.code, s.code);
            assignPrograms(s.code, programs);
            commit(editing === 'new' ? [...items, s] : items.map((x) => (x.code === editing.code ? s : x)));
            toast('Đã lưu môn học', s.name);
            setEditing(null);
          }}
        />
      )}
    </AppLayout>
  );
}

/** Tên chương trình đang dùng môn (tối đa 2, còn lại "+n"); bấm để mở lộ trình của chương trình đó. */
function ProgramChips({ code }: { code: string }) {
  const used = programsUsing(code);
  if (!used.length) return <span className="hint">Chưa thuộc chương trình nào</span>;
  return (
    <span className="tags">
      {used.slice(0, 2).map((p) => (
        <Link key={p.code} to={'/dao-tao/chuong-trinh/' + p.code} className="pill info" title={'Mở lộ trình ' + p.name}>
          {p.name}
        </Link>
      ))}
      {used.length > 2 && (
        <span className="pill" title={used.slice(2).map((p) => p.name).join(', ')}>
          +{used.length - 2}
        </span>
      )}
    </span>
  );
}

/** Gắn môn vào cuối lộ trình các chương trình được chọn, gỡ khỏi chương trình bỏ chọn. */
function assignPrograms(code: string, programs: string[]) {
  for (const p of PROGRAMS) {
    const list = (CURRICULUM[p.code] ??= []);
    const has = list.some((c) => c.code === code);
    if (programs.includes(p.code) && !has) list.push({ code, prereq: [] });
    if (!programs.includes(p.code) && has) {
      list.splice(list.findIndex((c) => c.code === code), 1);
      list.forEach((c) => (c.prereq = c.prereq.filter((x) => x !== code)));
    }
  }
}

/** Đổi mã môn thì đổi luôn trong lộ trình, môn tiên quyết và danh sách buổi học. */
function renameSubject(from: string, to: string) {
  for (const list of Object.values(CURRICULUM)) {
    for (const c of list) {
      if (c.code === from) c.code = to;
      c.prereq = c.prereq.map((x) => (x === from ? to : x));
    }
  }
  if (LESSONS[from]) {
    LESSONS[to] = LESSONS[from];
    delete LESSONS[from];
  }
}

function SubjectDrawer({ subject, items, onClose, onSave }: { subject: Subject | null; items: Subject[]; onClose: () => void; onSave: (s: Subject, programs: string[]) => void }) {
  const [f, setF] = useState({
    code: subject?.code ?? '',
    name: subject?.name ?? '',
    sessions: String(subject?.sessions ?? ''),
    weight: String(subject?.weight ?? 1),
    outcome: subject?.outcome ?? '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const used = subject ? programsUsing(subject.code) : [];
  const [programs, setPrograms] = useState(used.map((p) => p.code));
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
    onSave({ code, name: f.name.trim(), sessions: Number(f.sessions), weight: Number(f.weight), outcome: f.outcome.trim(), openedClasses: subject?.openedClasses ?? 0 }, programs);
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
      <fieldset className="field check-list">
        <legend>Thuộc chương trình</legend>
        {PROGRAMS.map((p) => (
          <label key={p.code}>
            <input
              type="checkbox"
              checked={programs.includes(p.code)}
              onChange={(e) => setPrograms((x) => (e.target.checked ? [...x, p.code] : x.filter((c) => c !== p.code)))}
            />
            {p.name}
            {!p.active && <small> (ngừng áp dụng)</small>}
          </label>
        ))}
        <p className="hint">Chương trình mới chọn được thêm môn vào cuối lộ trình; sắp xếp lại ở trang lộ trình.</p>
      </fieldset>
      {!!subject?.openedClasses && (
        <Box icon="lock" tone="warn" title="Không xoá được môn này">
          Môn đã có {subject.openedClasses} lớp học. Bạn vẫn sửa được tên và mô tả.
        </Box>
      )}
    </Drawer>
  );
}
