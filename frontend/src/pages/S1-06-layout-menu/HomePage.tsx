import { useParams } from 'react-router-dom';
import { AppLayout } from '../../components/AppLayout';
import { shortName, useAuth } from '../../data/auth';
import { ADMIN_ROLE, ROLES } from '../../data/permissions';

const ADMIN_STATS = [
  ['Tài khoản đang hoạt động', '286', 'trên 300 người dùng'],
  ['Tài khoản bị khoá', '4', 'cần xem lại lý do'],
  ['Đăng nhập hôm nay', '132', 'phiên đang mở'],
];

// Bản 360px của Học viên trong thiết kế. Khi tích hợp: GET /student/schedule?date=today.
const LESSONS = [
  ['18:30 – 20:30', 'Lập trình web cơ bản', 'Phòng 302'],
  ['Thứ Năm, 18:30', 'Cơ sở dữ liệu', 'Phòng 204'],
];

// S1-06. Trang chủ theo vai trò đang dùng. Người giữ nhiều vai trò thấy đúng trang của vai trò hiện tại.
export function HomePage() {
  const { user } = useAuth();
  const role = user ? ROLES[user.active] : '';
  return (
    <AppLayout crumb={['Trang chủ']}>
      {user && (
        <>
          <div className="h">
            <div>
              <h2>Xin chào, {shortName(user.name)}</h2>
              <p>
                {user.roles.length > 1
                  ? `Bạn đang dùng vai trò ${role}. Bạn giữ ${user.roles.length} vai trò và đổi qua lại ở góc trên bên phải, không cần đăng nhập lại.`
                  : `Bạn đang đăng nhập với vai trò ${role}. Menu bên trái chỉ hiện những mục bạn được dùng.`}
              </p>
            </div>
          </div>
          {user.active === ADMIN_ROLE && (
            <div className="stats">
              {ADMIN_STATS.map(([label, value, sub]) => (
                <div className="stat" key={label}>
                  <span>{label}</span>
                  <b>{value}</b>
                  <span>{sub}</span>
                </div>
              ))}
            </div>
          )}
          {user.active === 0 && (
            <div className="today">
              <h3>Lịch học hôm nay</h3>
              {LESSONS.map(([time, name, room]) => (
                <div className="panel" key={name}>
                  <span>{time}</span>
                  <b>{name}</b>
                  <small>{room}</small>
                </div>
              ))}
            </div>
          )}
          <div className="dash">
            {user.roles.length > 1 ? `Trang chủ của vai trò ${role} (giống mọi người dùng khác có vai trò này)` : 'Vùng nội dung của từng trang'}
          </div>
        </>
      )}
    </AppLayout>
  );
}

/** Module chưa có màn hình trong thiết kế: giữ layout và menu, phần nội dung để trống. */
export function ModulePage() {
  const name = decodeURIComponent(useParams().name ?? '');
  return (
    <AppLayout crumb={[name]} module={name}>
      <div className="h">
        <div>
          <h2>{name}</h2>
        </div>
      </div>
      <div className="dash">Vùng nội dung của từng trang</div>
    </AppLayout>
  );
}
