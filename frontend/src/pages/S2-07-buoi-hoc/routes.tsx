import type { RouteObject } from 'react-router-dom';
import { SessionsPage } from './SessionsPage';

// Đường dẫn của story này. App.tsx tự gom mọi pages/*/routes.tsx nên thêm story không phải sửa App.tsx.
export default [
  { path: '/dao-tao/mon-hoc/:code', element: <SessionsPage /> },
] satisfies RouteObject[];
