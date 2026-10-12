import type { RouteObject } from 'react-router-dom';
import { MatrixPage } from './MatrixPage';

export default [{ path: '/chuyen-can/:cls', element: <MatrixPage /> }] satisfies RouteObject[];
