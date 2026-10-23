import type { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { AppModule } from './app.module';

describe('Khung backend NestJS', () => {
  let app: INestApplication;

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({ imports: [AppModule] }).compile();
    app = moduleRef.createNestApplication();
    app.setGlobalPrefix('api');
    await app.init();
  });

  afterAll(() => app.close());

  it('GET /api/health trả SYSTEM_OK', async () => {
    const res = await request(app.getHttpServer()).get('/api/health').expect(200);
    expect(res.body.code).toBe('SYSTEM_OK');
  });

  it('POST /api/auth/login: đúng mật khẩu thì 200, sai thì 401', async () => {
    const ok = await request(app.getHttpServer()).post('/api/auth/login').send({ email: 'A25200212001@tms.vn', password: '123456' }).expect(200);
    expect(ok.body.data.user.roles).toEqual(['admin']);
    const bad = await request(app.getHttpServer()).post('/api/auth/login').send({ email: 'A25200212001@tms.vn', password: 'sai' }).expect(401);
    expect(bad.body.error.code).toBe('AUTH_INVALID_CREDENTIALS');
  });
});
