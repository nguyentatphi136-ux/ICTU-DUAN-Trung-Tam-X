import type { RouteObject } from 'react-router-dom';
import { ExportButton } from '../S5-04-xuat-chuyen-can/ExportButton';
import { MatrixPage } from './MatrixPage';

// S5-04 thêm nút Xuất Excel ở đầu bảng chuyên cần.
export default [{ path: '/chuyen-can/:cls', element: <MatrixPage head={(rows, filtered) => <ExportButton rows={rows} filtered={filtered} />} /> }] satisfies RouteObject[];
