import type { RouteObject } from 'react-router-dom';
import { ClassGradePage, ProgramGradePage } from './GradeConfigPages';

export default [
  { path: '/dao-tao/cach-tinh-diem', element: <ProgramGradePage /> },
  { path: '/lop-hoc/:cls/cach-tinh-diem', element: <ClassGradePage /> },
] satisfies RouteObject[];
