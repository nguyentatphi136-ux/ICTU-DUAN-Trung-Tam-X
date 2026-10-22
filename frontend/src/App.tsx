import { Fragment, useSyncExternalStore } from 'react';
import { useRoutes, type RouteObject } from 'react-router-dom';
import { sharedVersion, subscribeShared } from './data/shared';
import { ErrorPage } from './pages/S1-07-trang-loi/ErrorPage';
import { StoryIndexPage } from './pages/StoryIndexPage';

// Mỗi story là một thư mục trong pages/, tự khai báo đường dẫn ở routes.tsx. Vite gom tất cả lúc build,
// nên mỗi nhánh feature/<thư mục>_NPPL chỉ thêm thư mục của mình mà không phải sửa tệp này.
// S1-07 (trang lỗi) là nền: AppLayout và trang 404 dùng nó, nên mọi nhánh đều có.
// S1-09, S1-10, S2-03, S2-11 là thành phần con của trang cha nên không có thư mục riêng.
const storyRoutes = Object.values(import.meta.glob<{ default: RouteObject[] }>('./pages/*/routes.tsx', { eager: true })).flatMap((m) => m.default);

export function App() {
  // Dữ liệu dùng chung (data/shared.ts) vừa tải xong thì dựng lại trang đang mở để mọi menu hiện cùng một bản.
  const version = useSyncExternalStore(subscribeShared, sharedVersion);
  const page = useRoutes([...storyRoutes, { path: '/man-hinh', element: <StoryIndexPage /> }, { path: '*', element: <ErrorPage code="404" /> }]);
  return <Fragment key={version}>{page}</Fragment>;
}
