import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Icon, type IconName } from './Icon';

// Khung trang dạng thẻ giữa màn hình: nút đóng góc trái, nhãn "Bảo mật" góc phải.
export function CardPage({ closeTo, children, footer }: { closeTo: string; children: ReactNode; footer?: ReactNode }) {
  return (
    <div className="page">
      <div className="card">
        <div className="card-top">
          <Link className="x" to={closeTo} aria-label="Đóng">
            <Icon name="x" />
          </Link>
          <span className="sec">
            <Icon name="lock" />
            Bảo mật
          </span>
        </div>
        {children}
      </div>
      {footer}
    </div>
  );
}

export function Badge({ icon, amber }: { icon: IconName; amber?: boolean }) {
  return (
    <div className={'badge' + (amber ? ' amber' : '')}>
      <Icon name={icon} />
    </div>
  );
}
