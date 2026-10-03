module.exports = {
  secret: process.env.JWT_SECRET || 'development-only-change-this-secret',
  expiresIn: process.env.JWT_EXPIRES_IN || '1h',
  algorithm: 'HS256',
};
