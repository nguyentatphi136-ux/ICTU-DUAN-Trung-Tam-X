import { api, apiEnabled, json } from './api';
import type { Mark } from './attendance';

export interface StudentAttendanceDTO {
  studentId: number;
  studentCode: string;
  studentName: string;
  status: Mark | null;
  notes: string;
  approvedLeave: boolean;
}

export interface AttendanceAuditLogDTO {
  id: number;
  sessionId: number;
  studentId?: number;
  studentCode?: string;
  studentName?: string;
  actorId?: number;
  actorName: string;
  actorRole: string;
  oldStatus: string;
  newStatus: string;
  reason?: string;
  createdAt: string;
}

export interface AttendanceSheetDTO {
  sessionId: number;
  classId: number;
  classCode: string;
  className: string;
  sessionNumber: number;
  sessionDate: string;
  startTime: string;
  endTime: string;
  roomName: string;
  subjectName: string;
  lessonTopic: string;
  instructorName: string;

  isToday: boolean;
  isPast: boolean;
  isFuture: boolean;
  canEdit: boolean;
  isCancelled: boolean;
  cancelReason?: string;

  saved: boolean;
  savedAt?: string;
  savedBy?: string;

  summary: Record<string, number>;
  totalStudents: number;
  presentCount: number;
  lateCount: number;
  excusedAbsenceCount: number;
  unexcusedAbsenceCount: number;
  attendanceRate: string;

  students: StudentAttendanceDTO[];
  logs: AttendanceAuditLogDTO[];
}

export interface SaveAttendancePayload {
  sessionId: number;
  reason?: string;
  items: {
    studentId?: number;
    studentCode?: string;
    status: string;
    notes?: string;
  }[];
}

/**
 * Lấy chi tiết bảng điểm danh của một buổi học (S4-07)
 */
export async function getAttendanceSheetApi(sessionId: number | string): Promise<AttendanceSheetDTO> {
  return api<AttendanceSheetDTO>(`/api/attendance/session/${sessionId}`);
}

/**
 * Lưu / cập nhật bảng điểm danh của một buổi học (S4-07)
 */
export async function saveAttendanceApi(sessionId: number | string, payload: SaveAttendancePayload): Promise<AttendanceSheetDTO> {
  return api<AttendanceSheetDTO>(`/api/attendance/session/${sessionId}`, json('POST', payload));
}

/**
 * Đánh dấu nhanh tất cả học viên có mặt cho một buổi học (S4-07 AC2)
 */
export async function markAllPresentApi(sessionId: number | string): Promise<AttendanceSheetDTO> {
  return api<AttendanceSheetDTO>(`/api/attendance/session/${sessionId}/all`, json('POST', {}));
}

/**
 * Lấy lịch sử nhật ký kiểm toán sửa điểm danh (S4-07 AC4)
 */
export async function getAttendanceLogsApi(sessionId: number | string): Promise<AttendanceAuditLogDTO[]> {
  return api<AttendanceAuditLogDTO[]>(`/api/attendance/session/${sessionId}/logs`);
}
