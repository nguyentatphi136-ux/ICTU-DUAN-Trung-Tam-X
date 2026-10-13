import type { RouteObject } from 'react-router-dom';
import { ComposeNoticePage, NoticeListPage } from './ClassNoticePages';

export default [
  { path: '/thong-bao-lop', element: <NoticeListPage /> },
  { path: '/thong-bao-lop/soan', element: <ComposeNoticePage /> },
  { path: '/thong-bao-lop/:id', element: <NoticeListPage /> },
] satisfies RouteObject[];
