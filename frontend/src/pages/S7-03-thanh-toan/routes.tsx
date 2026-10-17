import type { RouteObject } from 'react-router-dom';
import { ReceiptPage, StudentFeePage } from './PaymentPages';

export default [
  { path: '/hoc-phi/cong-no/:code', element: <StudentFeePage /> },
  { path: '/hoc-phi/bien-lai/:id', element: <ReceiptPage /> },
] satisfies RouteObject[];
