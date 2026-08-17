package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.dto.AssignmentAttachmentResponse;
import com.coursistant.lms.module.assignment.dto.AssignmentResponse;
import com.coursistant.lms.module.assignment.dto.AssignmentSummaryResponse;
import com.coursistant.lms.module.assignment.dto.CreateAssignmentRequest;
import com.coursistant.lms.module.assignment.dto.DueDateChangePreviewRequest;
import com.coursistant.lms.module.assignment.dto.DueDateChangePreviewResponse;
import com.coursistant.lms.module.assignment.dto.PatchAssignmentRequest;
import com.coursistant.lms.module.assignment.dto.ReceiptSummaryResponse;
import com.coursistant.lms.module.assignment.dto.UpcomingAssignmentDeadlineResponse;
import com.coursistant.lms.module.assignment.dto.UpcomingAssignmentQueryRow;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.entity.AssignmentAttachment;
import com.coursistant.lms.module.assignment.entity.AssignmentGrade;
import com.coursistant.lms.module.assignment.entity.AssignmentRubricVersion;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmission;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionFile;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionReceipt;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionStagingFile;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionVersion;
import com.coursistant.lms.module.assignment.repository.AssignmentAttachmentMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentGradeMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentRubricVersionMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionFileMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionReceiptMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionStagingFileMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionVersionMapper;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.group.entity.CourseGroup;
import com.coursistant.lms.module.course.group.entity.GroupMembership;
import com.coursistant.lms.module.course.group.repository.CourseGroupMapper;
import com.coursistant.lms.module.course.group.repository.GroupMembershipMapper;
import com.coursistant.lms.module.course.group.service.GroupAccessService;
import com.coursistant.lms.module.interaction.notification.service.NotificationRecipientResolver;
import com.coursistant.lms.module.tenant.service.TenantTimezoneService;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Individual assignment lifecycle: create, read (role-shaped), patch, publish/unpublish, delete,
 * the due-date change preview, and instructor attachments.
 */
@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private static final String GRADE_STATUS_RELEASED = "Released";

    @Resource
    private AssignmentMapper assignmentMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private AssignmentAttachmentMapper assignmentAttachmentMapper;

    @Resource
    private AssignmentRubricVersionMapper assignmentRubricVersionMapper;

    @Resource
    private AssignmentSubmissionMapper assignmentSubmissionMapper;

    @Resource
    private AssignmentSubmissionVersionMapper assignmentSubmissionVersionMapper;

    @Resource
    private AssignmentSubmissionStagingFileMapper assignmentSubmissionStagingFileMapper;

    @Resource
    private AssignmentSubmissionReceiptMapper assignmentSubmissionReceiptMapper;

    @Resource
    private AssignmentSubmissionFileMapper assignmentSubmissionFileMapper;

    @Resource
    private AssignmentGradeMapper assignmentGradeMapper;

    @Resource
    private EnrollmentMapper enrollmentMapper;

    @Resource
    private AssignmentAccessService assignmentAccessService;

    @Resource
    private GroupAccessService groupAccessService;

    @Resource
    private GroupMembershipMapper groupMembershipMapper;

    @Resource
    private CourseGroupMapper courseGroupMapper;

    @Resource
    private AssignmentFilePolicy assignmentFilePolicy;

    @Resource
    private AssignmentStorageService assignmentStorageService;

    @Resource
    private AssignmentTimeSupport assignmentTimeSupport;

    @Resource
    private AssignmentResponseAssembler assignmentResponseAssembler;

    @Resource
    private SubmissionStatusCalculator submissionStatusCalculator;

    @Resource
    private AssignmentSubmissionService assignmentSubmissionService;

    @Resource
    private AssignmentAuditService assignmentAuditService;

    @Resource
    private AssignmentNotificationService assignmentNotificationService;

    @Resource
    private NotificationRecipientResolver notificationRecipientResolver;

    @Resource
    private TenantTimezoneService tenantTimezoneService;

    // ------------------------------------------------------------------ read

    /**
     * Lists assignments ordered by due date then id. Students see Published assignments only;
     * staff see drafts as well.
     */
    public List<AssignmentResponse> list(HttpServletRequest request, Integer courseId, Integer userId) {
        assignmentAccessService.requireCourse(courseId);
        boolean staffView = assignmentAccessService.isStaffViewer(request, courseId, userId);
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(courseId);

        List<Assignment> assignments = staffView
                ? assignmentMapper.selectByCourseId(courseId)
                : assignmentMapper.selectByCourseIdAndState(courseId, AssignmentAccessService.STATE_PUBLISHED);
        if (assignments == null) {
            throw AssignmentErrors.fail(log, courseId, null, userId, ErrorType.INTERNAL_ERROR,
                    "Assignment list query returned null");
        }
        assignments.sort(Comparator
                .comparing(Assignment::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Assignment::getId));

        LocalDateTime now = assignmentTimeSupport.nowUtc();
        int activeStudentCount = staffView ? activeStudents(courseId).size() : 0;
        boolean submitFrozen = !staffView && assignmentAccessService.isSubmitFrozen(courseId, userId);

        List<AssignmentResponse> result = new ArrayList<>();
        for (Assignment assignment : assignments) {
            result.add(staffView
                    ? buildStaffResponse(assignment, zone, activeStudentCount)
                    : buildStudentResponse(assignment, zone, userId, now, submitFrozen));
        }
        return result;
    }

    /**
     * Slim list for any active course member: title, due, type, and (for students) current
     * submission status. Students see Published only; staff also see Draft. Staff omit
     * {@code submissionStatus} (they are not gradees on the roster).
     */
    public List<AssignmentSummaryResponse> listSummaries(HttpServletRequest request, Integer courseId,
                                                         Integer userId) {
        assignmentAccessService.requireCourse(courseId);
        assignmentAccessService.requireActiveMember(courseId, userId);
        boolean staffView = assignmentAccessService.isStaffViewer(request, courseId, userId);
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(courseId);

        List<Assignment> assignments = staffView
                ? assignmentMapper.selectByCourseId(courseId)
                : assignmentMapper.selectByCourseIdAndState(courseId, AssignmentAccessService.STATE_PUBLISHED);
        if (assignments == null) {
            throw AssignmentErrors.fail(log, courseId, null, userId, ErrorType.INTERNAL_ERROR,
                    "Assignment summary list query returned null");
        }
        assignments.sort(Comparator
                .comparing(Assignment::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Assignment::getId));

        LocalDateTime now = assignmentTimeSupport.nowUtc();
        List<AssignmentSummaryResponse> result = new ArrayList<>();
        for (Assignment assignment : assignments) {
            AssignmentSummaryResponse item = new AssignmentSummaryResponse();
            item.setId(assignment.getId());
            item.setTitle(assignment.getTitle());
            item.setDueAtUtc(assignmentTimeSupport.toInstant(assignment.getDueAt()));
            item.setDueAtLocal(assignmentTimeSupport.toZone(assignment.getDueAt(), zone));
            item.setTimezone(zone.getId());
            item.setSubmissionType(assignment.getSubmissionType());
            if (!staffView) {
                AssignmentSubmissionVersion version = resolveCurrentVersion(assignment, userId);
                boolean groupAssignment = AssignmentAccessService.SUBMISSION_TYPE_GROUP.equals(
                        assignment.getSubmissionType());
                List<LocalDateTime> stagingCreatedAts = groupAssignment
                        ? List.of()
                        : assignmentSubmissionService.activeStagingCreatedAts(assignment.getId(), userId, now);
                item.setSubmissionStatus(submissionStatusCalculator.calculate(
                        assignment.getDueAt(),
                        assignment.getLateUntil(),
                        now,
                        version == null ? null : version.getSubmittedAt(),
                        version == null ? null : version.getUsedGraceBuffer(),
                        stagingCreatedAts));
            }
            result.add(item);
        }
        return result;
    }

    /**
     * Dashboard: Published assignments across the user's active enrollments with dueAt in
     * {@code [now, now+days]} (UTC, inclusive).
     */
    public List<UpcomingAssignmentDeadlineResponse> listUpcomingDeadlines(Integer userId, Integer days) {
        if (userId == null) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        ZoneId zone = tenantTimezoneService.requireZoneForUser(userId);
        int windowDays = normalizeDays(days, 14, 30);
        LocalDateTime now = assignmentTimeSupport.nowUtc();
        LocalDateTime toUtc = now.plusDays(windowDays);

        List<UpcomingAssignmentQueryRow> rows =
                assignmentMapper.selectPublishedUpcomingForUser(userId, now, toUtc);
        if (rows == null) {
            throw new ApiException(ErrorType.INTERNAL_ERROR, "Upcoming assignment query returned null");
        }

        Integer userTenantId = tenantTimezoneService.requireUserTenantId(userId);
        List<UpcomingAssignmentDeadlineResponse> result = new ArrayList<>();
        for (UpcomingAssignmentQueryRow row : rows) {
            if (dropIfCrossTenant(userId, userTenantId, row.getCourseId())) {
                continue;
            }
            Assignment assignment = new Assignment();
            assignment.setId(row.getId());
            assignment.setCourseId(row.getCourseId());
            assignment.setTitle(row.getTitle());
            assignment.setDueAt(row.getDueAt());
            assignment.setLateUntil(row.getLateUntil());
            assignment.setSubmissionType(row.getSubmissionType());
            assignment.setGroupSetId(row.getGroupSetId());

            AssignmentSubmissionVersion version = resolveCurrentVersion(assignment, userId);
            boolean groupAssignment = AssignmentAccessService.SUBMISSION_TYPE_GROUP.equals(
                    assignment.getSubmissionType());
            List<LocalDateTime> stagingCreatedAts = groupAssignment
                    ? List.of()
                    : assignmentSubmissionService.activeStagingCreatedAts(assignment.getId(), userId, now);

            UpcomingAssignmentDeadlineResponse item = new UpcomingAssignmentDeadlineResponse();
            item.setCourseId(row.getCourseId());
            item.setCourseCode(row.getCourseCode());
            item.setAssignmentId(row.getId());
            item.setTitle(row.getTitle());
            item.setDueAtUtc(assignmentTimeSupport.toInstant(row.getDueAt()));
            item.setDueAtLocal(assignmentTimeSupport.toZone(row.getDueAt(), zone));
            item.setTimezone(zone.getId());
            item.setSubmissionStatus(submissionStatusCalculator.calculate(
                    assignment.getDueAt(),
                    assignment.getLateUntil(),
                    now,
                    version == null ? null : version.getSubmittedAt(),
                    version == null ? null : version.getUsedGraceBuffer(),
                    stagingCreatedAts));
            result.add(item);
        }
        return result;
    }

    private boolean dropIfCrossTenant(Integer userId, Integer userTenantId, Integer courseId) {
        if (courseId == null) {
            return true;
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getTenantId() == null) {
            log.error("Upcoming deadlines dropped: missing course/tenant userId={} courseId={} userTenantId={}",
                    userId, courseId, userTenantId);
            return true;
        }
        if (!userTenantId.equals(course.getTenantId())) {
            log.error("Upcoming deadlines cross-tenant filtered userId={} courseId={} userTenantId={} courseTenantId={}",
                    userId, courseId, userTenantId, course.getTenantId());
            return true;
        }
        return false;
    }

    private int normalizeDays(Integer days, int defaultDays, int maxDays) {
        if (days == null || days < 1) {
            return defaultDays;
        }
        return Math.min(days, maxDays);
    }

    public AssignmentResponse detail(HttpServletRequest request, Integer courseId, Integer assignmentId,
                                     Integer userId) {
        Assignment assignment = assignmentAccessService.requireAssignmentReadable(request, courseId, assignmentId, userId);
        boolean staffView = assignmentAccessService.isStaffViewer(request, courseId, userId);
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(courseId);
        if (staffView) {
            return buildStaffResponse(assignment, zone, activeStudents(courseId).size());
        }
        return buildStudentResponse(assignment, zone, userId, assignmentTimeSupport.nowUtc(),
                assignmentAccessService.isSubmitFrozen(courseId, userId));
    }

    // ----------------------------------------------------------------- write

    /**
     * Creates a Draft assignment. Individual is the default; Group requires {@code groupSetId}.
     */
    @Transactional
    public AssignmentResponse create(Integer courseId, Integer userId, CreateAssignmentRequest body) {
        assignmentAccessService.requireCourseWritable(courseId, userId);
        if (body == null) {
            throw AssignmentErrors.fail(log, courseId, null, userId, ErrorType.PARAM_MISSING, "Request body is required");
        }
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(courseId);

        String title = requireText(courseId, null, userId, body.getTitle(), "title");
        BigDecimal points = requirePositivePoints(courseId, null, userId, body.getPointsPossible());
        if (body.getDueAt() == null) {
            throw AssignmentErrors.fail(log, courseId, null, userId, ErrorType.PARAM_MISSING, "dueAt is required");
        }
        assignmentFilePolicy.validateFileConstraints(body.getMaxFileCount(), body.getMaxFileSizeBytes(),
                body.getAllowedFileTypes());

        LocalDateTime dueAt = assignmentTimeSupport.toUtc(body.getDueAt(), zone);
        LocalDateTime lateUntil = assignmentTimeSupport.toUtc(body.getLateUntil(), zone);
        requireLateUntilAfterDue(courseId, null, userId, dueAt, lateUntil);

        String submissionType = normalizeSubmissionType(courseId, null, userId, body.getSubmissionType());
        Integer groupSetId = resolveGroupSetId(courseId, null, userId, submissionType, body.getGroupSetId());

        Assignment assignment = new Assignment();
        assignment.setCourseId(courseId);
        assignment.setTitle(title);
        assignment.setDescription(body.getDescription() == null ? "" : body.getDescription());
        assignment.setPointsPossible(points);
        assignment.setDueAt(dueAt);
        assignment.setLateUntil(lateUntil);
        assignment.setSubmissionType(submissionType);
        assignment.setGroupSetId(groupSetId);
        assignment.setAllowedFileTypes(assignmentFilePolicy.toAllowedTypesJson(body.getAllowedFileTypes()));
        assignment.setMaxFileSizeBytes(body.getMaxFileSizeBytes());
        assignment.setMaxFileCount(body.getMaxFileCount());
        assignment.setState(AssignmentAccessService.STATE_DRAFT);
        assignment.setCreatedBy(userId);
        assignmentMapper.insert(assignment);

        assignmentAuditService.write(courseId, assignment.getId(), userId, AssignmentAuditService.ASSIGNMENT_CREATED,
                Map.of("title", title, "state", AssignmentAccessService.STATE_DRAFT,
                        "submissionType", submissionType));

        return buildStaffResponse(requireAssignment(courseId, assignment.getId(), userId), zone,
                activeStudents(courseId).size());
    }

    @Transactional
    public AssignmentResponse patch(Integer courseId, Integer assignmentId, Integer userId,
                                    PatchAssignmentRequest body) {
        Assignment existing = assignmentAccessService.requireAssignmentConfigurable(courseId, assignmentId, userId);
        if (body == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.PARAM_MISSING,
                    "Request body is required");
        }
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(courseId);

        boolean typeOrSetTouched = body.getSubmissionType() != null || body.getGroupSetId() != null;
        if (typeOrSetTouched && assignmentMapper.countSubmissionVersionsByAssignmentId(assignmentId) >= 1) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.ASSIGNMENT_TYPE_LOCKED,
                    "submissionType and groupSetId cannot be changed after a submission version exists");
        }

        Assignment patch = new Assignment();
        patch.setId(assignmentId);
        Map<String, Object> auditDetail = new LinkedHashMap<>();
        Integer resolvedGroupSetId = null;
        boolean clearGroupSetId = false;

        if (typeOrSetTouched) {
            String nextType = body.getSubmissionType() != null
                    ? normalizeSubmissionType(courseId, assignmentId, userId, body.getSubmissionType())
                    : existing.getSubmissionType();
            Integer requestedSetId = body.getGroupSetId() != null ? body.getGroupSetId() : existing.getGroupSetId();
            if (AssignmentAccessService.SUBMISSION_TYPE_INDIVIDUAL.equals(nextType)) {
                resolvedGroupSetId = null;
                clearGroupSetId = true;
            } else {
                resolvedGroupSetId = resolveGroupSetId(courseId, assignmentId, userId, nextType, requestedSetId);
            }
            if (!nextType.equals(existing.getSubmissionType())) {
                patch.setSubmissionType(nextType);
                auditDetail.put("submissionTypeFrom", existing.getSubmissionType());
                auditDetail.put("submissionTypeTo", nextType);
            }
            if (!java.util.Objects.equals(resolvedGroupSetId, existing.getGroupSetId())) {
                auditDetail.put("groupSetIdFrom", existing.getGroupSetId());
                auditDetail.put("groupSetIdTo", resolvedGroupSetId);
            }
        }

        if (body.getTitle() != null) {
            patch.setTitle(requireText(courseId, assignmentId, userId, body.getTitle(), "title"));
            auditDetail.put("title", patch.getTitle());
        }
        if (body.getDescription() != null) {
            patch.setDescription(body.getDescription());
            auditDetail.put("descriptionChanged", true);
        }

        Integer gradedWithOldPointsCount = null;
        if (body.getPointsPossible() != null) {
            BigDecimal newPoints = requirePositivePoints(courseId, assignmentId, userId, body.getPointsPossible());
            if (newPoints.compareTo(existing.getPointsPossible()) != 0) {
                gradedWithOldPointsCount = assignmentMapper.countGradesByAssignmentId(assignmentId);
                patch.setPointsPossible(newPoints);
                auditDetail.put("pointsPossibleFrom", existing.getPointsPossible());
                auditDetail.put("pointsPossibleTo", newPoints);
            }
        }

        LocalDateTime newDueAt = body.getDueAt() == null ? null : assignmentTimeSupport.toUtc(body.getDueAt(), zone);
        boolean clearLateUntil = Boolean.TRUE.equals(body.getClearLateUntil());
        LocalDateTime newLateUntil = clearLateUntil
                ? null
                : (body.getLateUntil() == null ? null : assignmentTimeSupport.toUtc(body.getLateUntil(), zone));

        LocalDateTime effectiveDueAt = newDueAt != null ? newDueAt : existing.getDueAt();
        LocalDateTime effectiveLateUntil = clearLateUntil
                ? null
                : (newLateUntil != null ? newLateUntil : existing.getLateUntil());
        requireLateUntilAfterDue(courseId, assignmentId, userId, effectiveDueAt, effectiveLateUntil);

        boolean shortening = isShortening(existing, effectiveDueAt, effectiveLateUntil);
        if (shortening && !Boolean.TRUE.equals(body.getConfirmShortenDueDate())) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId,
                    ErrorType.ASSIGNMENT_DUE_SHORTEN_CONFIRM_REQUIRED,
                    "Set confirmShortenDueDate=true to move the deadline earlier");
        }
        if (newDueAt != null) {
            patch.setDueAt(newDueAt);
            auditDetail.put("dueAtFrom", existing.getDueAt());
            auditDetail.put("dueAtTo", newDueAt);
        }
        if (newLateUntil != null) {
            patch.setLateUntil(newLateUntil);
            auditDetail.put("lateUntilTo", newLateUntil);
        }

        if (body.getAllowedFileTypes() != null || body.getMaxFileSizeBytes() != null || body.getMaxFileCount() != null) {
            Integer maxFileCount = body.getMaxFileCount() != null ? body.getMaxFileCount() : existing.getMaxFileCount();
            Long maxFileSizeBytes = body.getMaxFileSizeBytes() != null
                    ? body.getMaxFileSizeBytes() : existing.getMaxFileSizeBytes();
            List<String> allowedTypes = body.getAllowedFileTypes() != null
                    ? body.getAllowedFileTypes()
                    : assignmentFilePolicy.parseAllowedTypes(existing.getAllowedFileTypes());
            assignmentFilePolicy.validateFileConstraints(maxFileCount, maxFileSizeBytes, allowedTypes);
            patch.setMaxFileCount(maxFileCount);
            patch.setMaxFileSizeBytes(maxFileSizeBytes);
            patch.setAllowedFileTypes(assignmentFilePolicy.toAllowedTypesJson(allowedTypes));
            auditDetail.put("fileConstraintsChanged", true);
        }

        assignmentMapper.updateById(patch);
        if (clearLateUntil) {
            assignmentMapper.updateLateUntil(assignmentId, null);
            auditDetail.put("lateUntilCleared", true);
        }
        if (typeOrSetTouched && (clearGroupSetId || resolvedGroupSetId != null
                || !java.util.Objects.equals(resolvedGroupSetId, existing.getGroupSetId()))) {
            assignmentMapper.updateGroupSetId(assignmentId, resolvedGroupSetId);
        }

        assignmentAuditService.write(courseId, assignmentId, userId, AssignmentAuditService.ASSIGNMENT_UPDATED, auditDetail);
        if (shortening) {
            assignmentAuditService.write(courseId, assignmentId, userId, AssignmentAuditService.DUE_DATE_SHORTENED,
                    Map.of("previousDueAt", String.valueOf(existing.getDueAt()),
                            "newDueAt", String.valueOf(effectiveDueAt)));
        }
        if (gradedWithOldPointsCount != null && gradedWithOldPointsCount > 0) {
            assignmentAuditService.write(courseId, assignmentId, userId,
                    AssignmentAuditService.POINTS_CHANGED_AFTER_GRADING,
                    Map.of("previousPointsPossible", String.valueOf(existing.getPointsPossible()),
                            "newPointsPossible", String.valueOf(body.getPointsPossible()),
                            "gradedWithOldPointsCount", gradedWithOldPointsCount));
        }

        Assignment updated = requireAssignment(courseId, assignmentId, userId);
        return buildStaffResponse(updated, zone, activeStudents(courseId).size());
    }

    @Transactional
    public AssignmentResponse publish(Integer courseId, Integer assignmentId, Integer userId) {
        Assignment assignment = assignmentAccessService.requireAssignmentConfigurable(courseId, assignmentId, userId);
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(courseId);
        if (AssignmentAccessService.STATE_PUBLISHED.equals(assignment.getState())) {
            return buildStaffResponse(assignment, zone, activeStudents(courseId).size());
        }

        if (AssignmentAccessService.SUBMISSION_TYPE_GROUP.equals(assignment.getSubmissionType())) {
            if (assignment.getGroupSetId() == null) {
                throw AssignmentErrors.fail(log, courseId, assignmentId, userId,
                        ErrorType.ASSIGNMENT_GROUP_SET_REQUIRED,
                        "Group assignment requires a linked group set before publish");
            }
            groupAccessService.requireGroupSetInCourse(courseId, assignment.getGroupSetId());
        }

        assignmentMapper.updateState(assignmentId, AssignmentAccessService.STATE_PUBLISHED);
        assignmentAuditService.write(courseId, assignmentId, userId, AssignmentAuditService.ASSIGNMENT_PUBLISHED, (String) null);

        Assignment updated = requireAssignment(courseId, assignmentId, userId);
        assignmentNotificationService.recordAssignmentPublished(updated);
        return buildStaffResponse(updated, zone, activeStudents(courseId).size());
    }

    @Transactional
    public AssignmentResponse unpublish(Integer courseId, Integer assignmentId, Integer userId) {
        Assignment assignment = assignmentAccessService.requireAssignmentConfigurable(courseId, assignmentId, userId);
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(courseId);
        requireNoSubmissions(courseId, assignmentId, userId, "unpublish");

        if (AssignmentAccessService.STATE_DRAFT.equals(assignment.getState())) {
            return buildStaffResponse(assignment, zone, activeStudents(courseId).size());
        }
        assignmentMapper.updateState(assignmentId, AssignmentAccessService.STATE_DRAFT);
        assignmentAuditService.write(courseId, assignmentId, userId, AssignmentAuditService.ASSIGNMENT_UNPUBLISHED, (String) null);
        return buildStaffResponse(requireAssignment(courseId, assignmentId, userId), zone, activeStudents(courseId).size());
    }

    @Transactional
    public void delete(Integer courseId, Integer assignmentId, Integer userId) {
        assignmentAccessService.requireAssignmentConfigurable(courseId, assignmentId, userId);
        requireNoSubmissions(courseId, assignmentId, userId, "delete");

        List<String> orphanKeys = new ArrayList<>();
        for (AssignmentAttachment attachment : requireList(
                assignmentAttachmentMapper.selectByAssignmentId(assignmentId),
                courseId, assignmentId, userId, "Attachment")) {
            orphanKeys.add(attachment.getObjectKey());
        }
        for (AssignmentRubricVersion rubric : requireList(
                assignmentRubricVersionMapper.selectByAssignmentIdOrderByVersionDesc(assignmentId),
                courseId, assignmentId, userId, "Rubric version")) {
            orphanKeys.add(rubric.getObjectKey());
        }
        for (AssignmentSubmissionStagingFile staging : requireList(
                assignmentSubmissionStagingFileMapper.selectByAssignmentId(assignmentId),
                courseId, assignmentId, userId, "Staging file")) {
            orphanKeys.add(staging.getObjectKey());
        }

        assignmentAuditService.write(courseId, assignmentId, userId, AssignmentAuditService.ASSIGNMENT_DELETED, (String) null);
        // The rubric pointer is a FK onto assignment_rubric_version, which cascades from assignment.
        assignmentMapper.updateCurrentRubricVersionId(assignmentId, null);
        assignmentMapper.deleteById(assignmentId);

        for (String key : orphanKeys) {
            assignmentStorageService.deleteQuietly(key);
        }
    }

    /**
     * Dry run for a due date change: reports how many students are affected and whether the
     * instructor must confirm. Nothing is written.
     */
    public DueDateChangePreviewResponse previewDueDateChange(Integer courseId, Integer assignmentId, Integer userId,
                                                             DueDateChangePreviewRequest body) {
        Assignment assignment = assignmentAccessService.requireAssignmentConfigurable(courseId, assignmentId, userId);
        if (body == null || (body.getDueAt() == null && body.getLateUntil() == null
                && !Boolean.TRUE.equals(body.getClearLateUntil()))) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.PARAM_MISSING,
                    "dueAt or lateUntil is required");
        }
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(courseId);

        LocalDateTime newDueAt = body.getDueAt() == null
                ? assignment.getDueAt() : assignmentTimeSupport.toUtc(body.getDueAt(), zone);
        LocalDateTime newLateUntil = Boolean.TRUE.equals(body.getClearLateUntil())
                ? null
                : (body.getLateUntil() == null ? assignment.getLateUntil()
                : assignmentTimeSupport.toUtc(body.getLateUntil(), zone));
        requireLateUntilAfterDue(courseId, assignmentId, userId, newDueAt, newLateUntil);

        List<Enrollment> students = activeStudents(courseId);
        int submitted = 0;
        int becomingLate = 0;
        for (Enrollment student : students) {
            AssignmentSubmissionVersion version = currentVersionOf(assignmentId, student.getUserId());
            if (version == null) {
                continue;
            }
            submitted++;
            boolean wasOnTime = !SubmissionStatusCalculator.SUBMITTED_LATE.equals(
                    submissionStatusCalculator.calculateForVersion(assignment.getDueAt(),
                            version.getSubmittedAt(), version.getUsedGraceBuffer()));
            boolean willBeLate = SubmissionStatusCalculator.SUBMITTED_LATE.equals(
                    submissionStatusCalculator.calculateForVersion(newDueAt,
                            version.getSubmittedAt(), version.getUsedGraceBuffer()));
            if (wasOnTime && willBeLate) {
                becomingLate++;
            }
        }

        DueDateChangePreviewResponse response = new DueDateChangePreviewResponse();
        response.setCurrentDueAt(assignment.getDueAt());
        response.setCurrentLateUntil(assignment.getLateUntil());
        response.setNewDueAt(newDueAt);
        response.setNewLateUntil(newLateUntil);
        boolean shortening = isShortening(assignment, newDueAt, newLateUntil);
        response.setShortening(shortening);
        response.setConfirmationRequired(shortening);
        response.setActiveStudentCount(students.size());
        response.setSubmittedCount(submitted);
        response.setNotSubmittedCount(students.size() - submitted);
        response.setSubmissionsBecomingLateCount(becomingLate);
        response.setGradedCount(assignmentMapper.countGradesByAssignmentId(assignmentId));
        return response;
    }

    // ----------------------------------------------------------- attachments

    @Transactional
    public List<AssignmentAttachmentResponse> uploadAttachments(Integer courseId, Integer assignmentId, Integer userId,
                                                                MultipartFile[] files) {
        assignmentAccessService.requireAssignmentConfigurable(courseId, assignmentId, userId);
        if (files == null || files.length == 0) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.PARAM_MISSING,
                    "At least one file is required");
        }

        List<AssignmentAttachmentResponse> created = new ArrayList<>();
        for (MultipartFile file : files) {
            assignmentFilePolicy.validateAttachmentFile(file);
            String objectKey = assignmentFilePolicy.attachmentKey(courseId, assignmentId, file.getOriginalFilename());
            assignmentStorageService.upload(objectKey, file, courseId, assignmentId, userId);

            AssignmentAttachment attachment = new AssignmentAttachment();
            attachment.setAssignmentId(assignmentId);
            attachment.setObjectKey(objectKey);
            attachment.setOriginalName(assignmentFilePolicy.sanitizeFilename(file.getOriginalFilename()));
            attachment.setContentType(file.getContentType());
            attachment.setSizeBytes(file.getSize());
            attachment.setUploadedBy(userId);
            assignmentAttachmentMapper.insert(attachment);

            assignmentAuditService.write(courseId, assignmentId, userId, AssignmentAuditService.ATTACHMENT_ADDED,
                    Map.of("attachmentId", attachment.getId(), "originalName", attachment.getOriginalName()));
            created.add(assignmentResponseAssembler.toAttachmentResponse(courseId,
                    assignmentAttachmentMapper.selectById(attachment.getId())));
        }
        return created;
    }

    public ResponseEntity<InputStreamResource> downloadAttachment(HttpServletRequest request, Integer courseId,
                                                                  Integer assignmentId, Integer attachmentId,
                                                                  Integer userId) {
        assignmentAccessService.requireAssignmentReadable(request, courseId, assignmentId, userId);
        AssignmentAttachment attachment = requireAttachment(courseId, assignmentId, attachmentId, userId);
        return assignmentStorageService.stream(attachment.getObjectKey(), attachment.getOriginalName(),
                attachment.getContentType(), true, courseId, assignmentId, userId);
    }

    @Transactional
    public void deleteAttachment(Integer courseId, Integer assignmentId, Integer attachmentId, Integer userId) {
        assignmentAccessService.requireAssignmentConfigurable(courseId, assignmentId, userId);
        AssignmentAttachment attachment = requireAttachment(courseId, assignmentId, attachmentId, userId);
        assignmentAttachmentMapper.deleteById(attachmentId);
        assignmentAuditService.write(courseId, assignmentId, userId, AssignmentAuditService.ATTACHMENT_DELETED,
                Map.of("attachmentId", attachmentId, "originalName", String.valueOf(attachment.getOriginalName())));
        assignmentStorageService.deleteQuietly(attachment.getObjectKey());
    }

    // ---------------------------------------------------------- shared reads

    public Assignment requireAssignment(Integer courseId, Integer assignmentId, Integer userId) {
        Assignment assignment = assignmentMapper.selectByCourseIdAndId(courseId, assignmentId);
        if (assignment == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.ASSIGNMENT_NOT_FOUND, null);
        }
        return assignment;
    }

    public List<Enrollment> activeStudents(Integer courseId) {
        List<Enrollment> students = enrollmentMapper.selectActiveStudentsByCourseId(courseId);
        if (students == null) {
            throw AssignmentErrors.fail(log, courseId, null, null, ErrorType.INTERNAL_ERROR,
                    "Active student roster query returned null");
        }
        return students;
    }

    public List<Integer> activeStudentIds(Integer courseId) {
        return activeStudents(courseId).stream().map(Enrollment::getUserId).collect(Collectors.toList());
    }

    // -------------------------------------------------------------- internals

    private AssignmentResponse buildStaffResponse(Assignment assignment, ZoneId zone, int activeStudentCount) {
        List<AssignmentGrade> grades = requireList(
                assignmentGradeMapper.selectByAssignmentId(assignment.getId()),
                assignment.getCourseId(), assignment.getId(), null, "Grade");
        int released = 0;
        for (AssignmentGrade grade : grades) {
            if (GRADE_STATUS_RELEASED.equals(grade.getStatus())) {
                released++;
            }
        }
        return assignmentResponseAssembler.toStaffResponse(
                assignment,
                zone,
                requireList(assignmentAttachmentMapper.selectByAssignmentId(assignment.getId()),
                        assignment.getCourseId(), assignment.getId(), null, "Attachment"),
                currentRubric(assignment),
                activeStudentCount,
                assignmentMapper.countSubmissionsByAssignmentId(assignment.getId()),
                grades.size(),
                released);
    }

    private AssignmentResponse buildStudentResponse(Assignment assignment, ZoneId zone, Integer userId,
                                                    LocalDateTime now, boolean submitFrozen) {
        boolean groupAssignment = AssignmentAccessService.SUBMISSION_TYPE_GROUP.equals(assignment.getSubmissionType());
        GroupMembership membership = null;
        CourseGroup group = null;
        if (groupAssignment && assignment.getGroupSetId() != null) {
            membership = groupMembershipMapper.selectByGroupSetIdAndUserId(assignment.getGroupSetId(), userId);
            if (membership != null) {
                group = courseGroupMapper.selectById(membership.getGroupId());
            }
        }

        AssignmentSubmissionVersion version = resolveCurrentVersion(assignment, userId);
        List<LocalDateTime> personalStaging =
                assignmentSubmissionService.activeStagingCreatedAts(assignment.getId(), userId, now);
        // Group status must not fork on personal staging; Individual still uses it.
        List<LocalDateTime> statusStagingAts = groupAssignment ? List.of() : personalStaging;

        String status = submissionStatusCalculator.calculate(assignment.getDueAt(), assignment.getLateUntil(), now,
                version == null ? null : version.getSubmittedAt(),
                version == null ? null : version.getUsedGraceBuffer(),
                statusStagingAts);
        boolean windowOpen = submissionStatusCalculator.isWindowOpen(assignment.getDueAt(), assignment.getLateUntil(), now);
        boolean noGroupMembership = groupAssignment && membership == null;
        boolean accepting = !noGroupMembership && !submitFrozen
                && submissionStatusCalculator.acceptSubmit(assignment.getDueAt(),
                assignment.getLateUntil(), now, personalStaging);

        AssignmentGrade grade = assignmentGradeMapper.selectByAssignmentIdAndStudentUserId(assignment.getId(), userId);
        ReceiptSummaryResponse receipt = null;
        if (version != null) {
            AssignmentSubmissionReceipt receiptEntity =
                    assignmentSubmissionReceiptMapper.selectBySubmissionVersionId(version.getId());
            List<AssignmentSubmissionFile> files =
                    assignmentSubmissionFileMapper.selectBySubmissionVersionId(version.getId());
            receipt = assignmentResponseAssembler.toReceiptSummary(receiptEntity, files);
        }

        AssignmentResponse response = assignmentResponseAssembler.toStudentResponse(
                assignment,
                zone,
                requireList(assignmentAttachmentMapper.selectByAssignmentId(assignment.getId()),
                        assignment.getCourseId(), assignment.getId(), userId, "Attachment"),
                currentRubric(assignment),
                status,
                version == null ? null : version.getSubmittedAt(),
                version == null ? null : version.getVersionNo(),
                version == null ? null : version.getUsedGraceBuffer(),
                windowOpen,
                accepting,
                personalStaging.size(),
                grade,
                receipt);
        if (groupAssignment) {
            if (membership != null) {
                response.setGroupId(membership.getGroupId());
                response.setGroupName(group == null ? null : group.getName());
            } else {
                response.setSubmissionEligibility("NO_GROUP_MEMBERSHIP");
                response.setAcceptingSubmissions(false);
            }
        }
        return response;
    }

    private AssignmentRubricVersion currentRubric(Assignment assignment) {
        if (assignment.getCurrentRubricVersionId() == null) {
            return null;
        }
        return assignmentRubricVersionMapper.selectById(assignment.getCurrentRubricVersionId());
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
     * Individual: owner submission. Group: the viewer's current membership group's submission.
     */
    private AssignmentSubmissionVersion resolveCurrentVersion(Assignment assignment, Integer userId) {
        if (assignment == null || userId == null) {
            return null;
        }
        if (!AssignmentAccessService.SUBMISSION_TYPE_GROUP.equals(assignment.getSubmissionType())) {
            return currentVersionOf(assignment.getId(), userId);
        }
        if (assignment.getGroupSetId() == null) {
            return null;
        }
        GroupMembership membership =
                groupMembershipMapper.selectByGroupSetIdAndUserId(assignment.getGroupSetId(), userId);
        if (membership == null) {
            return null;
        }
        AssignmentSubmission submission = assignmentSubmissionMapper
                .selectByAssignmentIdAndGroupId(assignment.getId(), membership.getGroupId());
        if (submission == null || submission.getCurrentVersionId() == null) {
            return null;
        }
        return assignmentSubmissionVersionMapper.selectById(submission.getCurrentVersionId());
    }

    private void requireNoSubmissions(Integer courseId, Integer assignmentId, Integer userId, String action) {
        if (assignmentMapper.countSubmissionVersionsByAssignmentId(assignmentId) > 0) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.ASSIGNMENT_HAS_SUBMISSIONS,
                    "Cannot " + action + " an assignment that already has submissions");
        }
    }

    private String normalizeSubmissionType(Integer courseId, Integer assignmentId, Integer userId, String raw) {
        if (raw == null || raw.isBlank()) {
            return AssignmentAccessService.SUBMISSION_TYPE_INDIVIDUAL;
        }
        if (AssignmentAccessService.SUBMISSION_TYPE_INDIVIDUAL.equals(raw)
                || AssignmentAccessService.SUBMISSION_TYPE_GROUP.equals(raw)) {
            return raw;
        }
        throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.BAD_REQUEST,
                "submissionType must be Individual or Group");
    }

    private Integer resolveGroupSetId(Integer courseId, Integer assignmentId, Integer userId,
                                      String submissionType, Integer groupSetId) {
        if (AssignmentAccessService.SUBMISSION_TYPE_INDIVIDUAL.equals(submissionType)) {
            if (groupSetId != null) {
                throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.BAD_REQUEST,
                        "Individual assignments cannot link a group set");
            }
            return null;
        }
        if (groupSetId == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.ASSIGNMENT_GROUP_SET_REQUIRED,
                    "Group assignment requires a linked group set");
        }
        groupAccessService.requireGroupSetInCourse(courseId, groupSetId);
        return groupSetId;
    }

    /**
     * A change is "shortening" when it moves the last accepted moment earlier.
     */
    private boolean isShortening(Assignment existing, LocalDateTime newDueAt, LocalDateTime newLateUntil) {
        LocalDateTime currentEnd = existing.getLateUntil() != null ? existing.getLateUntil() : existing.getDueAt();
        LocalDateTime newEnd = newLateUntil != null ? newLateUntil : newDueAt;
        if (currentEnd == null || newEnd == null) {
            return false;
        }
        if (newEnd.isBefore(currentEnd)) {
            return true;
        }
        return newDueAt != null && existing.getDueAt() != null && newDueAt.isBefore(existing.getDueAt());
    }

    private void requireLateUntilAfterDue(Integer courseId, Integer assignmentId, Integer userId,
                                          LocalDateTime dueAt, LocalDateTime lateUntil) {
        if (lateUntil != null && dueAt != null && lateUntil.isBefore(dueAt)) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.BAD_REQUEST,
                    "lateUntil must not be earlier than dueAt");
        }
    }

    private String requireText(Integer courseId, Integer assignmentId, Integer userId, String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.PARAM_MISSING,
                    field + " is required");
        }
        return value.trim();
    }

    private BigDecimal requirePositivePoints(Integer courseId, Integer assignmentId, Integer userId, BigDecimal points) {
        if (points == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.PARAM_MISSING,
                    "pointsPossible is required");
        }
        if (points.compareTo(BigDecimal.ZERO) <= 0) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.BAD_REQUEST,
                    "pointsPossible must be greater than 0");
        }
        return points;
    }

    private AssignmentAttachment requireAttachment(Integer courseId, Integer assignmentId, Integer attachmentId,
                                                   Integer userId) {
        AssignmentAttachment attachment = assignmentAttachmentMapper.selectById(attachmentId);
        if (attachment == null || !assignmentId.equals(attachment.getAssignmentId())) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.NOT_FOUND,
                    "Attachment " + attachmentId + " does not belong to this assignment");
        }
        return attachment;
    }

    /**
     * Mapper returning null is a load failure, not an empty collection (Tier-1 Load≠Empty).
     */
    private <T> List<T> requireList(List<T> values, Integer courseId, Integer assignmentId, Integer userId,
                                    String what) {
        if (values == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.INTERNAL_ERROR,
                    what + " query returned null");
        }
        return values;
    }
}
