import { api, apiEnabled } from './api';

export interface SubjectGradeItemDTO {
  subjectCode: string;
  subjectName: string;
  sessions: number;
  weight: number;
  attendanceScore?: number;
  assignmentScore?: number;
  examScore?: number;
  finalScore?: number;
  result: string; // 'Đạt' hoặc 'Không đạt'
  isLocked: boolean;
}

export interface TranscriptResponseDTO {
  studentId: number;
  studentCode: string;
  studentName: string;
  dateOfBirth: string;
  email: string;
  phone: string;
  programCode: string;
  programName: string;
  classCode: string;
  className: string;
  studyPeriod: string;

  centerName: string;
  centerAddress: string;
  transcriptNumber: string;
  issueDate: string;
  signatoryTitle: string;
  signatoryName: string;

  gpa: number | null;
  gpaFormula: string;
  totalSubjects: number;
  passedSubjects: number;
  failedSubjects: number;
  totalSessions: number;
  completedSessions: number;
  attendanceRate: number | null;
  isCompleted: boolean;

  subjects: SubjectGradeItemDTO[];
}

/**
 * Lấy bảng điểm toàn khoá của học viên đang đăng nhập (S8-03)
 */
export async function getMyTranscriptApi(): Promise<TranscriptResponseDTO> {
  return api<TranscriptResponseDTO>('/api/transcript/mine');
}

/**
 * Lấy bảng điểm toàn khoá của một học viên theo mã (S8-03)
 */
export async function getStudentTranscriptApi(studentCode: string): Promise<TranscriptResponseDTO> {
  return api<TranscriptResponseDTO>(`/api/transcripts/student/${studentCode}`);
}

/**
 * Lấy đường dẫn in / xuất PDF chính thức từ máy chủ (S8-03 AC3)
 */
export function getTranscriptPdfUrl(studentCode: string): string {
  return `/api/transcripts/student/${studentCode}/pdf`;
}
