package com.ems.service;

import com.ems.model.ClassSubject;
import com.ems.model.Klass;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý kho dữ liệu Lớp học và Giáo trình buổi học chuẩn của Trung tâm
 * Tác vụ Jira: IDTTX-247 / IDTTX-263 [BE] - S4-01
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class ClassStore {

    private static final Map<String, Klass> CLASS_MAP = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> SUBJECT_TOPICS = new HashMap<>();
    private static final List<String> ROOM_LIST = Arrays.asList("P.401", "P.301", "P.302", "Lab 1", "Zoom 01", "Meet WEB");

    static {
        initTopics();
        resetToDefault();
    }

    private static void initTopics() {
        SUBJECT_TOPICS.put("GIT-01", Arrays.asList(
                "Làm quen Git & GitHub", "Nhánh (Branch) và gộp nhánh", "Tạo Pull Request & Code Review", "Xử lý xung đột (Merge Conflict)"
        ));
        SUBJECT_TOPICS.put("WEB-01", Arrays.asList(
                "Cấu trúc tài liệu HTML5", "Semantic HTML & Accessibility", "CSS Selector & Độ ưu tiên", "Box Model & Box Sizing",
                "Flexbox căn bản", "Flexbox nâng cao & căn chỉnh", "CSS Grid Layout", "Responsive Web Design",
                "Media Queries cho thiết bị di động", "Typography & Hệ màu hiện đại", "Styling Biểu mẫu & Hiệu ứng Hover", "Thực hành hoàn thiện giao diện Landing Page"
        ));
        SUBJECT_TOPICS.put("WEB-02", Arrays.asList(
                "Biến, Kiểu dữ liệu & Toán tử", "Cấu trúc điều kiện & Vòng lặp", "Hàm (Functions) & Scope", "Mảng & Các phương thức xử lý mảng",
                "Đối tượng (Objects) trong JavaScript", "Thao tác DOM căn bản", "Xử lý sự kiện (Event Handling)", "Biểu mẫu và xác thực đầu vào",
                "Lưu trữ cục bộ (LocalStorage/SessionStorage)", "Lập trình bất đồng bộ (Asynchronous)", "Xử lý Promise", "Cú pháp Async/Await hiện đại",
                "Giao tiếp API bằng Fetch & Axios", "Thực hành xây dựng ứng dụng Web tương tác hoàn chỉnh"
        ));
        SUBJECT_TOPICS.put("DB-01", Arrays.asList(
                "Tổng quan hệ CSDL quan hệ", "Thiết kế lược đồ ERD chuẩn 3NF", "Truy vấn dữ liệu SELECT căn bản", "Liên kết bảng (INNER, LEFT JOIN)",
                "Gom nhóm và thống kê (GROUP BY, HAVING)", "Truy vấn con (Subquery)", "Chỉ mục (Index) và tối ưu truy vấn", "Quản lý giao dịch (Transaction ACID)",
                "Chuẩn hoá dữ liệu thực tế", "Ôn tập và bài kiểm tra thiết kế CSDL"
        ));
        SUBJECT_TOPICS.put("WEB-03", Arrays.asList(
                "Giới thiệu React & JSX", "Components & Props", "State & Vòng đời Component", "Xử lý sự kiện trong React",
                "Render có điều kiện", "Render danh sách & Keys", "Kiểm soát Forms trong React", "Chia sẻ trạng thái (Lifting State Up)",
                "React Hooks (useState, useEffect)", "Custom Hooks tái sử dụng", "Điều hướng trang với React Router", "Dự án SPA cuối môn"
        ));
        SUBJECT_TOPICS.put("BE-01", Arrays.asList(
                "Kiến trúc Node.js Runtime", "Quản lý thư viện NPM", "Xây dựng Server với Express", "Thiết kế RESTful API chuẩn",
                "Middleware kiến trúc", "Kết nối và thao tác CSDL MySQL", "Xác thực người dùng & JWT Token", "Validate dữ liệu & Xử lý lỗi tập trung",
                "Xử lý tải lên tệp tin (File Upload)", "Bảo mật API (CORS, Rate Limit)", "Viết Unit Tests cho Endpoint", "Đóng gói Docker và triển khai"
        ));
        SUBJECT_TOPICS.put("JV-01", Arrays.asList(
                "Cú pháp Java Core căn bản", "Lập trình hướng đối tượng (OOP)", "Kế thừa và Đa hình", "Interface và Abstract Class",
                "Java Collections Framework (List, Map, Set)", "Xử lý Ngoại lệ (Exceptions)", "Java I/O và Files", "Generics trong Java",
                "Lambda Expression & Stream API", "JDBC kết nối cơ sở dữ liệu", "CRUD với PreparedStatement", "Mô hình MVC trong Java",
                "Java Servlet & Kiến trúc Web", "JSP, JSTL & Hoàn thiện dự án nhỏ"
        ));
        SUBJECT_TOPICS.put("TEST-01", Arrays.asList(
                "Tổng quan kiểm thử phần mềm", "Quy trình kiểm thử theo Agile/Scrum", "Kỹ thuật thiết kế Test Case hộp đen", "Phân tích giá trị biên (BVA)",
                "Phân vùng tương đương (EP)", "Bảng quyết định & Chuyển đổi trạng thái", "Kiểm thử hồi quy & Smoke Test", "Thực hành viết Test Plan & Test Cases"
        ));
    }

    public static synchronized void resetToDefault() {
        CLASS_MAP.clear();

        // Lớp WEB-K16: Lớp Chuẩn bị (64 buổi) - Lớp trọng tâm để Quản lý đào tạo sinh thời khoá biểu tự động
        List<ClassSubject> webK16Subs = new ArrayList<>();
        webK16Subs.add(new ClassSubject("GIT-01", "Git và làm việc nhóm", 4, "LE HOANG NAM", Collections.singletonList("PHAM GIA HUY")));
        webK16Subs.add(new ClassSubject("WEB-01", "HTML & CSS nền tảng", 12, "NGO THI THU", Collections.singletonList("NGO BAO TU")));
        webK16Subs.add(new ClassSubject("WEB-02", "JavaScript cơ bản", 14, "NGO THI THU"));
        webK16Subs.add(new ClassSubject("DB-01", "Cơ sở dữ liệu", 10, "VU DUC LONG"));
        webK16Subs.add(new ClassSubject("WEB-03", "React", 12, "DANG MINH SON"));
        webK16Subs.add(new ClassSubject("BE-01", "Node.js và API", 12, "VU DUC LONG"));

        Klass webK16 = new Klass("WEB-K16", "Lập trình web Full-stack", "2026-10-20", 30, "Kết hợp", "Chuẩn bị", "Chưa xếp lịch", "Meet WEB", webK16Subs);
        CLASS_MAP.put(webK16.getCode(), webK16);

        // Lớp chuẩn 60 buổi mẫu: Lớp JAVA-K15 (60 buổi)
        List<ClassSubject> java60Subs = new ArrayList<>();
        java60Subs.add(new ClassSubject("GIT-01", "Git và làm việc nhóm", 4, "LE HOANG NAM"));
        java60Subs.add(new ClassSubject("JV-01", "Java cơ bản & Hướng đối tượng", 14, "DANG MINH SON"));
        java60Subs.add(new ClassSubject("DB-01", "Cơ sở dữ liệu quan hệ", 10, "VU DUC LONG"));
        java60Subs.add(new ClassSubject("JV-02", "Lập trình Web Servlet/JSP & Spring Boot", 20, "DANG MINH SON"));
        java60Subs.add(new ClassSubject("JV-03", "Dự án thực tế & Triển khai hệ thống", 12, "DANG MINH SON"));

        Klass java60 = new Klass("JAVA-K15", "Lập trình Java Doanh nghiệp", "2026-11-01", 30, "Trực tiếp", "Chuẩn bị", "Chưa xếp lịch", "Lab 1", java60Subs);
        CLASS_MAP.put(java60.getCode(), java60);

        // Lớp TEST-K06: 32 buổi
        List<ClassSubject> testSubs = new ArrayList<>();
        testSubs.add(new ClassSubject("GIT-01", "Git và làm việc nhóm", 4, "PHAM THI VY"));
        testSubs.add(new ClassSubject("TEST-01", "Kiểm thử phần mềm cơ bản", 8, "PHAM THI VY"));
        testSubs.add(new ClassSubject("TEST-02", "Kiểm thử tự động", 12, "PHAM THI VY"));
        testSubs.add(new ClassSubject("TEST-03", "Kiểm thử API", 8, "PHAM THI VY"));

        Klass testK06 = new Klass("TEST-K06", "Kiểm thử phần mềm", "2026-11-02", 25, "Trực tuyến", "Chuẩn bị", "Chưa xếp lịch", "Zoom 01", testSubs);
        CLASS_MAP.put(testK06.getCode(), testK06);

        // Lớp WEB-K15: Đang học
        List<ClassSubject> webK15Subs = new ArrayList<>(webK16Subs);
        Klass webK15 = new Klass("WEB-K15", "Lập trình web Full-stack", "2026-08-12", 30, "Trực tiếp", "Đang học", "Tối 2-4-6", "P.401", webK15Subs);
        CLASS_MAP.put(webK15.getCode(), webK15);
    }

    public static List<Klass> getAllClasses() {
        List<Klass> list = new ArrayList<>(CLASS_MAP.values());
        list.sort(Comparator.comparing(Klass::getCode));
        return Collections.unmodifiableList(list);
    }

    public static Klass findByCode(String code) {
        if (code == null) return null;
        return CLASS_MAP.get(code.trim().toUpperCase());
    }

    public static String getTopicForLesson(String subjectCode, int lessonIndex) {
        List<String> topics = SUBJECT_TOPICS.get(subjectCode);
        if (topics != null && lessonIndex >= 1 && lessonIndex <= topics.size()) {
            return topics.get(lessonIndex - 1);
        }
        return "Bài học số " + lessonIndex;
    }

    public static List<String> getAvailableRooms() {
        return Collections.unmodifiableList(ROOM_LIST);
    }

    public static synchronized void saveClass(Klass klass) {
        if (klass != null && klass.getCode() != null) {
            CLASS_MAP.put(klass.getCode(), klass);
        }
    }
}
