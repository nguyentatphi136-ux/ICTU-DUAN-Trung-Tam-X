import { useParams, type RouteObject } from 'react-router-dom';
import { GradePage } from './GradePage';

/** Đổi học viên thì dựng lại trang để ô điểm, nhận xét lấy đúng dữ liệu của bài mới. */
function Keyed() {
  return <GradePage key={useParams().student} />;
}

export default [{ path: '/bai-tap/:id/cham/:student', element: <Keyed /> }] satisfies RouteObject[];
