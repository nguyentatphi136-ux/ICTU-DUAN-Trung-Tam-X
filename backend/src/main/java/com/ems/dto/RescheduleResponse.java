package com.ems.dto;

import com.ems.model.ClassSession;

import java.io.Serializable;

public class RescheduleResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String action; // RESCHEDULE, UPDATE, CANCEL
    private ClassSession session;
    private boolean notificationSent;
    private int recipientCount;
    private String message;
    private String notificationError;

    public RescheduleResponse() {}

    public RescheduleResponse(boolean success, String action, ClassSession session,
                              boolean notificationSent, int recipientCount, String message) {
        this.success = success;
        this.action = action;
        this.session = session;
        this.notificationSent = notificationSent;
        this.recipientCount = recipientCount;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public ClassSession getSession() {
        return session;
    }

    public void setSession(ClassSession session) {
        this.session = session;
    }

    public boolean isNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public int getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(int recipientCount) {
        this.recipientCount = recipientCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNotificationError() {
        return notificationError;
    }

    public void setNotificationError(String notificationError) {
        this.notificationError = notificationError;
    }
}
