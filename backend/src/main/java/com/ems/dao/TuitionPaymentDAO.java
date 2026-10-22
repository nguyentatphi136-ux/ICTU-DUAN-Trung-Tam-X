package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.PaymentAdjustment;
import com.ems.model.PaymentReceipt;
import com.ems.model.Student;
import com.ems.model.StudentTuitionPlan;
import com.ems.model.TuitionInstallmentBill;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO xử lý nghiệp vụ Học phí, Đợt thu, Biên lai thanh toán và Bút toán điều chỉnh
 * Phục vụ Jira task S7-03 – EP-08
 */
public class TuitionPaymentDAO {

    // =========================================================================
    // 1. SINH MÃ DUY NHẤT (RECEIPT & ADJUSTMENT CODE GENERATION)
    // =========================================================================

    public synchronized String generateNextReceiptCode(Connection conn) throws SQLException {
        int year = LocalDate.now().getYear();
        String prefix = "BL-" + year + "-";
        String sql = "SELECT receipt_code FROM payment_receipts WHERE receipt_code LIKE ? ORDER BY id DESC";

        int maxSeq = 0;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prefix + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("receipt_code");
                    if (code != null && code.startsWith(prefix)) {
                        try {
                            int seq = Integer.parseInt(code.substring(prefix.length()));
                            if (seq > maxSeq) {
                                maxSeq = seq;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return String.format("%s%06d", prefix, maxSeq + 1);
    }

    public synchronized String generateNextAdjustmentCode(Connection conn) throws SQLException {
        int year = LocalDate.now().getYear();
        String prefix = "DC-" + year + "-";
        String sql = "SELECT adjustment_code FROM payment_adjustments WHERE adjustment_code LIKE ? ORDER BY id DESC";

        int maxSeq = 0;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prefix + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("adjustment_code");
                    if (code != null && code.startsWith(prefix)) {
                        try {
                            int seq = Integer.parseInt(code.substring(prefix.length()));
                            if (seq > maxSeq) {
                                maxSeq = seq;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return String.format("%s%06d", prefix, maxSeq + 1);
    }

    // =========================================================================
    // 2. QUERY & LOCK (INSTALLMENT BILLS, PLANS, STUDENTS)
    // =========================================================================

    public TuitionInstallmentBill findBillById(long billId, Connection conn) throws SQLException {
        String sql = "SELECT id, tuition_plan_id, installment_order, due_date, expected_amount, paid_amount, status, last_reminder_sent_at " +
                "FROM tuition_installment_bills WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, billId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapBill(rs);
                }
            }
        }
        return null;
    }

    public TuitionInstallmentBill findBillForUpdate(long billId, Connection conn) throws SQLException {
        String sql = "SELECT id, tuition_plan_id, installment_order, due_date, expected_amount, paid_amount, status, last_reminder_sent_at " +
                "FROM tuition_installment_bills WHERE id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, billId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapBill(rs);
                }
            }
        }
        return null;
    }

    public StudentTuitionPlan findPlanById(long planId, Connection conn) throws SQLException {
        String sql = "SELECT id, enrollment_id, student_id, original_tuition, discount_type, discount_value, discount_amount, " +
                "discount_reason, discount_approved_by, final_payable_amount, paid_amount, debt_amount, status, created_at, updated_at " +
                "FROM student_tuition_plans WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, planId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapPlan(rs);
                }
            }
        }
        return null;
    }

    public StudentTuitionPlan findPlanForUpdate(long planId, Connection conn) throws SQLException {
        String sql = "SELECT id, enrollment_id, student_id, original_tuition, discount_type, discount_value, discount_amount, " +
                "discount_reason, discount_approved_by, final_payable_amount, paid_amount, debt_amount, status, created_at, updated_at " +
                "FROM student_tuition_plans WHERE id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, planId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapPlan(rs);
                }
            }
        }
        return null;
    }

    public StudentTuitionPlan findPlanByStudentId(long studentId, Connection conn) throws SQLException {
        String sql = "SELECT id, enrollment_id, student_id, original_tuition, discount_type, discount_value, discount_amount, " +
                "discount_reason, discount_approved_by, final_payable_amount, paid_amount, debt_amount, status, created_at, updated_at " +
                "FROM student_tuition_plans WHERE student_id = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapPlan(rs);
                }
            }
        }
        return null;
    }

    public List<TuitionInstallmentBill> listBillsByPlanId(long planId, Connection conn) throws SQLException {
        String sql = "SELECT id, tuition_plan_id, installment_order, due_date, expected_amount, paid_amount, status, last_reminder_sent_at " +
                "FROM tuition_installment_bills WHERE tuition_plan_id = ? ORDER BY installment_order ASC";
        List<TuitionInstallmentBill> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, planId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBill(rs));
                }
            }
        }
        return list;
    }

    public Student findStudentById(long studentId, Connection conn) throws SQLException {
        String sql = "SELECT s.id, s.user_id, s.student_code, s.entry_level, s.enrollment_status, s.created_at, s.updated_at, " +
                "u.full_name, u.email, u.phone, c.class_name, p.program_name " +
                "FROM students s " +
                "LEFT JOIN users u ON s.user_id = u.id " +
                "LEFT JOIN enrollments e ON s.id = e.student_id " +
                "LEFT JOIN classes c ON e.class_id = c.id " +
                "LEFT JOIN programs p ON c.program_id = p.id " +
                "WHERE s.id = ? " +
                "ORDER BY e.id DESC LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapStudent(rs);
                }
            }
        }
        return null;
    }

    public Student findStudentByCode(String studentCode, Connection conn) throws SQLException {
        String sql = "SELECT s.id, s.user_id, s.student_code, s.entry_level, s.enrollment_status, s.created_at, s.updated_at, " +
                "u.full_name, u.email, u.phone, c.class_name, p.program_name " +
                "FROM students s " +
                "LEFT JOIN users u ON s.user_id = u.id " +
                "LEFT JOIN enrollments e ON s.id = e.student_id " +
                "LEFT JOIN classes c ON e.class_id = c.id " +
                "LEFT JOIN programs p ON c.program_id = p.id " +
                "WHERE s.student_code = ? " +
                "ORDER BY e.id DESC LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapStudent(rs);
                }
            }
        }
        return null;
    }

    public Student findStudentByUserId(long userId, Connection conn) throws SQLException {
        String sql = "SELECT s.id, s.user_id, s.student_code, s.entry_level, s.enrollment_status, s.created_at, s.updated_at, " +
                "u.full_name, u.email, u.phone, c.class_name, p.program_name " +
                "FROM students s " +
                "LEFT JOIN users u ON s.user_id = u.id " +
                "LEFT JOIN enrollments e ON s.id = e.student_id " +
                "LEFT JOIN classes c ON e.class_id = c.id " +
                "LEFT JOIN programs p ON c.program_id = p.id " +
                "WHERE s.user_id = ? " +
                "ORDER BY e.id DESC LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapStudent(rs);
                }
            }
        }
        return null;
    }

    // =========================================================================
    // 3. TÍNH CÔNG NỢ ĐÁNG TIN CẬY TRỰC TIẾP TỪ DATABASE
    // =========================================================================

    /**
     * Tính tổng số tiền thu thực tế của một đợt = tổng (amount_paid của các biên lai) + tổng (delta_amount các bút toán điều chỉnh)
     */
    public BigDecimal calculateTotalPaidForBill(long billId, Connection conn) throws SQLException {
        String sql = """
            SELECT
                (COALESCE((SELECT SUM(amount_paid) FROM payment_receipts WHERE installment_bill_id = ?), 0)
                +
                COALESCE((SELECT SUM(pa.delta_amount) FROM payment_adjustments pa
                          JOIN payment_receipts pr ON pa.payment_receipt_id = pr.id
                          WHERE pr.installment_bill_id = ?), 0)) AS actual_paid
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, billId);
            stmt.setLong(2, billId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal paid = rs.getBigDecimal("actual_paid");
                    return paid != null ? paid : BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Tính số tiền thực tế của một biên lai sau các bút toán điều chỉnh
     */
    public BigDecimal calculateNetReceiptAmount(long receiptId, Connection conn) throws SQLException {
        String sql = """
            SELECT pr.amount_paid + COALESCE((SELECT SUM(pa.delta_amount) FROM payment_adjustments pa WHERE pa.payment_receipt_id = pr.id), 0) AS net_amount
            FROM payment_receipts pr WHERE pr.id = ?
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, receiptId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal net = rs.getBigDecimal("net_amount");
                    return net != null ? net : BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }

    // =========================================================================
    // 4. INSERT & UPDATE
    // =========================================================================

    public long insertReceipt(PaymentReceipt receipt, Connection conn) throws SQLException {
        String sql = "INSERT INTO payment_receipts (receipt_code, installment_bill_id, amount_paid, payment_method, " +
                "payment_date, reference_code, notes, collected_by, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, receipt.getReceiptCode());
            stmt.setLong(2, receipt.getInstallmentBillId());
            stmt.setBigDecimal(3, receipt.getAmountPaid());
            stmt.setString(4, receipt.getPaymentMethod());
            stmt.setDate(5, Date.valueOf(receipt.getPaymentDate()));
            stmt.setString(6, receipt.getReferenceCode());
            stmt.setString(7, receipt.getNotes());
            stmt.setLong(8, receipt.getCollectedBy());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Thêm mới biên lai thất bại, không có bản ghi nào được ghi nhận.");
            }
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    receipt.setId(id);
                    return id;
                } else {
                    throw new SQLException("Thêm mới biên lai thất bại, không nhận được khóa chính id.");
                }
            }
        }
    }

    public long insertAdjustment(PaymentAdjustment adjustment, Connection conn) throws SQLException {
        String sql = "INSERT INTO payment_adjustments (adjustment_code, payment_receipt_id, adjustment_type, delta_amount, " +
                "reason, adjusted_by, created_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, adjustment.getAdjustmentCode());
            stmt.setLong(2, adjustment.getPaymentReceiptId());
            stmt.setString(3, adjustment.getAdjustmentType());
            stmt.setBigDecimal(4, adjustment.getDeltaAmount());
            stmt.setString(5, adjustment.getReason());
            stmt.setLong(6, adjustment.getAdjustedBy());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Thêm bút toán điều chỉnh thất bại, không có bản ghi nào được tạo.");
            }
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    adjustment.setId(id);
                    return id;
                } else {
                    throw new SQLException("Thêm bút toán điều chỉnh thất bại, không nhận được id sinh tự động.");
                }
            }
        }
    }

    public void updateBillAmountsAndStatus(long billId, BigDecimal paidAmount, String status, Connection conn) throws SQLException {
        String sql = "UPDATE tuition_installment_bills SET paid_amount = ?, status = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, paidAmount);
            stmt.setString(2, status);
            stmt.setLong(3, billId);
            stmt.executeUpdate();
        }
    }

    public void updatePlanAmountsAndStatus(long planId, BigDecimal paidAmount, BigDecimal debtAmount, String status, Connection conn) throws SQLException {
        String sql = "UPDATE student_tuition_plans SET paid_amount = ?, debt_amount = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, paidAmount);
            stmt.setBigDecimal(2, debtAmount);
            stmt.setString(3, status);
            stmt.setLong(4, planId);
            stmt.executeUpdate();
        }
    }

    public void recordAuditLog(Connection conn, Long userId, String action, String entityType, String entityId, String oldValues, String newValues) throws SQLException {
        String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, old_values, new_values, created_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (userId != null) stmt.setLong(1, userId); else stmt.setNull(1, java.sql.Types.BIGINT);
            stmt.setString(2, action);
            stmt.setString(3, entityType);
            stmt.setString(4, entityId);
            stmt.setString(5, oldValues);
            stmt.setString(6, newValues);
            stmt.executeUpdate();
        }
    }

    // =========================================================================
    // 5. QUERY PAYMENT RECEIPTS & ADJUSTMENTS (DETAIL & LIST)
    // =========================================================================

    public PaymentReceipt findReceiptById(long id, Connection conn) throws SQLException {
        String sql = getReceiptDetailSelectSql() + " WHERE pr.id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    PaymentReceipt r = mapReceipt(rs);
                    r.setAdjustments(listAdjustmentsByReceiptId(r.getId(), conn));
                    return r;
                }
            }
        }
        return null;
    }

    public PaymentReceipt findReceiptByCode(String receiptCode, Connection conn) throws SQLException {
        String sql = getReceiptDetailSelectSql() + " WHERE pr.receipt_code = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, receiptCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    PaymentReceipt r = mapReceipt(rs);
                    r.setAdjustments(listAdjustmentsByReceiptId(r.getId(), conn));
                    return r;
                }
            }
        }
        return null;
    }

    public List<PaymentAdjustment> listAdjustmentsByReceiptId(long receiptId, Connection conn) throws SQLException {
        String sql = "SELECT pa.id, pa.adjustment_code, pa.payment_receipt_id, pa.adjustment_type, pa.delta_amount, pa.reason, " +
                "pa.adjusted_by, pa.created_at, u.full_name AS adjuster_name, pr.receipt_code " +
                "FROM payment_adjustments pa " +
                "LEFT JOIN users u ON pa.adjusted_by = u.id " +
                "LEFT JOIN payment_receipts pr ON pa.payment_receipt_id = pr.id " +
                "WHERE pa.payment_receipt_id = ? ORDER BY pa.id ASC";
        List<PaymentAdjustment> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, receiptId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PaymentAdjustment a = new PaymentAdjustment();
                    a.setId(rs.getLong("id"));
                    a.setAdjustmentCode(rs.getString("adjustment_code"));
                    a.setPaymentReceiptId(rs.getLong("payment_receipt_id"));
                    a.setAdjustmentType(rs.getString("adjustment_type"));
                    a.setDeltaAmount(rs.getBigDecimal("delta_amount"));
                    a.setReason(rs.getString("reason"));
                    a.setAdjustedBy(rs.getLong("adjusted_by"));
                    a.setCreatedAt(rs.getTimestamp("created_at"));
                    a.setAdjusterName(rs.getString("adjuster_name"));
                    a.setReceiptCode(rs.getString("receipt_code"));
                    list.add(a);
                }
            }
        }
        return list;
    }

    public List<PaymentReceipt> listReceiptsByStudentId(long studentId, Connection conn) throws SQLException {
        String sql = getReceiptDetailSelectSql() + " WHERE stp.student_id = ? ORDER BY pr.id DESC";
        List<PaymentReceipt> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PaymentReceipt r = mapReceipt(rs);
                    r.setAdjustments(listAdjustmentsByReceiptId(r.getId(), conn));
                    list.add(r);
                }
            }
        }
        return list;
    }

    public List<PaymentReceipt> listReceiptsByBillId(long billId, Connection conn) throws SQLException {
        String sql = getReceiptDetailSelectSql() + " WHERE pr.installment_bill_id = ? ORDER BY pr.id ASC";
        List<PaymentReceipt> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, billId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PaymentReceipt r = mapReceipt(rs);
                    r.setAdjustments(listAdjustmentsByReceiptId(r.getId(), conn));
                    list.add(r);
                }
            }
        }
        return list;
    }

    public List<PaymentReceipt> listAllReceipts(int page, int pageSize, String keyword, Long studentId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            StringBuilder sql = new StringBuilder(getReceiptDetailSelectSql());
            sql.append(" WHERE 1=1 ");
            List<Object> params = new ArrayList<>();

            if (studentId != null) {
                sql.append(" AND stp.student_id = ? ");
                params.add(studentId);
            }
            if (keyword != null && !keyword.isBlank()) {
                sql.append(" AND (pr.receipt_code LIKE ? OR s.student_code LIKE ? OR u.full_name LIKE ? OR pr.reference_code LIKE ?) ");
                String kw = "%" + keyword.trim() + "%";
                params.add(kw);
                params.add(kw);
                params.add(kw);
                params.add(kw);
            }

            sql.append(" ORDER BY pr.id DESC ");
            if (pageSize > 0) {
                int offset = Math.max(0, (page - 1) * pageSize);
                sql.append(" LIMIT ? OFFSET ? ");
                params.add(pageSize);
                params.add(offset);
            }

            List<PaymentReceipt> list = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        PaymentReceipt r = mapReceipt(rs);
                        r.setAdjustments(listAdjustmentsByReceiptId(r.getId(), conn));
                        list.add(r);
                    }
                }
            }
            return list;
        }
    }

    public int countReceipts(String keyword, Long studentId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            StringBuilder sql = new StringBuilder(
                    "SELECT COUNT(*) FROM payment_receipts pr " +
                    "JOIN tuition_installment_bills tib ON pr.installment_bill_id = tib.id " +
                    "JOIN student_tuition_plans stp ON tib.tuition_plan_id = stp.id " +
                    "JOIN students s ON stp.student_id = s.id " +
                    "LEFT JOIN users u ON s.user_id = u.id " +
                    "WHERE 1=1 "
            );
            List<Object> params = new ArrayList<>();
            if (studentId != null) {
                sql.append(" AND stp.student_id = ? ");
                params.add(studentId);
            }
            if (keyword != null && !keyword.isBlank()) {
                sql.append(" AND (pr.receipt_code LIKE ? OR s.student_code LIKE ? OR u.full_name LIKE ? OR pr.reference_code LIKE ?) ");
                String kw = "%" + keyword.trim() + "%";
                params.add(kw);
                params.add(kw);
                params.add(kw);
                params.add(kw);
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        }
        return 0;
    }

    // =========================================================================
    // 6. HELPER MAPPER METHODS
    // =========================================================================

    private String getReceiptDetailSelectSql() {
        return """
            SELECT pr.id, pr.receipt_code, pr.installment_bill_id, pr.amount_paid, pr.payment_method, pr.payment_date,
                   pr.reference_code, pr.notes, pr.collected_by, pr.created_at,
                   collector.full_name AS collector_name,
                   s.id AS student_id, s.student_code, u.full_name AS student_name,
                   p.program_name, c.class_name,
                   tib.installment_order, tib.expected_amount AS bill_expected_amount,
                   (tib.expected_amount - tib.paid_amount) AS bill_remaining_debt,
                   (pr.amount_paid + COALESCE((SELECT SUM(pa.delta_amount) FROM payment_adjustments pa WHERE pa.payment_receipt_id = pr.id), 0)) AS net_amount
            FROM payment_receipts pr
            JOIN tuition_installment_bills tib ON pr.installment_bill_id = tib.id
            JOIN student_tuition_plans stp ON tib.tuition_plan_id = stp.id
            JOIN students s ON stp.student_id = s.id
            LEFT JOIN users u ON s.user_id = u.id
            LEFT JOIN users collector ON pr.collected_by = collector.id
            LEFT JOIN enrollments e ON stp.enrollment_id = e.id
            LEFT JOIN classes c ON e.class_id = c.id
            LEFT JOIN programs p ON c.program_id = p.id
        """;
    }

    private PaymentReceipt mapReceipt(ResultSet rs) throws SQLException {
        PaymentReceipt r = new PaymentReceipt();
        r.setId(rs.getLong("id"));
        r.setReceiptCode(rs.getString("receipt_code"));
        r.setInstallmentBillId(rs.getLong("installment_bill_id"));
        r.setAmountPaid(rs.getBigDecimal("amount_paid"));
        r.setPaymentMethod(rs.getString("payment_method"));
        Date pDate = rs.getDate("payment_date");
        if (pDate != null) r.setPaymentDate(pDate.toLocalDate());
        r.setReferenceCode(rs.getString("reference_code"));
        r.setNotes(rs.getString("notes"));
        r.setCollectedBy(rs.getLong("collected_by"));
        r.setCreatedAt(rs.getTimestamp("created_at"));

        r.setCollectorName(rs.getString("collector_name"));
        r.setStudentId(rs.getLong("student_id"));
        r.setStudentCode(rs.getString("student_code"));
        r.setStudentName(rs.getString("student_name"));
        r.setProgramName(rs.getString("program_name"));
        r.setClassName(rs.getString("class_name"));
        r.setInstallmentOrder(rs.getInt("installment_order"));
        r.setExpectedBillAmount(rs.getBigDecimal("bill_expected_amount"));

        BigDecimal net = rs.getBigDecimal("net_amount");
        r.setNetAmount(net != null ? net : r.getAmountPaid());

        BigDecimal rem = rs.getBigDecimal("bill_remaining_debt");
        r.setBillRemainingDebtAfter(rem != null ? (rem.compareTo(BigDecimal.ZERO) > 0 ? rem : BigDecimal.ZERO) : BigDecimal.ZERO);
        return r;
    }

    private TuitionInstallmentBill mapBill(ResultSet rs) throws SQLException {
        TuitionInstallmentBill b = new TuitionInstallmentBill();
        b.setId(rs.getLong("id"));
        b.setTuitionPlanId(rs.getLong("tuition_plan_id"));
        b.setInstallmentOrder(rs.getInt("installment_order"));
        Date dDate = rs.getDate("due_date");
        if (dDate != null) b.setDueDate(dDate.toLocalDate());
        b.setExpectedAmount(rs.getBigDecimal("expected_amount"));
        b.setPaidAmount(rs.getBigDecimal("paid_amount"));
        b.setStatus(rs.getString("status"));
        b.setLastReminderSentAt(rs.getTimestamp("last_reminder_sent_at"));
        return b;
    }

    private StudentTuitionPlan mapPlan(ResultSet rs) throws SQLException {
        StudentTuitionPlan p = new StudentTuitionPlan();
        p.setId(rs.getLong("id"));
        p.setEnrollmentId(rs.getObject("enrollment_id") != null ? rs.getLong("enrollment_id") : null);
        p.setStudentId(rs.getLong("student_id"));
        p.setOriginalTuition(rs.getBigDecimal("original_tuition"));
        p.setDiscountType(rs.getString("discount_type"));
        p.setDiscountValue(rs.getBigDecimal("discount_value"));
        p.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        p.setDiscountReason(rs.getString("discount_reason"));
        p.setDiscountApprovedBy(rs.getObject("discount_approved_by") != null ? rs.getLong("discount_approved_by") : null);
        p.setFinalPayableAmount(rs.getBigDecimal("final_payable_amount"));
        p.setPaidAmount(rs.getBigDecimal("paid_amount"));
        p.setDebtAmount(rs.getBigDecimal("debt_amount"));
        p.setStatus(rs.getString("status"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        return p;
    }

    private Student mapStudent(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getLong("id"));
        s.setUserId(rs.getLong("user_id"));
        s.setStudentCode(rs.getString("student_code"));
        s.setEntryLevel(rs.getString("entry_level"));
        s.setEnrollmentStatus(rs.getString("enrollment_status"));
        s.setCreatedAt(rs.getTimestamp("created_at"));
        s.setUpdatedAt(rs.getTimestamp("updated_at"));
        s.setFullName(rs.getString("full_name"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("phone"));
        s.setClassName(rs.getString("class_name"));
        s.setProgramName(rs.getString("program_name"));
        return s;
    }
}
