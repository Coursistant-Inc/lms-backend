package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.dto.GradeResponse;
import com.coursistant.lms.module.assignment.dto.GradeTransitionResponse;
import com.coursistant.lms.module.assignment.dto.GradeTransitionSkip;
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
import com.coursistant.lms.module.assignment.repository.AssignmentGradeMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionFileMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionVersionMapper;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
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

    // ---------------------------------------------------------------- roster

    /**
     * Roster of active Students with their submission state and grade state. TAs and the
     * Instructor are never listed: they are graders, not gradees.
     */
    public GradingRosterResponse getRoster(Integer courseId, Integer assignmentId, Integer userId) {
        Course course = assignmentAccessService.requireCourse(courseId);
        assignmentAccessService.requireCanGrade(courseId, userId);
        Assignment assignment = requireAssignment(courseId, assignmentId, userId);

        LocalDateTime now = assignmentTimeSupport.nowUtc();
        List<Enrollment> students = activeStudents(courseId);
        Map<Integer, AssignmentGrade> gradesByStudent = gradesByStudent(assignmentId);

        GradingRosterResponse response = new GradingRosterResponse();
        response.setAssignmentId(assignmentId);
        response.setAssignmentTitle(assignment.getTitle());
        response.setPointsPossible(assignment.getPointsPossible());
        response.setDueAt(assignment.getDueAt());
        response.setLateUntil(assignment.getLateUntil());
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

    public GradingViewResponse getGradingView(Integer courseId, Integer assignmentId, Integer studentUserId,
                                              Integer userId, String timezoneHeader) {
        Course course = assignmentAccessService.requireCourse(courseId);
        assignmentAccessService.requireCanGrade(courseId, userId);
        Assignment assignment = requireAssignment(courseId, assignmentId, userId);
        requireInRoster(courseId, assignmentId, studentUserId, userId);

        ZoneId zone = assignmentTimeSupport.zoneOrUtc(timezoneHeader);
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

        boolean correctedAfterRelease = existing != null && GRADE_RELEASED.equals(existing.getStatus());
        if (correctedAfterRelease) {
            assignmentAuditService.write(courseId, assignmentId, userId,
                    AssignmentAuditService.GRADE_CORRECTED_AFTER_RELEASE,
                    Map.of("studentUserId", studentUserId, "score", score));
            Assignment assignmentForNotify = assignment;
            Integer studentForNotify = studentUserId;
            assignmentNotificationService.afterCommit(
                    () -> assignmentNotificationService.notifyGradeCorrectedAfterRelease(
                            assignmentForNotify, studentForNotify));
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

    // ------------------------------------------------- release / retract

    @Transactional
    public GradeTransitionResponse releaseAll(Integer courseId, Integer assignmentId, Integer userId) {
        assignmentAccessService.requireReleaseWritable(courseId, userId);
        Assignment assignment = requireAssignment(courseId, assignmentId, userId);

        List<Integer> studentUserIds = new ArrayList<>();
        for (AssignmentGrade grade : grades(assignmentId)) {
            if (GRADE_ENTERED.equals(grade.getStatus())) {
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
            List<Integer> notified = new ArrayList<>(response.getChangedStudentUserIds());
            assignmentAuditService.write(courseId, assignmentId, userId, AssignmentAuditService.GRADES_RELEASED,
                    Map.of("studentUserIds", notified));
            assignmentNotificationService.afterCommit(
                    () -> assignmentNotificationService.notifyGradesReleased(assignment, notified));
        }
        return response;
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
            item.setSubmittedAt(version.getSubmittedAt());
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
            item.setReleasedAt(grade.getReleasedAt());
            item.setHasAnnotatedFile(grade.getAnnotatedObjectKey() != null);
        }
        return item;
    }

    private GradeResponse toGradeResponse(Assignment assignment, AssignmentGrade grade) {
        GradeResponse response = new GradeResponse();
        response.setId(grade.getId());
        response.setAssignmentId(grade.getAssignmentId());
        response.setStudentUserId(grade.getStudentUserId());
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
            response.setAnnotatedFileUrl(assignmentResponseAssembler.absoluteUrl(
                    "/v2/courses/" + assignment.getCourseId() + "/assignments/" + assignment.getId()
                            + "/grades/" + grade.getStudentUserId() + "/annotated-file"));
        }
        response.setEnteredBy(grade.getEnteredBy());
        response.setEnteredAt(grade.getEnteredAt());
        response.setEditedBy(grade.getEditedBy());
        response.setUpdatedAt(grade.getUpdatedAt());
        response.setReleasedAt(grade.getReleasedAt());
        response.setAiAssisted(grade.getAiAssisted());
        return response;
    }

    private AssignmentSubmissionVersion currentVersionOf(Integer assignmentId, Integer studentUserId) {
        AssignmentSubmission submission = assignmentSubmissionMapper
                .selectByAssignmentIdAndOwnerUserId(assignmentId, studentUserId);
        if (submission == null || submission.getCurrentVersionId() == null) {
            return null;
        }
        return assignmentSubmissionVersionMapper.selectById(submission.getCurrentVersionId());
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
            byStudent.put(grade.getStudentUserId(), grade);
        }
        return byStudent;
    }

    private List<Enrollment> activeStudents(Integer courseId) {
        List<Enrollment> students = enrollmentMapper.selectActiveStudentsByCourseId(courseId);
        if (students == null) {
            throw AssignmentErrors.fail(log, courseId, null, null, ErrorType.INTERNAL_ERROR,
                    "Active student roster query returned null");
        }
        return students;
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
