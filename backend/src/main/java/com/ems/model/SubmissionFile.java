package com.ems.model;

import java.io.Serializable;

/**
 * Model biểu diễn tệp nộp bài của học viên (S6-01):
 * Hỗ trợ xem trước tệp tài liệu (PDF, source code Java/JSP/SQL, tệp zip...).
 */
public class SubmissionFile implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private long sizeBytes;
    private String formattedSize;
    private String fileType; // PDF, JAVA, JSP, SQL, ZIP, DOC, MD
    private String previewContent; // Nội dung mẫu để xem trước trực tiếp trên giao diện

    public SubmissionFile() {}

    public SubmissionFile(String name, long sizeBytes, String formattedSize, String fileType, String previewContent) {
        this.name = name;
        this.sizeBytes = sizeBytes;
        this.formattedSize = formattedSize;
        this.fileType = fileType;
        this.previewContent = previewContent;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getFormattedSize() { return formattedSize; }
    public void setFormattedSize(String formattedSize) { this.formattedSize = formattedSize; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getPreviewContent() { return previewContent; }
    public void setPreviewContent(String previewContent) { this.previewContent = previewContent; }

    public boolean isPdf() {
        return "PDF".equalsIgnoreCase(fileType) || (name != null && name.toLowerCase().endsWith(".pdf"));
    }

    public boolean isCode() {
        return "JAVA".equalsIgnoreCase(fileType) || "JSP".equalsIgnoreCase(fileType)
                || "SQL".equalsIgnoreCase(fileType) || "MD".equalsIgnoreCase(fileType)
                || (name != null && (name.endsWith(".java") || name.endsWith(".jsp") || name.endsWith(".sql") || name.endsWith(".js") || name.endsWith(".html")));
    }
}
