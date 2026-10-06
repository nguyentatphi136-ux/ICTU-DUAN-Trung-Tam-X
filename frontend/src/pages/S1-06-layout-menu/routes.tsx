import type { RouteObject } from 'react-router-dom';
import { HomePage, ModulePage } from './HomePage';

// Đường dẫn của story này. App.tsx tự gom mọi pages/*/routes.tsx nên thêm story không phải sửa App.tsx.
export default [
  { path: '/', element: <HomePage /> },
  { path: '/module/:name', element: <ModulePage /> },
] satisfies RouteObject[];
