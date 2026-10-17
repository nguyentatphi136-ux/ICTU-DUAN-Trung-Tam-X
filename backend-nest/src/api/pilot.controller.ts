import { Controller, Get, ServiceUnavailableException } from '@nestjs/common';
import { DatabaseService } from '../database/database.service';

/** API thử nghiệm để kiểm tra server và kết nối CSDL. Cùng dạng phản hồi { success, code, message } với backend Java. */
@Controller()
export class PilotController {
  constructor(private readonly database: DatabaseService) {}

  @Get('health')
  health() {
    return { success: true, code: 'SYSTEM_OK', message: 'Dịch vụ đang hoạt động.' };
  }

  @Get('health/db')
  async databaseHealth() {
    try {
      await this.database.ping();
    } catch (e) {
      // Lỗi gộp (AggregateError, khi thử cả IPv4 và IPv6) có message rỗng, nên ghi kèm mã lỗi.
      const err = e as Error & { code?: string };
      const message = err.message || `Không kết nối được MySQL (${err.code ?? 'lỗi không rõ'}). Kiểm tra DATABASE_URL và MySQL đã chạy chưa.`;
      throw new ServiceUnavailableException({ success: false, error: { code: 'DATABASE_UNAVAILABLE', message } });
    }
    return { success: true, code: 'DATABASE_OK', message: 'Kết nối CSDL thành công.' };
  }
}
