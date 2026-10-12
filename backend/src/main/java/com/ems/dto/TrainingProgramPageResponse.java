package com.ems.dto;

import java.util.List;

/**
 * DTO trả về danh sách phân trang Chương trình đào tạo
 */
public class TrainingProgramPageResponse {
    private List<TrainingProgramResponse> items;
    private long total;
    private int page;
    private int pageSize;
    private int totalPages;

    public TrainingProgramPageResponse() {
    }

    public TrainingProgramPageResponse(List<TrainingProgramResponse> items, long total, int page, int pageSize) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
    }

    public List<TrainingProgramResponse> getItems() {
        return items;
    }

    public void setItems(List<TrainingProgramResponse> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
