const { LEAD_SOURCE, LEAD_STATUS } = require('../constants/leads');

let leadIdCounter = 1;

/**
 * Danh sách Lead lưu trữ trong bộ nhớ (in-memory) cho môi trường demo / testing.
 * Khi kết nối CSDL thực tế (MySQL/PostgreSQL), các hàm này sẽ tương tác qua Repository / DAO.
 */
const leads = [];

/**
 * Tạo một Lead mới trong hệ thống.
 * Đảm bảo các trường quản trị nội bộ luôn có giá trị mặc định an toàn.
 */
function createLead({
  fullName,
  phone,
  email = null,
  course = null,
  interestedProgramId = null,
  message = null,
  preferredTime = null,
  source = LEAD_SOURCE.WEBSITE,
  ipAddress = null,
}) {
  const now = new Date().toISOString();
  const newLead = {
    id: String(leadIdCounter++),
    fullName: fullName.trim(),
    phone: phone.trim(),
    email: email && typeof email === 'string' && email.trim() ? email.trim().toLowerCase() : null,
    course: course && typeof course === 'string' && course.trim() ? course.trim() : null,
    interestedProgramId: interestedProgramId ? Number(interestedProgramId) : null,
    assignedCounselorId: null, // Khách không được quyền tự chỉ định tư vấn viên
    status: LEAD_STATUS.NEW,   // Mặc định luôn là 'NEW'
    rejectReason: null,
    notes: message && typeof message === 'string' && message.trim() ? message.trim() : null,
    preferredTime: preferredTime && typeof preferredTime === 'string' && preferredTime.trim() ? preferredTime.trim() : null,
    convertedStudentId: null,
    source: source || LEAD_SOURCE.WEBSITE,
    ipAddress: ipAddress || null,
    createdAt: now,
    updatedAt: now,
  };

  leads.unshift(newLead);
  return newLead;
}

/**
 * Tìm kiếm Lead theo ID.
 */
function findLeadById(id) {
  return leads.find((lead) => String(lead.id) === String(id));
}

/**
 * Kiểm tra xem có yêu cầu gửi trùng trong khoảng thời gian ngắn (debounce/anti-duplicate) hay không.
 * Cơ chế: Cùng số điện thoại gửi liên tiếp trong vòng `windowMs` (mặc định 10.000ms = 10s).
 * Giúp chống người dùng bấm nút submit nhiều lần do lag mạng hoặc double-click.
 * Không chặn đăng ký hợp lệ trong tương lai.
 */
function findRecentSubmission({ phone, windowMs = 10000 }) {
  if (!phone) return null;
  const normalizedPhone = phone.replace(/[\s.-]/g, '');
  const now = Date.now();

  return leads.find((lead) => {
    const leadPhone = (lead.phone || '').replace(/[\s.-]/g, '');
    if (leadPhone !== normalizedPhone) return false;

    const leadCreatedTime = new Date(lead.createdAt).getTime();
    return now - leadCreatedTime < windowMs;
  });
}

/**
 * Lấy toàn bộ danh sách Lead (bản sao).
 */
function getAllLeads() {
  return [...leads];
}

/**
 * Xóa trắng danh sách Lead (dùng cho unit test).
 */
function clearLeads() {
  leads.length = 0;
  leadIdCounter = 1;
}

/**
 * Chuyển đổi Lead sang đối tượng trả về công khai an toàn cho khách truy cập.
 * Không để lộ các thông tin quản trị hoặc trường nội bộ.
 */
function toPublicLead(lead) {
  if (!lead) return null;
  return {
    id: lead.id,
    fullName: lead.fullName,
    phone: lead.phone,
    email: lead.email,
    course: lead.course,
    status: lead.status,
    createdAt: lead.createdAt,
  };
}

module.exports = {
  clearLeads,
  createLead,
  findLeadById,
  findRecentSubmission,
  getAllLeads,
  leads,
  toPublicLead,
};
