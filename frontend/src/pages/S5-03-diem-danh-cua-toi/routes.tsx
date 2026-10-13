import type { RouteObject } from 'react-router-dom';
import { MyAttendancePage } from './MyAttendancePage';

export default [{ path: '/diem-danh-cua-toi', element: <MyAttendancePage /> }] satisfies RouteObject[];
