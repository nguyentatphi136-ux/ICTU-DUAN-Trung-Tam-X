/**
 * Sanitize chuß╗ùi v─ân bß║ún: loß║íi bß╗Å c├íc thß║╗ HTML / Script tiß╗üm ß║⌐n nguy c╞í XSS
 */
function sanitizeText(value) {
  if (typeof value !== 'string') return '';
  return value
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '') // Xo├í script tags
    .replace(/<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>/gi, '')   // Xo├í style tags
    .replace(/<[^>]*>/g, '')                                           // Xo├í mß╗ìi HTML tags c├▓n lß║íi
    .replace(/javascript:/gi, '')                                      // Chß║╖n javascript pseudo-protocol
    .trim();
}

/**
 * Validate dß╗» liß╗çu ─æ─âng k├╜ t╞░ vß║Ñn tß╗½ biß╗âu mß║½u c├┤ng khai.
 * Trß║ú vß╗ü { isValid, errors, sanitizedData }
 */
function validateConsultationRequest(body) {
  const errors = [];

  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    return {
      isValid: false,
      errors: ['Dß╗» liß╗çu gß╗¡i l├¬n phß║úi l├á mß╗Öt ─æß╗æi t╞░ß╗úng JSON hß╗úp lß╗ç.'],
      sanitizedData: null,
    };
  }

  // 1. Nhß║¡n diß╗çn c├íc tr╞░ß╗¥ng dß╗» liß╗çu v├á hß╗ù trß╗ú cß║ú camelCase v├á snake_case
  const rawFullName = body.fullName ?? body.name ?? body.full_name;
  const rawPhone = body.phone;
  const rawEmail = body.email;
  const rawCourse = body.course ?? body.program ?? body.programName ?? body.program_name;
  const rawInterestedProgramId = body.interestedProgramId ?? body.programId ?? body.interested_program_id;
  const rawMessage = body.message ?? body.notes ?? body.note ?? body.content;
  const rawPreferredTime = body.preferredTime ?? body.time ?? body.preferred_time;

  // 2. Validate Hß╗ì v├á t├¬n (Bß║»t buß╗Öc)
  if (rawFullName === undefined || rawFullName === null || (typeof rawFullName === 'string' && !rawFullName.trim())) {
    errors.push('Vui l├▓ng nhß║¡p hß╗ì v├á t├¬n.');
  } else if (typeof rawFullName !== 'string') {
    errors.push('Hß╗ì v├á t├¬n phß║úi l├á chuß╗ùi k├╜ tß╗▒.');
  } else {
    const trimmed = rawFullName.trim();
    if (trimmed.length < 2) {
      errors.push('Hß╗ì v├á t├¬n qu├í ngß║»n (tß╗æi thiß╗âu 2 k├╜ tß╗▒).');
    } else if (trimmed.length > 100) {
      errors.push('Hß╗ì v├á t├¬n kh├┤ng ─æ╞░ß╗úc v╞░ß╗út qu├í 100 k├╜ tß╗▒.');
    }
  }

  // 3. Validate Sß╗æ ─æiß╗çn thoß║íi (Bß║»t buß╗Öc)
  if (rawPhone === undefined || rawPhone === null || (typeof rawPhone === 'string' && !rawPhone.trim())) {
    errors.push('Vui l├▓ng nhß║¡p sß╗æ ─æiß╗çn thoß║íi.');
  } else if (typeof rawPhone !== 'string') {
    errors.push('Sß╗æ ─æiß╗çn thoß║íi phß║úi l├á chuß╗ùi k├╜ tß╗▒.');
  } else {
    const rawClean = rawPhone.trim();
    if (rawClean.length > 20) {
      errors.push('Sß╗æ ─æiß╗çn thoß║íi kh├┤ng ─æ╞░ß╗úc v╞░ß╗út qu├í 20 k├╜ tß╗▒.');
    } else {
      const normalizedDigits = rawClean.replace(/[\s.-]/g, '');
      // ─Éß╗ïnh dß║íng sß╗æ ─æiß╗çn thoß║íi Viß╗çt Nam: bß║»t ─æß║ºu bß║▒ng 0 hoß║╖c +84 / 84, theo sau l├á 9-10 chß╗» sß╗æ
      const phoneRegex = /^(?:0|\+84|84)(?:3|5|7|8|9)\d{8}$/;
      // Hß╗ù trß╗ú dß║íng tß╗òng qu├ít h╞ín nß║┐u c├│ sß╗æ b├án / sß╗æ chuß║⌐n 10 sß╗æ
      const generalVnPhoneRegex = /^(?:0|\+84|84)\d{9,10}$/;

      if (!phoneRegex.test(normalizedDigits) && !generalVnPhoneRegex.test(normalizedDigits)) {
        errors.push('Sß╗æ ─æiß╗çn thoß║íi ch╞░a ─æ├║ng ─æß╗ïnh dß║íng (v├¡ dß╗Ñ: 0912345678 hoß║╖c +84912345678).');
      }
    }
  }

  // 4. Validate Email (T├╣y chß╗ìn, nß║┐u c├│ th├¼ phß║úi ─æ├║ng ─æß╗ïnh dß║íng)
  if (rawEmail !== undefined && rawEmail !== null && typeof rawEmail === 'string' && rawEmail.trim() !== '') {
    const trimmedEmail = rawEmail.trim();
    if (trimmedEmail.length > 150) {
      errors.push('Email kh├┤ng ─æ╞░ß╗úc v╞░ß╗út qu├í 150 k├╜ tß╗▒.');
    } else {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(trimmedEmail)) {
        errors.push('Email ch╞░a ─æ├║ng ─æß╗ïnh dß║íng (v├¡ dß╗Ñ: example@gmail.com).');
      }
    }
  } else if (rawEmail !== undefined && rawEmail !== null && typeof rawEmail !== 'string') {
    errors.push('Email phß║úi l├á chuß╗ùi k├╜ tß╗▒.');
  }

  // 5. Validate Course (Kho├í hß╗ìc quan t├óm - t├╣y chß╗ìn)
  if (rawCourse !== undefined && rawCourse !== null) {
    if (typeof rawCourse !== 'string') {
      errors.push('Kho├í hß╗ìc quan t├óm phß║úi l├á chuß╗ùi k├╜ tß╗▒.');
    } else if (rawCourse.trim().length > 100) {
      errors.push('T├¬n kho├í hß╗ìc kh├┤ng ─æ╞░ß╗úc v╞░ß╗út qu├í 100 k├╜ tß╗▒.');
    }
  }

  // 6. Validate Message / Notes (Nß╗Öi dung cß║ºn t╞░ vß║Ñn - t├╣y chß╗ìn)
  if (rawMessage !== undefined && rawMessage !== null) {
    if (typeof rawMessage !== 'string') {
      errors.push('Nß╗Öi dung t╞░ vß║Ñn phß║úi l├á chuß╗ùi k├╜ tß╗▒.');
    } else if (rawMessage.trim().length > 1000) {
      errors.push('Nß╗Öi dung t╞░ vß║Ñn kh├┤ng ─æ╞░ß╗úc v╞░ß╗út qu├í 1000 k├╜ tß╗▒.');
    }
  }

  // 7. Validate Preferred Time (Khung giß╗¥ thuß║¡n tiß╗çn - t├╣y chß╗ìn)
  if (rawPreferredTime !== undefined && rawPreferredTime !== null) {
    if (typeof rawPreferredTime !== 'string') {
      errors.push('Khung giß╗¥ thuß║¡n tiß╗çn phß║úi l├á chuß╗ùi k├╜ tß╗▒.');
    } else if (rawPreferredTime.trim().length > 100) {
      errors.push('Khung giß╗¥ thuß║¡n tiß╗çn kh├┤ng ─æ╞░ß╗úc v╞░ß╗út qu├í 100 k├╜ tß╗▒.');
    }
  }

  // 8. Validate Program ID (Nß║┐u c├│)
  let interestedProgramId = null;
  if (rawInterestedProgramId !== undefined && rawInterestedProgramId !== null) {
    const parsedId = Number(rawInterestedProgramId);
    if (!Number.isInteger(parsedId) || parsedId <= 0) {
      errors.push('M├ú ch╞░╞íng tr├¼nh quan t├óm (interestedProgramId) phß║úi l├á sß╗æ nguy├¬n d╞░╞íng.');
    } else {
      interestedProgramId = parsedId;
    }
  }

  if (errors.length > 0) {
    return {
      isValid: false,
      errors,
      sanitizedData: null,
    };
  }

  // Chuß║⌐n h├│a v├á l├ám sß║ích dß╗» liß╗çu ─æß║ºu v├áo (loß║íi bß╗Å nguy c╞í XSS)
  const sanitizedData = {
    fullName: sanitizeText(rawFullName),
    phone: typeof rawPhone === 'string' ? rawPhone.trim() : '',
    email: rawEmail && typeof rawEmail === 'string' && rawEmail.trim() ? sanitizeText(rawEmail).toLowerCase() : null,
    course: rawCourse ? sanitizeText(rawCourse) : null,
    interestedProgramId,
    message: rawMessage ? sanitizeText(rawMessage) : null,
    preferredTime: rawPreferredTime ? sanitizeText(rawPreferredTime) : null,
  };

  return {
    isValid: true,
    errors: [],
    sanitizedData,
  };
}

module.exports = {
  sanitizeText,
  validateConsultationRequest,
};
