// Dữ liệu và hàm lưu trữ cho S2-04 đến S2-07 (Chương trình đào tạo, Môn học, Gắn môn, Buổi học).
// Hỗ trợ lưu trữ bền vững và đồng bộ dữ liệu giữa các menu theo thiết kế Figma TMS.
import { persisted } from './store';

export type Program = {
  id?: number;
  code: string;
  name: string;
  description: string;
  hours: number;
  fee: number;
  runningClasses: number;
  active: boolean;
};

export type Subject = {
  id?: number;
  code: string;
  name: string;
  sessions: number;
  weight: number;
  outcome: string;
  openedClasses: number;
};

export type CurriculumItem = {
  code: string;
  prereq: string[];
};

export type Lesson = {
  topic: string;
  goal: string;
};

export const PROGRAMS: Program[] = persisted('programs.v2', [
  { code: 'WEB-FS', name: 'Lập trình web Full-stack', description: 'Đào tạo lập trình viên Full-stack toàn diện từ giao diện người dùng đến cơ sở dữ liệu và API.', hours: 240, fee: 9500000, runningClasses: 3, active: true },
  { code: 'DATA-01', name: 'Phân tích dữ liệu', description: 'Trang bị kỹ năng phân tích, trực quan hoá dữ liệu và SQL chuyên sâu cho phân tích nghiệp vụ.', hours: 180, fee: 5500000, runningClasses: 2, active: true },
  { code: 'UIUX-01', name: 'Thiết kế UI/UX', description: 'Quy trình nghiên cứu trải nghiệm người dùng, xây dựng wireframe và thiết kế giao diện Figma chuẩn chuyên nghiệp.', hours: 150, fee: 5000000, runningClasses: 1, active: true },
  { code: 'JAVA-BE', name: 'Lập trình Java Backend', description: 'Phát triển dịch vụ backend bằng Java, Spring Boot, JPA và kiến trúc microservices.', hours: 200, fee: 6000000, runningClasses: 1, active: true },
  { code: 'TEST-01', name: 'Kiểm thử phần mềm', description: 'Quy trình kiểm thử thủ công và tự động, viết test case, quản lý bug và đảm bảo chất lượng hệ thống.', hours: 120, fee: 3000000, runningClasses: 0, active: true },
  { code: 'PHP-OLD', name: 'Lập trình PHP cơ bản', description: 'Khoá học PHP cũ (ngừng áp dụng để chuyển sang khung chương trình mới).', hours: 120, fee: 3000000, runningClasses: 0, active: false },
], true);

export const SUBJECTS: Subject[] = persisted('subjects', [
  { code: 'WEB-01', name: 'HTML & CSS nền tảng', sessions: 12, weight: 1, outcome: 'Dựng được trang web tĩnh có bố cục đáp ứng chuẩn Responsive Web Design.', openedClasses: 5 },
  { code: 'WEB-02', name: 'JavaScript cơ bản', sessions: 14, weight: 2, outcome: 'Viết được JavaScript xử lý sự kiện DOM, gọi REST API và lập trình bất đồng bộ.', openedClasses: 5 },
  { code: 'WEB-03', name: 'React', sessions: 12, weight: 2, outcome: 'Dựng được ứng dụng một trang (SPA) bằng React, quản lý state và hook nâng cao.', openedClasses: 3 },
  { code: 'DB-01', name: 'Cơ sở dữ liệu', sessions: 10, weight: 2, outcome: 'Thiết kế được lược đồ quan hệ, viết được truy vấn SQL có JOIN và GROUP BY, hiểu chỉ mục cơ bản.', openedClasses: 6 },
  { code: 'BE-01', name: 'Node.js và API', sessions: 12, weight: 3, outcome: 'Viết được REST API bảo mật với xác thực JWT, phân quyền và kết nối cơ sở dữ liệu.', openedClasses: 3 },
  { code: 'GIT-01', name: 'Git và làm việc nhóm', sessions: 4, weight: 1, outcome: 'Thành thạo Git theo luồng nhánh feature branch, giải quyết conflict và pull request.', openedClasses: 8 },
  { code: 'UX-01', name: 'Nguyên lý thiết kế', sessions: 8, weight: 1, outcome: 'Áp dụng được nguyên lý bố cục lưới, phân cấp thị giác, bảng màu và typography.', openedClasses: 0 },
  { code: 'TEST-01', name: 'Kiểm thử cơ bản', sessions: 8, weight: 1, outcome: 'Viết được test case kiểm thử chức năng và báo cáo lỗi rõ ràng trên Jira.', openedClasses: 0 },
  { code: 'DEVOPS-01', name: 'Triển khai ứng dụng', sessions: 6, weight: 1, outcome: 'Đóng gói Docker và đưa ứng dụng lên máy chủ đám mây với CI/CD cơ bản.', openedClasses: 0 },
  { code: 'ENG-01', name: 'Tiếng Anh chuyên ngành', sessions: 10, weight: 1, outcome: 'Đọc hiểu tài liệu kỹ thuật tiếng Anh và giao tiếp cơ bản trong dự án phần mềm.', openedClasses: 0 },
  { code: 'SOFT-01', name: 'Kỹ năng phỏng vấn', sessions: 4, weight: 1, outcome: 'Chuẩn bị CV chuyên nghiệp và tự tin trả lời phỏng vấn kỹ thuật với nhà tuyển dụng.', openedClasses: 0 },
  { code: 'WEB-00', name: 'HTML & CSS (khoá cũ)', sessions: 12, weight: 1, outcome: 'Học phần cũ phục vụ các lớp chuyển tiếp.', openedClasses: 4 },
], true);

export const CURRICULUM: Record<string, CurriculumItem[]> = persisted('curriculum', {
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
}, true);

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

export const LESSONS: Record<string, Lesson[]> = persisted('lessons', {
  'WEB-01': HTML_LESSONS,
  'WEB-00': [
    ...HTML_LESSONS,
    { topic: 'Biến CSS và chủ đề', goal: 'Đổi màu toàn trang bằng biến CSS' },
    { topic: 'Hiệu ứng chuyển động', goal: 'Dùng transition và animation đơn giản' },
    { topic: 'Tối ưu hiển thị', goal: 'Nén ảnh, tải font đúng cách' },
    { topic: 'Bài tập cuối môn', goal: 'Hoàn thành trang landing page' },
  ],
});

export const loadPrograms = (): Program[] => PROGRAMS;
export const savePrograms = (list: Program[]): void => {
  PROGRAMS.splice(0, PROGRAMS.length, ...list);
};

export const loadSubjects = (): Subject[] => SUBJECTS;
export const saveSubjects = (list: Subject[]): void => {
  SUBJECTS.splice(0, SUBJECTS.length, ...list);
};

export const loadCurriculum = (): Record<string, CurriculumItem[]> => CURRICULUM;
export const saveCurriculum = (map: Record<string, CurriculumItem[]>): void => {
  for (const k of Object.keys(CURRICULUM)) delete CURRICULUM[k];
  Object.assign(CURRICULUM, map);
};

export const loadLessons = (): Record<string, Lesson[]> => LESSONS;
export const saveLessons = (map: Record<string, Lesson[]>): void => {
  for (const k of Object.keys(LESSONS)) delete LESSONS[k];
  Object.assign(LESSONS, map);
};

/** Số chương trình đang dùng một môn (cột "Dùng trong"). */
export function programsUsing(code: string, programsList = PROGRAMS, curriculumMap = CURRICULUM): Program[] {
  return programsList.filter((p) => curriculumMap[p.code]?.some((c) => c.code === code));
}

export const money = (n: number) => n.toLocaleString('vi-VN') + ' ₫';
