import { Link, useNavigate, useParams } from 'react-router-dom';
import { Icon, type IconName } from '../../components/Icon';
import './error.css';

type Code = '403' | '404' | '500';

const PAGES: Record<Code, { icon: IconName; title: string; text: string }> = {
  '403': {
    icon: 'lock',
    title: 'Bạn không có quyền truy cập trang này',
    text: 'Tài khoản của bạn chưa được cấp quyền cho chức năng này. Hãy liên hệ quản trị hệ thống nếu bạn cần dùng.',
  },
  '404': {
    icon: 'search',
    title: 'Không tìm thấy trang',
    text: 'Đường dẫn có thể đã đổi hoặc bị gõ nhầm. Bạn có thể về trang chủ rồi đi tiếp từ menu.',
  },
  '500': {
    icon: 'alert',
    title: 'Hệ thống đang gặp sự cố',
    text: 'Lỗi này không phải do bạn. Vui lòng thử lại sau ít phút. Mã sự cố: TMS-8F21A',
  },
};

// S1-07. Ba trang lỗi dùng chung một mẫu: số lỗi lớn mờ, biểu tượng, lời nhắn tiếng Việt và hai nút đi tiếp.
export function ErrorPage({ code: fixed }: { code?: Code }) {
  const params = useParams();
  const navigate = useNavigate();
  const raw = fixed ?? params.code ?? '404';
  const code: Code = raw in PAGES ? (raw as Code) : '404';
  const p = PAGES[code];

  return (
    <div className="er-page">
      <Link to="/" className="logo">
        <span>
          <Icon name="cap" />
        </span>
        TMS.
      </Link>
      <main className="er-main">
        <div className="er-num" aria-hidden="true">
          {code}
        </div>
        <div className="badge">
          <Icon name={p.icon} />
        </div>
        <div className="eyebrow">Lỗi {code}</div>
        <h1>{p.title}</h1>
        <p>{p.text}</p>
        <div className="er-acts">
          {code === '500' ? (
            <>
              <button type="button" className="btn primary er-btn" onClick={() => window.location.reload()}>
                Thử lại
              </button>
              <Link className="btn er-btn" to="/">
                Về trang chủ
              </Link>
            </>
          ) : (
            <>
              <Link className="btn primary er-btn" to="/">
                Về trang chủ
              </Link>
              <button type="button" className="btn er-btn" onClick={() => navigate(-1)}>
                Quay lại
              </button>
            </>
          )}
        </div>
      </main>
    </div>
  );
}
