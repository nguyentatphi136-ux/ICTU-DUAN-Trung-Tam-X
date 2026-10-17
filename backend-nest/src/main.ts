import 'reflect-metadata';
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
  // Nạp backend-nest/.env nếu có (PORT, DATABASE_URL). Không có file thì dùng biến môi trường sẵn có.
  try {
    process.loadEnvFile();
  } catch {
    // Không có .env
  }
  const app = await NestFactory.create(AppModule);
  // Cùng tiền tố /api với backend Java, để frontend (Vite) chuyển tiếp được mà không phải sửa đường dẫn.
  app.setGlobalPrefix('api');
  const port = Number(process.env.PORT ?? 3000);
  await app.listen(port);
  console.log(`Backend NestJS chạy tại http://localhost:${port}/api`);
}

void bootstrap();
