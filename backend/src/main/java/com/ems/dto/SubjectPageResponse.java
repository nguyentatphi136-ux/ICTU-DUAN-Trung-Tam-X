package com.ems.dto;

import java.io.Serializable;
import java.util.List;

public class SubjectPageResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<SubjectResponse> items;
    private long total;
    private int page;
    private int pageSize;
    private int totalPages;

    public SubjectPageResponse() {}

    public SubjectPageResponse(List<SubjectResponse> items, long total, int page, int pageSize) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) total / (pageSize > 0 ? pageSize : 10));
    }

    public List<SubjectResponse> getItems() {
        return items;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
