import type { RouteObject } from 'react-router-dom';
import { useAuth } from '../../data/auth';
import { ConsultationPage } from '../S2-08-dang-ky-tu-van/ConsultationPage';
import { HomePage, ModulePage } from './HomePage';

// Mở web khi chưa đăng nhập thì vào trang Đăng ký tư vấn (S2-08), đã đăng nhập thì vào trang chủ theo vai trò.
function Root() {
  return useAuth().user ? <HomePage /> : <ConsultationPage />;
}

// Đường dẫn của story này. App.tsx tự gom mọi pages/*/routes.tsx nên thêm story không phải sửa App.tsx.
export default [
  { path: '/', element: <Root /> },
  { path: '/module/:name', element: <ModulePage /> },
] satisfies RouteObject[];
