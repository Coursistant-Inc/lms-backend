package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.dto.MyGradeResponse;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.entity.AssignmentGrade;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmission;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionVersion;
import com.coursistant.lms.module.assignment.repository.AssignmentGradeMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionVersionMapper;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A student's own grade list for a course. Scores, feedback, and annotated files appear only
 * for grades in {@code Released}; anything still {@code Entered} (or Ungraded) is reported as
 * not released, without leaking the pending score.
 */
@Service
public class AssignmentMyGradesService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentMyGradesService.class);

    @Resource
    private AssignmentMapper assignmentMapper;

    @Resource
    private AssignmentGradeMapper assignmentGradeMapper;

    @Resource
    private AssignmentSubmissionMapper assignmentSubmissionMapper;

    @Resource
    private AssignmentSubmissionVersionMapper assignmentSubmissionVersionMapper;

    @Resource
    private AssignmentSubmissionService assignmentSubmissionService;

    @Resource
    private AssignmentAccessService assignmentAccessService;

    @Resource
    private AssignmentTimeSupport assignmentTimeSupport;

    @Resource
    private SubmissionStatusCalculator submissionStatusCalculator;

    @Resource
    private AssignmentResponseAssembler assignmentResponseAssembler;

    public List<MyGradeResponse> listMyGrades(Integer courseId, Integer userId, String timezoneHeader) {
        assignmentAccessService.requireCourse(courseId);
        assignmentAccessService.requireStudentMember(courseId, userId);
        ZoneId zone = assignmentTimeSupport.requireZone(timezoneHeader);
        LocalDateTime now = assignmentTimeSupport.nowUtc();

        List<Assignment> assignments = assignmentMapper.selectByCourseIdAndState(courseId,
                AssignmentAccessService.STATE_PUBLISHED);
        if (assignments == null) {
            throw AssignmentErrors.fail(log, courseId, null, userId, ErrorType.INTERNAL_ERROR,
                    "Assignment list query returned null");
        }
        assignments.sort(Comparator
                .comparing(Assignment::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Assignment::getId));

        List<MyGradeResponse> result = new ArrayList<>();
        for (Assignment assignment : assignments) {
            result.add(toMyGrade(assignment, userId, zone, now));
        }
        return result;
    }

    private MyGradeResponse toMyGrade(Assignment assignment, Integer userId, ZoneId zone, LocalDateTime now) {
        MyGradeResponse response = new MyGradeResponse();
        response.setAssignmentId(assignment.getId());
        response.setAssignmentTitle(assignment.getTitle());
        response.setTitle(assignment.getTitle());
        response.setPointsPossible(assignment.getPointsPossible());
        response.setDueAt(assignment.getDueAt());
        response.setDueAtLocal(assignmentTimeSupport.toZone(assignment.getDueAt(), zone));

        AssignmentSubmissionVersion version = currentVersionOf(assignment.getId(), userId);
        response.setSubmittedAt(version == null ? null : version.getSubmittedAt());
        response.setVersionNo(version == null ? null : version.getVersionNo());
        response.setSubmissionStatus(submissionStatusCalculator.calculate(assignment.getDueAt(),
                assignment.getLateUntil(), now,
                version == null ? null : version.getSubmittedAt(),
                version == null ? null : version.getUsedGraceBuffer(),
                stagingCreatedAtsIfRelevant(assignment, version, userId, now)));

        AssignmentGrade grade = assignmentGradeMapper.selectByAssignmentIdAndStudentUserId(assignment.getId(), userId);
        boolean released = grade != null && AssignmentGradingService.GRADE_RELEASED.equals(grade.getStatus());
        response.setReleased(released);
        response.setItemType("Individual");
        if (released) {
            response.setGradeDisplay("Released");
            response.setScore(grade.getScore());
            response.setPointsEarned(grade.getScore());
            response.setFeedbackHtml(grade.getFeedbackHtml());
            boolean hasTextFeedback = grade.getFeedbackHtml() != null && !grade.getFeedbackHtml().isBlank();
            response.setHasFeedback(hasTextFeedback);
            response.setReleasedAt(grade.getReleasedAt());
            boolean hasAnnotated = grade.getAnnotatedObjectKey() != null;
            response.setHasAnnotatedFile(hasAnnotated);
            if (hasAnnotated) {
                response.setAnnotatedOriginalName(grade.getAnnotatedOriginalName());
                response.setAnnotatedFileUrl(assignmentResponseAssembler.absoluteUrl(
                        "/v2/courses/" + assignment.getCourseId() + "/assignments/" + assignment.getId()
                                + "/students/" + userId + "/grade/annotated-file"));
            }
        } else if (SubmissionStatusCalculator.NOT_SUBMITTED_CLOSED.equals(response.getSubmissionStatus())) {
            response.setGradeDisplay("DashClosed");
            response.setHasFeedback(false);
        } else {
            response.setGradeDisplay("NotGradedYet");
            response.setHasFeedback(false);
        }
        return response;
    }

    private AssignmentSubmissionVersion currentVersionOf(Integer assignmentId, Integer userId) {
        AssignmentSubmission submission =
                assignmentSubmissionMapper.selectByAssignmentIdAndOwnerUserId(assignmentId, userId);
        if (submission == null || submission.getCurrentVersionId() == null) {
            return null;
        }
        return assignmentSubmissionVersionMapper.selectById(submission.getCurrentVersionId());
    }

    /**
     * Staging times only change the status of an unsubmitted assignment inside the grace buffer,
     * so the lookup is skipped for the rest of the list.
     */
    private List<LocalDateTime> stagingCreatedAtsIfRelevant(Assignment assignment, AssignmentSubmissionVersion version,
                                                            Integer userId, LocalDateTime now) {
        if (version != null
                || !submissionStatusCalculator.isWithinGraceWindow(assignment.getDueAt(), assignment.getLateUntil(), now)) {
            return List.of();
        }
        return assignmentSubmissionService.activeStagingCreatedAts(assignment.getId(), userId, now);
    }
}
