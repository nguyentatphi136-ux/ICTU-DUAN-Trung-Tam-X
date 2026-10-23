import { Link, type RouteObject } from 'react-router-dom';
import { HistoryAction } from '../S5-08-phien-ban/Versions';
import { SubmissionsPage } from './SubmissionsPage';

// Cột thao tác: S5-08 mở lịch sử nộp, S6-01 mở màn hình chấm.
export default [
  {
    path: '/bai-tap/:id',
    element: (
      <SubmissionsPage
        action={(a, r, refresh) => (
          <>
            <HistoryAction a={a} r={r} onChange={refresh} />
            <Link to={`/bai-tap/${a.id}/cham/${r.st.code}`} className="link">
              {r.sub?.state === 'Đã chấm' ? 'Xem bài và điểm' : 'Chấm bài'}
            </Link>
          </>
        )}
      />
    ),
  },
] satisfies RouteObject[];
