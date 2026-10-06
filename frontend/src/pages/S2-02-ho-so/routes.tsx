import type { RouteObject } from 'react-router-dom';
import { ProfilePage } from './ProfilePage';

// Đường dẫn của story này. App.tsx tự gom mọi pages/*/routes.tsx nên thêm story không phải sửa App.tsx.
export default [
  { path: '/ho-so', element: <ProfilePage /> },
] satisfies RouteObject[];
