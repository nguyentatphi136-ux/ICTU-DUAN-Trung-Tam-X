import type { RouteObject } from 'react-router-dom';
import { ReportsPage } from './ReportsPage';

export default [
  { path: '/bao-cao', element: <ReportsPage /> },
  { path: '/bao-cao/:report', element: <ReportsPage /> },
] satisfies RouteObject[];
