const express = require('express');
const authRoutes = require('./routes/auth.routes');
const adminRoutes = require('./routes/admin.routes');
const { router: consultationRoutes } = require('./routes/consultation.routes');

const app = express();

app.disable('x-powered-by');
app.use(express.json({ limit: '10kb' }));
app.use('/api/auth', authRoutes);
app.use('/admin', adminRoutes);
app.use('/api/public', consultationRoutes);
app.use('/api', consultationRoutes);

app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: 'Không tìm thấy API',
  });
});

app.use((error, req, res, next) => {
  if (res.headersSent) return next(error);

  const isInvalidJson = error instanceof SyntaxError && error.status === 400 && 'body' in error;
  res.status(isInvalidJson ? 400 : 500).json({
    success: false,
    message: isInvalidJson ? 'Dữ liệu JSON không hợp lệ' : 'Đã xảy ra lỗi máy chủ',
  });
});

module.exports = app;
