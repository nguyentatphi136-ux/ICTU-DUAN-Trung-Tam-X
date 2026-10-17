package com.ems.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * S2-01: Đọc tệp nhập người dùng (.xlsx, .xls hoặc .csv) thành danh sách dòng
 * {fullName, email, phone, role, dateOfBirth, gender, address}.
 * Cột được nhận theo tiêu đề ở dòng đầu (tiếng Việt hoặc tiếng Anh), không phụ thuộc thứ tự.
 */
public final class UserImportParser {
    static final String[] FIELDS = {"fullName", "email", "phone", "role", "dateOfBirth", "gender", "address"};

    private UserImportParser() {}

    public static List<Map<String, Object>> parse(String fileName, InputStream in) throws IOException {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) return parseWorkbook(in);
        if (name.endsWith(".csv")) return parseCsv(in);
        throw new IllegalArgumentException("Chỉ nhận tệp .xlsx, .xls hoặc .csv.");
    }

    /** Vị trí cột của từng trường theo tiêu đề; -1 nếu tệp không có cột đó. */
    static int[] columnIndexes(List<String> headers) {
        int[] idx = new int[FIELDS.length];
        java.util.Arrays.fill(idx, -1);
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i).replace("﻿", "").toLowerCase(Locale.ROOT).replaceAll("[\\s_]", "");
            int field;
            if (h.contains("email") || h.contains("mail")) field = 1;
            else if (h.contains("thoại") || h.contains("phone") || h.contains("sđt") || h.contains("sdt")) field = 2;
            else if (h.contains("trò") || h.contains("role")) field = 3;
            else if (h.contains("sinh") || h.contains("dob") || h.contains("birth")) field = 4;
            else if (h.contains("tính") || h.contains("gender")) field = 5;
            else if (h.contains("chỉ") || h.contains("address")) field = 6;
            else if (h.contains("họ") || h.contains("tên") || h.contains("name")) field = 0;
            else continue;
            if (idx[field] < 0) idx[field] = i;
        }
        if (idx[0] < 0 || idx[1] < 0) {
            throw new IllegalArgumentException("Dòng đầu của tệp phải có cột \"Họ và tên\" và \"Email\". Hãy dùng tệp mẫu.");
        }
        return idx;
    }

    private static Map<String, Object> toRow(int[] idx, List<String> cells) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int f = 0; f < FIELDS.length; f++) {
            int c = idx[f];
            row.put(FIELDS[f], c >= 0 && c < cells.size() ? cells.get(c).trim() : "");
        }
        return row;
    }

    private static boolean blank(List<String> cells) {
        return cells.stream().allMatch(c -> c == null || c.isBlank());
    }

    static List<Map<String, Object>> parseWorkbook(InputStream in) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            int[] idx = null;
            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    cells.add(cellText(row.getCell(c), formatter));
                }
                if (blank(cells)) continue;
                if (idx == null) {
                    idx = columnIndexes(cells);
                } else {
                    rows.add(toRow(idx, cells));
                }
            }
        } catch (org.apache.poi.EncryptedDocumentException | org.apache.poi.UnsupportedFileFormatException e) {
            throw new IllegalArgumentException("Không đọc được tệp Excel (tệp có mật khẩu hoặc bị hỏng).");
        }
        return rows;
    }

    /** Ô ngày đổi về YYYY-MM-DD; ô số giữ nguyên chữ số (số điện thoại không bị dạng 9.12E8). */
    private static String cellText(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        if (type == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate().toString();
            }
            double v = cell.getNumericCellValue();
            if (v == Math.rint(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
        }
        return formatter.formatCellValue(cell);
    }

    static List<Map<String, Object>> parseCsv(InputStream in) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            int[] idx = null;
            while ((line = reader.readLine()) != null) {
                List<String> cells = splitCsvLine(line);
                if (blank(cells)) continue;
                if (idx == null) {
                    idx = columnIndexes(cells);
                } else {
                    rows.add(toRow(idx, cells));
                }
            }
        }
        return rows;
    }

    /** Tách một dòng CSV, hỗ trợ dấu phẩy hoặc chấm phẩy và giá trị trong ngoặc kép. */
    static List<String> splitCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        char delimiter = line.contains(";") && !line.contains(",") ? ';' : ',';
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens;
    }

    /** Tệp mẫu CSV (có BOM để Excel mở đúng tiếng Việt). Vai trò ghi bằng tiếng Việt. */
    public static String templateCsv() {
        return "﻿Họ và tên,Email,Số điện thoại,Vai trò,Ngày sinh,Giới tính,Địa chỉ\r\n"
                + "Nguyễn Văn An,an.nguyen@tms.vn,0912345678,Học viên,2003-05-15,Nam,Thái Nguyên\r\n"
                + "Trần Thị Bình,binh.tran@tms.vn,0987654321,Học viên,2002-11-20,Nữ,Hà Nội\r\n"
                + "Lê Hoàng Cường,cuong.le@tms.vn,0903112233,Giảng viên,1990-08-10,Nam,Đà Nẵng\r\n"
                + "Phạm Thu Dung,dung.pt@tms.vn,0356789123,Trợ giảng,1998-04-25,Nữ,Bắc Ninh\r\n";
    }
}
