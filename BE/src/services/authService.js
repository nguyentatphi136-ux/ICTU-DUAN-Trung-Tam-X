import { hashPassword } from './passwordService.js';
import { withAuthDatabase } from '../storage/authDatabase.js';

const INVALID_TOKEN_MESSAGE = 'Token không hợp lệ hoặc đã sử dụng';

export async function resetPassword({ token, email, newPassword } = {}) {
  if (
    typeof token !== 'string' ||
    !token.trim() ||
    typeof email !== 'string' ||
    !email.trim() ||
    typeof newPassword !== 'string' ||
    newPassword.length < 8
  ) {
    return {
      status: 400,
      body: { message: 'Vui lòng cung cấp token, email và mật khẩu mới có ít nhất 8 ký tự' },
    };
  }

  const normalizedEmail = email.trim().toLowerCase();

  return withAuthDatabase(async (database) => {
    const resetToken = database.reset_tokens.find(
      (record) => record.token === token.trim() && record.email === normalizedEmail,
    );
    const expiresAt = resetToken ? Date.parse(resetToken.expires_at) : Number.NaN;

    if (
      !resetToken ||
      resetToken.is_used === true ||
      !Number.isFinite(expiresAt) ||
      Date.now() >= expiresAt
    ) {
      return { status: 400, body: { message: INVALID_TOKEN_MESSAGE } };
    }

    const user = database.users.find((record) => record.email === normalizedEmail);
    if (!user) {
      return { status: 404, body: { message: 'Không tìm thấy tài khoản' } };
    }

    user.password_hash = await hashPassword(newPassword);
    resetToken.is_used = true;

    return { status: 200, body: { message: 'Đặt lại mật khẩu thành công' } };
  });
}
