// Mẫu rubric (S6-02): tiêu chí kèm trọng số, tổng 100%; lưu thành mẫu để gắn vào nhiều bài tập.
// Khi tích hợp: GET/POST/PUT /rubric-templates. Bài tập giữ bản sao tiêu chí lúc gắn, sửa mẫu sau không ảnh hưởng bài đã giao.
import type { Criterion } from './assignments';
import { persisted } from './store';

export type Rubric = { id: number; name: string; criteria: Criterion[] };

export const RUBRICS: Rubric[] = persisted('rubrics', [
  {
    id: 1,
    name: 'Bài tập lập trình web',
    criteria: [
      { name: 'Đủ chức năng theo yêu cầu', weight: 40, hint: 'Làm đúng và đủ các chức năng trong đề bài' },
      { name: 'Chất lượng mã nguồn', weight: 30, hint: 'Đặt tên rõ, chia hàm hợp lý, không lặp mã' },
      { name: 'Giao diện và trải nghiệm', weight: 20, hint: 'Bố cục gọn, có trạng thái tải và báo lỗi' },
      { name: 'Báo cáo và trình bày', weight: 10, hint: 'Báo cáo nêu rõ cách làm và hạn chế' },
    ],
  },
  {
    id: 2,
    name: 'Bài tập lập trình cơ bản',
    criteria: [
      { name: 'Kết quả đúng', weight: 60 },
      { name: 'Cách trình bày mã', weight: 25 },
      { name: 'Kiểm thử', weight: 15 },
    ],
  },
  {
    id: 3,
    name: 'Dự án cuối môn',
    criteria: [
      { name: 'Phân tích yêu cầu', weight: 15 },
      { name: 'Thiết kế', weight: 20 },
      { name: 'Chức năng', weight: 35 },
      { name: 'Chất lượng mã', weight: 20 },
      { name: 'Thuyết trình', weight: 10 },
    ],
  },
  {
    id: 4,
    name: 'Bài thuyết trình',
    criteria: [
      { name: 'Nội dung', weight: 40 },
      { name: 'Cấu trúc', weight: 20 },
      { name: 'Trình bày', weight: 25 },
      { name: 'Trả lời câu hỏi', weight: 15 },
    ],
  },
]);

/** Điểm mỗi tiêu chí chấm trên thang 10. */
export const RUBRIC_SCALE = 10;

/** Điểm tổng theo trọng số, quy về điểm tối đa của bài, làm tròn một chữ số thập phân. */
export function rubricTotal(criteria: Criterion[], scores: Record<string, number>, max: number) {
  const raw = criteria.reduce((n, c) => n + ((scores[c.name] ?? 0) * c.weight) / 100, 0);
  return Math.round(((raw * max) / RUBRIC_SCALE) * 10) / 10;
}

/** Phần góp của một tiêu chí vào điểm tổng, ví dụ 9 × 40% = 3,6. */
export const contribution = (c: Criterion, s: number, max: number) => Math.round(((s * c.weight) / 100) * (max / RUBRIC_SCALE) * 10) / 10;
