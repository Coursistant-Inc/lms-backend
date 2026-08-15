package com.coursistant.lms.module.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything a grader needs for one student: the roster row, the submission history, the
 * rubric in force, and the current grade (absent when Ungraded).
 */
@Schema(name = "GradingViewResponse", description = "Full grading workspace for one student or group")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GradingViewResponse {

    private Integer assignmentId;
    private String assignmentTitle;
    private GradingRosterItemResponse student;
    private SubmissionVersionResponse currentVersion;
    private List<SubmissionVersionResponse> versions = new ArrayList<>();
    private RubricResponse rubric;
    private GradeResponse grade;
    private Boolean gradingWritable;
    private Integer prevStudentId;
    private Integer nextStudentId;

    public Integer getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Integer assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    public void setAssignmentTitle(String assignmentTitle) {
        this.assignmentTitle = assignmentTitle;
    }

    public GradingRosterItemResponse getStudent() {
        return student;
    }

    public void setStudent(GradingRosterItemResponse student) {
        this.student = student;
    }

    public SubmissionVersionResponse getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(SubmissionVersionResponse currentVersion) {
        this.currentVersion = currentVersion;
    }

    public List<SubmissionVersionResponse> getVersions() {
        return versions;
    }

    public void setVersions(List<SubmissionVersionResponse> versions) {
        this.versions = versions;
    }

    public RubricResponse getRubric() {
        return rubric;
    }

    public void setRubric(RubricResponse rubric) {
        this.rubric = rubric;
    }

    public GradeResponse getGrade() {
        return grade;
    }

    public void setGrade(GradeResponse grade) {
        this.grade = grade;
    }

    public Boolean getGradingWritable() {
        return gradingWritable;
    }

    public void setGradingWritable(Boolean gradingWritable) {
        this.gradingWritable = gradingWritable;
    }

    public Integer getPrevStudentId() {
        return prevStudentId;
    }

    public void setPrevStudentId(Integer prevStudentId) {
        this.prevStudentId = prevStudentId;
    }

    public Integer getNextStudentId() {
        return nextStudentId;
    }

    public void setNextStudentId(Integer nextStudentId) {
        this.nextStudentId = nextStudentId;
    }
}
