package com.ems.util;

import com.ems.model.PaymentAdjustment;
import com.ems.model.PaymentReceipt;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Tiện ích sinh tệp PDF Biên lai thanh toán học phí theo chuẩn TMS
 */
public final class ReceiptPdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FMT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        MONEY_FMT = new DecimalFormat("#,##0 ₫", symbols);
    }

    private ReceiptPdfGenerator() {}

    public static void generateReceiptPdf(PaymentReceipt receipt, OutputStream outputStream) throws Exception {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, outputStream);
        document.open();

        BaseFont baseFont = resolveBaseFont();
        Font orgTitleFont = new Font(baseFont, 12, Font.BOLD, new Color(30, 41, 59));
        Font orgSubFont = new Font(baseFont, 9, Font.NORMAL, new Color(100, 116, 139));
        Font titleFont = new Font(baseFont, 16, Font.BOLD, new Color(15, 23, 42));
        Font receiptCodeFont = new Font(baseFont, 11, Font.BOLD, new Color(2, 132, 199));
        Font boldFont = new Font(baseFont, 10, Font.BOLD, new Color(15, 23, 42));
        Font normalFont = new Font(baseFont, 10, Font.NORMAL, new Color(51, 65, 85));
        Font hintFont = new Font(baseFont, 9, Font.ITALIC, new Color(100, 116, 139));
        Font tableHeaderFont = new Font(baseFont, 10, Font.BOLD, new Color(255, 255, 255));

        // 1. Header: Đơn vị đào tạo
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{65f, 35f});

        PdfPCell orgCell = new PdfPCell();
        orgCell.setBorder(Rectangle.NO_BORDER);
        orgCell.addElement(new Paragraph("TRUNG TÂM ĐÀO TẠO TMS", orgTitleFont));
        orgCell.addElement(new Paragraph("Tầng 3, cơ sở Cầu Giấy, Hà Nội · Hotline: 024 0000 0000", orgSubFont));
        headerTable.addCell(orgCell);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph pCode = new Paragraph("Mã phiếu: " + receipt.getReceiptCode(), receiptCodeFont);
        pCode.setAlignment(Element.ALIGN_RIGHT);
        logoCell.addElement(pCode);
        headerTable.addCell(logoCell);

        document.add(headerTable);

        // Divider
        Paragraph space = new Paragraph(" ", new Font(baseFont, 6));
        document.add(space);

        // 2. Tiêu đề Biên lai
        Paragraph title = new Paragraph("BIÊN LAI THU HỌC PHÍ", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(10f);
        title.setSpacingAfter(15f);
        document.add(title);

        // 3. Thông tin học viên & đợt nộp (Grid 2 cột)
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{22f, 28f, 22f, 28f});
        infoTable.setSpacingAfter(12f);

        addInfoRow(infoTable, "Họ và tên học viên:", receipt.getStudentName() != null ? receipt.getStudentName() : "–",
                "Mã học viên:", receipt.getStudentCode() != null ? receipt.getStudentCode() : "–", normalFont, boldFont);
        addInfoRow(infoTable, "Chương trình học:", receipt.getProgramName() != null ? receipt.getProgramName() : "–",
                "Lớp:", receipt.getClassName() != null ? receipt.getClassName() : "Chưa xếp lớp", normalFont, boldFont);
        addInfoRow(infoTable, "Ngày thu:", receipt.getPaymentDate() != null ? receipt.getPaymentDate().format(DATE_FMT) : "–",
                "Hình thức thu:", formatMethod(receipt), normalFont, boldFont);
        addInfoRow(infoTable, "Người thu tiền:", receipt.getCollectorName() != null ? receipt.getCollectorName() : "Bộ phận Kế toán",
                "Trạng thái đợt:", receipt.getBillRemainingDebtAfter() != null && receipt.getBillRemainingDebtAfter().compareTo(BigDecimal.ZERO) == 0 ? "Đã thanh toán đủ" : "Thanh toán một phần", normalFont, boldFont);

        document.add(infoTable);

        // 4. Bảng nội dung thu tiền
        PdfPTable itemTable = new PdfPTable(3);
        itemTable.setWidthPercentage(100);
        itemTable.setWidths(new float[]{50f, 25f, 25f});
        itemTable.setSpacingBefore(5f);
        itemTable.setSpacingAfter(12f);

        // Header cells
        Color headerBg = new Color(30, 41, 59);
        addTableHeaderCell(itemTable, "Nội dung thanh toán", tableHeaderFont, headerBg, Element.ALIGN_LEFT);
        addTableHeaderCell(itemTable, "Phải thu theo đợt", tableHeaderFont, headerBg, Element.ALIGN_RIGHT);
        addTableHeaderCell(itemTable, "Số tiền thu lần này", tableHeaderFont, headerBg, Element.ALIGN_RIGHT);

        // Row 1: Giao dịch gốc
        String installmentText = "Học phí đợt " + (receipt.getInstallmentOrder() != null ? receipt.getInstallmentOrder() : 1);
        if (receipt.getNotes() != null && !receipt.getNotes().isBlank()) {
            installmentText += "\n(Ghi chú: " + receipt.getNotes().trim() + ")";
        }
        addTableCell(itemTable, installmentText, normalFont, Element.ALIGN_LEFT);
        addTableCell(itemTable, receipt.getExpectedBillAmount() != null ? formatMoney(receipt.getExpectedBillAmount()) : "–", normalFont, Element.ALIGN_RIGHT);
        addTableCell(itemTable, formatMoney(receipt.getAmountPaid()), boldFont, Element.ALIGN_RIGHT);

        // Các dòng bút toán điều chỉnh nếu có
        if (receipt.getAdjustments() != null && !receipt.getAdjustments().isEmpty()) {
            for (PaymentAdjustment adj : receipt.getAdjustments()) {
                String adjText = "Bút toán điều chỉnh " + adj.getAdjustmentCode() + " (" + adj.getAdjustmentType() + ")\nLý do: " + adj.getReason();
                addTableCell(itemTable, adjText, hintFont, Element.ALIGN_LEFT);
                addTableCell(itemTable, "–", normalFont, Element.ALIGN_RIGHT);
                String deltaSign = adj.getDeltaAmount().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                addTableCell(itemTable, deltaSign + formatMoney(adj.getDeltaAmount()), boldFont, Element.ALIGN_RIGHT);
            }
        }

        document.add(itemTable);

        // 5. Phần Tổng kết số tiền
        BigDecimal netAmount = receipt.getNetAmount();
        String words = VietnameseNumberToWords.convert(netAmount);

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new float[]{40f, 60f});
        summaryTable.setSpacingAfter(15f);

        PdfPCell sumTitleCell = new PdfPCell(new Phrase("Tổng số tiền thực thu:", boldFont));
        sumTitleCell.setBorder(Rectangle.NO_BORDER);
        PdfPCell sumValueCell = new PdfPCell(new Phrase(formatMoney(netAmount), new Font(baseFont, 12, Font.BOLD, new Color(2, 132, 199))));
        sumValueCell.setBorder(Rectangle.NO_BORDER);
        sumValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.addCell(sumTitleCell);
        summaryTable.addCell(sumValueCell);

        PdfPCell wordsCell = new PdfPCell(new Phrase("Bằng chữ: " + words, new Font(baseFont, 10, Font.ITALIC, new Color(30, 41, 59))));
        wordsCell.setColspan(2);
        wordsCell.setBorder(Rectangle.NO_BORDER);
        wordsCell.setPaddingTop(4f);
        summaryTable.addCell(wordsCell);

        if (receipt.getBillRemainingDebtAfter() != null) {
            PdfPCell debtTitleCell = new PdfPCell(new Phrase("Đợt này còn nợ sau lần thu này:", normalFont));
            debtTitleCell.setBorder(Rectangle.NO_BORDER);
            debtTitleCell.setPaddingTop(4f);
            PdfPCell debtValueCell = new PdfPCell(new Phrase(formatMoney(receipt.getBillRemainingDebtAfter()), boldFont));
            debtValueCell.setBorder(Rectangle.NO_BORDER);
            debtValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            debtValueCell.setPaddingTop(4f);
            summaryTable.addCell(debtTitleCell);
            summaryTable.addCell(debtValueCell);
        }

        document.add(summaryTable);

        // 6. Chữ ký 2 bên
        PdfPTable signTable = new PdfPTable(2);
        signTable.setWidthPercentage(100);
        signTable.setWidths(new float[]{50f, 50f});
        signTable.setSpacingBefore(15f);
        signTable.setSpacingAfter(40f);

        PdfPCell leftSign = new PdfPCell();
        leftSign.setBorder(Rectangle.NO_BORDER);
        leftSign.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p1 = new Paragraph("Người nộp tiền", boldFont);
        p1.setAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph("(Ký, ghi rõ họ tên)", hintFont);
        p2.setAlignment(Element.ALIGN_CENTER);
        leftSign.addElement(p1);
        leftSign.addElement(p2);
        signTable.addCell(leftSign);

        PdfPCell rightSign = new PdfPCell();
        rightSign.setBorder(Rectangle.NO_BORDER);
        rightSign.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p3 = new Paragraph("Người thu tiền", boldFont);
        p3.setAlignment(Element.ALIGN_CENTER);
        Paragraph p4 = new Paragraph("(Ký, ghi rõ họ tên)", hintFont);
        p4.setAlignment(Element.ALIGN_CENTER);
        Paragraph pCollector = new Paragraph("\n\n" + (receipt.getCollectorName() != null ? receipt.getCollectorName() : ""), boldFont);
        pCollector.setAlignment(Element.ALIGN_CENTER);
        rightSign.addElement(p3);
        rightSign.addElement(p4);
        rightSign.addElement(pCollector);
        signTable.addCell(rightSign);

        document.add(signTable);

        // 7. Footer tra cứu
        Paragraph footer = new Paragraph("Mã tra cứu: " + receipt.getReceiptCode() +
                " · Biên lai điện tử trích xuất từ Hệ thống Quản lý Đào tạo TMS lúc " +
                (receipt.getCreatedAt() != null ? receipt.getCreatedAt().toString() : "") +
                " · Vui lòng bảo lưu để đối chiếu.", hintFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
    }

    private static BaseFont resolveBaseFont() {
        String[] fontPaths = {
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/times.ttf",
                "C:/Windows/Fonts/tahoma.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"
        };
        for (String path : fontPaths) {
            try {
                File f = new File(path);
                if (f.exists() && f.canRead()) {
                    return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception ignored) {}
        }
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("Không thể nạp font cho PDF generator", e);
        }
    }

    private static void addInfoRow(PdfPTable table, String label1, String val1, String label2, String val2, Font fontLabel, Font fontVal) {
        PdfPCell c1 = new PdfPCell(new Phrase(label1, fontLabel));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(3f);
        PdfPCell c2 = new PdfPCell(new Phrase(val1, fontVal));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(3f);
        PdfPCell c3 = new PdfPCell(new Phrase(label2, fontLabel));
        c3.setBorder(Rectangle.NO_BORDER);
        c3.setPadding(3f);
        PdfPCell c4 = new PdfPCell(new Phrase(val2, fontVal));
        c4.setBorder(Rectangle.NO_BORDER);
        c4.setPadding(3f);

        table.addCell(c1);
        table.addCell(c2);
        table.addCell(c3);
        table.addCell(c4);
    }

    private static void addTableHeaderCell(PdfPTable table, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private static void addTableCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(6f);
        cell.setBorderColor(new Color(226, 232, 240));
        table.addCell(cell);
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return MONEY_FMT.format(amount);
    }

    private static String formatMethod(PaymentReceipt receipt) {
        String method = "BANK_TRANSFER".equalsIgnoreCase(receipt.getPaymentMethod()) || "Chuyển khoản".equalsIgnoreCase(receipt.getPaymentMethod())
                ? "Chuyển khoản" : "Tiền mặt";
        if (receipt.getReferenceCode() != null && !receipt.getReferenceCode().isBlank()) {
            method += " (Mã GD: " + receipt.getReferenceCode().trim() + ")";
        }
        return method;
    }
}
