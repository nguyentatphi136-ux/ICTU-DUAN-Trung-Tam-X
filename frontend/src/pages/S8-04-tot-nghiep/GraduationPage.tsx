import { useState } from 'react';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Filter, Modal, Pill } from '../../components/ui';
import { thresholdOf } from '../../data/attendance';
import { audit } from '../../data/audit';
import { useAuth } from '../../data/auth';
import { CLASSES, findClass, totalSessions } from '../../data/classes';
import { TODAY, viDateTime } from '../../data/funnel';
import { gkey, LOCKS, transcriptOf } from '../../data/grades';
import { viDate } from '../../data/leads';
import { notify } from '../../data/notifications';
import { ROLES } from '../../data/permissions';
import { persisted, saveAll } from '../../data/store';
import { STUDENTS, type Student } from '../../data/students';
import { debtOf } from '../../data/tuition';
import { vnd } from '../../data/vnd';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import { MODULE } from '../S8-02-diem-tong-ket/FinalPages';
import './graduation.css';

/** Lần kiểm tra gần nhất theo lớp. Khi tích hợp: POST /classes/:code/graduation-check. */
const CHECKED: Record<string, string> = persisted('grad-checks', {});

/** Kiểm tra ba điều kiện: đạt toàn bộ môn (đã chốt), chuyên cần trên ngưỡng, không còn nợ học phí. */
export function eligibility(st: Student) {
  const t = transcriptOf(st.code)!;
  const min = 100 - thresholdOf(st.program);
  const open = t.k.subjects.filter((s) => !LOCKS[gkey(t.k.code, s.code)]);
  const notPassed = t.rows.filter((r) => r.f.result !== 'Đạt');
  const debt = debtOf(st.code);
  const reasons: string[] = [];
  if (open.length) reasons.push(`Môn ${open.map((s) => s.code).join(', ')} chưa chốt điểm`);
  if (notPassed.length) reasons.push(notPassed.length === 1 ? `Chưa đạt môn ${notPassed[0].s.code}` : `Chưa đạt ${notPassed.length} môn`);
  const lowAtt = t.attendance != null && t.attendance < min;
  if (lowAtt) reasons.push(`Chuyên cần dưới ngưỡng ${min}%`);
  if (debt.owed > 0) reasons.push(`Còn nợ học phí ${debt.next ? `đợt ${debt.next.no}` : ''}`.trim());
  return { t, min, open, lowAtt, debt, reasons, ok: !reasons.length };
}

// S8-04. Xét điều kiện hoàn thành khoá theo lớp và xác nhận tốt nghiệp hàng loạt cho nhóm đủ điều kiện.
export function GraduationPage() {
  const { user } = useAuth();
  const toast = useToast();
  const classes = CLASSES.filter((k) => k.status === 'Kết thúc' || (k.status === 'Đang học' && k.done >= totalSessions(k)));
  const [cls, setCls] = useState(classes.find((k) => STUDENTS.some((s) => s.cls === k.code && s.status === 'Đang học'))?.code ?? classes[0]?.code ?? '');
  const [tab, setTab] = useState<'ok' | 'no' | 'done'>('no');
  const [pick, setPick] = useState<Set<string> | null>(null);
  const [confirm, setConfirm] = useState(false);
  const [, setTick] = useState(0);
  if (user?.active !== 5 && user?.active !== 6) return <ErrorPage code="403" />;
  const k = findClass(cls);
  const roster = STUDENTS.filter((s) => s.cls === cls && s.status !== 'Thôi học');
  const done = roster.filter((s) => s.status === 'Đã tốt nghiệp');
  const checked = roster.filter((s) => s.status !== 'Đã tốt nghiệp').map((s) => ({ s, e: eligibility(s) }));
  const ok = checked.filter((r) => r.e.ok);
  const no = checked.filter((r) => !r.e.ok);
  const sel = pick ?? new Set(ok.map((r) => r.s.code));
  const chosen = ok.filter((r) => sel.has(r.s.code));
  const at = CHECKED[cls] ?? TODAY + 'T09:00';
  const list = tab === 'ok' ? ok : tab === 'no' ? no : done.map((s) => ({ s, e: eligibility(s) }));

  return (
    <AppLayout crumb={[MODULE, 'Xét hoàn thành khoá']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Xét điều kiện hoàn thành khoá</h2>
          <p>
            Lớp {cls} · {k?.program} · {roster.length} học viên · {k?.status === 'Kết thúc' ? 'đã kết thúc' : 'đã học đủ buổi'}
          </p>
        </div>
        <Filter label="Lớp" value={cls} options={classes.map((x) => x.code)} onChange={(v) => v && (setCls(v), setPick(null))} width={200} />
      </div>
      <div className="gr-conds">
        <span className="hint">Ba điều kiện được kiểm tra</span>
        <div>
          <span>
            <Icon name="award" size={16} />
            Đạt toàn bộ môn
          </span>
          <span>
            <Icon name="check" size={16} />
            Chuyên cần từ {100 - thresholdOf(k?.program ?? '')}% (vắng không quá {thresholdOf(k?.program ?? '')}%)
          </span>
          <span>
            <Icon name="wallet" size={16} />
            Không còn nợ học phí
          </span>
        </div>
      </div>
      <div className="tn-tabs">
        <nav className="tabs">
          {(
            [
              ['ok', `Đủ điều kiện (${ok.length})`],
              ['no', `Chưa đủ (${no.length})`],
              ['done', `Đã tốt nghiệp (${done.length})`],
            ] as const
          ).map(([key, label]) => (
            <a key={key} href="#" className={tab === key ? 'on' : undefined} onClick={(e) => (e.preventDefault(), setTab(key))}>
              {label}
            </a>
          ))}
        </nav>
        <span className="hint">
          Kiểm tra lúc {viDateTime(at)} ·{' '}
          <button
            type="button"
            className="link gr-inline"
            onClick={() => {
              CHECKED[cls] = TODAY + 'T' + new Date().toTimeString().slice(0, 5);
              saveAll();
              setTick((t) => t + 1);
              toast('Đã kiểm tra lại', `${ok.length} đủ điều kiện, ${no.length} chưa đủ`);
            }}
          >
            Kiểm tra lại
          </button>
        </span>
      </div>
      <div className="panel">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                {tab === 'ok' && (
                  <th style={{ width: 40 }}>
                    <input type="checkbox" aria-label="Chọn tất cả" checked={chosen.length === ok.length && ok.length > 0} onChange={(e) => setPick(new Set(e.target.checked ? ok.map((r) => r.s.code) : []))} />
                  </th>
                )}
                <th>Học viên</th>
                <th>Kết quả môn</th>
                <th>Chuyên cần</th>
                <th>Học phí</th>
                <th>{tab === 'no' ? 'Lý do chưa đủ' : tab === 'ok' ? 'Điểm trung bình' : 'Trạng thái'}</th>
              </tr>
            </thead>
            <tbody>
              {list.map(({ s, e }) => (
                <tr key={s.code}>
                  {tab === 'ok' && (
                    <td>
                      <input
                        type="checkbox"
                        aria-label={`Chọn ${s.name}`}
                        checked={sel.has(s.code)}
                        onChange={(ev) => {
                          const n = new Set(sel);
                          if (ev.target.checked) n.add(s.code);
                          else n.delete(s.code);
                          setPick(n);
                        }}
                      />
                    </td>
                  )}
                  <td>
                    <b className="strong">{s.name}</b>
                    <div className="hint">{s.code}</div>
                  </td>
                  <td className={e.open.length || e.t.passed < e.t.subjects ? 'bad-t' : undefined}>{e.open.length ? 'Chưa chốt' : `${e.t.passed}/${e.t.subjects} môn`}</td>
                  <td className={e.lowAtt ? 'bad-t' : undefined}>{e.t.attendance ?? '–'}%</td>
                  <td className={e.debt.owed ? 'bad-t' : undefined}>{e.debt.owed ? `Nợ ${vnd(e.debt.owed)}` : 'Đã đủ'}</td>
                  <td className={tab === 'no' ? 'bad-t' : undefined}>
                    {tab === 'no' ? e.reasons.join(', ') : tab === 'ok' ? (e.t.gpa == null ? '–' : e.t.gpa.toLocaleString('vi-VN')) : <Pill tone="ok">Đã tốt nghiệp</Pill>}
                  </td>
                </tr>
              ))}
              {!list.length && (
                <tr>
                  <td colSpan={6} className="empty">
                    Không có học viên trong nhóm này.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
      <div className="panel fe-pad fe-bulk">
        <div>
          <b>{ok.length} học viên đủ điều kiện, chưa xác nhận tốt nghiệp</b>
          <p className="hint">{tab === 'ok' ? 'Bỏ chọn học viên chưa muốn xác nhận lần này.' : 'Chuyển sang thẻ Đủ điều kiện để chọn và xác nhận hàng loạt.'}</p>
        </div>
        <button type="button" className="btn primary" disabled={!chosen.length} onClick={() => setConfirm(true)}>
          Xác nhận tốt nghiệp {chosen.length} học viên
        </button>
      </div>
      {confirm && (
        <ConfirmGraduation
          cls={cls}
          program={k?.program ?? ''}
          names={chosen.map((r) => r.s.name)}
          subjects={k?.subjects.length ?? 0}
          min={100 - thresholdOf(k?.program ?? '')}
          onClose={() => setConfirm(false)}
          onConfirm={(date) => {
            for (const { s } of chosen) s.status = 'Đã tốt nghiệp';
            const stamp = TODAY + 'T' + new Date().toTimeString().slice(0, 5);
            audit({ at: stamp, by: user.name, role: ROLES[user.active], entity: 'Điểm', action: 'Xác nhận tốt nghiệp', target: `Lớp ${cls} · ${chosen.length} học viên`, changes: [{ field: 'Trạng thái', before: 'Đang học', after: `Đã tốt nghiệp ${viDate(date)}` }] });
            notify({ at: stamp, kind: 'Điểm', title: 'Chúc mừng bạn đã hoàn thành khoá học', body: `Lớp ${cls} · ngày tốt nghiệp ${viDate(date)}. Bạn tải được bảng điểm toàn khoá.`, link: '/bang-diem', to: { users: chosen.map((r) => r.s.name) } });
            saveAll();
            toast(`Đã xác nhận tốt nghiệp ${chosen.length} học viên`, `Lớp ${cls}`);
            setConfirm(false);
            setPick(null);
            setTab('done');
          }}
        />
      )}
    </AppLayout>
  );
}

function ConfirmGraduation({ cls, program, names, subjects, min, onClose, onConfirm }: { cls: string; program: string; names: string[]; subjects: number; min: number; onClose: () => void; onConfirm: (date: string) => void }) {
  const [date, setDate] = useState(TODAY);
  const [all, setAll] = useState(false);
  return (
    <Modal onClose={onClose} label="Xác nhận tốt nghiệp">
      <h3 style={{ marginBottom: 4 }}>Xác nhận tốt nghiệp {names.length} học viên?</h3>
      <p className="hint" style={{ marginBottom: 12 }}>
        Lớp {cls} · {program}
      </p>
      <div className="gr-names">
        <p>{all || names.length <= 5 ? names.join(', ') : `${names.slice(0, 5).join(', ')} và ${names.length - 5} học viên khác`}</p>
        <p className="ok-t">
          Tất cả đạt {subjects}/{subjects} môn, chuyên cần từ {min}%, không nợ học phí.
        </p>
        {names.length > 5 && !all && (
          <button type="button" className="link gr-inline" onClick={() => setAll(true)}>
            Xem đủ danh sách
          </button>
        )}
      </div>
      <div className="field">
        <label htmlFor="gr-date">Ngày tốt nghiệp</label>
        <div className="inp">
          <input id="gr-date" type="date" max={TODAY} value={date} onChange={(e) => setDate(e.target.value)} />
        </div>
      </div>
      <b>Sau khi xác nhận</b>
      <ul className="gr-after">
        <li>Hồ sơ {names.length} học viên chuyển sang trạng thái Đã tốt nghiệp</li>
        <li>Học viên nhận thông báo và tải được bảng điểm toàn khoá</li>
        <li>Thao tác được ghi vào nhật ký, không hoàn tác hàng loạt được</li>
      </ul>
      <div className="acts">
        <button type="button" className="btn" style={{ width: 90 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className="btn primary" disabled={!date} onClick={() => onConfirm(date)}>
          Xác nhận
        </button>
      </div>
    </Modal>
  );
}
