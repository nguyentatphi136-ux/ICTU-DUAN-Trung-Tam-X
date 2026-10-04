/**
 * Middleware chống spam và giới hạn tần suất gửi yêu cầu (Rate Limiter)
 * Áp dụng cho các endpoint công khai như gửi biểu mẫu tư vấn.
 */
function createRateLimiter({
  windowMs = 60 * 1000, // 1 phút
  max = 5,              // Tối đa 5 lượt gửi trong 1 phút trên mỗi IP
  message = 'Bạn đã gửi quá nhiều yêu cầu tư vấn. Vui lòng thử lại sau ít phút!',
} = {}) {
  const store = new Map();

  // Định kỳ dọn dẹp các record hết hạn để tránh leak bộ nhớ
  const cleanupInterval = setInterval(() => {
    const now = Date.now();
    for (const [key, record] of store.entries()) {
      if (now > record.resetTime) {
        store.delete(key);
      }
    }
  }, 30 * 1000);

  // Không chặn tiến trình Node.js thoát
  if (cleanupInterval.unref) {
    cleanupInterval.unref();
  }

  const limiter = (req, res, next) => {
    // Lấy IP từ X-Forwarded-For (nếu chạy sau reverse proxy) hoặc socket remoteAddress
    const forwarded = req.headers['x-forwarded-for'];
    const ip = (typeof forwarded === 'string' ? forwarded.split(',')[0].trim() : '') ||
      req.ip ||
      req.socket?.remoteAddress ||
      'unknown-ip';

    const now = Date.now();
    let record = store.get(ip);

    if (!record || now > record.resetTime) {
      record = {
        count: 1,
        resetTime: now + windowMs,
      };
      store.set(ip, record);
    } else {
      record.count += 1;
    }

    const remaining = Math.max(0, max - record.count);
    const resetSeconds = Math.ceil((record.resetTime - now) / 1000);

    res.setHeader('X-RateLimit-Limit', max);
    res.setHeader('X-RateLimit-Remaining', remaining);
    res.setHeader('X-RateLimit-Reset', resetSeconds);

    if (record.count > max) {
      return res.status(429).json({
        success: false,
        code: 'RATE_LIMIT_EXCEEDED',
        message,
        retryAfter: resetSeconds,
      });
    }

    return next();
  };

  limiter.reset = () => {
    store.clear();
  };

  return limiter;
}

module.exports = {
  createRateLimiter,
};
