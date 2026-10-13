import type { RouteObject } from 'react-router-dom';
import { TranscriptPage, TranscriptPrintPage } from './TranscriptPages';

export default [
  { path: '/bang-diem', element: <TranscriptPage /> },
  { path: '/bang-diem/:code', element: <TranscriptPage /> },
  { path: '/bang-diem/:code/in', element: <TranscriptPrintPage /> },
] satisfies RouteObject[];
