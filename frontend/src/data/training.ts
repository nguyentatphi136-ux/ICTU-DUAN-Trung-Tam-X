// Dữ liệu mẫu cho S2-04 đến S2-07. Khi tích hợp: /training/programs, /training/subjects, /training/programs/:code/subjects,
// /training/subjects/:code/sessions. Mảng ở đây được sửa trực tiếp để các trang dùng chung một nguồn khi chưa có API.
export type Program = { code: string; name: string; description: string; hours: number; fee: number; runningClasses: number; active: boolean };
export type Subject = { code: string; name: string; sessions: number; weight: number; outcome: string; openedClasses: number };
export type CurriculumItem = { code: string; prereq: string[] };
export type Lesson = { topic: string; goal: string };

export const PROGRAMS: Program[] = [
  { code: 'WEB-FS', name: 'Lập trình web Full-stack', description: '', hours: 240, fee: 28000000, runningClasses: 3, active: true },
  { code: 'DATA-01', name: 'Phân tích dữ liệu', description: '', hours: 180, fee: 22000000, runningClasses: 2, active: true },
  { code: 'UIUX-01', name: 'Thiết kế UI/UX', description: '', hours: 150, fee: 18500000, runningClasses: 1, active: true },
  { code: 'JAVA-BE', name: 'Lập trình Java Backend', description: '', hours: 200, fee: 24000000, runningClasses: 1, active: true },
  { code: 'TEST-01', name: 'Kiểm thử phần mềm', description: '', hours: 120, fee: 15000000, runningClasses: 0, active: true },
  { code: 'PHP-OLD', name: 'Lập trình PHP cơ bản', description: '', hours: 120, fee: 12000000, runningClasses: 0, active: false },
];

export const SUBJECTS: Subject[] = [
  { code: 'WEB-01', name: 'HTML & CSS nền tảng', sessions: 12, weight: 1, outcome: 'Dựng được trang web tĩnh có bố cục đáp ứng.', openedClasses: 5 },
  { code: 'WEB-02', name: 'JavaScript cơ bản', sessions: 14, weight: 2, outcome: 'Viết được JavaScript xử lý sự kiện và gọi API.', openedClasses: 5 },
  { code: 'WEB-03', name: 'React', sessions: 12, weight: 2, outcome: 'Dựng được ứng dụng một trang bằng React.', openedClasses: 3 },
  { code: 'DB-01', name: 'Cơ sở dữ liệu', sessions: 10, weight: 2, outcome: 'Thiết kế được lược đồ quan hệ, viết được truy vấn SQL có JOIN và GROUP BY, hiểu chỉ mục cơ bản.', openedClasses: 6 },
  { code: 'BE-01', name: 'Node.js và API', sessions: 12, weight: 3, outcome: 'Viết được REST API có xác thực.', openedClasses: 3 },
  { code: 'GIT-01', name: 'Git và làm việc nhóm', sessions: 4, weight: 1, outcome: 'Dùng được Git theo nhánh và pull request.', openedClasses: 8 },
  { code: 'UX-01', name: 'Nguyên lý thiết kế', sessions: 8, weight: 1, outcome: 'Áp dụng được nguyên lý bố cục, màu và chữ.', openedClasses: 0 },
  { code: 'TEST-01', name: 'Kiểm thử cơ bản', sessions: 8, weight: 1, outcome: 'Viết được test case và báo lỗi rõ ràng.', openedClasses: 0 },
  { code: 'DEVOPS-01', name: 'Triển khai ứng dụng', sessions: 6, weight: 1, outcome: 'Đưa được ứng dụng lên máy chủ.', openedClasses: 0 },
  { code: 'ENG-01', name: 'Tiếng Anh chuyên ngành', sessions: 10, weight: 1, outcome: 'Đọc được tài liệu kỹ thuật tiếng Anh.', openedClasses: 0 },
  { code: 'SOFT-01', name: 'Kỹ năng phỏng vấn', sessions: 4, weight: 1, outcome: 'Tự tin trả lời phỏng vấn kỹ thuật.', openedClasses: 0 },
  { code: 'WEB-00', name: 'HTML & CSS (khoá cũ)', sessions: 12, weight: 1, outcome: '', openedClasses: 4 },
];

export const CURRICULUM: Record<string, CurriculumItem[]> = {
  'WEB-FS': [
    { code: 'GIT-01', prereq: [] },
    { code: 'WEB-01', prereq: [] },
    { code: 'WEB-02', prereq: ['WEB-01'] },
    { code: 'DB-01', prereq: [] },
    { code: 'WEB-03', prereq: ['WEB-02'] },
    { code: 'BE-01', prereq: ['WEB-02', 'DB-01'] },
  ],
  'DATA-01': [{ code: 'DB-01', prereq: [] }, { code: 'GIT-01', prereq: [] }],
  'JAVA-BE': [{ code: 'DB-01', prereq: [] }, { code: 'GIT-01', prereq: [] }],
  'UIUX-01': [{ code: 'UX-01', prereq: [] }, { code: 'GIT-01', prereq: [] }],
};

const HTML_LESSONS: Lesson[] = [
  { topic: 'Cấu trúc trang HTML', goal: 'Dựng được trang có tiêu đề, đoạn văn, danh sách, liên kết' },
  { topic: 'Thẻ ngữ nghĩa và biểu mẫu', goal: 'Dùng đúng header, main, section; tạo form có nhãn' },
  { topic: 'CSS cơ bản', goal: 'Áp dụng selector, màu, font, khoảng cách' },
  { topic: 'Box model', goal: 'Giải thích được margin, border, padding và box-sizing' },
  { topic: 'Flexbox', goal: 'Dàn được thanh menu và lưới thẻ bằng flex' },
  { topic: 'CSS Grid', goal: 'Dựng bố cục trang hai cột bằng grid' },
  { topic: 'Responsive', goal: 'Trang hiển thị tốt ở 360px và 1440px' },
  { topic: 'Bài tập giữa môn', goal: 'Hoàn thành trang giới thiệu cá nhân' },
];

export const LESSONS: Record<string, Lesson[]> = {
  'WEB-01': HTML_LESSONS,
  'WEB-00': [
    ...HTML_LESSONS,
    { topic: 'Biến CSS và chủ đề', goal: 'Đổi màu toàn trang bằng biến CSS' },
    { topic: 'Hiệu ứng chuyển động', goal: 'Dùng transition và animation đơn giản' },
    { topic: 'Tối ưu hiển thị', goal: 'Nén ảnh, tải font đúng cách' },
    { topic: 'Bài tập cuối môn', goal: 'Hoàn thành trang landing page' },
  ],
};

/** Số chương trình đang dùng một môn (cột "Dùng trong"). */
export const programsUsing = (code: string) => PROGRAMS.filter((p) => CURRICULUM[p.code]?.some((c) => c.code === code));

export const money = (n: number) => n.toLocaleString('vi-VN') + ' ₫';
