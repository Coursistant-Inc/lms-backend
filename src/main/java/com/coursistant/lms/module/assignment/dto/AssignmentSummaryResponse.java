package com.coursistant.lms.module.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Slim assignment list card for any course member: title, due, type, and (students only)
 * current submission status. {@code id} is included so the client can open the detail page.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentSummaryResponse {

    private Integer id;
    private String title;
    /** UTC. */
    private LocalDateTime dueAt;
    /** {@code dueAt} in the caller's X-Timezone. */
    private LocalDateTime dueAtLocal;
    private String timezone;
    private String timezoneLabel;
    private String submissionType;
    private String submissionStatus;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public LocalDateTime getDueAtLocal() {
        return dueAtLocal;
    }

    public void setDueAtLocal(LocalDateTime dueAtLocal) {
        this.dueAtLocal = dueAtLocal;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getTimezoneLabel() {
        return timezoneLabel;
    }

    public void setTimezoneLabel(String timezoneLabel) {
        this.timezoneLabel = timezoneLabel;
    }

    public String getSubmissionType() {
        return submissionType;
    }

    public void setSubmissionType(String submissionType) {
        this.submissionType = submissionType;
    }

    public String getSubmissionStatus() {
        return submissionStatus;
    }

    public void setSubmissionStatus(String submissionStatus) {
        this.submissionStatus = submissionStatus;
    }
}
