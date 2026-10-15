import { useTitle } from '../components/ui';
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
  ['S2', 'Thùng rác: khôi phục, xoá vĩnh viễn (chương trình, môn, buổi học)', '/dao-tao/thung-rac'],
  ['S3-01', 'Phễu lead dạng Kanban, lý do từ chối', '/tuyen-sinh/pheu'],
  ['S3-02', 'Nhật ký chăm sóc (bấm vào thẻ lead trong phễu)', '/tuyen-sinh/pheu?lead=12'],
  ['S3-03', 'Chuyển lead thành học viên (lead VU THANH DAT ở cột Chốt)', '/tuyen-sinh/pheu?lead=23'],
  ['S3-04', 'Danh sách học viên', '/hoc-vien'],
  ['S3-04', 'Hồ sơ học viên (sửa hồ sơ, ghi chú)', '/hoc-vien/HV260101'],
  ['S3-07', 'Danh sách lớp kèm chỉ số và bộ lọc', '/lop-hoc'],
  ['S3-05', 'Mở lớp mới (nút Mở lớp mới), vòng đời lớp (menu cuối dòng)', '/lop-hoc'],
  ['S3-06', 'Ghi danh học viên vào lớp WEB-K16', '/lop-hoc/WEB-K16/ghi-danh'],
  ['S3-08', 'Phân công giảng viên và trợ giảng (lớp WEB-K16)', '/lop-hoc/WEB-K16/phan-cong'],
  ['S3-09', 'Phòng học vật lý và trực tuyến', '/phong-hoc'],
  ['S3-09', 'Xếp phòng cho lớp, cảnh báo vượt sức chứa (lớp WEB-K16)', '/lop-hoc/WEB-K16/phong'],
  ['S4-01', 'Sinh thời khoá biểu theo mẫu lặp (lớp WEB-K16)', '/lop-hoc/WEB-K16/sinh-lich'],
  ['S4-02', 'Cảnh báo trùng giảng viên, trùng phòng (bấm Xem trước ở S4-01)', '/lop-hoc/WEB-K16/sinh-lich'],
  ['S4-04', 'Thời khoá biểu dạng lịch tuần và tháng', '/thoi-khoa-bieu'],
  ['S4-03', 'Sửa, dời, huỷ một buổi (bấm vào buổi sắp tới trên lịch)', '/thoi-khoa-bieu?buoi=WEB-K15-40'],
  ['S4-05', 'Lịch học của tôi (đăng nhập bằng tài khoản Học viên)', '/lich-hoc'],
  ['S4-06', 'Bảo lưu, chuyển lớp, thôi học (nút trên hồ sơ học viên)', '/hoc-vien/HV260101'],
  ['S4-07', 'Điểm danh (giảng viên: buổi hôm nay; buổi đã qua chỉ xem)', '/diem-danh'],
  ['S4-08', 'Học viên gửi đơn xin phép vắng', '/xin-phep'],
  ['S4-08', 'Giảng viên duyệt đơn xin phép', '/duyet-phep'],
  ['S5-01', 'Bảng chuyên cần của lớp (ma trận buổi)', '/chuyen-can/WEB-K15'],
  ['S5-02', 'Ngưỡng vắng theo chương trình', '/dao-tao/nguong-vang'],
  ['S5-02', 'Cờ vượt ngưỡng trên danh sách lớp (tab Học viên)', '/lop-hoc/WEB-K15/hoc-vien'],
  ['S5-03', 'Lịch sử điểm danh của học viên', '/diem-danh-cua-toi'],
  ['S5-04', 'Xuất bảng chuyên cần ra Excel (nút Xuất Excel)', '/chuyen-can/WEB-K15'],
  ['S5-05', 'Tạo bài tập, lưu nháp, giao cho lớp', '/bai-tap/moi?lop=WEB-K15'],
  ['S5-06', 'Bài tập của tôi (học viên)', '/bai-tap-cua-toi'],
  ['S5-07', 'Nộp bài: tệp và liên kết repo (Bài 14)', '/bai-tap-cua-toi/7'],
  ['S5-10', 'Danh sách bài nộp của một bài tập (giảng viên)', '/bai-tap/6'],
  ['S5-08', 'Phiên bản đã nộp và nộp lại (học viên, Bài 13)', '/bai-tap-cua-toi/6'],
  ['S5-08', 'Bài đã chấm bị khoá nộp lại (học viên, Bài 11)', '/bai-tap-cua-toi/4'],
  ['S5-09', 'Cấu hình nộp muộn khi soạn bài tập; nhãn trễ ở danh sách bài nộp', '/bai-tap/9/sua'],
  ['S6-01', 'Chấm điểm, công bố, sang bài kế tiếp (giảng viên)', '/bai-tap/6/cham/HV260101'],
  ['S6-02', 'Tạo mẫu rubric', '/bai-tap/rubric'],
  ['S6-02', 'Chấm theo rubric (bài tập cuối môn, cần có bài nộp)', '/bai-tap/9'],
  ['S6-03', 'Yêu cầu làm lại (nút trong màn hình chấm)', '/bai-tap/6/cham/HV260101'],
  ['S6-04', 'Học viên xem điểm và nhận xét (Bài 11)', '/bai-tap-cua-toi/4'],
  ['S6-05', 'Giảng viên tải học liệu theo buổi', '/hoc-lieu/WEB-K15'],
  ['S6-06', 'Học viên xem và tải học liệu', '/hoc-lieu-cua-toi'],
  ['S6-07', 'Soạn thông báo tới lớp', '/thong-bao-lop/soan'],
  ['S6-07', 'Thông báo đã gửi và số đã đọc', '/thong-bao-lop'],
  ['S6-08', 'Mốc nhắc lịch và mẫu email (Quản lý đào tạo)', '/nhac-lich'],
  ['S6-08', 'Cài đặt cá nhân: tắt nhắc lịch học', '/cai-dat'],
  ['S6-09', 'Chuông thông báo (góc trên phải, sau khi đăng nhập)', '/bai-tap-cua-toi'],
  ['S6-10', 'Danh sách thông báo đầy đủ', '/thong-bao'],
  ['S6-11', 'Nhật ký thao tác trên điểm, học phí, vai trò', '/quan-tri/nhat-ky'],
  ['S7-01', 'Biểu phí theo chương trình, phiên bản mới', '/hoc-phi/bieu-phi'],
  ['S7-02', 'Kế hoạch thanh toán theo đợt, sinh hàng loạt', '/hoc-phi/ke-hoach'],
  ['S7-03', 'Ghi nhận thanh toán, biên lai, bút toán điều chỉnh', '/hoc-phi/cong-no/HV260142'],
  ['S7-04', 'Học bổng, giảm giá; vượt 30% cần phê duyệt', '/hoc-phi/ke-hoach/HV260142'],
  ['S7-05', 'Bảng công nợ, lọc quá hạn, xuất Excel', '/hoc-phi/cong-no'],
  ['S7-06', 'Nhắc học phí trước hạn và khi quá hạn', '/hoc-phi/nhac-hoc-phi'],
  ['S7-07', 'Học phí của tôi và tải lại biên lai', '/hoc-phi-cua-toi'],
  ['S7-08', 'Báo cáo doanh thu theo lớp, chương trình', '/hoc-phi/bao-cao'],
  ['S8-01', 'Cách tính điểm theo chương trình, ghi đè ở cấp lớp', '/dao-tao/cach-tinh-diem'],
  ['S8-02', 'Điểm tổng kết môn, cách tính chi tiết, chốt điểm', '/diem-tong-ket/WEB-K15/WEB-02'],
  ['S8-03', 'Bảng điểm toàn khoá và mẫu PDF', '/bang-diem/HV260101'],
  ['S8-04', 'Xét hoàn thành khoá, xác nhận tốt nghiệp', '/tot-nghiep'],
  ['S8-05', 'Khảo sát: khai báo, mở theo lớp, học viên trả lời', '/khao-sat'],
  ['S8-06', 'Kết quả khảo sát theo lớp, giảng viên', '/khao-sat/ket-qua'],
  ['S8-07', 'Dashboard Quản lý đào tạo', '/'],
  ['S8-08', 'Dashboard Giảng viên', '/'],
];

export function StoryIndexPage() {
  useTitle('Danh sách màn hình');
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
              <tr key={id + to}>
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
