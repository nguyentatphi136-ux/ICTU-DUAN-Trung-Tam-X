import { useRoutes, type RouteObject } from 'react-router-dom';
import { ErrorPage } from './pages/S1-07-trang-loi/ErrorPage';
import { StoryIndexPage } from './pages/StoryIndexPage';

// Mỗi story là một thư mục trong pages/, tự khai báo đường dẫn ở routes.tsx. Vite gom tất cả lúc build,
// nên mỗi nhánh feature/<thư mục>_NPPL chỉ thêm thư mục của mình mà không phải sửa tệp này.
// S1-07 (trang lỗi) là nền: AppLayout và trang 404 dùng nó, nên mọi nhánh đều có.
// S1-09, S1-10, S2-03, S2-10, S2-11 là thành phần con của trang cha nên không có thư mục riêng.
const storyRoutes = Object.values(import.meta.glob<{ default: RouteObject[] }>('./pages/*/routes.tsx', { eager: true })).flatMap((m) => m.default);

export function App() {
  return useRoutes([...storyRoutes, { path: '/man-hinh', element: <StoryIndexPage /> }, { path: '*', element: <ErrorPage code="404" /> }]);
}
