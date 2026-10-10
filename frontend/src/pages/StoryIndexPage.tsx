import { Link } from 'react-router-dom';
import { ThemeToggle } from '../components/ThemeToggle';

// Mục lục màn hình theo backlog, để nhóm kiểm thử mở nhanh từng story. Đường dẫn khớp tên khung trong Figma.
const STORIES: [string, string, string][] = [
  ['S1-01', 'Đăng nhập', '/dang-nhap'],
  ['S1-02', 'Phiên đăng nhập: cảnh báo, đăng xuất, hết hạn, khôi phục nháp', '/phien'],
  ['S1-03', 'Quên mật khẩu', '/quen-mat-khau'],
  ['S1-03', 'Đặt mật khẩu mới (liên kết trong email)', '/dat-lai-mat-khau?token=demo'],
  ['S1-03', 'Liên kết hết hiệu lực', '/dat-lai-mat-khau?token=het-han'],
  ['S1-04', 'Đổi mật khẩu', '/doi-mat-khau'],
  ['S1-05', 'Ma trận phân quyền', '/quan-tri/phan-quyen'],
  ['S1-06', 'Layout và menu theo vai trò', '/'],
  ['S1-07', 'Trang lỗi 403', '/loi/403'],
  ['S1-07', 'Trang lỗi 404', '/loi/404'],
  ['S1-07', 'Trang lỗi 500', '/loi/500'],
  ['S1-08', 'Tài khoản người dùng (S1-09 gán vai trò, S1-10 khoá)', '/quan-tri/tai-khoan'],
  ['S2-01', 'Nhập người dùng từ Excel', '/quan-tri/nhap-excel'],
  ['S2-02', 'Hồ sơ cá nhân (S2-03 ảnh đại diện)', '/ho-so'],
  ['S2-04', 'Chương trình đào tạo', '/dao-tao/chuong-trinh'],
  ['S2-05', 'Môn học', '/dao-tao/mon-hoc'],
  ['S2-06', 'Gắn môn vào chương trình', '/dao-tao/chuong-trinh/WEB-FS'],
  ['S2-07', 'Buổi học trong môn', '/dao-tao/mon-hoc/WEB-01'],
  ['S2-08', 'Đăng ký tư vấn (công khai)', '/dang-ky-tu-van'],
  ['S2-09', 'Lead (S2-11 tìm kiếm và bộ lọc)', '/tuyen-sinh/lead'],
  ['S2-10', 'Phân công lead (chọn lead rồi bấm Phân công)', '/tuyen-sinh/lead'],
];

export function StoryIndexPage() {
  return (
    <div className="page">
      <div className="card" style={{ maxWidth: 720 }}>
        <ThemeToggle className="theme-in" />
        <h1 style={{ textAlign: 'left', marginBottom: 6 }}>Màn hình TMS</h1>
        <p className="hint" style={{ marginBottom: 16 }}>
          Trang sau đăng nhập cần tài khoản mẫu (xem mục "Tài khoản mẫu để thử" ở trang đăng nhập). Quản trị hệ thống thấy đủ 12 module.
        </p>
        <table>
          <tbody>
            {STORIES.map(([id, name, to]) => (
              <tr key={to}>
                <td style={{ width: 70 }}>{id}</td>
                <td>
                  <Link to={to}>{name}</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
