import type { RouteObject } from 'react-router-dom';
import { StudentListPage } from './StudentListPage';
import { StudentProfilePage } from './StudentProfilePage';

export default [
  { path: '/hoc-vien', element: <StudentListPage /> },
  { path: '/hoc-vien/:code', element: <StudentProfilePage /> },
] satisfies RouteObject[];
