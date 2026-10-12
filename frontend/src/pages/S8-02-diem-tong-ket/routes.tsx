import type { RouteObject } from 'react-router-dom';
import { FinalListPage, FinalPage } from './FinalPages';

export default [
  { path: '/diem-tong-ket', element: <FinalListPage /> },
  { path: '/diem-tong-ket/:cls/:subject', element: <FinalPage /> },
] satisfies RouteObject[];
