package com.ems.model;

/**
 * Bốn trạng thái điểm danh học viên theo đặc tả S4-07 (Jira EP-05):
 * 1. Có mặt (PRESENT)
 * 2. Đi muộn (LATE)
 * 3. Vắng có phép (EXCUSED_ABSENCE)
 * 4. Vắng không phép (UNEXCUSED_ABSENCE)
 */
public enum AttendanceStatus {
    PRESENT("Có mặt", "PRESENT"),
    LATE("Đi muộn", "LATE"),
    EXCUSED_ABSENCE("Vắng có phép", "EXCUSED_ABSENCE"),
    UNEXCUSED_ABSENCE("Vắng không phép", "UNEXCUSED_ABSENCE");

    private final String label;
    private final String code;

    AttendanceStatus(String label, String code) {
        this.label = label;
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return code;
    }

    /**
     * Chuyển đổi linh hoạt từ chuỗi (tiếng Việt có dấu, không dấu, tên Enum hoặc mã code)
     */
    public static AttendanceStatus from(String input) {
        if (input == null || input.trim().isEmpty()) {
            return PRESENT; // Mặc định là Có mặt theo luồng điểm danh nhanh
        }
        String clean = input.trim();
        for (AttendanceStatus s : values()) {
            if (s.name().equalsIgnoreCase(clean)
                    || s.code.equalsIgnoreCase(clean)
                    || s.label.equalsIgnoreCase(clean)) {
                return s;
            }
        }
        // Hỗ trợ thêm các từ viết tắt / nhãn ngắn trong giao diện S4-07
        if (clean.equalsIgnoreCase("Muộn") || clean.equalsIgnoreCase("Di muon") || clean.equalsIgnoreCase("Muon")) {
            return LATE;
        }
        if (clean.equalsIgnoreCase("Có phép") || clean.equalsIgnoreCase("Co phep") || clean.equalsIgnoreCase("Vang co phep")) {
            return EXCUSED_ABSENCE;
        }
        if (clean.equalsIgnoreCase("Vắng") || clean.equalsIgnoreCase("Vang") || clean.equalsIgnoreCase("Vang khong phep")) {
            return UNEXCUSED_ABSENCE;
        }
        if (clean.equalsIgnoreCase("Co mat")) {
            return PRESENT;
        }

        throw new IllegalArgumentException("Trạng thái điểm danh không hợp lệ: '" + input
                + "'. Bốn trạng thái hợp lệ theo quy định S4-07: Có mặt, Đi muộn, Vắng có phép, Vắng không phép.");
    }
}
