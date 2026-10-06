import type { RouteObject } from 'react-router-dom';
import { ImportUsersPage } from './ImportUsersPage';

// Đường dẫn của story này. App.tsx tự gom mọi pages/*/routes.tsx nên thêm story không phải sửa App.tsx.
export default [
  { path: '/quan-tri/nhap-excel', element: <ImportUsersPage /> },
] satisfies RouteObject[];
