// Khảo sát chất lượng (S8-05, S8-06). Khi tích hợp: GET/PUT /surveys/questions, POST /surveys/runs, POST /surveys/runs/:id/answers.
// Câu trả lời lưu tách khỏi danh sách người đã trả lời nên không gắn được tên với nội dung.
import { TODAY } from './funnel';
import { addDays } from './schedule';
import { persisted } from './store';
import { STUDENTS } from './students';

export type Question = { id: string; text: string; kind: 'scale' | 'text' };
export type Answer = { scores: Record<string, number>; texts: Record<string, string> };
export type SurveyRun = { id: number; cls: string; title: string; questions: Question[]; opened: string; due: string; size: number; responded: string[]; answers: Answer[]; closed?: boolean };

export const QUESTIONS: Question[] = persisted('survey-questions', [
  { id: 'q1', text: 'Giảng viên giải thích bài dễ hiểu', kind: 'scale' },
  { id: 'q2', text: 'Tốc độ giảng dạy phù hợp với bạn', kind: 'scale' },
  { id: 'q3', text: 'Bài tập giúp bạn nắm chắc kiến thức', kind: 'scale' },
  { id: 'q4', text: 'Phòng học và thiết bị đáp ứng tốt', kind: 'scale' },
  { id: 'q5', text: 'Bạn sẵn sàng giới thiệu trung tâm cho người quen', kind: 'scale' },
  { id: 'q6', text: 'Điều bạn muốn trung tâm cải thiện là gì?', kind: 'text' },
]);

const hash = (s: string) => [...s].reduce((h, c) => (h * 31 + c.charCodeAt(0)) >>> 0, 7);
const TEXTS: Record<string, string[]> = {
  'WEB-K15': ['Thầy giảng hơi nhanh ở phần gọi API, mong có thêm ví dụ từng bước.', 'Nên có buổi ôn tập trước bài tập cuối môn.', 'Muốn có thêm bài tập nhỏ sau mỗi buổi.'],
  'WEB-K14': ['Phòng P.401 máy chiếu mờ, khó nhìn mã ở hàng cuối.', 'Bài tập hợp lý, em thích phần nhận xét chi tiết.', 'Mong có tài liệu trước buổi học một ngày.'],
  'DATA-K07': ['Phần thống kê hơi khó, cần thêm ví dụ thực tế.', 'Học trực tuyến thỉnh thoảng bị trễ tiếng.'],
  'UIUX-K09': ['Muốn được góp ý portfolio nhiều hơn.', 'Phòng học hơi nóng buổi chiều.'],
};
/** Lệch điểm theo lớp và câu để kết quả mẫu có chênh lệch (S8-06). */
const BIAS: Record<string, Record<string, number>> = { 'WEB-K15': { q2: -0.9, q4: -0.3 }, 'WEB-K14': { q2: 0, q4: -0.2 } };

function run(id: number, cls: string, title: string, opened: string, due: string, done: number, size?: number, closed?: boolean): SurveyRun {
  const roster = STUDENTS.filter((s) => s.cls === cls).map((s) => s.code);
  const n = size ?? roster.length;
  // Học viên NGUYEN MINH ANH chưa trả lời khảo sát giữa khoá để có cái làm thử (S8-05).
  const order = [...roster.filter((c) => c !== 'HV260101'), ...roster.filter((c) => c === 'HV260101')];
  const responded = roster.length ? order.slice(0, Math.min(done, roster.length)) : Array.from({ length: done }, (_, i) => `${cls}-${i}`);
  const qs = QUESTIONS.map((q) => ({ ...q }));
  const answers = responded.map((code, i): Answer => {
    const scores: Record<string, number> = {};
    for (const q of qs) if (q.kind === 'scale') scores[q.id] = Math.max(1, Math.min(5, Math.round(4.2 + (BIAS[cls]?.[q.id] ?? 0) + ((hash(code + q.id) % 21) - 10) / 10)));
    const pool = TEXTS[cls] ?? [];
    return { scores, texts: i % 2 && pool.length ? { q6: pool[i % pool.length] } : {} };
  });
  return { id, cls, title, questions: qs, opened, due, size: n, responded, answers, closed };
}

export const RUNS: SurveyRun[] = persisted('survey-runs', [
  run(5, 'WEB-K15', 'Khảo sát giữa khoá', addDays(TODAY, -6), '2026-10-31', 22),
  run(4, 'DATA-K07', 'Khảo sát giữa khoá', addDays(TODAY, -6), '2026-10-31', 9),
  run(3, 'UIUX-K09', 'Khảo sát giữa khoá', '2026-09-28', '2026-10-05', 17, undefined, true),
  run(2, 'WEB-K14', 'Khảo sát cuối khoá', '2026-06-15', '2026-06-30', 25, 27, true),
  run(1, 'WEB-K15', 'Khảo sát đầu khoá', '2026-08-15', '2026-08-25', 28, undefined, true),
]);
/** Bản nháp của học viên, theo "mã đợt|mã học viên". Chỉ học viên đó đọc được. */
export const DRAFTS: Record<string, Answer> = persisted('survey-drafts', {});

export const isOpenRun = (r: SurveyRun) => !r.closed && r.due >= TODAY;
export const runState = (r: SurveyRun) => (isOpenRun(r) ? 'Đang mở' : 'Đã đóng');
export const notAnswered = (r: SurveyRun) => STUDENTS.filter((s) => s.cls === r.cls && s.status !== 'Thôi học' && !r.responded.includes(s.code));
