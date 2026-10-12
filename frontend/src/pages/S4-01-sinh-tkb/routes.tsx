import type { RouteObject } from 'react-router-dom';
import { GeneratePage } from './GeneratePage';

export default [{ path: '/lop-hoc/:code/sinh-lich', element: <GeneratePage /> }] satisfies RouteObject[];
