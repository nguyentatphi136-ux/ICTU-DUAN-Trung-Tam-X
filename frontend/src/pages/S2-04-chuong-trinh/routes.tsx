import type { RouteObject } from 'react-router-dom';
import { ProgramListPage } from './ProgramListPage';

// Đường dẫn của story này. App.tsx tự gom mọi pages/*/routes.tsx nên thêm story không phải sửa App.tsx.
export default [
  { path: '/dao-tao/chuong-trinh', element: <ProgramListPage /> },
] satisfies RouteObject[];
