import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { Icon } from '../../components/Icon';
import { Pager, paginate, Pill, usePaging } from '../../components/ui';
import { heldSessions, markTone, sheetOf, statOf, thresholdOf, type Mark } from '../../data/attendance';
import { useAuth } from '../../data/auth';
import { findClass, totalSessions } from '../../data/classes';
import { shortDate } from '../../data/schedule';
import { useMe } from '../S4-05-lich-cua-toi/MySchedulePage';
import { MODULE } from '../S4-07-diem-danh/AttendancePage';
import './mine.css';

type Tab = 'all' | 'absent' | 'late';

// S5-03. Học viên xem lịch sử điểm danh của mình: danh sách buổi kèm trạng thái, tỷ lệ chuyên cần hiện tại,
// số buổi còn được vắng. Khi tiến gần ngưỡng (còn 3 buổi trở xuống) trang hiện cảnh báo và nút gửi đơn xin phép.
export function MyAttendancePage() {
  const { user } = useAuth();
  const me = useMe();
  const [tab, setTab] = useState<Tab>('all');
  const { page, setPage, size, setSize } = usePaging(10);
  if (user && user.active !== 0) return <Navigate to="/diem-danh" replace />;
  const k = findClass(me?.cls);
  if (!me || !k)
    return (
      <AppLayout crumb={[MODULE, 'Điểm danh của tôi']} module={MODULE}>
        <div className="dash">Bạn chưa được xếp lớp nên chưa có điểm danh.</div>
      </AppLayout>
    );

  const total = totalSessions(k);
  const a = statOf(me.code, k.code, k.program, total);
  const rows = heldSessions(k.code)
    .map((s) => ({ s, m: sheetOf(s)?.marks[me.code] as Mark | undefined, note: sheetOf(s)?.notes[me.code] }))
    .filter((r) => r.m)
    .reverse();
  const absent = rows.filter((r) => r.m === 'Vắng có phép' || r.m === 'Vắng không phép');
  const late = rows.filter((r) => r.m === 'Đi muộn');
  const list = tab === 'absent' ? absent : tab === 'late' ? late : rows;
  const view = paginate(list, page, size);
  const near = a.over || a.left <= 3;
  const pick = (t: Tab) => () => (setTab(t), setPage(1));

  return (
    <AppLayout crumb={[MODULE, 'Điểm danh của tôi']} module={MODULE}>
      <div className="h">
        <div>
          <h2>Điểm danh của tôi</h2>
          <p>
            Lớp {k.code} · đã học {a.held}/{total} buổi
          </p>
        </div>
      </div>
      <div className="stats mn-stats">
        <div className="stat">
          <b className={a.over ? 'bad' : near ? 'warn' : undefined}>{a.rate}%</b>
          <span>Tỷ lệ chuyên cần hiện tại</span>
        </div>
        <div className="stat">
          <b>{a.absent} buổi</b>
          <span>
            Đã vắng ({a.excused} có phép, {a.unexcused} không phép)
          </span>
        </div>
        <div className="stat">
          <b className={near ? 'bad' : undefined}>{Math.max(0, a.left)} buổi</b>
          <span>{a.over ? 'Đã vượt ngưỡng vắng' : 'Còn được vắng trước khi bị cảnh cáo'}</span>
        </div>
        <div className="stat mn-th">
          <span>Ngưỡng vắng</span>
          <b>
            {a.absent} / {a.max}
          </b>
          <span>
            {thresholdOf(k.program)}% của {total} buổi
          </span>
        </div>
      </div>
      {near && (
        <div className={'box ' + (a.over ? 'bad' : 'warn') + ' mn-warn'}>
          <Icon name="alert" size={20} />
          <div>
            <b>{a.over ? 'Bạn đã vượt ngưỡng vắng' : 'Bạn đang tiến gần ngưỡng vắng'}</b>
            {a.over
              ? `Bạn đã vắng ${a.absent} buổi, quá ngưỡng ${a.max} buổi. Hãy liên hệ giảng viên hoặc Quản lý đào tạo.`
              : `Vắng thêm ${a.left + 1} buổi nữa bạn sẽ vượt ngưỡng ${a.max} buổi và bị cảnh cáo. Nếu có lý do chính đáng, hãy gửi đơn xin phép trước buổi học.`}
          </div>
          <Link to="/xin-phep" className="btn">
            Gửi đơn xin phép
          </Link>
        </div>
      )}
      <div className="seg mn-tabs" role="tablist">
        <button type="button" role="tab" aria-selected={tab === 'all'} className={tab === 'all' ? 'on' : undefined} onClick={pick('all')}>
          Tất cả ({rows.length})
        </button>
        <button type="button" role="tab" aria-selected={tab === 'absent'} className={tab === 'absent' ? 'on' : undefined} onClick={pick('absent')}>
          Vắng ({absent.length})
        </button>
        <button type="button" role="tab" aria-selected={tab === 'late'} className={tab === 'late' ? 'on' : undefined} onClick={pick('late')}>
          Đi muộn ({late.length})
        </button>
      </div>
      <div className="panel">
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Buổi</th>
                <th>Ngày</th>
                <th>Bài học</th>
                <th>Trạng thái</th>
                <th>Ghi chú</th>
              </tr>
            </thead>
            <tbody>
              {view.items.map(({ s, m, note }) => (
                <tr key={s.id}>
                  <td className="strong">{s.no}</td>
                  <td className="strong">{shortDate(s.date)}</td>
                  <td>
                    {s.subject} · Bài {s.lesson}
                  </td>
                  <td>
                    <Pill tone={markTone(m)}>{m}</Pill>
                  </td>
                  <td>{note ?? ''}</td>
                </tr>
              ))}
              {!view.items.length && (
                <tr>
                  <td colSpan={5} className="empty">
                    Không có buổi nào.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pager page={view.page} pages={view.pages} onPage={setPage} size={size} onSize={setSize}>
          Đang hiện {view.from}–{view.to} trong {list.length} buổi
        </Pager>
      </div>
    </AppLayout>
  );
}
