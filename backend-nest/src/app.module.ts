import { Module } from '@nestjs/common';
import { PilotController } from './api/pilot.controller';
import { AuthController } from './auth/auth.controller';
import { AuthService } from './auth/auth.service';
import { DatabaseService } from './database/database.service';

// Mỗi story sau này là một module trong src/modules/<mã story>, rồi thêm vào imports ở đây.
@Module({
  imports: [],
  controllers: [PilotController, AuthController],
  providers: [AuthService, DatabaseService],
})
export class AppModule {}
