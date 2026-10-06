import type { RouteObject } from 'react-router-dom';
import { LoginPage } from './LoginPage';

// Đường dẫn của story này. App.tsx tự gom mọi pages/*/routes.tsx nên thêm story không phải sửa App.tsx.
export default [
  { path: '/dang-nhap', element: <LoginPage /> },
] satisfies RouteObject[];
