import type { RouteObject } from 'react-router-dom';
import { ChangePasswordPage } from './ChangePasswordPage';

// Đường dẫn của story này. App.tsx tự gom mọi pages/*/routes.tsx nên thêm story không phải sửa App.tsx.
export default [
  { path: '/doi-mat-khau', element: <ChangePasswordPage /> },
] satisfies RouteObject[];
