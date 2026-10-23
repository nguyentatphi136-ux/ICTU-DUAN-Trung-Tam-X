package com.ems.dao;

import com.ems.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Data Access Object cho nghiệp vụ S6-01 (Jira IDTTX-273 / Subtask IDTTX-280 [BE]):
 * Quản lý bài tập, danh sách bài nộp cả lớp 25 học viên, chấm điểm, công bố và nhật ký sửa điểm.
 * Đồng bộ chuẩn 100% với dữ liệu và yêu cầu nghiệp vụ Jira dự án EMS:
 * - Lớp học: WEB-K15 (Sĩ số 25 học viên)
 * - Bài tập: Bài 14: Gọi API danh sách sản phẩm (Môn WEB-02)
 * - Giảng viên: VU DUC LONG (Vũ Đức Long)
 * - Học viên trọng tâm: HV260101 (NGUYEN MINH ANH)
 */
public class AssignmentGradingDAO {

    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final AtomicLong LOG_ID_GEN = new AtomicLong(100);

    // Kho dữ liệu In-Memory Demo
    private static final Map<Integer, AssignmentInfo> ASSIGNMENTS = new ConcurrentHashMap<>();
    private static final Map<Integer, List<AssignmentSubmission>> SUBMISSIONS = new ConcurrentHashMap<>();

    static {
        initSeedData();
    }

    public static void initSeedData() {
        ASSIGNMENTS.clear();
        SUBMISSIONS.clear();

        // 1. Khởi tạo bài tập trọng tâm Sprint 6 (Khớp 100% với Jira & assignments.ts)
        AssignmentInfo a14 = new AssignmentInfo(
                14,
                "Bài 14: Gọi API danh sách sản phẩm",
                "WEB-K15",
                "Lập trình Web Full-stack K15",
                "WEB-02",
                "Lập trình JavaScript & Web Frontend",
                "B14",
                "VU DUC LONG",
                "2026-10-23 23:59",
                10.0,
                "Dùng fetch gọi API danh sách sản phẩm và hiển thị dạng lưới thẻ.\n"
                        + "- Có trạng thái đang tải (Loading Spinner)\n"
                        + "- Báo lỗi khi API hỏng (Error boundary)\n"
                        + "- Nộp liên kết repo GitHub và tệp nén mã nguồn.",
                "Cho nộp"
        );
        ASSIGNMENTS.put(a14.getId(), a14);

        // Alias ID 101 để tương thích mọi đường link truy cập trước đó
        AssignmentInfo a101 = new AssignmentInfo(
                101,
                "Bài 14: Gọi API danh sách sản phẩm",
                "WEB-K15",
                "Lập trình Web Full-stack K15",
                "WEB-02",
                "Lập trình JavaScript & Web Frontend",
                "B14",
                "VU DUC LONG",
                "2026-10-23 23:59",
                10.0,
                a14.getDescription(),
                "Cho nộp"
        );
        ASSIGNMENTS.put(a101.getId(), a101);

        // 2. Danh sách chuẩn 25 học viên lớp WEB-K15 đồng bộ từ STUDENTS & StudentStatusDAO
        String[][] studentsSeed = {
                {"HV260101", "NGUYEN MINH ANH", "minhanh.ng@gmail.com"},
                {"HV260102", "HOANG YEN NHI", "nhi.hy@gmail.com"},
                {"HV260125", "BUI THANH TAM", "tam.bt@gmail.com"},
                {"HV260136", "HOANG GIA BAO", "bao.hg@gmail.com"},
                {"HV260137", "VU KHANH LINH", "linh.vk@gmail.com"},
                {"HV260139", "PHAM NGOC HAN", "han.pn@gmail.com"},
                {"HV260140", "LE MINH KHOI", "khoi.lm@gmail.com"},
                {"HV260142", "DO TUAN KIET", "kiet.dt@gmail.com"},
                {"HV260103", "TRAN VAN AN", "an.tv@gmail.com"},
                {"HV260104", "LE THI BINH", "binh.lt@gmail.com"},
                {"HV260105", "PHAM MINH CHI", "chi.pm@gmail.com"},
                {"HV260106", "HOANG THANH DUNG", "dung.ht@gmail.com"},
                {"HV260107", "VU HUU GIANG", "giang.vh@gmail.com"},
                {"HV260108", "DANG NGOC HANH", "hanh.dn@gmail.com"},
                {"HV260109", "BUI DUC KHANH", "khanh.bd@gmail.com"},
                {"HV260110", "DO GIA LINH", "linh.dg@gmail.com"},
                {"HV260111", "NGO VAN MINH", "minh.nv@gmail.com"},
                {"HV260112", "DUONG THI NGOC", "ngoc.dt@gmail.com"},
                {"HV260113", "LY MINH PHUONG", "phuong.lm@gmail.com"},
                {"HV260114", "NGUYEN THANH QUAN", "quan.nt@gmail.com"},
                {"HV260115", "TRAN HUU SON", "son.th@gmail.com"},
                {"HV260116", "LE NGOC THAO", "thao.ln@gmail.com"},
                {"HV260117", "PHAM DUC TUAN", "tuan.pd@gmail.com"},
                {"HV260118", "HOANG GIA VY", "vy.hg@gmail.com"},
                {"HV260119", "VU VAN HIEU", "hieu.vv@gmail.com"}
        };

        List<AssignmentSubmission> subsList14 = new CopyOnWriteArrayList<>();
        List<AssignmentSubmission> subsList101 = new CopyOnWriteArrayList<>();

        for (int i = 0; i < studentsSeed.length; i++) {
            String code = studentsSeed[i][0];
            String name = studentsSeed[i][1];
            String email = studentsSeed[i][2];

            boolean isLate = (i == 4 || i == 13 || i == 21); // Vài bài nộp muộn
            double lateHours = isLate ? (i == 4 ? 2.0 : (i == 13 ? 5.5 : 1.0)) : 0.0;
            String submittedTime = isLate ? "2026-10-24 01:30" : "2026-10-23 " + (19 + (i % 4)) + ":" + String.format("%02d", (i * 7) % 60);

            String repoUser = name.toLowerCase().replace(" ", "").replace("đ", "d")
                    .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                    .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                    .replaceAll("[ìíịỉĩ]", "i")
                    .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                    .replaceAll("[ùúụủũưừứựửữ]", "u")
                    .replaceAll("[ỳýỵỷỹ]", "y");
            String repoUrl = "https://github.com/" + repoUser + "/bai-14-api-products";

            String studentNote = (i == 0)
                    ? "Em đã bổ sung thêm tính năng tìm kiếm sản phẩm và hiển thị trạng thái loading như yêu cầu."
                    : (i % 4 == 0 ? "Em gửi thầy bài nộp, có làm thêm kiểm thử biên cho API." : "Em nộp bài đúng hạn ạ.");

            AssignmentSubmission sub = new AssignmentSubmission(
                    1000 + i + 1,
                    14,
                    code,
                    name,
                    email,
                    "WEB-K15",
                    isLate,
                    lateHours
            );

            // Tệp đính kèm phiên bản mới nhất
            List<SubmissionFile> files = new ArrayList<>();
            files.add(new SubmissionFile(
                    "de-bai-14.pdf",
                    184320,
                    "180 KB",
                    "PDF",
                    "ĐỀ BÀI THỰC HÀNH SỐ 14: GỌI API DANH SÁCH SẢN PHẨM\n"
                            + "Môn học: WEB-02 (Lập trình JavaScript & Web Frontend)\n"
                            + "Giảng viên: VU DUC LONG\n"
                            + "Lớp: WEB-K15\n\n"
                            + "1. YÊU CẦU BẮT BUỘC:\n"
                            + "   - Sử dụng fetch() hoặc async/await gọi endpoint GET /api/products.\n"
                            + "   - Hiển thị danh sách sản phẩm dạng card grid (hình ảnh, tên, giá, nút mua).\n"
                            + "   - Xử lý hiệu ứng Loading Skeleton hoặc Spinner khi dữ liệu đang tải.\n"
                            + "   - Bắt lỗi mạng (try...catch) và hiển thị thông báo thân thiện cho người dùng.\n\n"
                            + "2. HÌNH THỨC NỘP:\n"
                            + "   - Đính kèm mã nguồn và liên kết GitHub repo."
            ));

            files.add(new SubmissionFile(
                    "ProductList.js",
                    4250,
                    "4.2 KB",
                    "CODE",
                    "// ProductList.js - Module gọi API và render sản phẩm\n"
                            + "async function fetchProducts() {\n"
                            + "    const container = document.getElementById('product-grid');\n"
                            + "    const loader = document.getElementById('loading-spinner');\n"
                            + "    try {\n"
                            + "        loader.style.display = 'block';\n"
                            + "        const res = await fetch('https://api.ems.edu.vn/v1/products');\n"
                            + "        if (!res.ok) throw new Error(`HTTP Lỗi: ${res.status}`);\n"
                            + "        const products = await res.json();\n"
                            + "        renderProducts(products);\n"
                            + "    } catch (err) {\n"
                            + "        showError(`Không thể tải sản phẩm: ${err.message}`);\n"
                            + "    } finally {\n"
                            + "        loader.style.display = 'none';\n"
                            + "    }\n"
                            + "}\n"
            ));

            files.add(new SubmissionFile(
                    "bai-14-" + code.toLowerCase() + ".zip",
                    1240000,
                    "1.2 MB",
                    "ZIP",
                    "[Mã nguồn hoàn chỉnh bài tập số 14 gồm index.html, style.css và ProductList.js]"
            ));

            int versionNo = (i % 3 == 0) ? 2 : 1;
            SubmissionVersion v1 = new SubmissionVersion(versionNo, submittedTime, files, repoUrl, studentNote);
            sub.addVersion(v1);

            // Dữ liệu chấm điểm mẫu ban đầu
            if (i == 0) {
                // Học viên HV260101 (NGUYEN MINH ANH): Đã chấm và ĐÃ CÔNG BỐ
                SubmissionGrade grade = new SubmissionGrade(9.0, "Làm đủ yêu cầu, code sạch, có xử lý lỗi mạng và spinner tải rất tốt.", "VU DUC LONG", "2026-10-23 20:30:00", true);
                grade.setPublishedAt("2026-10-23 20:35:00");
                grade.setPublishedBy("VU DUC LONG");

                grade.addAuditLog(new GradeAuditLog(
                        LOG_ID_GEN.incrementAndGet(),
                        "2026-10-23 20:35:00",
                        "VU DUC LONG",
                        "Công bố điểm",
                        9.0,
                        9.0,
                        null,
                        "Công bố điểm 9."
                ));
                grade.addAuditLog(new GradeAuditLog(
                        LOG_ID_GEN.incrementAndGet(),
                        "2026-10-23 20:30:00",
                        "VU DUC LONG",
                        "Chấm lần đầu",
                        null,
                        9.0,
                        null,
                        "Chấm lần đầu: 9."
                ));
                sub.setGrade(grade);
            } else if (i == 1) {
                // Học viên HV260102 (HOANG YEN NHI): Đã chấm nhưng LƯU NHÁP (CHƯA CÔNG BỐ)
                SubmissionGrade grade = new SubmissionGrade(8.5, "Đúng yêu cầu, cần thêm kiểm thử cho trường hợp biên danh sách rỗng.", "VU DUC LONG", "2026-10-23 21:00:00", false);
                grade.addAuditLog(new GradeAuditLog(
                        LOG_ID_GEN.incrementAndGet(),
                        "2026-10-23 21:00:00",
                        "VU DUC LONG",
                        "Chấm lần đầu",
                        null,
                        8.5,
                        null,
                        "Chấm lần đầu: 8,5."
                ));
                sub.setGrade(grade);
            } else if (i == 2) {
                // Học viên HV260125: Đã chấm nháp, từng sửa điểm
                SubmissionGrade grade = new SubmissionGrade(8.0, "Code gọn, nên tách hàm render nhỏ hơn.", "VU DUC LONG", "2026-10-23 21:15:00", false);
                grade.addAuditLog(new GradeAuditLog(
                        LOG_ID_GEN.incrementAndGet(),
                        "2026-10-23 21:15:00",
                        "VU DUC LONG",
                        "Sửa điểm",
                        7.5,
                        8.0,
                        "Xem lại repo thấy có tách file CSS riêng",
                        "Điểm 7,5 → 8. Lý do: Xem lại repo thấy có tách file CSS riêng."
                ));
                grade.addAuditLog(new GradeAuditLog(
                        LOG_ID_GEN.incrementAndGet(),
                        "2026-10-23 21:05:00",
                        "VU DUC LONG",
                        "Chấm lần đầu",
                        null,
                        7.5,
                        null,
                        "Chấm lần đầu: 7,5."
                ));
                sub.setGrade(grade);
            }

            subsList14.add(sub);

            // Clone đối tượng cho assignment 101 để độc lập
            AssignmentSubmission sub101 = new AssignmentSubmission(
                    2000 + i + 1,
                    101,
                    code,
                    name,
                    email,
                    "WEB-K15",
                    isLate,
                    lateHours
            );
            sub101.addVersion(v1);
            if (sub.getGrade() != null) {
                sub101.setGrade(sub.getGrade());
            }
            subsList101.add(sub101);
        }

        SUBMISSIONS.put(14, subsList14);
        SUBMISSIONS.put(101, subsList101);
    }

    public AssignmentInfo getAssignment(int assignmentId) {
        AssignmentInfo a = ASSIGNMENTS.get(assignmentId);
        if (a == null && !ASSIGNMENTS.isEmpty()) {
            return ASSIGNMENTS.values().iterator().next();
        }
        return a;
    }

    public List<AssignmentSubmission> getSubmissions(int assignmentId) {
        List<AssignmentSubmission> list = SUBMISSIONS.get(assignmentId);
        if (list == null && !SUBMISSIONS.isEmpty()) {
            list = SUBMISSIONS.values().iterator().next();
        }
        return list != null ? list : Collections.emptyList();
    }

    public AssignmentSubmission getSubmission(int assignmentId, String studentCode) {
        List<AssignmentSubmission> list = getSubmissions(assignmentId);
        for (AssignmentSubmission sub : list) {
            if (sub.getStudentCode().equalsIgnoreCase(studentCode)) {
                return sub;
            }
        }
        return null;
    }

    public int getSubmissionIndex(int assignmentId, String studentCode) {
        List<AssignmentSubmission> list = getSubmissions(assignmentId);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getStudentCode().equalsIgnoreCase(studentCode)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Định dạng điểm hiển thị theo chuẩn Việt Nam (VD: "8,5", "9")
     */
    public static String formatScoreVN(double score) {
        if (score == Math.floor(score)) {
            return String.format("%.0f", score);
        }
        return String.format(Locale.US, "%.1f", score).replace('.', ',');
    }

    /**
     * Parse điểm từ chuỗi, chấp nhận cả dấu phẩy lẫn dấu chấm (VD: "8,5" hoặc "8.5")
     */
    public static double parseScoreVN(String scoreStr) throws NumberFormatException {
        if (scoreStr == null || scoreStr.trim().isEmpty()) {
            throw new NumberFormatException("Điểm rỗng");
        }
        String clean = scoreStr.trim().replace(',', '.');
        return Double.parseDouble(clean);
    }

    /**
     * Nghiệp vụ S6-01: Lưu điểm và nhận xét (Lưu nháp)
     * Đồng bộ chuẩn thông điệp và quy tắc kiểm duyệt:
     * - "Nhập số từ 0 đến [max], cho phép một chữ số thập phân."
     * - "Viết nhận xét để học viên biết cần cải thiện gì."
     * - "Điểm đã công bố, ghi lý do sửa điểm."
     */
    public synchronized boolean saveGrade(int assignmentId, String studentCode, double score,
                                          String comment, String reason, String actorName) throws IllegalArgumentException {
        AssignmentInfo assignment = getAssignment(assignmentId);
        if (assignment == null) {
            throw new IllegalArgumentException("Không tìm thấy bài tập!");
        }

        AssignmentSubmission sub = getSubmission(assignmentId, studentCode);
        if (sub == null) {
            throw new IllegalArgumentException("Không tìm thấy bài nộp của học viên: " + studentCode);
        }

        // 1. Kiểm tra giới hạn điểm số (0 <= score <= maxScore)
        if (score < 0.0 || score > assignment.getMaxScore()) {
            throw new IllegalArgumentException(String.format("Nhập số từ 0 đến %.0f, cho phép một chữ số thập phân.", assignment.getMaxScore()));
        }

        // 2. Kiểm tra nhận xét
        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("Viết nhận xét để học viên biết cần cải thiện gì.");
        }

        // Làm tròn 1 chữ số thập phân
        score = Math.round(score * 10.0) / 10.0;
        String nowStr = LocalDateTime.now().format(DT_FORMATTER);
        String formattedScore = formatScoreVN(score);

        SubmissionGrade currentGrade = sub.getGrade();

        if (currentGrade == null) {
            // Chấm lần đầu
            SubmissionGrade newGrade = new SubmissionGrade(score, comment.trim(), actorName, nowStr, false);
            GradeAuditLog log = new GradeAuditLog(
                    LOG_ID_GEN.incrementAndGet(),
                    nowStr,
                    actorName,
                    "Chấm điểm",
                    null,
                    score,
                    null,
                    "Chấm lần đầu: " + formattedScore + "."
            );
            log.setTarget(sub.getStudentCode() + " · " + assignment.getClassCode() + " · " + assignment.getShortTitle());
            newGrade.addAuditLog(log);
            sub.setGrade(newGrade);
        } else {
            // Sửa điểm hoặc cập nhật nhận xét
            Double oldScore = currentGrade.getScore();
            boolean scoreChanged = (oldScore == null || Math.abs(oldScore - score) > 0.001);

            // QUY TẮC S6-01: Điểm đã công bố, ghi lý do sửa điểm
            if (currentGrade.isPublished() && scoreChanged) {
                if (reason == null || reason.trim().isEmpty()) {
                    throw new IllegalArgumentException("Điểm đã công bố, ghi lý do sửa điểm.");
                }
            }

            if (scoreChanged) {
                String oldScoreFormatted = formatScoreVN(oldScore != null ? oldScore : 0.0);
                String reasonStr = (reason != null && !reason.trim().isEmpty()) ? " Lý do: " + reason.trim() + "." : "";
                String logText = "Điểm " + oldScoreFormatted + " → " + formattedScore + "." + reasonStr;

                GradeAuditLog log = new GradeAuditLog(
                        LOG_ID_GEN.incrementAndGet(),
                        nowStr,
                        actorName,
                        "Sửa điểm",
                        oldScore,
                        score,
                        reason != null ? reason.trim() : null,
                        logText
                );
                log.setTarget(sub.getStudentCode() + " · " + assignment.getClassCode() + " · " + assignment.getShortTitle());
                currentGrade.addAuditLog(log);
            } else if (!comment.trim().equals(currentGrade.getComment())) {
                GradeAuditLog log = new GradeAuditLog(
                        LOG_ID_GEN.incrementAndGet(),
                        nowStr,
                        actorName,
                        "Cập nhật nhận xét",
                        oldScore,
                        score,
                        null,
                        "Cập nhật nhận xét của giảng viên."
                );
                log.setTarget(sub.getStudentCode() + " · " + assignment.getClassCode() + " · " + assignment.getShortTitle());
                currentGrade.addAuditLog(log);
            }

            currentGrade.setScore(score);
            currentGrade.setComment(comment.trim());
            currentGrade.setGradedBy(actorName);
            currentGrade.setGradedAt(nowStr);
        }

        sub.updateState();
        return true;
    }

    /**
     * Nghiệp vụ S6-01: Giảng viên bấm Công bố điểm
     * Điểm chỉ hiển thị cho học viên sau khi giảng viên bấm Công bố.
     */
    public synchronized boolean publishGrade(int assignmentId, String studentCode, String actorName) throws IllegalStateException {
        AssignmentInfo assignment = getAssignment(assignmentId);
        AssignmentSubmission sub = getSubmission(assignmentId, studentCode);
        if (sub == null || sub.getGrade() == null) {
            throw new IllegalStateException("Học viên chưa có điểm nháp để công bố. Vui lòng chấm điểm trước.");
        }

        SubmissionGrade grade = sub.getGrade();
        String nowStr = LocalDateTime.now().format(DT_FORMATTER);

        grade.setPublished(true);
        grade.setPublishedAt(nowStr);
        grade.setPublishedBy(actorName);

        String scoreStr = formatScoreVN(grade.getScore());
        GradeAuditLog log = new GradeAuditLog(
                LOG_ID_GEN.incrementAndGet(),
                nowStr,
                actorName,
                "Công bố điểm",
                grade.getScore(),
                grade.getScore(),
                null,
                "Công bố điểm " + scoreStr + "."
        );
        if (assignment != null) {
            log.setTarget(sub.getStudentCode() + " · " + assignment.getClassCode() + " · " + assignment.getShortTitle());
        }
        grade.addAuditLog(log);
        sub.updateState();
        return true;
    }

    public synchronized int publishAllGraded(int assignmentId, String actorName) {
        List<AssignmentSubmission> list = getSubmissions(assignmentId);
        int count = 0;
        for (AssignmentSubmission sub : list) {
            if (sub.getGrade() != null && !sub.getGrade().isPublished()) {
                publishGrade(assignmentId, sub.getStudentCode(), actorName);
                count++;
            }
        }
        return count;
    }

    public Map<String, Object> getGradingStatistics(int assignmentId) {
        List<AssignmentSubmission> list = getSubmissions(assignmentId);
        int total = list.size();
        int graded = 0;
        int published = 0;
        int lateCount = 0;

        for (AssignmentSubmission sub : list) {
            if (sub.isLate()) {
                lateCount++;
            }
            if (sub.getGrade() != null) {
                graded++;
                if (sub.getGrade().isPublished()) {
                    published++;
                }
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("graded", graded);
        stats.put("published", published);
        stats.put("lateCount", lateCount);
        stats.put("pending", total - graded);
        stats.put("progressPercent", total > 0 ? (int) Math.round(((double) graded / total) * 100) : 0);
        return stats;
    }
}
