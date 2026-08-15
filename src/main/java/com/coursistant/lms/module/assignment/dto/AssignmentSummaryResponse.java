package com.coursistant.lms.module.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Slim assignment list card for any course member: title, due, type, and (students only)
 * current submission status. {@code id} is included so the client can open the detail page.
 */
@Schema(name = "AssignmentSummaryResponse", description = "Slim assignment card for course members")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentSummaryResponse {

    private Integer id;
    private String title;
    private Instant dueAtUtc;
    private LocalDateTime dueAtLocal;
    private String timezone;
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

    public Instant getDueAtUtc() {
        return dueAtUtc;
    }

    public void setDueAtUtc(Instant dueAtUtc) {
        this.dueAtUtc = dueAtUtc;
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
