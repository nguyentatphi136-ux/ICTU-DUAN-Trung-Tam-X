import type { RouteObject } from 'react-router-dom';
import { ClassDetailPage } from './ClassDetailPage';

export default [{ path: '/lop-hoc/:code/:tab?', element: <ClassDetailPage /> }] satisfies RouteObject[];
