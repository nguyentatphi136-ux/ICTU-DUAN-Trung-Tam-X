import type { RouteObject } from 'react-router-dom';
import { SessionDemoPage } from './SessionDemoPage';

// Đường dẫn của story này. App.tsx tự gom mọi pages/*/routes.tsx nên thêm story không phải sửa App.tsx.
export default [
  { path: '/phien', element: <SessionDemoPage /> },
] satisfies RouteObject[];
