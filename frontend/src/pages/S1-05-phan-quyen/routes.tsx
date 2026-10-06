import type { RouteObject } from 'react-router-dom';
import { PermissionMatrixPage } from './PermissionMatrixPage';

// Đường dẫn của story này. App.tsx tự gom mọi pages/*/routes.tsx nên thêm story không phải sửa App.tsx.
export default [
  { path: '/quan-tri/phan-quyen', element: <PermissionMatrixPage /> },
] satisfies RouteObject[];
