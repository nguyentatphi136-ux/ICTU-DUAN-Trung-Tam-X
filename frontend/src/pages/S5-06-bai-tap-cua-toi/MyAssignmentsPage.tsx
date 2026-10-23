import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Pill } from '../../components/ui';
import { ASSIGNMENTS, lateHours, score, stateOf, subOf, vdt, type Assignment, type MyState } from '../../data/assignments';
import { useAuth } from '../../data/auth';
import { TODAY } from '../../data/funnel';
import { findSession } from '../../data/schedule';
import { useMe } from '../S4-05-lich-cua-toi/MySchedulePage';
import { MODULE } from '../S5-05-bai-tap/AssignmentPages';
import './mine.css';

/** Thời điểm hiện tại theo ngày dữ liệu mẫu và giờ máy. */
export const nowIso = () => TODAY + 'T' + new Date().toTimeString().slice(0, 5);

/** Hạn đang áp dụng: hạn nộp lại nếu bị yêu cầu làm lại, còn lại là hạn của bài. */
export const dueOf = (a: Assignment, student: string) => subOf(a.id, student)?.redo?.due ?? a.due;

/** "Còn 5 giờ", "Còn 3 ngày", "Quá hạn 2 ngày" kèm sắc độ: đỏ dưới 24 giờ, vàng dưới 4 ngày. */
export function remaining(due: string): [string, 'off' | 'wait' | undefined] {
  const ms = Date.parse(due + ':00') - Date.parse(nowIso() + ':00');
  const h = Math.floor(ms / 3600000);
  if (ms < 0) return [`Quá hạn ${Math.ceil(-ms / 86400000)} ngày`, 'off'];
  if (h < 24) return [h < 1 ? 'Còn dưới 1 giờ' : `Còn ${h} giờ`, 'off'];
  const d = Math.floor(h / 24);
  return [`Còn ${d} ngày`, d < 4 ? 'wait' : undefined];
}

const GROUPS: MyState[] = ['Cần làm lại', 'Chưa nộp', 'Đã nộp', 'Đã chấm'];


// S5-06. Học viên xem bài tập của mình theo nhóm Cần làm lại, Chưa nộp, Đã nộp, Đã chấm; mỗi thẻ hiện thời gian còn lại
// tới hạn nộp, bài sắp hết hạn được làm nổi bật. Mặc định sắp theo hạn nộp gần nhất.
export function MyAssignmentsPage() {
  const { user } = useAuth();
  const me = useMe();
  const [tab, setTab] = useState<MyState | ''>('');
  if (user && user.active !== 0) return <Navigate to="/bai-tap" replace />;
  const list = ASSIGNMENTS.filter((a) => a.status === 'Đã giao' && a.cls === me?.cls).sort((a, b) => dueOf(a, me!.code).localeCompare(dueOf(b, me!.code)));
  const by = (g: MyState) => list.filter((a) => stateOf(a, me!.code) === g);

  return (
    <AppLayout crumb={[MODULE, 'Bài tập của tôi']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Bài tập của tôi</h2>
          <p>{me?.cls ? `Lớp ${me.cls} · sắp xếp theo hạn nộp gần nhất` : 'Bạn chưa được xếp lớp'}</p>
        </div>
      </div>
      <div className="seg ma-tabs" role="tablist">
        <button type="button" role="tab" aria-selected={!tab} className={!tab ? 'on' : undefined} onClick={() => setTab('')}>
          Tất cả ({list.length})
        </button>
        {(['Chưa nộp', 'Đã nộp', 'Đã chấm', 'Cần làm lại'] as MyState[]).map((g) => (
          <button key={g} type="button" role="tab" aria-selected={tab === g} className={tab === g ? 'on' : undefined} onClick={() => setTab(g)}>
            {g} ({by(g).length})
          </button>
        ))}
      </div>
      {GROUPS.filter((g) => (!tab || tab === g) && by(g).length).map((g) =>
        g === 'Đã chấm' && !tab ? (
          <section key={g} className="ma-group">
            <h3 className="section-t">Đã chấm ({by(g).length})</h3>
            <p className="ma-done">
              {by(g).map((a, i) => (
                <Link key={a.id} to={'/bai-tap-cua-toi/' + a.id}>
                  {i > 0 && <span> · </span>}
                  {a.title.split(':')[0]} · {score(subOf(a.id, me!.code)!.grade!.score)}
                </Link>
              ))}
            </p>
          </section>
        ) : (
          <section key={g} className="ma-group">
            <h3 className={'section-t' + (g === 'Cần làm lại' ? ' bad-t' : '')}>{g}</h3>
            <div className="ma-cards">
              {by(g).map((a) => (
                <Card key={a.id} a={a} code={me!.code} state={g} />
              ))}
            </div>
          </section>
        ),
      )}
      {!list.length && <div className="dash">Lớp của bạn chưa có bài tập nào.</div>}
    </AppLayout>
  );
}

function Card({ a, code, state }: { a: Assignment; code: string; state: MyState }) {
  const sub = subOf(a.id, code);
  const v = sub?.versions[0];
  const due = dueOf(a, code);
  const [left, tone] = remaining(due);
  const s = a.session ? findSession(a.session) : undefined;
  const late = v ? lateHours(a, v) : 0;
  return (
    <Link to={'/bai-tap-cua-toi/' + a.id} className={'panel ma-card' + (tone === 'off' && state !== 'Đã nộp' && state !== 'Đã chấm' ? ' urgent' : '')}>
      <div className="ma-top">
        <b>{a.title}</b>
        {state === 'Đã nộp' ? late ? <Pill tone="off">Nộp muộn {late} giờ</Pill> : <Pill tone="info">Đã nộp</Pill> : state === 'Đã chấm' ? <Pill tone="ok">{score(sub!.grade!.score)}</Pill> : <Pill tone={tone}>{left}</Pill>}
      </div>
      <small>
        {a.subject}
        {state === 'Cần làm lại' ? ' · giảng viên yêu cầu sửa và nộp lại' : v ? ` · nộp ${vdt(v.at).slice(0, 5)} ${v.at.slice(11, 16)} · bản ${v.no}` : s ? ` · Buổi ${s.no}` : ''}
      </small>
      <div className="ma-foot">
        <span>
          {state === 'Đã nộp' ? 'Chờ giảng viên chấm' : `${state === 'Cần làm lại' ? 'Hạn nộp lại' : 'Hạn nộp'} ${due.slice(0, 10) === TODAY ? 'hôm nay ' + due.slice(11) : vdt(due)}`}
        </span>
        <em>{state === 'Chưa nộp' ? 'Nộp bài' : state === 'Cần làm lại' ? 'Nộp lại' : 'Xem bài nộp'}</em>
      </div>
    </Link>
  );
}
