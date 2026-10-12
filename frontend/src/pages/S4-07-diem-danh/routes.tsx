import type { RouteObject } from 'react-router-dom';
import { AttendanceListPage, TakeAttendancePage } from './AttendancePage';

export default [
  { path: '/diem-danh', element: <AttendanceListPage /> },
  { path: '/diem-danh/:id', element: <TakeAttendancePage /> },
] satisfies RouteObject[];
