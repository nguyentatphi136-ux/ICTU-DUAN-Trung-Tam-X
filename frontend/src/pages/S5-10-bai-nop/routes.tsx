import type { RouteObject } from 'react-router-dom';
import { HistoryAction } from '../S5-08-phien-ban/Versions';
import { SubmissionsPage } from './SubmissionsPage';

// S5-08: cột thao tác mở lịch sử nộp.
export default [{ path: '/bai-tap/:id', element: <SubmissionsPage action={(a, r, refresh) => <HistoryAction a={a} r={r} onChange={refresh} />} /> }] satisfies RouteObject[];
