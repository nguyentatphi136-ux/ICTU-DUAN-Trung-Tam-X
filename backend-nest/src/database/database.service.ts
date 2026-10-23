import { Injectable, OnModuleDestroy } from '@nestjs/common';
import { createPool, type Pool } from 'mysql2/promise';

/**
 * Kết nối MySQL dùng chung (cùng CSDL ems_database với backend Java).
 * Pool chỉ được tạo ở lần truy vấn đầu, nên chưa có CSDL thì server vẫn khởi động được.
 */
@Injectable()
export class DatabaseService implements OnModuleDestroy {
  private pool?: Pool;

  private get db(): Pool {
    this.pool ??= createPool({
      uri: process.env.DATABASE_URL ?? 'mysql://root@localhost:3306/ems_database',
      connectionLimit: 10,
      dateStrings: true,
    });
    return this.pool;
  }

  /** Chạy câu SQL có tham số (?), trả về các dòng kết quả. */
  async query<T>(sql: string, params: unknown[] = []): Promise<T[]> {
    const [rows] = await this.db.query(sql, params);
    return rows as T[];
  }

  async ping(): Promise<void> {
    await this.db.query('SELECT 1');
  }

  async onModuleDestroy(): Promise<void> {
    await this.pool?.end();
  }
}
