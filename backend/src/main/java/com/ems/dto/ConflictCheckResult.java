package com.ems.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ConflictCheckResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean hasConflict;
    private List<ConflictDetail> conflicts = new ArrayList<>();
    private String message;

    public ConflictCheckResult() {
        this.hasConflict = false;
        this.message = "Không có xung đột lịch học.";
    }

    public ConflictCheckResult(boolean hasConflict, List<ConflictDetail> conflicts, String message) {
        this.hasConflict = hasConflict;
        this.conflicts = conflicts != null ? conflicts : new ArrayList<>();
        this.message = message;
    }

    public boolean isHasConflict() {
        return hasConflict;
    }

    public void setHasConflict(boolean hasConflict) {
        this.hasConflict = hasConflict;
    }

    public List<ConflictDetail> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<ConflictDetail> conflicts) {
        this.conflicts = conflicts != null ? conflicts : new ArrayList<>();
        this.hasConflict = !this.conflicts.isEmpty();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
