import { api, apiEnabled } from './api';

export interface AdjustmentRecordDTO {
  id: string; // ĐC-2026-000001
  paymentId: string;
  kind: 'Giảm khoản thu' | 'Tăng khoản thu' | 'Huỷ toàn bộ';
  delta: number;
  reason: string;
  by: string;
  at: string;
}

export interface PaymentReceiptResponseDTO {
  receiptCode: string; // BL-2026-000001
  studentCode: string;
  studentName: string;
  programName: string;
  classCode: string;
  installmentNo: number;
  installmentLabel: string;
  amountPaid: number;
  netAmount: number;
  amountInWords: string;
  paymentMethod: 'Tiền mặt' | 'Chuyển khoản';
  paymentDate: string;
  paymentTime: string;
  collectedBy: string;
  referenceCode?: string;
  notes?: string;

  installmentTotal: number;
  installmentPaidTotal: number;
  installmentRemaining: number;
  installmentStatus: string;

  centerName: string;
  centerAddress: string;

  adjustments: AdjustmentRecordDTO[];
}

export interface InstallmentDataDTO {
  no: number;
  label: string;
  amount: number;
  dueDate: string;
  paid: number;
  state: string;
}

export interface StudentPlanDataDTO {
  studentCode: string;
  studentName: string;
  programName: string;
  classCode: string;
  installments: InstallmentDataDTO[];
}

export interface StudentPaymentInfoDTO {
  plan: StudentPlanDataDTO;
  receipts: PaymentReceiptResponseDTO[];
}

export interface CreatePaymentRequestDTO {
  studentCode: string;
  installmentNo: number;
  amount: number;
  method: 'Tiền mặt' | 'Chuyển khoản';
  paymentDate?: string;
  referenceCode?: string;
  notes?: string;
}

export interface CreateAdjustmentRequestDTO {
  paymentId: string;
  kind: 'Giảm khoản thu' | 'Tăng khoản thu' | 'Huỷ toàn bộ';
  amount?: number;
  reason: string;
}

/**
 * Lấy kế hoạch học phí và danh sách biên lai của học viên (S7-03)
 */
export async function getStudentPaymentsApi(studentCode: string): Promise<StudentPaymentInfoDTO> {
  return api<StudentPaymentInfoDTO>(`/api/payments/student/${studentCode}`);
}

/**
 * Lấy chi tiết biên lai kèm số tiền bằng chữ và bút toán điều chỉnh (S7-03 AC4)
 */
export async function getReceiptApi(receiptCode: string): Promise<PaymentReceiptResponseDTO> {
  return api<PaymentReceiptResponseDTO>(`/api/payments/receipt/${receiptCode}`);
}

/**
 * Trả về URL in / tải PDF biên lai chính thức từ backend
 */
export function getReceiptPdfUrl(receiptCode: string): string {
  return `/api/payments/receipt/${receiptCode}/pdf`;
}

/**
 * Ghi nhận một khoản thanh toán học phí (S7-03 AC1, AC2, AC3, AC4)
 */
export async function recordPaymentApi(req: CreatePaymentRequestDTO): Promise<PaymentReceiptResponseDTO> {
  return api<PaymentReceiptResponseDTO>('/api/payments', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

/**
 * Lập bút toán điều chỉnh cho một biên lai đã lưu (S7-03 AC5)
 */
export async function createAdjustmentApi(req: CreateAdjustmentRequestDTO): Promise<PaymentReceiptResponseDTO> {
  return api<PaymentReceiptResponseDTO>(`/api/payments/${req.paymentId}/adjustments`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

/**
 * Lấy tình trạng học phí và danh sách biên lai của chính học viên đang đăng nhập (S7-07 AC1, AC2)
 */
export async function getMyPaymentsApi(): Promise<StudentPaymentInfoDTO> {
  return api<StudentPaymentInfoDTO>('/api/payments/mine');
}

export { apiEnabled };

