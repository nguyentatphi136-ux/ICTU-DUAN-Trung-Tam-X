import type { RouteObject } from 'react-router-dom';
import { ErrorPage } from './ErrorPage';

// Đường dẫn của story này. App.tsx tự gom mọi pages/*/routes.tsx nên thêm story không phải sửa App.tsx.
export default [
  { path: '/loi/:code', element: <ErrorPage /> },
] satisfies RouteObject[];
