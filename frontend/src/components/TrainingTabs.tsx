import { NavLink } from 'react-router-dom';
import { useAuth } from '../data/auth';
import { ADMIN_ROLE } from '../data/permissions';

// Dùng chung cho S2-04 đến S2-07 (Chương trình & môn học).
export const MODULE = 'Chương trình & môn học';

/** Chỉ Quản lý đào tạo (toàn quyền module) và quản trị được thêm, sửa, xoá. Vai trò khác chỉ xem. */
export function useCanEditTraining() {
  const { user } = useAuth();
  return !!user && (user.active === 5 || user.active === ADMIN_ROLE);
}

export function TrainingTabs() {
  const canEdit = useCanEditTraining();
  return (
    <nav className="tabs">
      <NavLink to="/dao-tao/chuong-trinh" className={({ isActive }) => (isActive ? 'on' : undefined)} end>
        Chương trình
      </NavLink>
      <NavLink to="/dao-tao/mon-hoc" className={({ isActive }) => (isActive ? 'on' : undefined)} end>
        Môn học
      </NavLink>
      {canEdit && (
        <NavLink to="/dao-tao/thung-rac" className={({ isActive }) => (isActive ? 'on' : undefined)} end>
          Thùng rác
        </NavLink>
      )}
    </nav>
  );
}
