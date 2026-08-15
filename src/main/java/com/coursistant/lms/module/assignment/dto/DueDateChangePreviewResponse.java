package com.coursistant.lms.module.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Impact summary for a proposed due date change. Nothing is persisted by the preview.
 */
@Schema(name = "DueDateChangePreviewResponse", description = "Impact summary for a proposed due date change")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DueDateChangePreviewResponse {

    private LocalDateTime currentDueAt;
    private LocalDateTime currentLateUntil;
    private LocalDateTime newDueAt;
    private LocalDateTime newLateUntil;
    private boolean shortening;
    private boolean confirmationRequired;
    private int activeStudentCount;
    private int submittedCount;
    private int notSubmittedCount;
    private int submissionsBecomingLateCount;
    private int gradedCount;

    public LocalDateTime getCurrentDueAt() {
        return currentDueAt;
    }

    public void setCurrentDueAt(LocalDateTime currentDueAt) {
        this.currentDueAt = currentDueAt;
    }

    public LocalDateTime getCurrentLateUntil() {
        return currentLateUntil;
    }

    public void setCurrentLateUntil(LocalDateTime currentLateUntil) {
        this.currentLateUntil = currentLateUntil;
    }

    public LocalDateTime getNewDueAt() {
        return newDueAt;
    }

    public void setNewDueAt(LocalDateTime newDueAt) {
        this.newDueAt = newDueAt;
    }

    public LocalDateTime getNewLateUntil() {
        return newLateUntil;
    }

    public void setNewLateUntil(LocalDateTime newLateUntil) {
        this.newLateUntil = newLateUntil;
    }

    public boolean isShortening() {
        return shortening;
    }

    public void setShortening(boolean shortening) {
        this.shortening = shortening;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }

    public int getActiveStudentCount() {
        return activeStudentCount;
    }

    public void setActiveStudentCount(int activeStudentCount) {
        this.activeStudentCount = activeStudentCount;
    }

    public int getSubmittedCount() {
        return submittedCount;
    }

    public void setSubmittedCount(int submittedCount) {
        this.submittedCount = submittedCount;
    }

    public int getNotSubmittedCount() {
        return notSubmittedCount;
    }

    public void setNotSubmittedCount(int notSubmittedCount) {
        this.notSubmittedCount = notSubmittedCount;
    }

    public int getSubmissionsBecomingLateCount() {
        return submissionsBecomingLateCount;
    }

    public void setSubmissionsBecomingLateCount(int submissionsBecomingLateCount) {
        this.submissionsBecomingLateCount = submissionsBecomingLateCount;
    }

    public int getGradedCount() {
        return gradedCount;
    }

    public void setGradedCount(int gradedCount) {
        this.gradedCount = gradedCount;
    }
}
