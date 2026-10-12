import { useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Box, Filter, Modal, Pill } from '../../components/ui';
import { audit } from '../../data/audit';
import { useAuth } from '../../data/auth';
import { CLASSES, findClass } from '../../data/classes';
import { TODAY, viDateTime } from '../../data/funnel';
import { configLine, configOf, finalOf, gkey, LOCK_LOGS, LOCKS, num, resultTone, SCORES, subjectProgress } from '../../data/grades';
import { notify } from '../../data/notifications';
import { ROLES } from '../../data/permissions';
import { saveAll } from '../../data/store';
import { STUDENTS } from '../../data/students';
import './final.css';

export const MODULE = 'Điểm tổng kết & tốt nghiệp';
const now = () => TODAY + 'T' + new Date().toTimeString().slice(0, 5);
const isManager = (role?: number) => role === 5 || role === 6;

/** Các cặp lớp, môn đã bắt đầu học mà người dùng được xem: giảng viên chỉ thấy môn mình dạy. */
export function finalPairs(name: string, role: number) {
  return CLASSES.filter((k) => k.status === 'Đang học' || k.status === 'Kết thúc').flatMap((k) =>
    subjectProgress(k)
      .filter((s) => s.state !== 'Chưa học' && (isManager(role) || s.teacher === name || s.tas.includes(name)))
      .map((s) => ({ k, s, key: gkey(k.code, s.code) })),
  );
}

export function classSummary(cls: string, subject: string) {
  const rows = STUDENTS.filter((s) => s.cls === cls).map((st) => ({ st, f: finalOf(cls, subject, st.code) }));
  const count = (f: (r: (typeof rows)[number]) => boolean) => rows.filter(f).length;
  return { rows, pass: count((r) => r.f.result === 'Đạt'), fail: count((r) => r.f.result.startsWith('Không đạt')), missing: count((r) => r.f.result === 'Chưa đủ điều kiện tính') };
}

// S8-02. Danh sách môn cần tổng kết: lớp, môn, trạng thái chốt, số đạt và không đạt.
export function FinalListPage() {
  const { user } = useAuth();
  const [cls, setCls] = useState('');
  const [state, setState] = useState('');
  const pairs = finalPairs(user?.name ?? '', user?.active ?? -1);
  // S8-03: học viên vào mục này thì xem bảng điểm toàn khoá của mình.
  if (user?.active === 0) return <Navigate to="/bang-diem" replace />;
  const rows = pairs.filter((p) => (!cls || p.k.code === cls) && (!state || (state === 'Đã chốt') === !!LOCKS[p.key]));
  return (
    <AppLayout crumb={[MODULE, 'Điểm tổng kết môn']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Điểm tổng kết môn</h2>
          <p>
            {pairs.length} môn · {pairs.filter((p) => !LOCKS[p.key]).length} môn chưa chốt điểm
          </p>
        </div>
      </div>
      <div className="bar">
        <Filter label="Lớp" value={cls} options={[...new Set(pairs.map((p) => p.k.code))]} onChange={setCls} width={180} />
        <Filter label="Trạng thái" value={state} options={['Chưa chốt', 'Đã chốt']} onChange={setState} width={200} />
      </div>
      <div className="panel">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Lớp</th>
                <th>Môn</th>
                <th>Giảng viên</th>
                <th>Kết quả</th>
                <th>Trạng thái</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {rows.map(({ k, s, key }) => {
                const sum = classSummary(k.code, s.code);
                return (
                  <tr key={key}>
                    <td className="code">{k.code}</td>
                    <td>
                      <b className="strong">{s.code}</b> <span className="hint">{s.name}</span>
                    </td>
                    <td>{s.teacher ?? '–'}</td>
                    <td className="hint">
                      {sum.pass} đạt · {sum.fail} không đạt{sum.missing ? ` · ${sum.missing} chưa đủ điều kiện` : ''}
                    </td>
                    <td>{LOCKS[key] ? <Pill tone="ok">Đã chốt</Pill> : <Pill tone="wait">{s.state === 'Đang học' ? 'Đang học' : 'Chưa chốt'}</Pill>}</td>
                    <td className="fe-used">
                      <Link className="link" to={`/diem-tong-ket/${k.code}/${s.code}`}>
                        Xem điểm
                      </Link>
                    </td>
                  </tr>
                );
              })}
              {!rows.length && (
                <tr>
                  <td colSpan={6} className="empty">
                    Không có môn nào khớp bộ lọc.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </AppLayout>
  );
}

// S8-02. Điểm tổng kết môn: tự tính lại khi điểm thành phần đổi, bấm một dòng để xem cách tính, chốt điểm, mở lại có nhật ký.
export function FinalPage() {
  const { cls = '', subject = '' } = useParams();
  const { user } = useAuth();
  const toast = useToast();
  const [sel, setSel] = useState('');
  const [, setTick] = useState(0);
  const [dialog, setDialog] = useState<'lock' | 'reopen' | 'request' | null>(null);
  const k = findClass(cls);
  const sub = k?.subjects.find((s) => s.code === subject);
  if (!k || !sub) return <AppLayout crumb={[MODULE]} module={MODULE}><p className="empty">Không tìm thấy môn {subject} của lớp {cls}.</p></AppLayout>;
  const key = gkey(cls, subject);
  const lock = LOCKS[key];
  const cfg = lock?.config ?? configOf(cls);
  const sum = classSummary(cls, subject);
  const mine = sub.teacher === user?.name;
  const canEdit = !lock && (mine || isManager(user?.active));
  const pick = sum.rows.find((r) => r.st.code === sel) ?? sum.rows[0];
  const lacking = sum.rows.filter((r) => r.f.missing.length);
  const logs = LOCK_LOGS.filter((l) => l.key === key);
  const refresh = () => setTick((t) => t + 1);

  function setScore(code: string, comp: string, text: string) {
    const v = Number(text.replace(',', '.'));
    const row = ((SCORES[key] ??= {})[code] ??= {});
    if (text.trim() === '') delete row[comp];
    else if (Number.isFinite(v) && v >= 0 && v <= 10) row[comp] = Math.round(v * 10) / 10;
    else return toast('Điểm không hợp lệ', 'Nhập số từ 0 đến 10');
    saveAll();
    refresh();
  }

  function log(action: string, text: string) {
    LOCK_LOGS.unshift({ key, at: now(), by: user!.name, text });
    audit({ at: now(), by: user!.name, role: ROLES[user!.active], entity: 'Điểm', action, target: `${cls} · ${subject}`, changes: [{ field: 'Trạng thái', before: lock ? 'Đã chốt' : 'Chưa chốt', after: lock ? 'Mở lại' : 'Đã chốt' }] });
  }

  return (
    <AppLayout crumb={[MODULE, cls, subject]} module={MODULE}>
      <Link to="/diem-tong-ket" className="back">
        <Icon name="back" size={16} />
        Điểm tổng kết môn
      </Link>
      <div className="h">
        <div>
          <h2>
            Điểm tổng kết môn {subject} {lock ? <Pill tone="ok">Đã chốt {viDateTime(lock.at)}</Pill> : <Pill tone="wait">Chưa chốt</Pill>}
          </h2>
          <p>
            Lớp {cls} · {sum.rows.length} học viên · {configLine(cfg)}
          </p>
        </div>
      </div>
      {lock ? (
        <Box icon="lock" tone="ok" title="Điểm môn này đã chốt">
          {lock.by} chốt ngày {viDateTime(lock.at)}. Muốn sửa điểm thành phần, cần Quản lý đào tạo mở lại.
          <span className="fn-act">
            {isManager(user?.active) ? (
              <button type="button" className="btn" onClick={() => setDialog('reopen')}>
                Mở lại
              </button>
            ) : (
              mine && (
                <button type="button" className="btn" onClick={() => setDialog('request')}>
                  Yêu cầu mở lại
                </button>
              )
            )}
          </span>
        </Box>
      ) : (
        <Box icon="alert" tone={lacking.length ? 'warn' : 'info'} title={lacking.length ? `${lacking.length} học viên chưa đủ điều kiện tính` : 'Điểm tự tính lại mỗi khi một điểm thành phần thay đổi'}>
          {lacking.length
            ? `${lacking
                .slice(0, 3)
                .map((r) => `${r.st.name} thiếu ${r.f.missing.map((m) => m.c.name.toLowerCase()).join(', ')}`)
                .join('; ')}. Nhập đủ điểm hoặc chốt với kết quả Chưa đủ điều kiện.`
            : 'Kiểm tra lại rồi bấm Chốt điểm. Sau khi chốt, điểm thành phần bị khoá.'}
          {canEdit && (
            <span className="fn-act">
              <button type="button" className="btn primary" onClick={() => setDialog('lock')}>
                Chốt điểm
              </button>
            </span>
          )}
        </Box>
      )}
      <div className="fn-grid">
        <div className="panel">
          <div className="scroll">
            <table className="rows fn-table">
              <thead>
                <tr>
                  <th>Học viên</th>
                  {cfg.comps.map((c) => (
                    <th key={c.key} className="num" title={`${c.weight}%`}>
                      {c.name.replace(/^Kiểm tra (\S)/, (_, ch: string) => ch.toUpperCase())}
                    </th>
                  ))}
                  <th className="num">Tổng kết</th>
                  <th>Kết quả</th>
                </tr>
              </thead>
              <tbody>
                {sum.rows.map(({ st, f }) => (
                  <tr key={st.code} className={pick?.st.code === st.code ? 'on' : undefined} tabIndex={0} onClick={() => setSel(st.code)} onKeyDown={(e) => e.key === 'Enter' && setSel(st.code)}>
                    <td>
                      <b className="strong">{st.name}</b>
                      <div className="hint">
                        {st.code} · <Link to={`/bang-diem/${st.code}`} onClick={(e) => e.stopPropagation()}>bảng điểm</Link>
                      </div>
                    </td>
                    {f.parts.map((p) => (
                      <td key={p.c.key} className={'num' + (p.v == null ? ' bad-t' : f.cfg.failOn.includes(p.c.key) && p.v < f.cfg.fail ? ' bad-t' : '')}>
                        {canEdit && (p.c.source === 'Nhập tay' || p.c.source === 'Rubric') && !p.note ? (
                          <input
                            className="fn-cell"
                            aria-label={`${p.c.name} của ${st.name}`}
                            defaultValue={p.v == null ? '' : num(p.v)}
                            placeholder="thiếu"
                            onClick={(e) => e.stopPropagation()}
                            onBlur={(e) => e.target.value !== (p.v == null ? '' : num(p.v)) && setScore(st.code, p.c.key, e.target.value)}
                          />
                        ) : p.v == null ? (
                          'thiếu'
                        ) : (
                          num(p.v)
                        )}
                      </td>
                    ))}
                    <td className={'num strong' + (f.total != null && f.result !== 'Đạt' ? ' bad-t' : '')}>{f.total == null ? '–' : num(f.total)}</td>
                    <td>
                      <Pill tone={resultTone(f.result)}>{f.result}</Pill>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="hint fe-pad">
            Cả lớp: {sum.pass} đạt, {sum.fail} không đạt, {sum.missing} chưa đủ điều kiện
          </p>
        </div>
        {pick && <Breakdown name={pick.st.name} f={pick.f} />}
      </div>
      {logs.length > 0 && (
        <section className="fn-log">
          <h3>Nhật ký</h3>
          {logs.map((l, i) => (
            <p key={i} className="hint">
              {viDateTime(l.at)} · {l.by} {l.text}
            </p>
          ))}
        </section>
      )}
      {dialog && (
        <LockDialog
          kind={dialog}
          lacking={lacking.length}
          onClose={() => setDialog(null)}
          onDone={(reason) => {
            if (dialog === 'lock') {
              LOCKS[key] = { at: now(), by: user!.name, config: JSON.parse(JSON.stringify(cfg)), values: Object.fromEntries(sum.rows.map((r) => [r.st.code, Object.fromEntries(r.f.parts.map((p) => [p.c.key, p.v]))])) };
              log('Chốt điểm', `chốt điểm môn ${subject} lớp ${cls}.`);
              toast('Đã chốt điểm', `${subject} · ${cls}`);
            } else if (dialog === 'reopen') {
              delete LOCKS[key];
              log('Mở lại điểm', `mở lại, lý do: ${reason}`);
              if (sub.teacher) notify({ at: now(), kind: 'Điểm', title: `Điểm môn ${subject} lớp ${cls} đã được mở lại`, body: `${user!.name}: ${reason}`, link: `/diem-tong-ket/${cls}/${subject}`, to: { users: [sub.teacher] } });
              toast('Đã mở lại điểm', `${subject} · ${cls}`);
            } else {
              LOCK_LOGS.unshift({ key, at: now(), by: user!.name, text: `yêu cầu mở lại, lý do: ${reason}` });
              notify({ at: now(), kind: 'Điểm', title: `Yêu cầu mở lại điểm ${subject} lớp ${cls}`, body: `${user!.name}: ${reason}`, link: `/diem-tong-ket/${cls}/${subject}`, to: { users: ['LE HOANG NAM'] } });
              toast('Đã gửi yêu cầu mở lại', 'Quản lý đào tạo sẽ nhận thông báo');
            }
            saveAll();
            setDialog(null);
            refresh();
          }}
        />
      )}
    </AppLayout>
  );
}

export function Breakdown({ name, f }: { name: string; f: ReturnType<typeof finalOf> }) {
  const late = f.failed.map((p) => `${p.c.name.toLowerCase()} ${num(p.v!)}`).join(', ');
  const crit = f.parts.filter((p) => f.cfg.failOn.includes(p.c.key) && p.v != null);
  return (
    <aside className="panel fe-pad fn-side">
      <h3>Cách tính của {name}</h3>
      {f.parts.map((p) => (
        <div key={p.c.key} className="fn-line">
          <span>{p.c.name}</span>
          <span className="hint">{p.v == null ? 'thiếu' : `${num(p.v)} × ${p.c.weight}%`}</span>
          <b>{p.v == null ? '–' : num(p.weighted, 2)}</b>
        </div>
      ))}
      <div className={'fn-total' + (f.result === 'Đạt' ? '' : ' bad')}>
        <span>Điểm tổng kết</span>
        <b>{f.total == null ? 'Chưa đủ điều kiện' : `${num(f.raw, 2)} → ${num(f.total)}`}</b>
      </div>
      <p className="hint">
        Làm tròn một chữ số thập phân.{' '}
        {f.result === 'Đạt'
          ? `Đạt vì từ ${num(f.cfg.pass)} trở lên${crit.length ? ` và ${crit.map((p) => `${p.c.name.toLowerCase()} ${num(p.v!)}`).join(', ')} không dưới điểm liệt ${num(f.cfg.fail)}` : ''}.`
          : f.result === 'Không đạt · điểm liệt'
            ? `Không đạt vì ${late} dưới điểm liệt ${num(f.cfg.fail)}.`
            : f.result === 'Không đạt'
              ? `Không đạt vì dưới điểm đạt ${num(f.cfg.pass)}.`
              : `Thiếu ${f.missing.map((m) => m.c.name.toLowerCase()).join(', ')}.`}
      </p>
      {f.parts.some((p) => p.note) && <p className="hint">{f.parts.map((p) => p.note).filter(Boolean).join(' ')}</p>}
      <p className="fn-auto">Điểm tự tính lại mỗi khi một điểm thành phần thay đổi.</p>
    </aside>
  );
}

function LockDialog({ kind, lacking, onClose, onDone }: { kind: 'lock' | 'reopen' | 'request'; lacking: number; onClose: () => void; onDone: (reason: string) => void }) {
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const title = kind === 'lock' ? 'Chốt điểm môn này?' : kind === 'reopen' ? 'Mở lại điểm đã chốt' : 'Yêu cầu mở lại điểm';
  return (
    <Modal onClose={onClose} label={title}>
      <h3 style={{ marginBottom: 8 }}>{title}</h3>
      {kind === 'lock' ? (
        <p className="hint" style={{ marginBottom: 16 }}>
          Sau khi chốt, điểm thành phần bị khoá với giảng viên; chỉ Quản lý đào tạo mở lại được. {lacking ? `${lacking} học viên sẽ được chốt với kết quả Chưa đủ điều kiện tính.` : ''}
        </p>
      ) : (
        <div className="field">
          <label htmlFor="fn-reason">Lý do</label>
          <textarea id="fn-reason" className="inp" rows={2} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Ví dụ: nhập nhầm điểm giữa môn của 2 học viên" />
        </div>
      )}
      {error && <p className="err" style={{ marginBottom: 12 }}>{error}</p>}
      <div className="acts">
        <button type="button" className="btn" style={{ width: 90 }} onClick={onClose}>
          Huỷ
        </button>
        <button type="button" className="btn primary" onClick={() => (kind !== 'lock' && !reason.trim() ? setError('Nhập lý do') : onDone(reason.trim()))}>
          {kind === 'lock' ? 'Chốt điểm' : kind === 'reopen' ? 'Mở lại' : 'Gửi yêu cầu'}
        </button>
      </div>
    </Modal>
  );
}
