import type { RouteObject } from 'react-router-dom';
import { SubmissionsPage } from './SubmissionsPage';

export default [{ path: '/bai-tap/:id', element: <SubmissionsPage /> }] satisfies RouteObject[];
