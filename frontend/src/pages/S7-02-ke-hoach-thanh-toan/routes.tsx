import type { RouteObject } from 'react-router-dom';
import { BulkPlanPage, PlanListPage, PlanPage } from './PlanPages';

export default [
  { path: '/hoc-phi/ke-hoach', element: <PlanListPage /> },
  { path: '/hoc-phi/ke-hoach/lop/:cls', element: <BulkPlanPage /> },
  { path: '/hoc-phi/ke-hoach/:code', element: <PlanPage /> },
] satisfies RouteObject[];
