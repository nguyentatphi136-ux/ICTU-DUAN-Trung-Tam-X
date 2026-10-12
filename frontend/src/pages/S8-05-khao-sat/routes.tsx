import type { RouteObject } from 'react-router-dom';
import { SurveyPage } from './SurveyPages';

export default [{ path: '/khao-sat', element: <SurveyPage /> }] satisfies RouteObject[];
