import type { RouteObject } from 'react-router-dom';
import { LeadListPage } from './LeadListPage';

// Đường dẫn của story này. App.tsx tự gom mọi pages/*/routes.tsx nên thêm story không phải sửa App.tsx.
export default [
  { path: '/tuyen-sinh/lead', element: <LeadListPage /> },
] satisfies RouteObject[];
