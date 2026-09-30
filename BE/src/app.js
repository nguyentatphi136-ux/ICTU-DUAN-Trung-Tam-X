import express from 'express';
import authRouter from './routes/authRoutes.js';

const app = express();

app.use(express.json({ limit: '16kb' }));
app.use('/auth', authRouter);

app.use((error, _request, response, _next) => {
  if (error instanceof SyntaxError && 'body' in error) {
    return response.status(400).json({ message: 'JSON không hợp lệ' });
  }

  console.error(error);
  return response.status(500).json({ message: 'Đã xảy ra lỗi máy chủ' });
});

export default app;
