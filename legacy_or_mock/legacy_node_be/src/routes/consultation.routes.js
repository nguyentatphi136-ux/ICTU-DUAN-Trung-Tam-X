const express = require('express');
const { LEAD_SOURCE } = require('../constants/leads');
const { createLead, findRecentSubmission, toPublicLead } = require('../data/leads');
const { createRateLimiter } = require('../middleware/rateLimiter');
const { validateConsultationRequest } = require('../validators/consultation.validator');

const router = express.Router();

// Cß║Ñu h├¼nh Rate Limit tß╗½ biß║┐n m├┤i tr╞░ß╗¥ng (mß║╖c ─æß╗ïnh: 5 requests / 60 gi├óy tr├¬n 1 IP)
const rateLimitMax = Number(process.env.CONSULTATION_RATE_LIMIT_MAX) || 5;
const rateLimitWindowMs = Number(process.env.CONSULTATION_RATE_LIMIT_WINDOW_MS) || 60 * 1000;
const consultationLimiter = createRateLimiter({
  max: rateLimitMax,
  windowMs: rateLimitWindowMs,
  message: 'Bß║ín ─æ├ú gß╗¡i qu├í nhiß╗üu y├¬u cß║ºu t╞░ vß║Ñn. Vui l├▓ng thß╗¡ lß║íi sau ├¡t ph├║t!',
});

// Th├┤ng ─æiß╗çp phß║ún hß╗ôi cam kß║┐t thß╗¥i gian (c├│ thß╗â cß║Ñu h├¼nh qua biß║┐n m├┤i tr╞░ß╗¥ng)
const contactPromise = process.env.CONSULTATION_CONTACT_PROMISE || 'thß╗¥i gian sß╗¢m nhß║Ñt';
const defaultSuccessMessage = `Cß║úm ╞ín bß║ín ─æ├ú ─æ─âng k├╜ t╞░ vß║Ñn. Trung t├óm sß║╜ li├¬n hß╗ç lß║íi trong ${contactPromise}.`;

/**
 * Handler ch├¡nh xß╗¡ l├╜ ─æ─âng k├╜ t╞░ vß║Ñn c├┤ng khai (S2-08)
 */
async function handleConsultationRequest(req, res, next) {
  try {
    const { isValid, errors, sanitizedData } = validateConsultationRequest(req.body);

    if (!isValid) {
      return res.status(400).json({
        success: false,
        code: 'VALIDATION_ERROR',
        message: 'Dß╗» liß╗çu ─æ─âng k├╜ t╞░ vß║Ñn kh├┤ng hß╗úp lß╗ç',
        errors,
      });
    }

    // C╞í chß║┐ chß╗æng gß╗¡i tr├╣ng nhiß╗üu lß║ºn trong thß╗¥i gian ngß║»n (Debouncing / Anti-duplicate click)
    // C├╣ng sß╗æ ─æiß╗çn thoß║íi gß╗¡i li├¬n tiß║┐p trong v├▓ng 10 gi├óy sß║╜ bß╗ï tß╗½ chß╗æi tß║ío mß╗¢i ─æß╗â tr├ính spam DB
    const recentSubmission = findRecentSubmission({
      phone: sanitizedData.phone,
      windowMs: 10 * 1000,
    });

    if (recentSubmission) {
      return res.status(409).json({
        success: false,
        code: 'DUPLICATE_SUBMISSION',
        message: 'Hß╗ç thß╗æng ─æang xß╗¡ l├╜ y├¬u cß║ºu tr╞░ß╗¢c ─æ├│ cß╗ºa bß║ín. Vui l├▓ng kh├┤ng bß║Ñm gß╗¡i li├¬n tß╗Ñc!',
        data: toPublicLead(recentSubmission),
      });
    }

    // Lß║Ñy ─æß╗ïa chß╗ë IP cß╗ºa client (kh├┤ng l╞░u trß╗» nhß║íy cß║úm, chß╗ë d├╣ng cho audit/bß║úo mß║¡t)
    const forwarded = req.headers['x-forwarded-for'];
    const ipAddress = (typeof forwarded === 'string' ? forwarded.split(',')[0].trim() : '') ||
      req.ip ||
      req.socket?.remoteAddress ||
      null;

    // Tß║ío Lead mß╗¢i ß╗ƒ trß║íng th├íi mß║╖c ─æß╗ïnh 'NEW', nguß╗ôn 'WEBSITE'
    // Tuyß╗çt ─æß╗æi kh├┤ng cho ph├⌐p client ghi ─æ├¿ c├íc tr╞░ß╗¥ng quß║ún trß╗ï nß╗Öi bß╗Ö
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

// Endpoint ch├¡nh thß╗⌐c theo ─æß║╖c tß║ú: POST /consultation-requests
router.post('/consultation-requests', consultationLimiter, handleConsultationRequest);

// Endpoint alias dß╗▒ ph├▓ng hß╗ù trß╗ú t├¡nh t╞░╞íng th├¡ch: POST /consultations
router.post('/consultations', consultationLimiter, handleConsultationRequest);

module.exports = {
  consultationLimiter,
  handleConsultationRequest,
  router,
};
