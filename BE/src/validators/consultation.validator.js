/**
 * Sanitize chuỗi văn bản: loại bỏ các thẻ HTML / Script tiềm ẩn nguy cơ XSS
 */
function sanitizeText(value) {
  if (typeof value !== 'string') return '';
  return value
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '') // Xoá script tags
    .replace(/<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>/gi, '')   // Xoá style tags
    .replace(/<[^>]*>/g, '')                                           // Xoá mọi HTML tags còn lại
    .replace(/javascript:/gi, '')                                      // Chặn javascript pseudo-protocol
    .trim();
}

/**
 * Validate dữ liệu đăng ký tư vấn từ biểu mẫu công khai.
 * Trả về { isValid, errors, sanitizedData }
 */
function validateConsultationRequest(body) {
  const errors = [];

  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    return {
      isValid: false,
      errors: ['Dữ liệu gửi lên phải là một đối tượng JSON hợp lệ.'],
      sanitizedData: null,
    };
  }

  // 1. Nhận diện các trường dữ liệu và hỗ trợ cả camelCase và snake_case
  const rawFullName = body.fullName ?? body.name ?? body.full_name;
  const rawPhone = body.phone;
  const rawEmail = body.email;
  const rawCourse = body.course ?? body.program ?? body.programName ?? body.program_name;
  const rawInterestedProgramId = body.interestedProgramId ?? body.programId ?? body.interested_program_id;
  const rawMessage = body.message ?? body.notes ?? body.note ?? body.content;
  const rawPreferredTime = body.preferredTime ?? body.time ?? body.preferred_time;

  // 2. Validate Họ và tên (Bắt buộc)
  if (rawFullName === undefined || rawFullName === null || (typeof rawFullName === 'string' && !rawFullName.trim())) {
    errors.push('Vui lòng nhập họ và tên.');
  } else if (typeof rawFullName !== 'string') {
    errors.push('Họ và tên phải là chuỗi ký tự.');
  } else {
    const trimmed = rawFullName.trim();
    if (trimmed.length < 2) {
      errors.push('Họ và tên quá ngắn (tối thiểu 2 ký tự).');
    } else if (trimmed.length > 100) {
      errors.push('Họ và tên không được vượt quá 100 ký tự.');
    }
  }

  // 3. Validate Số điện thoại (Bắt buộc)
  if (rawPhone === undefined || rawPhone === null || (typeof rawPhone === 'string' && !rawPhone.trim())) {
    errors.push('Vui lòng nhập số điện thoại.');
  } else if (typeof rawPhone !== 'string') {
    errors.push('Số điện thoại phải là chuỗi ký tự.');
  } else {
    const rawClean = rawPhone.trim();
    if (rawClean.length > 20) {
      errors.push('Số điện thoại không được vượt quá 20 ký tự.');
    } else {
      const normalizedDigits = rawClean.replace(/[\s.-]/g, '');
      // Định dạng số điện thoại Việt Nam: bắt đầu bằng 0 hoặc +84 / 84, theo sau là 9-10 chữ số
      const phoneRegex = /^(?:0|\+84|84)(?:3|5|7|8|9)\d{8}$/;
      // Hỗ trợ dạng tổng quát hơn nếu có số bàn / số chuẩn 10 số
      const generalVnPhoneRegex = /^(?:0|\+84|84)\d{9,10}$/;

      if (!phoneRegex.test(normalizedDigits) && !generalVnPhoneRegex.test(normalizedDigits)) {
        errors.push('Số điện thoại chưa đúng định dạng (ví dụ: 0912345678 hoặc +84912345678).');
      }
    }
  }

  // 4. Validate Email (Tùy chọn, nếu có thì phải đúng định dạng)
  if (rawEmail !== undefined && rawEmail !== null && typeof rawEmail === 'string' && rawEmail.trim() !== '') {
    const trimmedEmail = rawEmail.trim();
    if (trimmedEmail.length > 150) {
      errors.push('Email không được vượt quá 150 ký tự.');
    } else {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(trimmedEmail)) {
        errors.push('Email chưa đúng định dạng (ví dụ: example@gmail.com).');
      }
    }
  } else if (rawEmail !== undefined && rawEmail !== null && typeof rawEmail !== 'string') {
    errors.push('Email phải là chuỗi ký tự.');
  }

  // 5. Validate Course (Khoá học quan tâm - tùy chọn)
  if (rawCourse !== undefined && rawCourse !== null) {
    if (typeof rawCourse !== 'string') {
      errors.push('Khoá học quan tâm phải là chuỗi ký tự.');
    } else if (rawCourse.trim().length > 100) {
      errors.push('Tên khoá học không được vượt quá 100 ký tự.');
    }
  }

  // 6. Validate Message / Notes (Nội dung cần tư vấn - tùy chọn)
  if (rawMessage !== undefined && rawMessage !== null) {
    if (typeof rawMessage !== 'string') {
      errors.push('Nội dung tư vấn phải là chuỗi ký tự.');
    } else if (rawMessage.trim().length > 1000) {
      errors.push('Nội dung tư vấn không được vượt quá 1000 ký tự.');
    }
  }

  // 7. Validate Preferred Time (Khung giờ thuận tiện - tùy chọn)
  if (rawPreferredTime !== undefined && rawPreferredTime !== null) {
    if (typeof rawPreferredTime !== 'string') {
      errors.push('Khung giờ thuận tiện phải là chuỗi ký tự.');
    } else if (rawPreferredTime.trim().length > 100) {
      errors.push('Khung giờ thuận tiện không được vượt quá 100 ký tự.');
    }
  }

  // 8. Validate Program ID (Nếu có)
  let interestedProgramId = null;
  if (rawInterestedProgramId !== undefined && rawInterestedProgramId !== null) {
    const parsedId = Number(rawInterestedProgramId);
    if (!Number.isInteger(parsedId) || parsedId <= 0) {
      errors.push('Mã chương trình quan tâm (interestedProgramId) phải là số nguyên dương.');
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

  // Chuẩn hóa và làm sạch dữ liệu đầu vào (loại bỏ nguy cơ XSS)
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
