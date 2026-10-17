import { useState, type CSSProperties, type ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { useToast } from '../../components/Toast';
import { Pager, paginate, Pill, usePaging } from '../../components/ui';
import { findAssignment, lateHours, score, SUBMISSIONS, vdt, type Assignment, type Submission } from '../../data/assignments';
import { useAuth } from '../../data/auth';
import { findClass } from '../../data/classes';
import { TODAY } from '../../data/funnel';
import { notify } from '../../data/notifications';
import { saveAll } from '../../data/store';
import { rosterOn, STUDENTS, type Student } from '../../data/students';
import { ErrorPage } from '../S1-07-trang-loi/ErrorPage';
import { MODULE, teachable } from '../S5-05-bai-tap/AssignmentPages';
import './subs.css';

type Tab = 'Tất cả' | 'Chưa chấm' | 'Đã chấm' | 'Nộp muộn' | 'Chưa nộp';
const TABS: Tab[] = ['Tất cả', 'Chưa chấm', 'Đã chấm', 'Nộp muộn', 'Chưa nộp'];
export type SubRow = { st: Student; sub?: Submission; late: number };

/** Cả lớp kèm bài nộp (kể cả người chưa nộp), dùng chung cho S5-10, S6-01. */
export function rowsOf(a: Assignment): SubRow[] {
  const subs = SUBMISSIONS.filter((s) => s.assignment === a.id);
  const codes = new Set([...rosterOn(a.cls, TODAY).map((s) => s.code), ...subs.map((s) => s.student)]);
  return STUDENTS.filter((s) => codes.has(s.code)).map((st) => {
    const sub = subs.find((s) => s.student === st.code);
    return { st, sub, late: sub ? lateHours(a, sub.versions[0]) : 0 };
  });
}

const inTab = (r: SubRow, t: Tab) =>
  t === 'Tất cả' || (t === 'Chưa nộp' ? !r.sub : t === 'Nộp muộn' ? r.late > 0 : t === 'Đã chấm' ? r.sub?.state === 'Đã chấm' : r.sub?.state === 'Đã nộp');

type Props = {
  /** Cột thao tác thêm và ngăn chi tiết (S5-08 lịch sử nộp, S6-01 chấm bài). */
  action?: (a: Assignment, r: SubRow, refresh: () => void) => ReactNode;
};

// S5-10. Danh sách bài nộp của một bài tập: đủ danh sách lớp kể cả người chưa nộp, lọc Chưa chấm, Đã chấm, Nộp muộn,
// Chưa nộp; thanh tiến độ chấm; nhắc người chưa nộp trước khi chấm.
export function SubmissionsPage({ action }: Props) {
  const { id = '' } = useParams();
  const { user } = useAuth();
  const toast = useToast();
  const [tab, setTab] = useState<Tab>('Tất cả');
  const [, setTick] = useState(0);
  const { page, setPage, size, setSize } = usePaging(20);
  const a = findAssignment(Number(id));
  const k = findClass(a?.cls);
  if (!a || !k) return <ErrorPage code="404" />;
  if (!teachable(user?.active ?? -1, user!.name).includes(k)) return <ErrorPage code="403" />;
  const refresh = () => setTick((t) => t + 1);

  const rows = rowsOf(a).sort((x, y) => (x.sub ? 1 : 0) - (y.sub ? 1 : 0) || (y.sub?.versions[0].at ?? '').localeCompare(x.sub?.versions[0].at ?? ''));
  const count = (t: Tab) => rows.filter((r) => inTab(r, t)).length;
  const submitted = rows.filter((r) => r.sub);
  const graded = count('Đã chấm');
  const missing = rows.filter((r) => !r.sub);
  const view = paginate(
    rows.filter((r) => inTab(r, tab)),
    page,
    size,
  );

  function remind(list: SubRow[]) {
    notify({ at: TODAY + 'T' + new Date().toTimeString().slice(0, 5), kind: 'Bài tập', title: 'Nhắc nộp bài: ' + a!.title, body: `Hạn nộp ${vdt(a!.due)}. Bạn chưa nộp bài này.`, link: '/bai-tap-cua-toi/' + a!.id, to: { users: list.map((r) => r.st.name) } });
    saveAll();
    toast(`Đã gửi nhắc tới ${list.length} học viên`);
  }

  return (
    <AppLayout crumb={[MODULE, a.title.split(':')[0]]} module={MODULE}>
      <Link to="/bai-tap" className="back">
        <Icon name="back" size={16} />
        Bài tập
      </Link>
      <div className="h">
        <div>
          <h2>{a.title}</h2>
          <p>
            {a.cls} · {a.subject} · Hạn nộp {vdt(a.due)} · Điểm tối đa {a.max} · {a.late === 'Cho nộp' ? 'Cho nộp muộn' : 'Chặn nộp muộn'}
          </p>
        </div>
        <Link to={`/bai-tap/${a.id}/sua`} className="btn">
          Sửa bài tập
        </Link>
      </div>
      <div className="panel bn-progress">
        <div>
          <b>Tiến độ chấm</b>
          <span>
            Đã chấm {graded} / {submitted.length} bài đã nộp
          </span>
        </div>
        <i style={{ '--p': `${submitted.length ? (100 * graded) / submitted.length : 0}%` } as CSSProperties} />
        <small>
          {submitted.length} đã nộp ({count('Nộp muộn')} nộp muộn) · {missing.length} chưa nộp · tổng {rows.length} học viên
        </small>
      </div>
      <div className="bar bn-bar">
        <div className="seg bn-tabs" role="tablist">
          {TABS.map((t) => (
            <button key={t} type="button" role="tab" aria-selected={tab === t} className={tab === t ? 'on' : undefined} onClick={() => (setTab(t), setPage(1))}>
              {t} ({count(t)})
            </button>
          ))}
        </div>
        {missing.length > 0 && (
          <button type="button" className="btn" onClick={() => remind(missing)}>
            <Icon name="bell" size={16} />
            Nhắc {missing.length} bạn chưa nộp
          </button>
        )}
      </div>
      <div className="panel">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Học viên</th>
                <th>Thời điểm nộp</th>
                <th>Phiên bản</th>
                <th>Trạng thái</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {view.items.map((r) => (
                <tr key={r.st.code}>
                  <td>
                    <b className="strong">{r.st.name}</b>
                    <small className="lv-sub">{r.st.code}</small>
                  </td>
                  <td>
                    {r.sub ? (
                      <>
                        <span className="strong">
                          {vdt(r.sub.versions[0].at).slice(0, 5)} {r.sub.versions[0].at.slice(11, 16)}
                        </span>
                        <small className={'lv-sub ' + (r.late ? 'bad-t' : 'ok-t')}>{r.late ? `Nộp muộn trễ ${r.late} giờ` : 'Đúng hạn'}</small>
                      </>
                    ) : (
                      'Chưa nộp bài'
                    )}
                  </td>
                  <td className={r.sub ? 'code' : undefined}>{r.sub ? 'Bản ' + r.sub.versions[0].no : '–'}</td>
                  <td>
                    {!r.sub ? (
                      <Pill>Chưa nộp</Pill>
                    ) : r.sub.state === 'Đã chấm' ? (
                      <Pill tone="ok">Đã chấm · {score(r.sub.grade!.score)}</Pill>
                    ) : r.sub.state === 'Cần làm lại' ? (
                      <Pill tone="off">Cần làm lại</Pill>
                    ) : (
                      <Pill tone="wait">Chưa chấm</Pill>
                    )}
                  </td>
                  <td className="bn-act">
                    {!r.sub ? (
                      <button type="button" className="link" onClick={() => remind([r])}>
                        Gửi nhắc
                      </button>
                    ) : (
                      action?.(a, r, refresh)
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Pager page={view.page} pages={view.pages} onPage={setPage} size={size} onSize={setSize}>
          Đang hiện {view.from}–{view.to} trong {count(tab)} học viên
        </Pager>
      </div>
    </AppLayout>
  );
}
