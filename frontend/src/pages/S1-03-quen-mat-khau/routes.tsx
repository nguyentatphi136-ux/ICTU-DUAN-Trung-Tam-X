import type { RouteObject } from 'react-router-dom';
import { ForgotPasswordPage, ResetPasswordPage } from './ForgotPasswordPage';

// Đường dẫn của story này. App.tsx tự gom mọi pages/*/routes.tsx nên thêm story không phải sửa App.tsx.
export default [
  { path: '/quen-mat-khau', element: <ForgotPasswordPage /> },
  { path: '/dat-lai-mat-khau', element: <ResetPasswordPage /> },
] satisfies RouteObject[];
