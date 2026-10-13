import type { RouteObject } from 'react-router-dom';
import { PersonalSettingsPage, ReminderSettingsPage } from './ReminderPages';

export default [
  { path: '/nhac-lich', element: <ReminderSettingsPage /> },
  { path: '/cai-dat', element: <PersonalSettingsPage /> },
] satisfies RouteObject[];
