import type { RouteObject } from 'react-router-dom';
import { TrashPage } from './TrashPage';

// Thùng rác theo từng module (nhận xét mentor S2: xoá mềm, khôi phục, xoá vĩnh viễn).
export default [
  { path: '/dao-tao/thung-rac', element: <TrashPage area="training" /> },
  { path: '/quan-tri/thung-rac', element: <TrashPage area="users" /> },
  { path: '/tuyen-sinh/thung-rac', element: <TrashPage area="leads" /> },
] satisfies RouteObject[];
