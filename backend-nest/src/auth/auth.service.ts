import { Injectable, UnauthorizedException } from '@nestjs/common';
import type { AuthUser, LoginRequest } from './auth.contract';
import { DEMO_ACCOUNTS, DEMO_PASSWORD } from './demo-accounts';

@Injectable()
export class AuthService {
  /** Đăng nhập bằng tài khoản thử. Khi làm S1-01: đọc bảng users và so mật khẩu BCrypt qua DatabaseService. */
  login({ email, password }: LoginRequest): AuthUser {
    const user = process.env.NODE_ENV === 'production' ? undefined : DEMO_ACCOUNTS.find((a) => a.email.toLowerCase() === email?.trim().toLowerCase());
    if (!user || password !== DEMO_PASSWORD) {
      throw new UnauthorizedException({ success: false, error: { code: 'AUTH_INVALID_CREDENTIALS', message: 'Email hoặc mật khẩu không đúng.' } });
    }
    return user;
  }
}
