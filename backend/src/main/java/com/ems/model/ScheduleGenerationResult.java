package com.ems.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Model chứa kết quả sinh thời khoá biểu tự động và các số liệu dự kiến
 * Tác vụ Jira: IDTTX-247 / IDTTX-263 [BE] - S4-01
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class ScheduleGenerationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private Klass klass;
    private SchedulePattern pattern;
    private List<PlannedSession> sessions = new ArrayList<>();
    private List<SkippedDate> skipped = new ArrayList<>();

    private String firstDate;
    private String lastDate;
    private int sessionsPerWeek;
    private int plainWeeks;
    private int actualWeeks;
    private int weeksDelayed;

    public ScheduleGenerationResult() {
    }

    public ScheduleGenerationResult(Klass klass, SchedulePattern pattern, List<PlannedSession> sessions, List<SkippedDate> skipped) {
        this.klass = klass;
        this.pattern = pattern;
        this.sessions = sessions != null ? sessions : new ArrayList<>();
        this.skipped = skipped != null ? skipped : new ArrayList<>();
        calculateEstimates();
    }

    public void calculateEstimates() {
        if (pattern != null && pattern.getDays() != null) {
            this.sessionsPerWeek = pattern.getDays().size();
        }
        if (!sessions.isEmpty()) {
            this.firstDate = sessions.get(0).getDate();
            this.lastDate = sessions.get(sessions.size() - 1).getDate();

            if (sessionsPerWeek > 0) {
                this.plainWeeks = (int) Math.ceil((double) sessions.size() / sessionsPerWeek);
            }

            try {
                LocalDate dFirst = LocalDate.parse(firstDate);
                LocalDate dLast = LocalDate.parse(lastDate);
                long daysBetween = ChronoUnit.DAYS.between(dFirst, dLast);
                this.actualWeeks = (int) Math.round((double) daysBetween / 7.0) + 1;
                this.weeksDelayed = Math.max(0, actualWeeks - plainWeeks);
            } catch (Exception e) {
                this.actualWeeks = this.plainWeeks;
                this.weeksDelayed = 0;
            }
        }
    }

    public String getFormattedFirstDate() {
        if (firstDate == null) return "";
        try {
            LocalDate d = LocalDate.parse(firstDate);
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return firstDate;
        }
    }

    public String getFormattedLastDate() {
        if (lastDate == null) return "";
        try {
            LocalDate d = LocalDate.parse(lastDate);
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return lastDate;
        }
    }

    public Klass getKlass() {
        return klass;
    }

    public void setKlass(Klass klass) {
        this.klass = klass;
    }

    public SchedulePattern getPattern() {
        return pattern;
    }

    public void setPattern(SchedulePattern pattern) {
        this.pattern = pattern;
    }

    public List<PlannedSession> getSessions() {
        return sessions;
    }

    public void setSessions(List<PlannedSession> sessions) {
        this.sessions = sessions;
    }

    public List<SkippedDate> getSkipped() {
        return skipped;
    }

    public void setSkipped(List<SkippedDate> skipped) {
        this.skipped = skipped;
    }

    public String getFirstDate() {
        return firstDate;
    }

    public void setFirstDate(String firstDate) {
        this.firstDate = firstDate;
    }

    public String getLastDate() {
        return lastDate;
    }

    public void setLastDate(String lastDate) {
        this.lastDate = lastDate;
    }

    public int getSessionsPerWeek() {
        return sessionsPerWeek;
    }

    public void setSessionsPerWeek(int sessionsPerWeek) {
        this.sessionsPerWeek = sessionsPerWeek;
    }

    public int getPlainWeeks() {
        return plainWeeks;
    }

    public void setPlainWeeks(int plainWeeks) {
        this.plainWeeks = plainWeeks;
    }

    public int getActualWeeks() {
        return actualWeeks;
    }

    public void setActualWeeks(int actualWeeks) {
        this.actualWeeks = actualWeeks;
    }

    public int getWeeksDelayed() {
        return weeksDelayed;
    }

    public void setWeeksDelayed(int weeksDelayed) {
        this.weeksDelayed = weeksDelayed;
    }
}
