import type { RouteObject } from 'react-router-dom';
import { AssignmentEditorPage, AssignmentListPage } from './AssignmentPages';

export default [
  { path: '/bai-tap', element: <AssignmentListPage /> },
  { path: '/bai-tap/moi', element: <AssignmentEditorPage /> },
  { path: '/bai-tap/:id/sua', element: <AssignmentEditorPage /> },
] satisfies RouteObject[];
