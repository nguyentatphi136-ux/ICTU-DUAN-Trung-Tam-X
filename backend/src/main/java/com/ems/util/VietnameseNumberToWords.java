package com.ems.util;

import java.math.BigDecimal;

/**
 * Tiện ích đọc số tiền thành chữ bằng tiếng Việt chuẩn
 * Ví dụ: 10080000 -> "Mười triệu không trăm tám mươi nghìn đồng chẵn"
 */
public final class VietnameseNumberToWords {

    private static final String[] DIGITS = {
            "không", "một", "hai", "ba", "bốn",
            "năm", "sáu", "bảy", "tám", "chín"
    };

    private static final String[] UNITS = {
            "", "nghìn", "triệu", "tỷ", "nghìn tỷ", "triệu tỷ"
    };

    private VietnameseNumberToWords() {}

    public static String convert(BigDecimal amount) {
        if (amount == null) return "Không đồng";
        return convert(amount.longValue());
    }

    public static String convert(long amount) {
        if (amount == 0) {
            return "Không đồng";
        }
        if (amount < 0) {
            return "Âm " + convert(-amount);
        }

        String s = String.valueOf(amount);
        // Chia thành các nhóm 3 chữ số từ phải qua trái
        int len = s.length();
        int numGroups = (len + 2) / 3;
        int[] groups = new int[numGroups];

        for (int i = 0; i < numGroups; i++) {
            int end = len - i * 3;
            int start = Math.max(0, end - 3);
            groups[i] = Integer.parseInt(s.substring(start, end));
        }

        StringBuilder sb = new StringBuilder();
        for (int i = numGroups - 1; i >= 0; i--) {
            int g = groups[i];
            if (g > 0 || (i == 0 && sb.length() == 0)) {
                boolean showHundreds = (i < numGroups - 1);
                String groupText = readThreeDigits(g, showHundreds);
                if (!groupText.isEmpty()) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(groupText);
                    if (i < UNITS.length && !UNITS[i].isEmpty()) {
                        sb.append(" ").append(UNITS[i]);
                    }
                }
            }
        }

        String res = sb.toString().trim() + " đồng chẵn";
        // Viết hoa chữ cái đầu tiên
        if (!res.isEmpty()) {
            res = Character.toUpperCase(res.charAt(0)) + res.substring(1);
        }
        return res;
    }

    private static String readThreeDigits(int n, boolean showHundreds) {
        int h = n / 100;
        int t = (n % 100) / 10;
        int u = n % 10;

        StringBuilder sb = new StringBuilder();

        if (h > 0 || showHundreds) {
            sb.append(DIGITS[h]).append(" trăm");
        }

        if (t > 1) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(DIGITS[t]).append(" mươi");
            if (u == 1) {
                sb.append(" mốt");
            } else if (u == 4) {
                sb.append(" tư");
            } else if (u == 5) {
                sb.append(" lăm");
            } else if (u > 0) {
                sb.append(" ").append(DIGITS[u]);
            }
        } else if (t == 1) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("mười");
            if (u == 5) {
                sb.append(" lăm");
            } else if (u > 0) {
                sb.append(" ").append(DIGITS[u]);
            }
        } else { // t == 0
            if (u > 0) {
                if (sb.length() > 0) {
                    sb.append(" linh ");
                }
                sb.append(DIGITS[u]);
            }
        }

        return sb.toString().trim();
    }
}
