package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.dto.GradeResponse;
import com.coursistant.lms.module.assignment.dto.GradeTransitionResponse;
import com.coursistant.lms.module.assignment.dto.GradeTransitionSkip;
import com.coursistant.lms.module.assignment.dto.GroupMemberSummary;
import com.coursistant.lms.module.assignment.dto.GradingRosterItemResponse;
import com.coursistant.lms.module.assignment.dto.GradingRosterResponse;
import com.coursistant.lms.module.assignment.dto.GradingViewResponse;
import com.coursistant.lms.module.assignment.dto.SubmissionVersionResponse;
import com.coursistant.lms.module.assignment.dto.UpsertGradeRequest;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.entity.AssignmentGrade;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmission;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionFile;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionVersion;
import com.coursistant.lms.module.assignment.entity.AssignmentGradeReleaseRecipient;
import com.coursistant.lms.module.assignment.repository.AssignmentGradeMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentGradeReleaseRecipientMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionFileMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionVersionMapper;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.course.group.entity.CourseGroup;
import com.coursistant.lms.module.course.group.entity.GroupMembership;
import com.coursistant.lms.module.course.group.repository.CourseGroupMapper;
import com.coursistant.lms.module.course.group.repository.GroupMembershipMapper;
import com.coursistant.lms.module.course.group.service.GroupAccessService;
import com.coursistant.lms.module.interaction.notification.service.NotificationRecipientResolver;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.module.tenant.service.TenantTimezoneService;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Grading roster, per-student grading view, grade upsert, and the release / retract workflow.
 *
 * <p>"Ungraded" is the absence of a grade row, so a score is always required on write. Students
 * only ever see a score once its grade row reaches {@code Released}; graders keep working in
 * {@code Entered} until then.</p>
 */
@Service
public class AssignmentGradingService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentGradingService.class);

    public static final String GRADE_UNGRADED = "Ungraded";
    public static final String GRADE_ENTERED = "Entered";
    public static final String GRADE_RELEASED = "Released";

    @Resource
    private AssignmentMapper assignmentMapper;

    @Resource
    private AssignmentGradeMapper assignmentGradeMapper;

    @Resource
    private AssignmentSubmissionMapper assignmentSubmissionMapper;

    @Resource
    private AssignmentSubmissionVersionMapper assignmentSubmissionVersionMapper;

    @Resource
    private AssignmentSubmissionFileMapper assignmentSubmissionFileMapper;

    @Resource
    private EnrollmentMapper enrollmentMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private AssignmentAccessService assignmentAccessService;

    @Resource
    private AssignmentFilePolicy assignmentFilePolicy;

    @Resource
    private AssignmentStorageService assignmentStorageService;

    @Resource
    private AssignmentTimeSupport assignmentTimeSupport;

    @Resource
    private SubmissionStatusCalculator submissionStatusCalculator;

    @Resource
    private SubmissionResponseAssembler submissionResponseAssembler;

    @Resource
    private AssignmentRubricService assignmentRubricService;

    @Resource
    private AssignmentSubmissionService assignmentSubmissionService;

    @Resource
    private AssignmentResponseAssembler assignmentResponseAssembler;

    @Resource
    private AssignmentAuditService assignmentAuditService;

    @Resource
    private AssignmentNotificationService assignmentNotificationService;

    @Resource
    private NotificationRecipientResolver notificationRecipientResolver;

    @Resource
    private AssignmentGradeReleaseRecipientMapper assignmentGradeReleaseRecipientMapper;

    @Resource
    private CourseGroupMapper courseGroupMapper;

    @Resource
    private GroupMembershipMapper groupMembershipMapper;

    @Resource
    private GroupAccessService groupAccessService;

    @Resource
    private TenantTimezoneService tenantTimezoneService;

    // ---------------------------------------------------------------- roster

    /**
     * Individual: active Students. Group: every group in the linked set (including unsubmitted).
     */
    public GradingRosterResponse getRoster(Integer courseId, Integer assignmentId, Integer userId) {
        Course course = assignmentAccessService.requireCourse(courseId);
        assignmentAccessService.requireCanGrade(courseId, userId);
        Assignment assignment = requireAssignment(courseId, assignmentId, userId);

        if (AssignmentAccessService.SUBMISSION_TYPE_GROUP.equals(assignment.getSubmissionType())) {
            return getGroupRoster(course, assignment, userId);
        }

        LocalDateTime now = assignmentTimeSupport.nowUtc();
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(courseId);
        List<Enrollment> students = activeStudents(courseId);
        Map<Integer, AssignmentGrade> gradesByStudent = gradesByStudent(assignmentId);

        GradingRosterResponse response = new GradingRosterResponse();
        response.setAssignmentId(assignmentId);
        response.setAssignmentTitle(assignment.getTitle());
        response.setPointsPossible(assignment.getPointsPossible());
        populateRosterDeadlines(response, assignment, zone);
        response.setTotalStudents(students.size());
        response.setGradingWritable(assignmentAccessService.isGradingWritable(course));
        response.setGradingWritableUntil(assignmentAccessService.gradingWritableUntil(course));

        int submitted = 0;
        int late = 0;
        int notSubmitted = 0;
        int ungraded = 0;
        int entered = 0;
        int released = 0;

        for (Enrollment student : students) {
            GradingRosterItemResponse item = buildRosterItem(assignment, student.getUserId(),
                    gradesByStudent.get(student.getUserId()), now);
            response.getItems().add(item);
            if (SubmissionStatusCalculator.SUBMITTED.equals(item.getSubmissionStatus())) {
                submitted++;
            } else if (SubmissionStatusCalculator.SUBMITTED_LATE.equals(item.getSubmissionStatus())) {
                submitted++;
                late++;
            } else {
                notSubmitted++;
            }

            if (GRADE_RELEASED.equals(item.getGradeStatus())) {
                released++;
            } else if (GRADE_ENTERED.equals(item.getGradeStatus())) {
                entered++;
            } else {
                ungraded++;
            }
        }

        response.setSubmittedCount(submitted);
        response.setLateCount(late);
        response.setNotSubmittedCount(notSubmitted);
        response.setUngradedCount(ungraded);
        response.setEnteredCount(entered);
        response.setReleasedCount(released);
        return response;
    }

    private GradingRosterResponse getGroupRoster(Course course, Assignment assignment, Integer userId) {
        LocalDateTime now = assignmentTimeSupport.nowUtc();
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(course.getId());
        groupAccessService.requireGroupSetInCourse(course.getId(), assignment.getGroupSetId());
        List<CourseGroup> groups = courseGroupMapper.selectByGroupSetId(assignment.getGroupSetId());
        if (groups == null) {
            throw AssignmentErrors.fail(log, course.getId(), assignment.getId(), userId, ErrorType.INTERNAL_ERROR,
                    "Group roster query returned null");
        }
        Map<Integer, AssignmentGrade> gradesByGroup = gradesByGroup(assignment.getId());

        GradingRosterResponse response = new GradingRosterResponse();
        response.setAssignmentId(assignment.getId());
        response.setAssignmentTitle(assignment.getTitle());
        response.setPointsPossible(assignment.getPointsPossible());
        populateRosterDeadlines(response, assignment, zone);
        response.setTotalStudents(groups.size());
        response.setGradingWritable(assignmentAccessService.isGradingWritable(course));
        response.setGradingWritableUntil(assignmentAccessService.gradingWritableUntil(course));

        int submitted = 0;
        int late = 0;
        int notSubmitted = 0;
        int ungraded = 0;
        int entered = 0;
        int released = 0;

        for (CourseGroup group : groups) {
            GradingRosterItemResponse item = buildGroupRosterItem(assignment, group,
                    gradesByGroup.get(group.getId()), now);
            response.getItems().add(item);
            if (SubmissionStatusCalculator.SUBMITTED.equals(item.getSubmissionStatus())) {
                submitted++;
            } else if (SubmissionStatusCalculator.SUBMITTED_LATE.equals(item.getSubmissionStatus())) {
                submitted++;
                late++;
            } else {
                notSubmitted++;
            }
            if (GRADE_RELEASED.equals(item.getGradeStatus())) {
                released++;
            } else if (GRADE_ENTERED.equals(item.getGradeStatus())) {
                entered++;
            } else {
                ungraded++;
            }
        }
        response.setSubmittedCount(submitted);
        response.setLateCount(late);
        response.setNotSubmittedCount(notSubmitted);
        response.setUngradedCount(ungraded);
        response.setEnteredCount(entered);
        response.setReleasedCount(released);
        return response;
    }

    public GradingViewResponse getGradingView(Integer courseId, Integer assignmentId, Integer studentUserId,
                                              Integer userId) {
        Course course = assignmentAccessService.requireCourse(courseId);
        assignmentAccessService.requireCanGrade(courseId, userId);
        Assignment assignment = requireAssignment(courseId, assignmentId, userId);
        requireInRoster(courseId, assignmentId, studentUserId, userId);

        ZoneId zone = tenantTimezoneService.requireZoneForCourse(courseId);
        LocalDateTime now = assignmentTimeSupport.nowUtc();
        AssignmentGrade grade = assignmentGradeMapper.selectByAssignmentIdAndStudentUserId(assignmentId, studentUserId);

        GradingViewResponse response = new GradingViewResponse();
        response.setAssignmentId(assignmentId);
        response.setAssignmentTitle(assignment.getTitle());
        response.setStudent(buildRosterItem(assignment, studentUserId, grade, now));
        response.setGradingWritable(assignmentAccessService.isGradingWritable(course));
        fillPrevNext(response, courseId, studentUserId);

        List<SubmissionVersionResponse> versions = assignmentSubmissionService.versionHistory(assignment, studentUserId, zone);
        response.setVersions(versions);
        AssignmentSubmissionVersion currentVersion = currentVersionOf(assignmentId, studentUserId);
        response.setCurrentVersion(submissionResponseAssembler.toVersionResponse(assignment, currentVersion, zone, true));
        response.setRubric(assignmentRubricService.toResponse(assignment));
        if (grade != null) {
            response.setGrade(toGradeResponse(assignment, grade));
        }
        return response;
    }

    private void fillPrevNext(GradingViewResponse response, Integer courseId, Integer studentUserId) {
        List<Enrollment> students = activeStudents(courseId);
        for (int i = 0; i < students.size(); i++) {
            if (studentUserId.equals(students.get(i).getUserId())) {
                if (i > 0) {
                    response.setPrevStudentId(students.get(i - 1).getUserId());
                }
                if (i + 1 < students.size()) {
                    response.setNextStudentId(students.get(i + 1).getUserId());
                }
                return;
            }
        }
    }

    // ----------------------------------------------------------------- write

    /**
     * Creates or updates the student's grade. Editing a Released grade keeps it Released — use
     * retract first if the score should be hidden again while it is reworked.
     */
    @Transactional
    public GradeResponse upsertGrade(Integer courseId, Integer assignmentId, Integer studentUserId, Integer userId,
                                     UpsertGradeRequest body) {
        assignmentAccessService.requireGradingWritable(courseId, userId);
        Assignment assignment = requireAssignment(courseId, assignmentId, userId);
        requireInRoster(courseId, assignmentId, studentUserId, userId);
        if (body == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.PARAM_MISSING,
                    "Request body is required");
        }
        BigDecimal score = body.getScore();
        if (score == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.PARAM_MISSING,
                    "score is required; delete-to-ungrade is not supported");
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(assignment.getPointsPossible()) > 0) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.GRADE_SCORE_OUT_OF_RANGE,
                    "score must be between 0 and " + assignment.getPointsPossible());
        }

        AssignmentGrade existing = assignmentGradeMapper.selectByAssignmentIdAndStudentUserId(assignmentId, studentUserId);
        LocalDateTime now = assignmentTimeSupport.nowUtc();
        AssignmentSubmissionVersion currentVersion = currentVersionOf(assignmentId, studentUserId);

        AssignmentGrade grade = new AssignmentGrade();
        grade.setAssignmentId(assignmentId);
        grade.setStudentUserId(studentUserId);
        grade.setGroupId(null);
        grade.setScore(score);
        grade.setSubmissionVersionId(firstNonNull(body.getSubmissionVersionId(),
                currentVersion == null ? null : currentVersion.getId(),
                existing == null ? null : existing.getSubmissionVersionId()));
        grade.setRubricVersionId(firstNonNull(body.getRubricVersionId(),
                assignment.getCurrentRubricVersionId(),
                existing == null ? null : existing.getRubricVersionId()));
        grade.setFeedbackHtml(body.getFeedbackHtml() != null
                ? body.getFeedbackHtml()
                : (existing == null ? null : existing.getFeedbackHtml()));
        // The annotated file has its own endpoint; a score edit must never drop it.
        grade.setAnnotatedObjectKey(existing == null ? null : existing.getAnnotatedObjectKey());
        grade.setAnnotatedOriginalName(existing == null ? null : existing.getAnnotatedOriginalName());
        grade.setAnnotatedContentType(existing == null ? null : existing.getAnnotatedContentType());
        grade.setAnnotatedSizeBytes(existing == null ? null : existing.getAnnotatedSizeBytes());
        grade.setStatus(existing == null ? GRADE_ENTERED : existing.getStatus());
        grade.setReleasedAt(existing == null ? null : existing.getReleasedAt());
        grade.setEnteredBy(existing == null ? userId : existing.getEnteredBy());
        grade.setEnteredAt(existing == null ? now : existing.getEnteredAt());
        grade.setEditedBy(userId);
        grade.setAiAssisted(body.getAiAssisted() != null
                ? body.getAiAssisted()
                : (existing != null && Boolean.TRUE.equals(existing.getAiAssisted())));
        grade.setAiProvenanceJson(body.getAiProvenanceJson() != null
                ? body.getAiProvenanceJson()
                : (existing == null ? null : existing.getAiProvenanceJson()));
        assignmentGradeMapper.upsert(grade);

        Map<String, Object> auditDetail = new LinkedHashMap<>();
        auditDetail.put("studentUserId", studentUserId);
        auditDetail.put("score", score);
        auditDetail.put("status", grade.getStatus());
        auditDetail.put("created", existing == null);
        assignmentAuditService.write(courseId, assignmentId, userId, AssignmentAuditService.GRADE_UPSERTED, auditDetail);

        String newFeedback = grade.getFeedbackHtml();
        if (existing != null
                && GRADE_RELEASED.equals(existing.getStatus())
                && visibleGradeFieldsChanged(existing.getScore(), existing.getFeedbackHtml(), score, newFeedback)) {
            Integer correctionAuditId = assignmentAuditService.write(courseId, assignmentId, userId,
                    AssignmentAuditService.GRADE_CORRECTED_AFTER_RELEASE,
                    Map.of("studentUserId", studentUserId, "score", score));
            Course course = assignmentAccessService.requireCourse(courseId);
            List<Integer> recipients = notificationRecipientResolver.filterCandidateRecipients(
                    course, List.of(studentUserId));
            Assignment assignmentForNotify = assignment;
            Integer auditIdForNotify = correctionAuditId;
            assignmentNotificationService.afterCommit(
                    () -> assignmentNotificationService.notifyGradeCorrectedAfterRelease(
                            assignmentForNotify, recipients, auditIdForNotify));
        }

        return toGradeResponse(assignment, requireGrade(courseId, assignmentId, studentUserId, userId));
    }

    @Transactional
    public GradeResponse uploadAnnotatedFile(Integer courseId, Integer assignmentId, Integer studentUserId,
                                             Integer userId, MultipartFile file) {
        assignmentAccessService.requireGradingWritable(courseId, userId);
        Assignment assignment = requireAssignment(courseId, assignmentId, userId);
        requireInRoster(courseId, assignmentId, studentUserId, userId);
        assignmentFilePolicy.validateAnnotatedFile(file);

        AssignmentGrade existing = requireGrade(courseId, assignmentId, studentUserId, userId);
        String previousKey = existing.getAnnotatedObjectKey();

        String objectKey = assignmentFilePolicy.annotatedKey(courseId, assignmentId, studentUserId,
                file.getOriginalFilename());
        assignmentStorageService.upload(objectKey, file, courseId, assignmentId, userId);

        AssignmentGrade patch = new AssignmentGrade();
        patch.setId(existing.getId());
        patch.setAnnotatedObjectKey(objectKey);
        patch.setAnnotatedOriginalName(assignmentFilePolicy.sanitizeFilename(file.getOriginalFilename()));
        patch.setAnnotatedContentType(file.getContentType());
        patch.setAnnotatedSizeBytes(file.getSize());
        patch.setEditedBy(userId);
        assignmentGradeMapper.updateById(patch);

        assignmentAuditService.write(courseId, assignmentId, userId,
                AssignmentAuditService.GRADE_ANNOTATED_FILE_UPLOADED,
                Map.of("studentUserId", studentUserId, "originalName",
                        String.valueOf(patch.getAnnotatedOriginalName())));

        boolean annotatedKeyChanged = !Objects.equals(previousKey, objectKey);
        if (GRADE_RELEASED.equals(existing.getStatus()) && annotatedKeyChanged) {
            Integer correctionAuditId = assignmentAuditService.write(courseId, assignmentId, userId,
                    AssignmentAuditService.GRADE_CORRECTED_AFTER_RELEASE,
                    Map.of("studentUserId", studentUserId, "annotatedFileChanged", true));
            Course course = assignmentAccessService.requireCourse(courseId);
            List<Integer> recipients = notificationRecipientResolver.filterCandidateRecipients(
                    course, List.of(studentUserId));
            Assignment assignmentForNotify = assignment;
            Integer auditIdForNotify = correctionAuditId;
            assignmentNotificationService.afterCommit(
                    () -> assignmentNotificationService.notifyGradeCorrectedAfterRelease(
                            assignmentForNotify, recipients, auditIdForNotify));
        }

        if (previousKey != null && !previousKey.equals(objectKey)) {
            assignmentStorageService.deleteQuietly(previousKey);
        }
        return toGradeResponse(assignment, requireGrade(courseId, assignmentId, studentUserId, userId));
    }

    /**
     * Graders may always read the annotated file; the student may only read their own, and only
     * after the grade has been released.
     */
    public ResponseEntity<InputStreamResource> downloadAnnotatedFile(Integer courseId, Integer assignmentId,
                                                                     Integer studentUserId, Integer userId) {
        assignmentAccessService.requireCourse(courseId);
        boolean self = studentUserId.equals(userId);
        if (self) {
            assignmentAccessService.requireActiveMember(courseId, userId);
        } else {
            assignmentAccessService.requireCanGrade(courseId, userId);
        }
        requireAssignmentForReader(courseId, assignmentId, userId, self);

        AssignmentGrade grade = requireGrade(courseId, assignmentId, studentUserId, userId);
        if (self && !GRADE_RELEASED.equals(grade.getStatus())) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.GRADE_NOT_FOUND,
                    "This grade has not been released yet");
        }
        if (grade.getAnnotatedObjectKey() == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.NOT_FOUND,
                    "No annotated file for this grade");
        }
        return assignmentStorageService.stream(grade.getAnnotatedObjectKey(), grade.getAnnotatedOriginalName(),
                grade.getAnnotatedContentType(), true, courseId, assignmentId, userId);
    }

    @Transactional
    public GradeResponse uploadGroupAnnotatedFile(Integer courseId, Integer assignmentId, Integer groupId,
                                                  Integer userId, MultipartFile file) {
        assignmentAccessService.requireGradingWritable(courseId, userId);
        Assignment assignment = requireAssignment(courseId, assignmentId, userId);
        requireGroupAssignment(courseId, assignmentId, userId, assignment);
        requireGroupInRoster(courseId, assignment, groupId, userId);
        assignmentFilePolicy.validateAnnotatedFile(file);

        AssignmentGrade existing = requireGroupGrade(courseId, assignmentId, groupId, userId);
        String previousKey = existing.getAnnotatedObjectKey();

        String objectKey = assignmentFilePolicy.annotatedGroupKey(courseId, assignmentId, groupId,
                file.getOriginalFilename());
        assignmentStorageService.upload(objectKey, file, courseId, assignmentId, userId);

        AssignmentGrade patch = new AssignmentGrade();
        patch.setId(existing.getId());
        patch.setAnnotatedObjectKey(objectKey);
        patch.setAnnotatedOriginalName(assignmentFilePolicy.sanitizeFilename(file.getOriginalFilename()));
        patch.setAnnotatedContentType(file.getContentType());
        patch.setAnnotatedSizeBytes(file.getSize());
        patch.setEditedBy(userId);
        assignmentGradeMapper.updateById(patch);

        assignmentAuditService.write(courseId, assignmentId, userId,
                AssignmentAuditService.GRADE_ANNOTATED_FILE_UPLOADED,
                Map.of("groupId", groupId, "originalName",
                        String.valueOf(patch.getAnnotatedOriginalName())));

        boolean annotatedKeyChanged = !Objects.equals(previousKey, objectKey);
        if (GRADE_RELEASED.equals(existing.getStatus()) && annotatedKeyChanged) {
            Integer correctionAuditId = assignmentAuditService.write(courseId, assignmentId, userId,
                    AssignmentAuditService.GRADE_CORRECTED_AFTER_RELEASE,
                    Map.of("groupId", groupId, "annotatedFileChanged", true));
            Course course = assignmentAccessService.requireCourse(courseId);
            List<Integer> memberIds = groupMemberUserIds(groupId);
            List<Integer> recipients = notificationRecipientResolver.filterCandidateRecipients(course, memberIds);
            Assignment assignmentForNotify = assignment;
            Integer auditIdForNotify = correctionAuditId;
            assignmentNotificationService.afterCommit(
                    () -> assignmentNotificationService.notifyGradeCorrectedAfterRelease(
                            assignmentForNotify, recipients, auditIdForNotify));
        }

        if (previousKey != null && !previousKey.equals(objectKey)) {
            assignmentStorageService.deleteQuietly(previousKey);
        }
        return toGradeResponse(assignment, requireGroupGrade(courseId, assignmentId, groupId, userId));
    }

    /**
     * Staff may always read. Students may read only when the grade is Released and they appear
     * on that grade's release-recipient snapshot (joiners after release must not inherit access).
     */
    public ResponseEntity<InputStreamResource> downloadGroupAnnotatedFile(Integer courseId, Integer assignmentId,
                                                                          Integer groupId, Integer userId) {
        assignmentAccessService.requireCourse(courseId);
        boolean staff = assignmentAccessService.canGrade(courseId, userId);
        if (!staff) {
            assignmentAccessService.requireActiveMember(courseId, userId);
        }
        Assignment assignment = requireAssignmentForReader(courseId, assignmentId, userId, !staff);
        requireGroupAssignment(courseId, assignmentId, userId, assignment);
        requireGroupInRoster(courseId, assignment, groupId, userId);

        AssignmentGrade grade = assignmentGradeMapper.selectByAssignmentIdAndGroupId(assignmentId, groupId);
        if (grade == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.GRADE_NOT_FOUND,
                    "No grade for this group");
        }
        if (!staff) {
            if (!GRADE_RELEASED.equals(grade.getStatus()) || !isReleaseRecipient(grade.getId(), userId)) {
                throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.GRADE_NOT_FOUND,
                        "This grade has not been released yet");
            }
        }
        if (grade.getAnnotatedObjectKey() == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.NOT_FOUND,
                    "No annotated file for this grade");
        }
        return assignmentStorageService.stream(grade.getAnnotatedObjectKey(), grade.getAnnotatedOriginalName(),
                grade.getAnnotatedContentType(), true, courseId, assignmentId, userId);
    }

    // ------------------------------------------------- release / retract

    @Transactional
    public GradeTransitionResponse releaseAll(Integer courseId, Integer assignmentId, Integer userId) {
        assignmentAccessService.requireReleaseWritable(courseId, userId);
        Assignment assignment = requireAssignment(courseId, assignmentId, userId);

        if (AssignmentAccessService.SUBMISSION_TYPE_GROUP.equals(assignment.getSubmissionType())) {
            List<Integer> groupIds = new ArrayList<>();
            for (AssignmentGrade grade : grades(assignmentId)) {
                if (GRADE_ENTERED.equals(grade.getStatus()) && grade.getGroupId() != null) {
                    groupIds.add(grade.getGroupId());
                }
            }
            return applyGroupRelease(assignment, courseId, assignmentId, userId, groupIds);
        }

        List<Integer> studentUserIds = new ArrayList<>();
        for (AssignmentGrade grade : grades(assignmentId)) {
            if (GRADE_ENTERED.equals(grade.getStatus()) && grade.getStudentUserId() != null) {
                studentUserIds.add(grade.getStudentUserId());
            }
        }
        return applyRelease(assignment, courseId, assignmentId, userId, studentUserIds);
    }

    @Transactional
    public GradeTransitionResponse release(Integer courseId, Integer assignmentId, Integer userId,
                                           List<Integer> studentUserIds) {
        assignmentAccessService.requireReleaseWritable(courseId, userId);
        Assignment assignment = requireAssignment(courseId, assignmentId, userId);
        requireSelection(courseId, assignmentId, userId, studentUserIds);
        return applyRelease(assignment, courseId, assignmentId, userId, studentUserIds);
    }

    @Transactional
    public GradeTransitionResponse releaseGroups(Integer courseId, Integer assignmentId, Integer userId,
                                                 List<Integer> groupIds) {
        assignmentAccessService.requireReleaseWritable(courseId, userId);
        Assignment assignment = requireAssignment(courseId, assignmentId, userId);
        requireGroupAssignment(courseId, assignmentId, userId, assignment);
        requireGroupSelection(courseId, assignmentId, userId, groupIds);
        return applyGroupRelease(assignment, courseId, assignmentId, userId, groupIds);
    }

    /**
     * Pulls released grades back to Entered. Deliberately silent: a retraction is a correction in
     * progress, so students are not notified.
     */
    @Transactional
    public GradeTransitionResponse retract(Integer courseId, Integer assignmentId, Integer userId,
                                           List<Integer> studentUserIds) {
        assignmentAccessService.requireReleaseWritable(courseId, userId);
        requireAssignment(courseId, assignmentId, userId);
        requireSelection(courseId, assignmentId, userId, studentUserIds);

        GradeTransitionResponse response = new GradeTransitionResponse();
        for (Integer studentUserId : studentUserIds) {
            AssignmentGrade grade = assignmentGradeMapper
                    .selectByAssignmentIdAndStudentUserId(assignmentId, studentUserId);
            if (grade == null) {
                response.getSkipped().add(new GradeTransitionSkip(studentUserId, GRADE_UNGRADED));
                continue;
            }
            if (!GRADE_RELEASED.equals(grade.getStatus())) {
                response.getSkipped().add(new GradeTransitionSkip(studentUserId, grade.getStatus()));
                continue;
            }
            assignmentGradeMapper.updateStatus(grade.getId(), GRADE_ENTERED, null, userId);
            response.getChangedStudentUserIds().add(studentUserId);
        }
        response.setChangedCount(response.getChangedStudentUserIds().size());

        if (response.getChangedCount() > 0) {
            assignmentAuditService.write(courseId, assignmentId, userId, AssignmentAuditService.GRADES_RETRACTED,
                    Map.of("studentUserIds", response.getChangedStudentUserIds()));
        }
        return response;
    }

    @Transactional
    public GradeTransitionResponse retractGroups(Integer courseId, Integer assignmentId, Integer userId,
                                                 List<Integer> groupIds) {
        assignmentAccessService.requireReleaseWritable(courseId, userId);
        Assignment assignment = requireAssignment(courseId, assignmentId, userId);
        requireGroupAssignment(courseId, assignmentId, userId, assignment);
        requireGroupSelection(courseId, assignmentId, userId, groupIds);

        GradeTransitionResponse response = new GradeTransitionResponse();
        for (Integer groupId : groupIds) {
            AssignmentGrade grade = assignmentGradeMapper.selectByAssignmentIdAndGroupId(assignmentId, groupId);
            if (grade == null) {
                response.getSkipped().add(GradeTransitionSkip.forGroup(groupId, GRADE_UNGRADED));
                continue;
            }
            if (!GRADE_RELEASED.equals(grade.getStatus())) {
                response.getSkipped().add(GradeTransitionSkip.forGroup(groupId, grade.getStatus()));
                continue;
            }
            assignmentGradeReleaseRecipientMapper.deleteByGradeId(grade.getId());
            assignmentGradeMapper.updateStatus(grade.getId(), GRADE_ENTERED, null, userId);
            response.getChangedGroupIds().add(groupId);
        }
        response.setChangedCount(response.getChangedGroupIds().size());
        if (response.getChangedCount() > 0) {
            assignmentAuditService.write(courseId, assignmentId, userId, AssignmentAuditService.GRADES_RETRACTED,
                    Map.of("groupIds", response.getChangedGroupIds()));
        }
        return response;
    }

    @Transactional
    public GradeResponse upsertGroupGrade(Integer courseId, Integer assignmentId, Integer groupId, Integer userId,
                                          UpsertGradeRequest body) {
        assignmentAccessService.requireGradingWritable(courseId, userId);
        Assignment assignment = requireAssignment(courseId, assignmentId, userId);
        requireGroupAssignment(courseId, assignmentId, userId, assignment);
        requireGroupInRoster(courseId, assignment, groupId, userId);
        if (body == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.PARAM_MISSING,
                    "Request body is required");
        }
        BigDecimal score = body.getScore();
        if (score == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.PARAM_MISSING,
                    "score is required; delete-to-ungrade is not supported");
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(assignment.getPointsPossible()) > 0) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.GRADE_SCORE_OUT_OF_RANGE,
                    "score must be between 0 and " + assignment.getPointsPossible());
        }

        AssignmentGrade existing = assignmentGradeMapper.selectByAssignmentIdAndGroupId(assignmentId, groupId);
        LocalDateTime now = assignmentTimeSupport.nowUtc();
        AssignmentSubmissionVersion currentVersion = currentVersionOfGroup(assignmentId, groupId);

        AssignmentGrade grade = new AssignmentGrade();
        grade.setAssignmentId(assignmentId);
        grade.setGroupId(groupId);
        grade.setStudentUserId(null);
        grade.setScore(score);
        grade.setSubmissionVersionId(firstNonNull(body.getSubmissionVersionId(),
                currentVersion == null ? null : currentVersion.getId(),
                existing == null ? null : existing.getSubmissionVersionId()));
        grade.setRubricVersionId(firstNonNull(body.getRubricVersionId(),
                assignment.getCurrentRubricVersionId(),
                existing == null ? null : existing.getRubricVersionId()));
        grade.setFeedbackHtml(body.getFeedbackHtml() != null
                ? body.getFeedbackHtml()
                : (existing == null ? null : existing.getFeedbackHtml()));
        grade.setAnnotatedObjectKey(existing == null ? null : existing.getAnnotatedObjectKey());
        grade.setAnnotatedOriginalName(existing == null ? null : existing.getAnnotatedOriginalName());
        grade.setAnnotatedContentType(existing == null ? null : existing.getAnnotatedContentType());
        grade.setAnnotatedSizeBytes(existing == null ? null : existing.getAnnotatedSizeBytes());
        grade.setStatus(existing == null ? GRADE_ENTERED : existing.getStatus());
        grade.setReleasedAt(existing == null ? null : existing.getReleasedAt());
        grade.setEnteredBy(existing == null ? userId : existing.getEnteredBy());
        grade.setEnteredAt(existing == null ? now : existing.getEnteredAt());
        grade.setEditedBy(userId);
        grade.setAiAssisted(body.getAiAssisted() != null
                ? body.getAiAssisted()
                : (existing != null && Boolean.TRUE.equals(existing.getAiAssisted())));
        grade.setAiProvenanceJson(body.getAiProvenanceJson() != null
                ? body.getAiProvenanceJson()
                : (existing == null ? null : existing.getAiProvenanceJson()));
        assignmentGradeMapper.upsert(grade);

        assignmentAuditService.write(courseId, assignmentId, userId, AssignmentAuditService.GRADE_UPSERTED,
                Map.of("groupId", groupId, "score", score, "status", grade.getStatus(),
                        "created", existing == null));

        String newFeedback = grade.getFeedbackHtml();
        if (existing != null
                && GRADE_RELEASED.equals(existing.getStatus())
                && visibleGradeFieldsChanged(existing.getScore(), existing.getFeedbackHtml(), score, newFeedback)) {
            Integer correctionAuditId = assignmentAuditService.write(courseId, assignmentId, userId,
                    AssignmentAuditService.GRADE_CORRECTED_AFTER_RELEASE,
                    Map.of("groupId", groupId, "score", score));
            Course course = assignmentAccessService.requireCourse(courseId);
            List<Integer> memberIds = groupMemberUserIds(groupId);
            List<Integer> recipients = notificationRecipientResolver.filterCandidateRecipients(course, memberIds);
            Assignment assignmentForNotify = assignment;
            Integer auditIdForNotify = correctionAuditId;
            assignmentNotificationService.afterCommit(
                    () -> assignmentNotificationService.notifyGradeCorrectedAfterRelease(
                            assignmentForNotify, recipients, auditIdForNotify));
        }

        return toGradeResponse(assignment, requireGroupGrade(courseId, assignmentId, groupId, userId));
    }

    public GradingViewResponse getGroupGradingView(Integer courseId, Integer assignmentId, Integer groupId,
                                                   Integer userId) {
        Course course = assignmentAccessService.requireCourse(courseId);
        assignmentAccessService.requireCanGrade(courseId, userId);
        Assignment assignment = requireAssignment(courseId, assignmentId, userId);
        requireGroupAssignment(courseId, assignmentId, userId, assignment);
        CourseGroup group = requireGroupInRoster(courseId, assignment, groupId, userId);

        ZoneId zone = tenantTimezoneService.requireZoneForCourse(courseId);
        LocalDateTime now = assignmentTimeSupport.nowUtc();
        AssignmentGrade grade = assignmentGradeMapper.selectByAssignmentIdAndGroupId(assignmentId, groupId);

        GradingViewResponse response = new GradingViewResponse();
        response.setAssignmentId(assignmentId);
        response.setAssignmentTitle(assignment.getTitle());
        response.setStudent(buildGroupRosterItem(assignment, group, grade, now));
        response.setGradingWritable(assignmentAccessService.isGradingWritable(course));
        response.setVersions(assignmentSubmissionService.versionHistoryForGroup(assignment, groupId, zone));
        AssignmentSubmissionVersion currentVersion = currentVersionOfGroup(assignmentId, groupId);
        response.setCurrentVersion(submissionResponseAssembler.toVersionResponse(assignment, currentVersion, zone, true));
        response.setRubric(assignmentRubricService.toResponse(assignment));
        if (grade != null) {
            response.setGrade(toGradeResponse(assignment, grade));
        }
        return response;
    }

    private GradeTransitionResponse applyRelease(Assignment assignment, Integer courseId, Integer assignmentId,
                                                 Integer userId, List<Integer> studentUserIds) {
        LocalDateTime now = assignmentTimeSupport.nowUtc();
        GradeTransitionResponse response = new GradeTransitionResponse();

        for (Integer studentUserId : studentUserIds) {
            AssignmentGrade grade = assignmentGradeMapper
                    .selectByAssignmentIdAndStudentUserId(assignmentId, studentUserId);
            if (grade == null) {
                response.getSkipped().add(new GradeTransitionSkip(studentUserId, GRADE_UNGRADED));
                continue;
            }
            if (!GRADE_ENTERED.equals(grade.getStatus())) {
                response.getSkipped().add(new GradeTransitionSkip(studentUserId, grade.getStatus()));
                continue;
            }
            assignmentGradeMapper.updateStatus(grade.getId(), GRADE_RELEASED, now, userId);
            response.getChangedStudentUserIds().add(studentUserId);
        }
        response.setChangedCount(response.getChangedStudentUserIds().size());

        if (response.getChangedCount() > 0) {
            List<Integer> changed = new ArrayList<>(response.getChangedStudentUserIds());
            Integer releaseAuditId = assignmentAuditService.write(courseId, assignmentId, userId,
                    AssignmentAuditService.GRADES_RELEASED,
                    Map.of("studentUserIds", changed));
            Course course = assignmentAccessService.requireCourse(courseId);
            List<Integer> recipients = notificationRecipientResolver.filterCandidateRecipients(course, changed);
            Assignment assignmentForNotify = assignment;
            Integer auditIdForNotify = releaseAuditId;
            assignmentNotificationService.afterCommit(
                    () -> assignmentNotificationService.notifyGradesReleased(
                            assignmentForNotify, recipients, auditIdForNotify));
        }
        return response;
    }

    private GradeTransitionResponse applyGroupRelease(Assignment assignment, Integer courseId, Integer assignmentId,
                                                      Integer userId, List<Integer> groupIds) {
        LocalDateTime now = assignmentTimeSupport.nowUtc();
        GradeTransitionResponse response = new GradeTransitionResponse();
        List<Integer> notified = new ArrayList<>();

        for (Integer groupId : groupIds) {
            AssignmentGrade grade = assignmentGradeMapper.selectByAssignmentIdAndGroupId(assignmentId, groupId);
            if (grade == null) {
                response.getSkipped().add(GradeTransitionSkip.forGroup(groupId, GRADE_UNGRADED));
                continue;
            }
            if (!GRADE_ENTERED.equals(grade.getStatus())) {
                // Already-released grades are not re-released (joiners must not inherit).
                response.getSkipped().add(GradeTransitionSkip.forGroup(groupId, grade.getStatus()));
                continue;
            }
            assignmentGradeMapper.updateStatus(grade.getId(), GRADE_RELEASED, now, userId);
            rewriteReleaseSnapshot(grade, groupId, now);
            response.getChangedGroupIds().add(groupId);
            for (GroupMembership member : groupMembershipMapper.selectByGroupId(groupId)) {
                notified.add(member.getUserId());
            }
        }
        response.setChangedCount(response.getChangedGroupIds().size());
        if (response.getChangedCount() > 0) {
            Integer releaseAuditId = assignmentAuditService.write(courseId, assignmentId, userId,
                    AssignmentAuditService.GRADES_RELEASED,
                    Map.of("groupIds", response.getChangedGroupIds()));
            Course course = assignmentAccessService.requireCourse(courseId);
            List<Integer> recipients = notificationRecipientResolver.filterCandidateRecipients(course, notified);
            Assignment assignmentForNotify = assignment;
            Integer auditIdForNotify = releaseAuditId;
            assignmentNotificationService.afterCommit(
                    () -> assignmentNotificationService.notifyGradesReleased(
                            assignmentForNotify, recipients, auditIdForNotify));
        }
        return response;
    }

    private void rewriteReleaseSnapshot(AssignmentGrade grade, Integer groupId, LocalDateTime releasedAt) {
        assignmentGradeReleaseRecipientMapper.deleteByGradeId(grade.getId());
        List<GroupMembership> members = groupMembershipMapper.selectByGroupId(groupId);
        if (members == null) {
            return;
        }
        for (GroupMembership member : members) {
            AssignmentGradeReleaseRecipient recipient = new AssignmentGradeReleaseRecipient();
            recipient.setGradeId(grade.getId());
            recipient.setAssignmentId(grade.getAssignmentId());
            recipient.setGroupId(groupId);
            recipient.setStudentUserId(member.getUserId());
            recipient.setReleasedAt(releasedAt);
            assignmentGradeReleaseRecipientMapper.insert(recipient);
        }
    }

    // ------------------------------------------------------------- internals

    private GradingRosterItemResponse buildRosterItem(Assignment assignment, Integer studentUserId,
                                                      AssignmentGrade grade, LocalDateTime now) {
        GradingRosterItemResponse item = new GradingRosterItemResponse();
        item.setStudentUserId(studentUserId);
        User user = userMapper.selectById(studentUserId);
        if (user != null) {
            item.setStudentName(user.getName());
            item.setStudentEmail(user.getEmail());
        }

        AssignmentSubmission submission = assignmentSubmissionMapper
                .selectByAssignmentIdAndOwnerUserId(assignment.getId(), studentUserId);
        AssignmentSubmissionVersion version = null;
        if (submission != null) {
            item.setSubmissionId(submission.getId());
            if (submission.getCurrentVersionId() != null) {
                version = assignmentSubmissionVersionMapper.selectById(submission.getCurrentVersionId());
            }
        }
        if (version != null) {
            item.setSubmissionVersionId(version.getId());
            item.setVersionNo(version.getVersionNo());
            item.setSubmittedAt(assignmentTimeSupport.toInstant(version.getSubmittedAt()));
            item.setUsedGraceBuffer(version.getUsedGraceBuffer());
            List<AssignmentSubmissionFile> files =
                    assignmentSubmissionFileMapper.selectBySubmissionVersionId(version.getId());
            item.setFileCount(files == null ? 0 : files.size());
        }
        // Staging times only matter for a student with no version during the 5-minute grace buffer;
        // skipping the lookup outside it keeps the roster to a fixed number of queries per student.
        List<LocalDateTime> stagingCreatedAts = version == null
                && submissionStatusCalculator.isWithinGraceWindow(assignment.getDueAt(), assignment.getLateUntil(), now)
                ? assignmentSubmissionService.activeStagingCreatedAts(assignment.getId(), studentUserId, now)
                : List.of();
        item.setSubmissionStatus(submissionStatusCalculator.calculate(assignment.getDueAt(), assignment.getLateUntil(),
                now,
                version == null ? null : version.getSubmittedAt(),
                version == null ? null : version.getUsedGraceBuffer(),
                stagingCreatedAts));

        if (grade == null) {
            item.setGradeStatus(GRADE_UNGRADED);
        } else {
            item.setGradeStatus(grade.getStatus());
            item.setScore(grade.getScore());
            item.setReleasedAt(assignmentTimeSupport.toInstant(grade.getReleasedAt()));
            item.setHasAnnotatedFile(grade.getAnnotatedObjectKey() != null);
        }
        return item;
    }

    private GradingRosterItemResponse buildGroupRosterItem(Assignment assignment, CourseGroup group,
                                                           AssignmentGrade grade, LocalDateTime now) {
        GradingRosterItemResponse item = new GradingRosterItemResponse();
        item.setGroupId(group.getId());
        item.setGroupName(group.getName());
        List<GroupMemberSummary> members = groupMemberSummaries(group.getId());
        item.setMembers(members);
        item.setMemberCount(members.size());

        AssignmentSubmission submission = assignmentSubmissionMapper
                .selectByAssignmentIdAndGroupId(assignment.getId(), group.getId());
        AssignmentSubmissionVersion version = null;
        if (submission != null) {
            item.setSubmissionId(submission.getId());
            if (submission.getCurrentVersionId() != null) {
                version = assignmentSubmissionVersionMapper.selectById(submission.getCurrentVersionId());
            }
        }
        if (version != null) {
            item.setSubmissionVersionId(version.getId());
            item.setVersionNo(version.getVersionNo());
            item.setSubmittedAt(assignmentTimeSupport.toInstant(version.getSubmittedAt()));
            item.setUsedGraceBuffer(version.getUsedGraceBuffer());
            item.setActualSubmitterUserId(version.getActualSubmitterUserId());
            List<AssignmentSubmissionFile> files =
                    assignmentSubmissionFileMapper.selectBySubmissionVersionId(version.getId());
            item.setFileCount(files == null ? 0 : files.size());
        }
        // Group status ignores personal staging.
        item.setSubmissionStatus(submissionStatusCalculator.calculate(assignment.getDueAt(), assignment.getLateUntil(),
                now,
                version == null ? null : version.getSubmittedAt(),
                version == null ? null : version.getUsedGraceBuffer(),
                List.of()));

        if (grade == null) {
            item.setGradeStatus(GRADE_UNGRADED);
        } else {
            item.setGradeStatus(grade.getStatus());
            item.setScore(grade.getScore());
            item.setReleasedAt(assignmentTimeSupport.toInstant(grade.getReleasedAt()));
            item.setHasAnnotatedFile(grade.getAnnotatedObjectKey() != null);
        }
        return item;
    }

    private GradeResponse toGradeResponse(Assignment assignment, AssignmentGrade grade) {
        GradeResponse response = new GradeResponse();
        response.setId(grade.getId());
        response.setAssignmentId(grade.getAssignmentId());
        response.setStudentUserId(grade.getStudentUserId());
        response.setGroupId(grade.getGroupId());
        response.setSubmissionVersionId(grade.getSubmissionVersionId());
        response.setRubricVersionId(grade.getRubricVersionId());
        response.setScore(grade.getScore());
        response.setPointsPossible(assignment.getPointsPossible());
        response.setFeedbackHtml(grade.getFeedbackHtml());
        response.setStatus(grade.getStatus());
        response.setHasAnnotatedFile(grade.getAnnotatedObjectKey() != null);
        response.setAnnotatedOriginalName(grade.getAnnotatedOriginalName());
        response.setAnnotatedContentType(grade.getAnnotatedContentType());
        response.setAnnotatedSizeBytes(grade.getAnnotatedSizeBytes());
        if (grade.getAnnotatedObjectKey() != null) {
            String annotatedPath = grade.getGroupId() != null
                    ? "/v2/courses/" + assignment.getCourseId() + "/assignments/" + assignment.getId()
                    + "/groups/" + grade.getGroupId() + "/grade/annotated-file"
                    : "/v2/courses/" + assignment.getCourseId() + "/assignments/" + assignment.getId()
                    + "/students/" + grade.getStudentUserId() + "/grade/annotated-file";
            response.setAnnotatedFileUrl(assignmentResponseAssembler.absoluteUrl(annotatedPath));
        }
        response.setEnteredBy(grade.getEnteredBy());
        response.setEnteredAt(assignmentTimeSupport.toInstant(grade.getEnteredAt()));
        response.setEditedBy(grade.getEditedBy());
        response.setUpdatedAt(assignmentTimeSupport.toInstant(grade.getUpdatedAt()));
        response.setReleasedAt(assignmentTimeSupport.toInstant(grade.getReleasedAt()));
        response.setAiAssisted(grade.getAiAssisted());
        return response;
    }

    private boolean isReleaseRecipient(Integer gradeId, Integer studentUserId) {
        if (gradeId == null || studentUserId == null) {
            return false;
        }
        List<AssignmentGradeReleaseRecipient> recipients =
                assignmentGradeReleaseRecipientMapper.selectByGradeId(gradeId);
        if (recipients == null) {
            return false;
        }
        for (AssignmentGradeReleaseRecipient recipient : recipients) {
            if (studentUserId.equals(recipient.getStudentUserId())) {
                return true;
            }
        }
        return false;
    }

    private List<GroupMemberSummary> groupMemberSummaries(Integer groupId) {
        List<GroupMemberSummary> result = new ArrayList<>();
        if (groupId == null) {
            return result;
        }
        List<GroupMembership> memberships = groupMembershipMapper.selectByGroupId(groupId);
        if (memberships == null) {
            return result;
        }
        for (GroupMembership membership : memberships) {
            GroupMemberSummary summary = new GroupMemberSummary();
            summary.setUserId(membership.getUserId());
            User user = userMapper.selectById(membership.getUserId());
            if (user != null) {
                summary.setName(user.getName());
                summary.setEmail(user.getEmail());
            }
            result.add(summary);
        }
        return result;
    }

    private AssignmentSubmissionVersion currentVersionOf(Integer assignmentId, Integer studentUserId) {
        AssignmentSubmission submission = assignmentSubmissionMapper
                .selectByAssignmentIdAndOwnerUserId(assignmentId, studentUserId);
        if (submission == null || submission.getCurrentVersionId() == null) {
            return null;
        }
        return assignmentSubmissionVersionMapper.selectById(submission.getCurrentVersionId());
    }

    private AssignmentSubmissionVersion currentVersionOfGroup(Integer assignmentId, Integer groupId) {
        AssignmentSubmission submission = assignmentSubmissionMapper
                .selectByAssignmentIdAndGroupId(assignmentId, groupId);
        if (submission == null || submission.getCurrentVersionId() == null) {
            return null;
        }
        return assignmentSubmissionVersionMapper.selectById(submission.getCurrentVersionId());
    }

    private void requireGroupAssignment(Integer courseId, Integer assignmentId, Integer userId, Assignment assignment) {
        if (!AssignmentAccessService.SUBMISSION_TYPE_GROUP.equals(assignment.getSubmissionType())) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.BAD_REQUEST,
                    "This endpoint is only for Group assignments");
        }
    }

    private CourseGroup requireGroupInRoster(Integer courseId, Assignment assignment, Integer groupId, Integer userId) {
        if (assignment.getGroupSetId() == null || groupId == null) {
            throw AssignmentErrors.fail(log, courseId, assignment.getId(), userId, ErrorType.NOT_IN_GRADING_ROSTER,
                    "Group " + groupId + " is not in the grading roster");
        }
        return groupAccessService.requireGroupInSet(courseId, assignment.getGroupSetId(), groupId);
    }

    private AssignmentGrade requireGroupGrade(Integer courseId, Integer assignmentId, Integer groupId, Integer userId) {
        AssignmentGrade grade = assignmentGradeMapper.selectByAssignmentIdAndGroupId(assignmentId, groupId);
        if (grade == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.GRADE_NOT_FOUND, null);
        }
        return grade;
    }

    private void requireGroupSelection(Integer courseId, Integer assignmentId, Integer userId, List<Integer> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.PARAM_MISSING,
                    "groupIds is required");
        }
    }

    private void requireInRoster(Integer courseId, Integer assignmentId, Integer studentUserId, Integer userId) {
        Enrollment enrollment = enrollmentMapper.selectByCourseIdAndUserId(courseId, studentUserId);
        boolean inRoster = enrollment != null
                && Boolean.TRUE.equals(enrollment.getActive())
                && CoursePermissionService.ROLE_STUDENT.equals(enrollment.getCourseRole());
        if (!inRoster) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.NOT_IN_GRADING_ROSTER,
                    "User " + studentUserId + " is not an active Student in this course");
        }
    }

    private void requireSelection(Integer courseId, Integer assignmentId, Integer userId, List<Integer> studentUserIds) {
        if (studentUserIds == null || studentUserIds.isEmpty()) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.PARAM_MISSING,
                    "studentUserIds is required");
        }
    }

    private AssignmentGrade requireGrade(Integer courseId, Integer assignmentId, Integer studentUserId, Integer userId) {
        AssignmentGrade grade = assignmentGradeMapper.selectByAssignmentIdAndStudentUserId(assignmentId, studentUserId);
        if (grade == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.GRADE_NOT_FOUND, null);
        }
        return grade;
    }

    private Assignment requireAssignment(Integer courseId, Integer assignmentId, Integer userId) {
        Assignment assignment = assignmentMapper.selectByCourseIdAndId(courseId, assignmentId);
        if (assignment == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.ASSIGNMENT_NOT_FOUND, null);
        }
        return assignment;
    }

    /**
     * Students must not learn that a Draft assignment exists, even via the annotated-file route.
     */
    private Assignment requireAssignmentForReader(Integer courseId, Integer assignmentId, Integer userId,
                                                  boolean studentReader) {
        Assignment assignment = requireAssignment(courseId, assignmentId, userId);
        if (studentReader && !AssignmentAccessService.STATE_PUBLISHED.equals(assignment.getState())) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.ASSIGNMENT_NOT_FOUND, null);
        }
        return assignment;
    }

    private List<AssignmentGrade> grades(Integer assignmentId) {
        List<AssignmentGrade> grades = assignmentGradeMapper.selectByAssignmentId(assignmentId);
        if (grades == null) {
            throw AssignmentErrors.fail(log, null, assignmentId, null, ErrorType.INTERNAL_ERROR,
                    "Grade query returned null");
        }
        return grades;
    }

    private Map<Integer, AssignmentGrade> gradesByStudent(Integer assignmentId) {
        Map<Integer, AssignmentGrade> byStudent = new LinkedHashMap<>();
        for (AssignmentGrade grade : grades(assignmentId)) {
            if (grade.getStudentUserId() != null) {
                byStudent.put(grade.getStudentUserId(), grade);
            }
        }
        return byStudent;
    }

    private Map<Integer, AssignmentGrade> gradesByGroup(Integer assignmentId) {
        Map<Integer, AssignmentGrade> byGroup = new LinkedHashMap<>();
        for (AssignmentGrade grade : grades(assignmentId)) {
            if (grade.getGroupId() != null) {
                byGroup.put(grade.getGroupId(), grade);
            }
        }
        return byGroup;
    }

    private List<Enrollment> activeStudents(Integer courseId) {
        List<Enrollment> students = enrollmentMapper.selectActiveStudentsByCourseId(courseId);
        if (students == null) {
            throw AssignmentErrors.fail(log, courseId, null, null, ErrorType.INTERNAL_ERROR,
                    "Active student roster query returned null");
        }
        return students;
    }

    private void populateRosterDeadlines(GradingRosterResponse response, Assignment assignment, ZoneId zone) {
        response.setDueAtUtc(assignmentTimeSupport.toInstant(assignment.getDueAt()));
        response.setLateUntilUtc(assignmentTimeSupport.toInstant(assignment.getLateUntil()));
        response.setDueAtLocal(assignmentTimeSupport.toZone(assignment.getDueAt(), zone));
        response.setLateUntilLocal(assignmentTimeSupport.toZone(assignment.getLateUntil(), zone));
        response.setTimezone(zone.getId());
    }

    private List<Integer> groupMemberUserIds(Integer groupId) {
        List<Integer> result = new ArrayList<>();
        if (groupId == null) {
            return result;
        }
        List<GroupMembership> memberships = groupMembershipMapper.selectByGroupId(groupId);
        if (memberships == null) {
            return result;
        }
        for (GroupMembership membership : memberships) {
            if (membership != null && membership.getUserId() != null) {
                result.add(membership.getUserId());
            }
        }
        return result;
    }

    /**
     * Student-visible grade fields: score and feedback. Used to gate post-release correction audits.
     */
    static boolean visibleGradeFieldsChanged(BigDecimal oldScore, String oldFeedback,
                                             BigDecimal newScore, String newFeedback) {
        return !scoresEqual(oldScore, newScore) || !Objects.equals(oldFeedback, newFeedback);
    }

    static boolean scoresEqual(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
