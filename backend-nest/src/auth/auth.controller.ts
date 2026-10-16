import { Body, Controller, HttpCode, Post } from '@nestjs/common';
import type { LoginRequest, LoginResponse } from './auth.contract';
import { AuthService } from './auth.service';

@Controller('auth')
export class AuthController {
  constructor(private readonly auth: AuthService) {}

  @Post('login')
  @HttpCode(200)
  login(@Body() body: LoginRequest): LoginResponse {
    return { success: true, code: 'AUTH_LOGIN_SUCCESS', message: 'Đăng nhập thành công.', data: { user: this.auth.login(body ?? {}) } };
  }
}
