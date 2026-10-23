import type { RouteObject } from 'react-router-dom';
import { EnrollPage } from './EnrollPage';

export default [{ path: '/lop-hoc/:code/ghi-danh', element: <EnrollPage /> }] satisfies RouteObject[];
