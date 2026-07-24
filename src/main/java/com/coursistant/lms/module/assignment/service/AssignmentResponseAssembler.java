package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.dto.AssignmentAttachmentResponse;
import com.coursistant.lms.module.assignment.dto.AssignmentResponse;
import com.coursistant.lms.module.assignment.dto.ReceiptSummaryResponse;
import com.coursistant.lms.module.assignment.dto.RubricResponse;
import com.coursistant.lms.module.assignment.dto.StudentGradeViewResponse;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.entity.AssignmentAttachment;
import com.coursistant.lms.module.assignment.entity.AssignmentGrade;
import com.coursistant.lms.module.assignment.entity.AssignmentRubricVersion;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionFile;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionReceipt;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Shapes assignment payloads per audience. Staff see draft state and the grading counters;
 * students see only their own submission state and never the roster-wide numbers.
 */
@Component
public class AssignmentResponseAssembler {

    @Resource
    private AssignmentFilePolicy assignmentFilePolicy;

    @Resource
    private AssignmentTimeSupport assignmentTimeSupport;

    public AssignmentResponse toStaffResponse(Assignment assignment,
                                              ZoneId zone,
                                              List<AssignmentAttachment> attachments,
                                              AssignmentRubricVersion currentRubric,
                                              int activeStudentCount,
                                              int submissionCount,
                                              int gradedCount,
                                              int releasedCount) {
        AssignmentResponse response = toBase(assignment, zone, attachments, currentRubric);
        response.setActiveStudentCount(activeStudentCount);
        response.setSubmissionCount(submissionCount);
        response.setGradedCount(gradedCount);
        response.setReleasedCount(releasedCount);
        response.setCanEditStructure(submissionCount == 0);
        return response;
    }

    public AssignmentResponse toStudentResponse(Assignment assignment,
                                                ZoneId zone,
                                                List<AssignmentAttachment> attachments,
                                                AssignmentRubricVersion currentRubric,
                                                String submissionStatus,
                                                LocalDateTime submittedAt,
                                                Integer versionNo,
                                                Boolean usedGraceBuffer,
                                                boolean windowOpen,
                                                boolean acceptingSubmissions,
                                                int stagedFileCount,
                                                AssignmentGrade grade,
                                                ReceiptSummaryResponse receipt) {
        AssignmentResponse response = toBase(assignment, zone, attachments, currentRubric);
        response.setSubmissionStatus(submissionStatus);
        response.setSubmittedAt(submittedAt);
        response.setVersionNo(versionNo);
        response.setUsedGraceBuffer(usedGraceBuffer);
        response.setWindowOpen(windowOpen);
        response.setAcceptingSubmissions(acceptingSubmissions);
        response.setStagedFileCount(stagedFileCount);
        response.setReceipt(receipt);

        boolean gradeReleased = grade != null && AssignmentGradingService.GRADE_RELEASED.equals(grade.getStatus());
        response.setGradeReleased(gradeReleased);
        if (gradeReleased) {
            response.setScore(grade.getScore());
            response.setFeedback(grade.getFeedbackHtml());
            response.setGradeDisplay("Released");
            StudentGradeViewResponse gradeView = new StudentGradeViewResponse();
            gradeView.setScore(grade.getScore());
            gradeView.setPointsEarned(grade.getScore());
            gradeView.setPointsPossible(assignment.getPointsPossible());
            gradeView.setFeedbackHtml(grade.getFeedbackHtml());
            boolean hasTextFeedback = grade.getFeedbackHtml() != null && !grade.getFeedbackHtml().isBlank();
            gradeView.setHasFeedback(hasTextFeedback);
            gradeView.setReleasedAt(grade.getReleasedAt());
            gradeView.setGradeStatus(AssignmentGradingService.GRADE_RELEASED);
            response.setGrade(gradeView);
        } else if (SubmissionStatusCalculator.NOT_SUBMITTED_CLOSED.equals(submissionStatus)) {
            response.setGradeDisplay("DashClosed");
            response.setGrade(null);
            response.setFeedback(null);
        } else {
            response.setGradeDisplay("NotGradedYet");
            response.setGrade(null);
            response.setFeedback(null);
        }
        return response;
    }

    public ReceiptSummaryResponse toReceiptSummary(AssignmentSubmissionReceipt receipt,
                                                   List<AssignmentSubmissionFile> files) {
        if (receipt == null) {
            return null;
        }
        ReceiptSummaryResponse summary = new ReceiptSummaryResponse();
        summary.setId(receipt.getId());
        summary.setIssuedAt(receipt.getIssuedAt());
        List<ReceiptSummaryResponse.ReceiptFileSummary> fileSummaries = new ArrayList<>();
        if (files != null) {
            for (AssignmentSubmissionFile file : files) {
                ReceiptSummaryResponse.ReceiptFileSummary item = new ReceiptSummaryResponse.ReceiptFileSummary();
                item.setOriginalName(file.getOriginalName());
                item.setSizeBytes(file.getSizeBytes());
                item.setChecksumSha256(file.getChecksumSha256());
                fileSummaries.add(item);
            }
        }
        summary.setFiles(fileSummaries);
        return summary;
    }

    private AssignmentResponse toBase(Assignment assignment, ZoneId zone,
                                      List<AssignmentAttachment> attachments,
                                      AssignmentRubricVersion currentRubric) {
        AssignmentResponse response = new AssignmentResponse();
        response.setId(assignment.getId());
        response.setCourseId(assignment.getCourseId());
        response.setTitle(assignment.getTitle());
        response.setDescription(assignment.getDescription());
        response.setPointsPossible(assignment.getPointsPossible());
        response.setDueAt(assignment.getDueAt());
        response.setLateUntil(assignment.getLateUntil());
        response.setSubmissionType(assignment.getSubmissionType());
        response.setGroupSetId(assignment.getGroupSetId());
        response.setAllowedFileTypes(assignmentFilePolicy.parseAllowedTypes(assignment.getAllowedFileTypes()));
        response.setMaxFileSizeBytes(assignment.getMaxFileSizeBytes());
        response.setMaxFileCount(assignment.getMaxFileCount());
        response.setState(assignment.getState());
        response.setCurrentRubricVersionId(assignment.getCurrentRubricVersionId());
        response.setHasRubric(assignment.getCurrentRubricVersionId() != null);
        if (currentRubric != null) {
            response.setCurrentRubricVersionNo(currentRubric.getVersionNo());
            response.setCurrentRubric(toRubricResponse(assignment.getCourseId(), assignment.getId(),
                    currentRubric, 0, false));
        } else {
            response.setCurrentRubric(null);
        }
        response.setAttachments(toAttachmentResponses(assignment.getCourseId(), attachments));
        response.setCreatedBy(assignment.getCreatedBy());
        response.setCreatedAt(assignment.getCreatedAt());
        response.setUpdatedAt(assignment.getUpdatedAt());

        AssignmentResponse.LatePolicyResponse latePolicy = new AssignmentResponse.LatePolicyResponse();
        latePolicy.setAcceptsLate(assignment.getLateUntil() != null);
        latePolicy.setLateUntil(assignment.getLateUntil());
        response.setLatePolicy(latePolicy);

        AssignmentResponse.SubmissionAreaResponse submissionArea = new AssignmentResponse.SubmissionAreaResponse();
        submissionArea.setMaxFileCount(assignment.getMaxFileCount());
        submissionArea.setMaxFileSizeBytes(assignment.getMaxFileSizeBytes());
        submissionArea.setAllowedFileTypes(assignmentFilePolicy.parseAllowedTypes(assignment.getAllowedFileTypes()));
        response.setSubmissionArea(submissionArea);

        if (zone != null) {
            response.setTimezone(zone.getId());
            response.setTimezoneLabel(zone.getId());
            response.setDueAtLocal(assignmentTimeSupport.toZone(assignment.getDueAt(), zone));
            response.setLateUntilLocal(assignmentTimeSupport.toZone(assignment.getLateUntil(), zone));
        }
        return response;
    }

    public List<AssignmentAttachmentResponse> toAttachmentResponses(Integer courseId, List<AssignmentAttachment> attachments) {
        List<AssignmentAttachmentResponse> result = new ArrayList<>();
        if (attachments == null) {
            return result;
        }
        for (AssignmentAttachment attachment : attachments) {
            result.add(toAttachmentResponse(courseId, attachment));
        }
        return result;
    }

    public AssignmentAttachmentResponse toAttachmentResponse(Integer courseId, AssignmentAttachment attachment) {
        AssignmentAttachmentResponse response = new AssignmentAttachmentResponse();
        response.setId(attachment.getId());
        response.setAssignmentId(attachment.getAssignmentId());
        response.setOriginalName(attachment.getOriginalName());
        response.setContentType(attachment.getContentType());
        response.setSizeBytes(attachment.getSizeBytes());
        response.setUploadedBy(attachment.getUploadedBy());
        response.setCreatedAt(attachment.getCreatedAt());
        response.setDownloadUrl(absoluteUrl("/v2/courses/" + courseId + "/assignments/"
                + attachment.getAssignmentId() + "/attachments/" + attachment.getId() + "/download"));
        return response;
    }

    public RubricResponse toRubricResponse(Integer courseId, Integer assignmentId,
                                           AssignmentRubricVersion currentVersion,
                                           int totalVersions, boolean canRestorePrevious) {
        RubricResponse response = new RubricResponse();
        response.setAssignmentId(assignmentId);
        response.setTotalVersions(totalVersions);
        if (currentVersion == null) {
            response.setPosted(false);
            response.setCanRestorePrevious(canRestorePrevious);
            return response;
        }
        response.setPosted(true);
        response.setVersionId(currentVersion.getId());
        response.setVersionNo(currentVersion.getVersionNo());
        response.setOriginalName(currentVersion.getOriginalName());
        response.setContentType(currentVersion.getContentType());
        response.setSizeBytes(currentVersion.getSizeBytes());
        response.setUploadedBy(currentVersion.getUploadedBy());
        response.setUploadedAt(currentVersion.getCreatedAt());
        response.setCanRestorePrevious(canRestorePrevious);
        response.setDownloadUrl(absoluteUrl("/v2/courses/" + courseId + "/assignments/" + assignmentId + "/rubric/download"));
        return response;
    }

    String absoluteUrl(String path) {
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath().path(path).toUriString();
        } catch (IllegalStateException e) {
            return "/api" + path;
        }
    }
}
