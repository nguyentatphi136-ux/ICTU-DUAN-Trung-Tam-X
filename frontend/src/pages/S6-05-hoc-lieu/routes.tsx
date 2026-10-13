import type { RouteObject } from 'react-router-dom';
import { MaterialsPage } from './MaterialsPage';

export default [
  { path: '/hoc-lieu', element: <MaterialsPage /> },
  { path: '/hoc-lieu/:cls', element: <MaterialsPage /> },
] satisfies RouteObject[];
