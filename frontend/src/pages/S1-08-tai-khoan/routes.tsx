import type { RouteObject } from 'react-router-dom';
import { UserListPage } from './UserListPage';

// Đường dẫn của story này. App.tsx tự gom mọi pages/*/routes.tsx nên thêm story không phải sửa App.tsx.
export default [
  { path: '/quan-tri/tai-khoan', element: <UserListPage /> },
] satisfies RouteObject[];
