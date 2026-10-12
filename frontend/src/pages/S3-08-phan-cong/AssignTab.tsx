import { useEffect, useRef, useState } from 'react';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Box } from '../../components/ui';
import { CLASSES, isOpen, STAFF, type ClassSubject, type Klass } from '../../data/classes';
import { ROLES } from '../../data/permissions';
import { saveAll } from '../../data/store';

type Props = { k: Klass; manage: boolean; onChange: () => void };

/** Số lớp đang chạy mà người này dạy chính hoặc trợ giảng. */
const load = (name: string) => CLASSES.filter((c) => isOpen(c) && c.subjects.some((s) => s.teacher === name || s.tas.includes(name))).length;

// S3-08. Mỗi môn của lớp có một giảng viên chính và nhiều trợ giảng. Danh sách chọn chỉ gồm tài khoản có vai trò tương ứng.
// Phân công lưu ngay; giảng viên và trợ giảng thấy lớp trong danh sách lớp của mình.
export function AssignTab({ k, manage, onChange }: Props) {
  const toast = useToast();
  const missing = k.subjects.filter((s) => !s.teacher).length;
  const edit = manage && isOpen(k);

  const save = (s: ClassSubject, patch: Partial<ClassSubject>, msg: string) => {
    Object.assign(s, patch);
    saveAll();
    onChange();
    toast(msg, `${k.code} · ${s.name}`);
  };

  return (
    <>
      {missing > 0 && (
        <Box icon="alert" tone="warn" title={`${missing} trong ${k.subjects.length} môn chưa có giảng viên chính`}>
          {k.status === 'Chuẩn bị' ? 'Lớp chưa thể chuyển sang Đang học.' : 'Học viên chưa biết ai phụ trách các môn này.'}
        </Box>
      )}
      <div className="panel">
        <div className="scroll as-scroll">
          <table className="as-table">
            <thead>
              <tr>
                <th>Mã</th>
                <th>Môn học</th>
                <th>Số buổi</th>
                <th>Giảng viên chính</th>
                <th>Trợ giảng</th>
              </tr>
            </thead>
            <tbody>
              {k.subjects.map((s) => (
                <tr key={s.code}>
                  <td className="strong">{s.code}</td>
                  <td className="strong">{s.name}</td>
                  <td>{s.sessions}</td>
                  <td>
                    {edit ? (
                      <Picker role={2} label={s.teacher ?? 'Chưa phân công'} empty={!s.teacher} exclude={s.teacher ? [s.teacher] : []} onPick={(n) => save(s, { teacher: n }, 'Đã phân công ' + n)} />
                    ) : (
                      <span className={s.teacher ? 'strong' : 'hint'}>{s.teacher ?? 'Chưa phân công'}</span>
                    )}
                  </td>
                  <td>
                    <div className="chips">
                      {s.tas.map((t) => (
                        <span key={t} className="chip">
                          {t}
                          {edit && (
                            <button type="button" aria-label={'Bỏ ' + t} onClick={() => save(s, { tas: s.tas.filter((x) => x !== t) }, 'Đã bỏ trợ giảng ' + t)}>
                              <Icon name="x" />
                            </button>
                          )}
                        </span>
                      ))}
                      {edit && <Picker role={1} label="Thêm" add exclude={s.tas} onPick={(n) => save(s, { tas: [...s.tas, n] }, 'Đã thêm trợ giảng ' + n)} />}
                      {!edit && !s.tas.length && <span className="hint">–</span>}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      <p className="hint" style={{ marginTop: 12 }}>
        {edit ? `Phân công được lưu ngay. Giảng viên và trợ giảng sẽ thấy ${k.code} trong danh sách lớp của mình.` : 'Chỉ Quản lý đào tạo phân công được, và chỉ khi lớp đang chuẩn bị hoặc đang học.'}
      </p>
    </>
  );
}

type PickerProps = { role: 1 | 2; label: string; empty?: boolean; add?: boolean; exclude: string[]; onPick: (name: string) => void };

/** Ô chọn người có tìm kiếm. Chỉ liệt kê tài khoản có đúng vai trò (Giảng viên hoặc Trợ giảng). */
function Picker({ role, label, empty, add, exclude, onPick }: PickerProps) {
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState('');
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return;
    const f = (e: MouseEvent) => !ref.current?.contains(e.target as Node) && setOpen(false);
    document.addEventListener('mousedown', f);
    return () => document.removeEventListener('mousedown', f);
  }, [open]);
  const people = STAFF.filter((p) => p.role === role && !exclude.includes(p.name) && p.name.toLowerCase().includes(q.trim().toLowerCase()));

  return (
    <div className="as-pick" ref={ref}>
      <button type="button" className={add ? 'chip-add' : 'as-btn' + (empty ? ' empty' : '')} aria-expanded={open} onClick={() => setOpen((o) => !o)}>
        {add && <Icon name="plus" />}
        {label}
        {!add && <Icon name="down" />}
      </button>
      {open && (
        <div className="dropdown as-menu" role="listbox">
          <div className="inp lead">
            <Icon name="search" />
            <input autoFocus value={q} placeholder={role === 2 ? 'Tìm giảng viên' : 'Tìm trợ giảng'} onChange={(e) => setQ(e.target.value)} aria-label="Tìm theo tên" />
          </div>
          {people.map((p) => (
            <button
              key={p.name}
              type="button"
              role="option"
              aria-selected={false}
              onClick={() => {
                setOpen(false);
                onPick(p.name);
              }}
            >
              <b>{p.name}</b>
              <small>
                {p.email.replace(/\d+@.*/, '')} · đang dạy {load(p.name)} lớp
              </small>
            </button>
          ))}
          {!people.length && <p className="hint as-none">Không có ai khớp.</p>}
          <p className="hint as-note">Chỉ hiện tài khoản có vai trò {ROLES[role]}</p>
        </div>
      )}
    </div>
  );
}
