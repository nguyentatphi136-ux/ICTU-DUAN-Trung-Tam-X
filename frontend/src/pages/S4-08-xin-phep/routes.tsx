import type { RouteObject } from 'react-router-dom';
import { ApproveLeavesPage, MyLeavesPage } from './LeavePages';

export default [
  { path: '/xin-phep', element: <MyLeavesPage /> },
  { path: '/duyet-phep', element: <ApproveLeavesPage /> },
] satisfies RouteObject[];
