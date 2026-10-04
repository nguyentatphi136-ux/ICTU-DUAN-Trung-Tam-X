const express = require('express');
const { LEAD_SOURCE } = require('../constants/leads');
const { createLead, findRecentSubmission, toPublicLead } = require('../data/leads');
const { createRateLimiter } = require('../middleware/rateLimiter');
const { validateConsultationRequest } = require('../validators/consultation.validator');

const router = express.Router();

// Cấu hình Rate Limit từ biến môi trường (mặc định: 5 requests / 60 giây trên 1 IP)
const rateLimitMax = Number(process.env.CONSULTATION_RATE_LIMIT_MAX) || 5;
const rateLimitWindowMs = Number(process.env.CONSULTATION_RATE_LIMIT_WINDOW_MS) || 60 * 1000;
const consultationLimiter = createRateLimiter({
  max: rateLimitMax,
  windowMs: rateLimitWindowMs,
  message: 'Bạn đã gửi quá nhiều yêu cầu tư vấn. Vui lòng thử lại sau ít phút!',
});

// Thông điệp phản hồi cam kết thời gian (có thể cấu hình qua biến môi trường)
const contactPromise = process.env.CONSULTATION_CONTACT_PROMISE || 'thời gian sớm nhất';
const defaultSuccessMessage = `Cảm ơn bạn đã đăng ký tư vấn. Trung tâm sẽ liên hệ lại trong ${contactPromise}.`;

/**
 * Handler chính xử lý đăng ký tư vấn công khai (S2-08)
 */
async function handleConsultationRequest(req, res, next) {
  try {
    const { isValid, errors, sanitizedData } = validateConsultationRequest(req.body);

    if (!isValid) {
      return res.status(400).json({
        success: false,
        code: 'VALIDATION_ERROR',
        message: 'Dữ liệu đăng ký tư vấn không hợp lệ',
        errors,
      });
    }

    // Cơ chế chống gửi trùng nhiều lần trong thời gian ngắn (Debouncing / Anti-duplicate click)
    // Cùng số điện thoại gửi liên tiếp trong vòng 10 giây sẽ bị từ chối tạo mới để tránh spam DB
    const recentSubmission = findRecentSubmission({
      phone: sanitizedData.phone,
      windowMs: 10 * 1000,
    });

    if (recentSubmission) {
      return res.status(409).json({
        success: false,
        code: 'DUPLICATE_SUBMISSION',
        message: 'Hệ thống đang xử lý yêu cầu trước đó của bạn. Vui lòng không bấm gửi liên tục!',
        data: toPublicLead(recentSubmission),
      });
    }

    // Lấy địa chỉ IP của client (không lưu trữ nhạy cảm, chỉ dùng cho audit/bảo mật)
    const forwarded = req.headers['x-forwarded-for'];
    const ipAddress = (typeof forwarded === 'string' ? forwarded.split(',')[0].trim() : '') ||
      req.ip ||
      req.socket?.remoteAddress ||
      null;

    // Tạo Lead mới ở trạng thái mặc định 'NEW', nguồn 'WEBSITE'
    // Tuyệt đối không cho phép client ghi đè các trường quản trị nội bộ
    const newLead = createLead({
      fullName: sanitizedData.fullName,
      phone: sanitizedData.phone,
      email: sanitizedData.email,
      course: sanitizedData.course,
      interestedProgramId: sanitizedData.interestedProgramId,
      message: sanitizedData.message,
      preferredTime: sanitizedData.preferredTime,
      source: LEAD_SOURCE.WEBSITE,
      ipAddress,
    });

    const responseMessage = process.env.CONSULTATION_SUCCESS_MESSAGE || defaultSuccessMessage;

    return res.status(201).json({
      success: true,
      message: responseMessage,
      data: toPublicLead(newLead),
    });
  } catch (error) {
    return next(error);
  }
}

// Endpoint chính thức theo đặc tả: POST /consultation-requests
router.post('/consultation-requests', consultationLimiter, handleConsultationRequest);

// Endpoint alias dự phòng hỗ trợ tính tương thích: POST /consultations
router.post('/consultations', consultationLimiter, handleConsultationRequest);

module.exports = {
  consultationLimiter,
  handleConsultationRequest,
  router,
};
