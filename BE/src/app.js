const express = require('express');
const authRoutes = require('./routes/auth.routes');
const adminRoutes = require('./routes/admin.routes');
const trainingProgramsRoutes = require('./routes/training-programs.routes');
const { getAllSubjects } = require('./data/programs');

const app = express();

app.disable('x-powered-by');
app.use(express.json({ limit: '10kb' }));
app.use('/api/auth', authRoutes);
app.use('/admin', adminRoutes);
app.use('/api/training-programs', trainingProgramsRoutes);
app.use('/api/programs', trainingProgramsRoutes);
app.get('/api/subjects', (req, res) => res.status(200).json({ success: true, data: getAllSubjects() }));

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
