import { Route, Routes } from 'react-router-dom';
import { LoginPage } from './pages/S1-01-dang-nhap/LoginPage';
import { SessionDemoPage } from './pages/S1-02-phien-dang-nhap/SessionDemoPage';
import { ForgotPasswordPage } from './pages/S1-03-quen-mat-khau/ForgotPasswordPage';
import { ChangePasswordPage } from './pages/S1-04-doi-mat-khau/ChangePasswordPage';
import { PermissionMatrixPage } from './pages/S1-05-phan-quyen/PermissionMatrixPage';
import { HomePage } from './pages/S1-06-layout-menu/HomePage';
import { ErrorPage } from './pages/S1-07-trang-loi/ErrorPage';
import { UserListPage } from './pages/S1-08-tai-khoan/UserListPage';
import { StoryIndexPage } from './pages/StoryIndexPage';

// Bảng định tuyến. Mỗi story S1 nằm trong một thư mục riêng, đánh số theo backlog.
// S1-09 (gán vai trò) và S1-10 (khoá tài khoản) là thành phần con của trang S1-08.
export function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/s1" element={<StoryIndexPage />} />
      <Route path="/dang-nhap" element={<LoginPage />} />
      <Route path="/phien" element={<SessionDemoPage />} />
      <Route path="/quen-mat-khau" element={<ForgotPasswordPage />} />
      <Route path="/doi-mat-khau" element={<ChangePasswordPage />} />
      <Route path="/quan-tri/phan-quyen" element={<PermissionMatrixPage />} />
      <Route path="/quan-tri/tai-khoan" element={<UserListPage />} />
      <Route path="/loi/:code" element={<ErrorPage />} />
      <Route path="*" element={<ErrorPage code="404" />} />
    </Routes>
  );
}
