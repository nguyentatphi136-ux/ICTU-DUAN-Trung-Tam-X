import type { RouteObject } from 'react-router-dom';
import { ConsultationPage } from './ConsultationPage';

// Đường dẫn của story này. App.tsx tự gom mọi pages/*/routes.tsx nên thêm story không phải sửa App.tsx.
export default [
  { path: '/dang-ky-tu-van', element: <ConsultationPage /> },
] satisfies RouteObject[];
