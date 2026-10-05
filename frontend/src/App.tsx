import { Route, Routes } from 'react-router-dom';
import { LoginPage } from './pages/S1-01-dang-nhap/LoginPage';
import { SessionDemoPage } from './pages/S1-02-phien-dang-nhap/SessionDemoPage';
import { ForgotPasswordPage, ResetPasswordPage } from './pages/S1-03-quen-mat-khau/ForgotPasswordPage';
import { ChangePasswordPage } from './pages/S1-04-doi-mat-khau/ChangePasswordPage';
import { PermissionMatrixPage } from './pages/S1-05-phan-quyen/PermissionMatrixPage';
import { HomePage, ModulePage } from './pages/S1-06-layout-menu/HomePage';
import { ErrorPage } from './pages/S1-07-trang-loi/ErrorPage';
import { UserListPage } from './pages/S1-08-tai-khoan/UserListPage';
import { ImportUsersPage } from './pages/S2-01-nhap-excel/ImportUsersPage';
import { ProfilePage } from './pages/S2-02-ho-so/ProfilePage';
import { ProgramListPage } from './pages/S2-04-chuong-trinh/ProgramListPage';
import { SubjectListPage } from './pages/S2-05-mon-hoc/SubjectListPage';
import { CurriculumPage } from './pages/S2-06-gan-mon/CurriculumPage';
import { SessionsPage } from './pages/S2-07-buoi-hoc/SessionsPage';
import { ConsultationPage } from './pages/S2-08-dang-ky-tu-van/ConsultationPage';
import { LeadListPage } from './pages/S2-09-lead/LeadListPage';
import { StoryIndexPage } from './pages/StoryIndexPage';

// Bảng định tuyến. Mỗi story nằm trong một thư mục riêng, đánh số theo backlog.
// S1-09 (gán vai trò), S1-10 (khoá), S2-03 (ảnh đại diện), S2-10 (phân công), S2-11 (tìm lead) là thành phần con của trang cha.
export function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/man-hinh" element={<StoryIndexPage />} />
      <Route path="/dang-nhap" element={<LoginPage />} />
      <Route path="/phien" element={<SessionDemoPage />} />
      <Route path="/quen-mat-khau" element={<ForgotPasswordPage />} />
      <Route path="/dat-lai-mat-khau" element={<ResetPasswordPage />} />
      <Route path="/doi-mat-khau" element={<ChangePasswordPage />} />
      <Route path="/ho-so" element={<ProfilePage />} />
      <Route path="/quan-tri/phan-quyen" element={<PermissionMatrixPage />} />
      <Route path="/quan-tri/tai-khoan" element={<UserListPage />} />
      <Route path="/quan-tri/nhap-excel" element={<ImportUsersPage />} />
      <Route path="/dao-tao/chuong-trinh" element={<ProgramListPage />} />
      <Route path="/dao-tao/chuong-trinh/:code" element={<CurriculumPage />} />
      <Route path="/dao-tao/mon-hoc" element={<SubjectListPage />} />
      <Route path="/dao-tao/mon-hoc/:code" element={<SessionsPage />} />
      <Route path="/dang-ky-tu-van" element={<ConsultationPage />} />
      <Route path="/tuyen-sinh/lead" element={<LeadListPage />} />
      <Route path="/module/:name" element={<ModulePage />} />
      <Route path="/loi/:code" element={<ErrorPage />} />
      <Route path="*" element={<ErrorPage code="404" />} />
    </Routes>
  );
}
