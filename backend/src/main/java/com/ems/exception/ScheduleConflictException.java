package com.ems.exception;

import com.ems.dto.ConflictCheckResult;

public class ScheduleConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final ConflictCheckResult conflictResult;

    public ScheduleConflictException(String message, ConflictCheckResult conflictResult) {
        super(message);
        this.conflictResult = conflictResult;
    }

    public ScheduleConflictException(ConflictCheckResult conflictResult) {
        super(conflictResult != null ? conflictResult.getMessage() : "Xung đột thời khóa biểu.");
        this.conflictResult = conflictResult;
    }

    public ConflictCheckResult getConflictResult() {
        return conflictResult;
    }
}
