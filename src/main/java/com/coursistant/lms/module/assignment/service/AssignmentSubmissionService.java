package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.dto.GroupMemberSummary;
import com.coursistant.lms.module.assignment.dto.StagingFileResponse;
import com.coursistant.lms.module.assignment.dto.SubmissionResponse;
import com.coursistant.lms.module.assignment.dto.SubmissionVersionResponse;
import com.coursistant.lms.module.assignment.dto.SubmitAssignmentRequest;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmission;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionFile;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionReceipt;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionStagingFile;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionVersion;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionFileMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionReceiptMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionStagingFileMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionVersionMapper;
import com.coursistant.lms.module.course.group.entity.CourseGroup;
import com.coursistant.lms.module.course.group.entity.GroupMembership;
import com.coursistant.lms.module.course.group.repository.CourseGroupMapper;
import com.coursistant.lms.module.course.group.repository.GroupMembershipMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.module.tenant.service.TenantTimezoneService;
import com.coursistant.lms.module.file.storage.S3UploadRollback;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Student-facing submission flow.
 *
 * <p>Uploading is not submitting. Files first land in {@code assignment_submission_staging_file}
 * with a {@code created_at} / {@code expires_at} pair; only POST .../submissions turns them into
 * an immutable submission version with a receipt. The staging {@code created_at} is also what
 * makes the grace buffer decidable: a student who had staged a file before the deadline may
 * still press Submit for a few minutes afterwards without being marked late.</p>
 */
@Service
public class AssignmentSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentSubmissionService.class);

    /** How long an unsubmitted upload stays usable. */
    private static final int STAGING_TTL_HOURS = 24;

    @Resource
    private AssignmentMapper assignmentMapper;

    @Resource
    private AssignmentSubmissionMapper assignmentSubmissionMapper;

    @Resource
    private AssignmentSubmissionVersionMapper assignmentSubmissionVersionMapper;

    @Resource
    private AssignmentSubmissionFileMapper assignmentSubmissionFileMapper;

    @Resource
    private AssignmentSubmissionStagingFileMapper assignmentSubmissionStagingFileMapper;

    @Resource
    private AssignmentSubmissionReceiptMapper assignmentSubmissionReceiptMapper;

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
    private AssignmentResponseAssembler assignmentResponseAssembler;

    @Resource
    private AssignmentAuditService assignmentAuditService;

    @Resource
    private AssignmentNotificationService assignmentNotificationService;

    @Resource
    private GroupMembershipMapper groupMembershipMapper;

    @Resource
    private CourseGroupMapper courseGroupMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TenantTimezoneService tenantTimezoneService;

    @Resource
    private S3UploadRollback s3UploadRollback;

    // --------------------------------------------------------------- staging

    /**
     * Stores one or more files for a later Submit. Nothing here counts as a submission.
     */
    @Transactional
    public List<StagingFileResponse> uploadStagingFiles(Integer courseId, Integer assignmentId, Integer userId,
                                                        MultipartFile[] files) {
        assignmentAccessService.requireStudentSubmitContext(courseId, userId);
        Assignment assignment = requirePublishedAssignment(courseId, assignmentId, userId);
        if (files == null || files.length == 0) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.PARAM_MISSING,
                    "At least one file is required");
        }

        LocalDateTime now = assignmentTimeSupport.nowUtc();
        List<AssignmentSubmissionStagingFile> active = activeStagingFiles(assignmentId, userId, now);
        if (!submissionStatusCalculator.acceptSubmit(assignment.getDueAt(), assignment.getLateUntil(), now,
                createdAts(active))) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.SUBMISSION_WINDOW_CLOSED,
                    "The submission window for this assignment is closed");
        }
        if (active.size() + files.length > assignment.getMaxFileCount()) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.SUBMISSION_FILE_COUNT_EXCEEDED,
                    "At most " + assignment.getMaxFileCount() + " file(s) may be staged for this assignment");
        }

        List<String> allowedTypes = assignmentFilePolicy.parseAllowedTypes(assignment.getAllowedFileTypes());
        record PreparedStaging(MultipartFile file, String canonicalMime, String objectKey, String checksum) {
        }
        List<PreparedStaging> prepared = new ArrayList<>();
        for (MultipartFile file : files) {
            String canonicalMime = assignmentFilePolicy.validateSubmissionFile(
                    file, allowedTypes, assignment.getMaxFileSizeBytes());
            prepared.add(new PreparedStaging(
                    file,
                    canonicalMime,
                    assignmentFilePolicy.stagingKey(courseId, assignmentId, userId, file.getOriginalFilename()),
                    assignmentFilePolicy.checksumSha256(file)));
        }

        S3UploadRollback.Scope rollback = s3UploadRollback.open(courseId, null);
        List<StagingFileResponse> created = new ArrayList<>();
        try {
            for (PreparedStaging item : prepared) {
                assignmentStorageService.upload(item.objectKey(), item.file(), item.canonicalMime(),
                        courseId, assignmentId, userId);
                rollback.remember(assignmentFilePolicy.bucket(), item.objectKey());

                AssignmentSubmissionStagingFile staging = new AssignmentSubmissionStagingFile();
                staging.setAssignmentId(assignmentId);
                staging.setOwnerUserId(userId);
                staging.setObjectKey(item.objectKey());
                staging.setOriginalName(assignmentFilePolicy.sanitizeFilename(item.file().getOriginalFilename()));
                staging.setContentType(item.canonicalMime());
                staging.setSizeBytes(item.file().getSize());
                staging.setChecksumSha256(item.checksum());
                staging.setConsumed(false);
                staging.setCreatedAt(now);
                staging.setExpiresAt(now.plusHours(STAGING_TTL_HOURS));
                assignmentSubmissionStagingFileMapper.insert(staging);

                created.add(submissionResponseAssembler.toStagingResponse(staging));
            }
            return created;
        } catch (RuntimeException e) {
            rollback.abortIfNoTransaction();
            throw e;
        }
    }

    public List<StagingFileResponse> listStagingFiles(Integer courseId, Integer assignmentId, Integer userId) {
        assignmentAccessService.requireActiveMember(courseId, userId);
        requirePublishedAssignment(courseId, assignmentId, userId);
        LocalDateTime now = assignmentTimeSupport.nowUtc();
        return submissionResponseAssembler.toStagingResponses(activeStagingFiles(assignmentId, userId, now));
    }

    @Transactional
    public void deleteStagingFile(Integer courseId, Integer assignmentId, Integer stagingFileId, Integer userId) {
        assignmentAccessService.requireStudentSubmitContext(courseId, userId);
        requirePublishedAssignment(courseId, assignmentId, userId);

        AssignmentSubmissionStagingFile staging = assignmentSubmissionStagingFileMapper.selectById(stagingFileId);
        if (staging == null
                || !assignmentId.equals(staging.getAssignmentId())
                || !userId.equals(staging.getOwnerUserId())
                || Boolean.TRUE.equals(staging.getConsumed())) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.STAGING_FILE_INVALID,
                    "Staging file " + stagingFileId + " is not an active upload of this user");
        }
        assignmentSubmissionStagingFileMapper.deleteById(stagingFileId);
        assignmentStorageService.deleteQuietly(staging.getObjectKey());
    }

    // ---------------------------------------------------------------- submit

    /**
     * Consumes staged files into a new immutable version and issues a receipt.
     *
     * <p>Whether the attempt is accepted, and whether it consumes the grace buffer, is decided
     * from the {@code created_at} of the files actually being submitted — so selecting only files
     * staged after the deadline cannot borrow an earlier upload's grace eligibility.</p>
     */
    @Transactional
    public SubmissionResponse submit(Integer courseId, Integer assignmentId, Integer userId,
                                     SubmitAssignmentRequest body) {
        assignmentAccessService.requireStudentSubmitContext(courseId, userId);
        Assignment assignment = requirePublishedAssignment(courseId, assignmentId, userId);
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(courseId);
        LocalDateTime now = assignmentTimeSupport.nowUtc();

        List<AssignmentSubmissionStagingFile> active = activeStagingFiles(assignmentId, userId, now);
        List<AssignmentSubmissionStagingFile> selected = selectStagingFiles(courseId, assignmentId, userId, active,
                body == null ? null : body.getStagingFileIds());
        if (selected.isEmpty()) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.STAGING_FILE_INVALID,
                    "No active staged files to submit");
        }
        if (selected.size() > assignment.getMaxFileCount()) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.SUBMISSION_FILE_COUNT_EXCEEDED,
                    "At most " + assignment.getMaxFileCount() + " file(s) may be submitted for this assignment");
        }

        List<LocalDateTime> selectedCreatedAts = createdAts(selected);
        if (!submissionStatusCalculator.acceptSubmit(assignment.getDueAt(), assignment.getLateUntil(), now,
                selectedCreatedAts)) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.SUBMISSION_WINDOW_CLOSED,
                    "The submission window for this assignment is closed");
        }
        boolean usedGraceBuffer = submissionStatusCalculator.consumesGraceBuffer(assignment.getDueAt(),
                assignment.getLateUntil(), now, selectedCreatedAts);

        boolean groupAssignment = AssignmentAccessService.SUBMISSION_TYPE_GROUP.equals(assignment.getSubmissionType());
        Integer groupId = null;
        if (groupAssignment) {
            groupId = requireMembershipGroupId(courseId, assignment, userId);
        }

        AssignmentSubmission submission = groupAssignment
                ? assignmentSubmissionMapper.selectByAssignmentIdAndGroupId(assignmentId, groupId)
                : assignmentSubmissionMapper.selectByAssignmentIdAndOwnerUserId(assignmentId, userId);
        if (submission == null) {
            submission = new AssignmentSubmission();
            submission.setAssignmentId(assignmentId);
            if (groupAssignment) {
                submission.setGroupId(groupId);
                submission.setOwnerUserId(null);
            } else {
                submission.setOwnerUserId(userId);
                submission.setGroupId(null);
            }
            submission.setCurrentVersionId(null);
            assignmentSubmissionMapper.insert(submission);
        }

        Integer maxVersionNo = assignmentSubmissionVersionMapper.selectMaxVersionNo(submission.getId());
        int nextVersionNo = (maxVersionNo == null ? 0 : maxVersionNo) + 1;

        AssignmentSubmissionVersion version = new AssignmentSubmissionVersion();
        version.setSubmissionId(submission.getId());
        version.setAssignmentId(assignmentId);
        // Individual: owner = submitter. Group: owner_user_id stores the acting submitter for audit
        // trail compatibility; group ownership lives on assignment_submission.group_id.
        version.setOwnerUserId(userId);
        version.setActualSubmitterUserId(userId);
        version.setVersionNo(nextVersionNo);
        version.setSubmittedAt(now);
        version.setUsedGraceBuffer(usedGraceBuffer);
        assignmentSubmissionVersionMapper.insert(version);

        int sortOrder = 0;
        for (AssignmentSubmissionStagingFile staging : selected) {
            AssignmentSubmissionFile file = new AssignmentSubmissionFile();
            file.setSubmissionVersionId(version.getId());
            file.setObjectKey(staging.getObjectKey());
            file.setOriginalName(staging.getOriginalName());
            file.setContentType(staging.getContentType());
            file.setSizeBytes(staging.getSizeBytes());
            file.setChecksumSha256(staging.getChecksumSha256());
            file.setSortOrder(sortOrder++);
            assignmentSubmissionFileMapper.insert(file);
            // The row is kept (not deleted) so the object it points at is never reaped.
            assignmentSubmissionStagingFileMapper.updateConsumed(staging.getId(), true);
        }

        assignmentSubmissionMapper.updateCurrentVersionId(submission.getId(), version.getId());

        AssignmentSubmissionReceipt receipt = new AssignmentSubmissionReceipt();
        receipt.setSubmissionVersionId(version.getId());
        receipt.setIssuedAt(now);
        assignmentSubmissionReceiptMapper.insert(receipt);

        Map<String, Object> auditDetail = new LinkedHashMap<>();
        auditDetail.put("submissionId", submission.getId());
        auditDetail.put("submissionVersionId", version.getId());
        auditDetail.put("versionNo", nextVersionNo);
        auditDetail.put("fileCount", selected.size());
        auditDetail.put("usedGraceBuffer", usedGraceBuffer);
        assignmentAuditService.write(courseId, assignmentId, userId, AssignmentAuditService.SUBMISSION_CREATED, auditDetail);

        List<Integer> receiptRecipients = new ArrayList<>();
        if (groupAssignment) {
            List<GroupMembership> members = groupMembershipMapper.selectByGroupId(groupId);
            if (members != null) {
                for (GroupMembership member : members) {
                    if (member != null && member.getUserId() != null) {
                        receiptRecipients.add(member.getUserId());
                    }
                }
            }
            if (!receiptRecipients.contains(userId)) {
                receiptRecipients.add(userId);
            }
        } else {
            receiptRecipients.add(userId);
        }
        assignmentNotificationService.recordSubmissionReceived(
                assignment, receiptRecipients, submission.getId(), nextVersionNo, version.getId(), now);

        return buildSubmissionResponse(assignment, userId, zone, assignmentTimeSupport.nowUtc());
    }

    // ------------------------------------------------------------------ read

    public SubmissionResponse getMySubmission(HttpServletRequest request, Integer courseId, Integer assignmentId,
                                              Integer userId) {
        Assignment assignment = assignmentAccessService.requireAssignmentReadable(request, courseId, assignmentId, userId);
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(courseId);
        return buildSubmissionResponse(assignment, userId, zone, assignmentTimeSupport.nowUtc());
    }

    public List<SubmissionVersionResponse> listMyVersions(HttpServletRequest request, Integer courseId,
                                                          Integer assignmentId, Integer userId) {
        Assignment assignment = assignmentAccessService.requireAssignmentReadable(request, courseId, assignmentId, userId);
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(courseId);
        if (AssignmentAccessService.SUBMISSION_TYPE_GROUP.equals(assignment.getSubmissionType())) {
            if (assignment.getGroupSetId() == null) {
                return List.of();
            }
            GroupMembership membership =
                    groupMembershipMapper.selectByGroupSetIdAndUserId(assignment.getGroupSetId(), userId);
            if (membership == null) {
                return List.of();
            }
            return versionHistoryForGroup(assignment, membership.getGroupId(), zone);
        }
        return versionHistory(assignment, userId, zone);
    }

    /**
     * Staff (or owner) version history for a specific submission head id.
     */
    public List<SubmissionVersionResponse> listVersions(HttpServletRequest request, Integer courseId,
                                                        Integer assignmentId, Integer submissionId, Integer userId) {
        Assignment assignment = assignmentAccessService.requireAssignmentReadable(request, courseId, assignmentId, userId);
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(courseId);
        AssignmentSubmission submission = assignmentSubmissionMapper.selectById(submissionId);
        if (submission == null || !assignmentId.equals(submission.getAssignmentId())) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.NOT_FOUND,
                    "Submission " + submissionId + " does not belong to this assignment");
        }
        boolean owner = userId.equals(submission.getOwnerUserId())
                || (submission.getGroupId() != null && isGroupMember(submission.getGroupId(), userId));
        if (!owner && !assignmentAccessService.isStaffViewer(request, courseId, userId)) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.ACCESS_DENIED,
                    "Only the submitter or course staff may read submission versions");
        }
        return versionHistoryForSubmission(assignment, submission, zone);
    }

    /**
     * Version history for one student; also used by the grading view.
     */
    public List<SubmissionVersionResponse> versionHistory(Assignment assignment, Integer ownerUserId, ZoneId zone) {
        AssignmentSubmission submission = assignmentSubmissionMapper
                .selectByAssignmentIdAndOwnerUserId(assignment.getId(), ownerUserId);
        return versionHistoryForSubmission(assignment, submission, zone);
    }

    public List<SubmissionVersionResponse> versionHistoryForGroup(Assignment assignment, Integer groupId, ZoneId zone) {
        AssignmentSubmission submission = assignmentSubmissionMapper
                .selectByAssignmentIdAndGroupId(assignment.getId(), groupId);
        return versionHistoryForSubmission(assignment, submission, zone);
    }

    private List<SubmissionVersionResponse> versionHistoryForSubmission(Assignment assignment,
                                                                        AssignmentSubmission submission,
                                                                        ZoneId zone) {
        List<SubmissionVersionResponse> result = new ArrayList<>();
        if (submission == null) {
            return result;
        }
        List<AssignmentSubmissionVersion> versions =
                assignmentSubmissionVersionMapper.selectBySubmissionIdOrderByVersionDesc(submission.getId());
        if (versions == null) {
            throw AssignmentErrors.fail(log, assignment.getCourseId(), assignment.getId(), null,
                    ErrorType.INTERNAL_ERROR, "Submission version history query returned null");
        }
        for (AssignmentSubmissionVersion version : versions) {
            result.add(submissionResponseAssembler.toVersionResponse(assignment, version, zone, true));
        }
        return result;
    }

    public ResponseEntity<InputStreamResource> streamSubmissionFile(HttpServletRequest request, Integer courseId,
                                                                    Integer assignmentId, Integer submissionId,
                                                                    Integer fileId, Integer userId, boolean attachment) {
        assignmentAccessService.requireAssignmentReadable(request, courseId, assignmentId, userId);

        AssignmentSubmission submission = assignmentSubmissionMapper.selectById(submissionId);
        if (submission == null || !assignmentId.equals(submission.getAssignmentId())) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.NOT_FOUND,
                    "Submission " + submissionId + " does not belong to this assignment");
        }
        boolean owner = userId.equals(submission.getOwnerUserId())
                || (submission.getGroupId() != null && isGroupMember(submission.getGroupId(), userId));
        if (!owner && !assignmentAccessService.canGrade(courseId, userId)) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.ACCESS_DENIED,
                    "Only the submitter or grading staff may read submission files");
        }

        AssignmentSubmissionFile file = assignmentSubmissionFileMapper.selectById(fileId);
        if (file == null) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.NOT_FOUND,
                    "Submission file " + fileId + " does not exist");
        }
        AssignmentSubmissionVersion version = assignmentSubmissionVersionMapper.selectById(file.getSubmissionVersionId());
        if (version == null || !submissionId.equals(version.getSubmissionId())) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.NOT_FOUND,
                    "Submission file " + fileId + " does not belong to this submission");
        }
        if (!attachment && !assignmentFilePolicy.isPreviewable(file.getContentType(), file.getOriginalName())) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.UNSUPPORTED_FILE_TYPE,
                    "Preview is only available for PDF and image files; use download instead");
        }
        return assignmentStorageService.stream(file.getObjectKey(), file.getOriginalName(), file.getContentType(),
                attachment, courseId, assignmentId, userId);
    }

    // ------------------------------------------------------------- internals

    private SubmissionResponse buildSubmissionResponse(Assignment assignment, Integer viewerUserId, ZoneId zone,
                                                       LocalDateTime now) {
        boolean groupAssignment = AssignmentAccessService.SUBMISSION_TYPE_GROUP.equals(assignment.getSubmissionType());
        AssignmentSubmission submission;
        Integer groupId = null;
        CourseGroup group = null;
        if (groupAssignment) {
            GroupMembership membership = assignment.getGroupSetId() == null ? null
                    : groupMembershipMapper.selectByGroupSetIdAndUserId(assignment.getGroupSetId(), viewerUserId);
            if (membership == null) {
                SubmissionResponse ineligible = baseSubmissionShell(assignment, viewerUserId, zone, now, List.of());
                ineligible.setSubmissionEligibility("NO_GROUP_MEMBERSHIP");
                ineligible.setSubmissionStatus(submissionStatusCalculator.calculate(assignment.getDueAt(),
                        assignment.getLateUntil(), now, null, null, List.of()));
                ineligible.setAcceptingSubmissions(false);
                ineligible.setGraceWindowActive(false);
                ineligible.setTotalVersions(0);
                return ineligible;
            }
            groupId = membership.getGroupId();
            group = courseGroupMapper.selectById(groupId);
            submission = assignmentSubmissionMapper.selectByAssignmentIdAndGroupId(assignment.getId(), groupId);
        } else {
            submission = assignmentSubmissionMapper
                    .selectByAssignmentIdAndOwnerUserId(assignment.getId(), viewerUserId);
        }

        AssignmentSubmissionVersion currentVersion = null;
        if (submission != null && submission.getCurrentVersionId() != null) {
            currentVersion = assignmentSubmissionVersionMapper.selectById(submission.getCurrentVersionId());
        }
        List<AssignmentSubmissionStagingFile> active = activeStagingFiles(assignment.getId(), viewerUserId, now);
        // Group status must not fork on personal staging; Individual status still uses it.
        List<LocalDateTime> statusStagingAts = groupAssignment ? List.of() : createdAts(active);
        List<LocalDateTime> acceptStagingAts = createdAts(active);

        SubmissionResponse response = baseSubmissionShell(assignment, viewerUserId, zone, now, active);
        if (groupAssignment) {
            response.setOwnerUserId(null);
            response.setGroupId(groupId);
            response.setGroupName(group == null ? null : group.getName());
            response.setMembers(memberSummaries(groupId));
            if (currentVersion != null) {
                response.setActualSubmitterUserId(currentVersion.getActualSubmitterUserId());
            }
        } else {
            response.setOwnerUserId(viewerUserId);
        }
        response.setSubmissionId(submission == null ? null : submission.getId());

        response.setSubmissionStatus(submissionStatusCalculator.calculate(assignment.getDueAt(),
                assignment.getLateUntil(), now,
                currentVersion == null ? null : currentVersion.getSubmittedAt(),
                currentVersion == null ? null : currentVersion.getUsedGraceBuffer(),
                statusStagingAts));
        response.setGraceWindowActive(submissionStatusCalculator.isGraceEligible(assignment.getDueAt(),
                assignment.getLateUntil(), now, acceptStagingAts));
        boolean frozen = assignmentAccessService.isSubmitFrozen(assignment.getCourseId(), viewerUserId);
        response.setSubmitFrozen(frozen);
        response.setAcceptingSubmissions(!frozen && submissionStatusCalculator.acceptSubmit(assignment.getDueAt(),
                assignment.getLateUntil(), now, acceptStagingAts));

        if (submission != null) {
            Integer maxVersionNo = assignmentSubmissionVersionMapper.selectMaxVersionNo(submission.getId());
            response.setTotalVersions(maxVersionNo == null ? 0 : maxVersionNo);
        } else {
            response.setTotalVersions(0);
        }
        response.setCurrentVersion(submissionResponseAssembler.toVersionResponse(assignment, currentVersion, zone, true));
        if (currentVersion != null) {
            response.setUsedGraceBuffer(currentVersion.getUsedGraceBuffer());
            AssignmentSubmissionReceipt receiptEntity =
                    assignmentSubmissionReceiptMapper.selectBySubmissionVersionId(currentVersion.getId());
            List<AssignmentSubmissionFile> files =
                    assignmentSubmissionFileMapper.selectBySubmissionVersionId(currentVersion.getId());
            response.setReceipt(assignmentResponseAssembler.toReceiptSummary(receiptEntity, files));
            if (Boolean.TRUE.equals(currentVersion.getUsedGraceBuffer())
                    || !currentVersion.getSubmittedAt().isAfter(assignment.getDueAt())) {
                response.setDeadlineOutcome("ON_TIME");
            } else {
                response.setDeadlineOutcome("LATE");
            }
        }
        return response;
    }

    private SubmissionResponse baseSubmissionShell(Assignment assignment, Integer viewerUserId, ZoneId zone,
                                                   LocalDateTime now, List<AssignmentSubmissionStagingFile> active) {
        SubmissionResponse response = new SubmissionResponse();
        response.setAssignmentId(assignment.getId());
        response.setOwnerUserId(viewerUserId);
        response.setDueAtUtc(assignmentTimeSupport.toInstant(assignment.getDueAt()));
        response.setLateUntilUtc(assignmentTimeSupport.toInstant(assignment.getLateUntil()));
        response.setTimezone(zone == null ? null : zone.getId());
        response.setDueAtLocal(assignmentTimeSupport.toZone(assignment.getDueAt(), zone));
        response.setLateUntilLocal(assignmentTimeSupport.toZone(assignment.getLateUntil(), zone));
        response.setMaxFileCount(assignment.getMaxFileCount());
        response.setMaxFileSizeBytes(assignment.getMaxFileSizeBytes());
        response.setAllowedFileTypes(assignmentFilePolicy.parseAllowedTypes(assignment.getAllowedFileTypes()));
        response.setStagingFiles(submissionResponseAssembler.toStagingResponses(active));
        response.setWindowOpen(submissionStatusCalculator.isWindowOpen(assignment.getDueAt(),
                assignment.getLateUntil(), now));
        return response;
    }

    private Integer requireMembershipGroupId(Integer courseId, Assignment assignment, Integer userId) {
        if (assignment.getGroupSetId() == null) {
            throw AssignmentErrors.fail(log, courseId, assignment.getId(), userId,
                    ErrorType.ASSIGNMENT_GROUP_SET_REQUIRED, "Group assignment is missing a linked group set");
        }
        GroupMembership membership =
                groupMembershipMapper.selectByGroupSetIdAndUserId(assignment.getGroupSetId(), userId);
        if (membership == null) {
            throw AssignmentErrors.fail(log, courseId, assignment.getId(), userId, ErrorType.NO_GROUP_MEMBERSHIP,
                    "You must join or be assigned to a group before submitting.");
        }
        return membership.getGroupId();
    }

    private boolean isGroupMember(Integer groupId, Integer userId) {
        if (groupId == null || userId == null) {
            return false;
        }
        List<GroupMembership> members = groupMembershipMapper.selectByGroupId(groupId);
        if (members == null) {
            return false;
        }
        for (GroupMembership member : members) {
            if (userId.equals(member.getUserId())) {
                return true;
            }
        }
        return false;
    }

    private List<GroupMemberSummary> memberSummaries(Integer groupId) {
        List<GroupMemberSummary> result = new ArrayList<>();
        if (groupId == null) {
            return result;
        }
        List<GroupMembership> members = groupMembershipMapper.selectByGroupId(groupId);
        if (members == null) {
            return result;
        }
        for (GroupMembership membership : members) {
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

    private List<AssignmentSubmissionStagingFile> selectStagingFiles(Integer courseId, Integer assignmentId,
                                                                     Integer userId,
                                                                     List<AssignmentSubmissionStagingFile> active,
                                                                     List<Integer> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return active;
        }
        Map<Integer, AssignmentSubmissionStagingFile> byId = new LinkedHashMap<>();
        for (AssignmentSubmissionStagingFile staging : active) {
            byId.put(staging.getId(), staging);
        }
        List<AssignmentSubmissionStagingFile> selected = new ArrayList<>();
        for (Integer id : requestedIds) {
            AssignmentSubmissionStagingFile staging = byId.get(id);
            if (staging == null) {
                throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.STAGING_FILE_INVALID,
                        "Staging file " + id + " is missing, expired, or already consumed");
            }
            if (!selected.contains(staging)) {
                selected.add(staging);
            }
        }
        return selected;
    }

    /**
     * Creation times of the student's unexpired, unconsumed staging files — the input the status
     * calculator needs to decide grace-buffer eligibility.
     */
    public List<LocalDateTime> activeStagingCreatedAts(Integer assignmentId, Integer userId, LocalDateTime now) {
        return createdAts(activeStagingFiles(assignmentId, userId, now));
    }

    private List<AssignmentSubmissionStagingFile> activeStagingFiles(Integer assignmentId, Integer userId,
                                                                     LocalDateTime now) {
        List<AssignmentSubmissionStagingFile> rows = assignmentSubmissionStagingFileMapper
                .selectByAssignmentIdAndOwnerUserIdAndNotConsumed(assignmentId, userId);
        if (rows == null) {
            throw AssignmentErrors.fail(log, null, assignmentId, userId, ErrorType.INTERNAL_ERROR,
                    "Staging file query returned null");
        }
        List<AssignmentSubmissionStagingFile> active = new ArrayList<>();
        for (AssignmentSubmissionStagingFile row : rows) {
            if (row.getExpiresAt() == null || !now.isAfter(row.getExpiresAt())) {
                active.add(row);
            }
        }
        return active;
    }

    private List<LocalDateTime> createdAts(List<AssignmentSubmissionStagingFile> stagingFiles) {
        List<LocalDateTime> result = new ArrayList<>();
        for (AssignmentSubmissionStagingFile stagingFile : stagingFiles) {
            result.add(stagingFile.getCreatedAt());
        }
        return result;
    }

    /**
     * Students must never learn that a Draft assignment exists.
     */
    private Assignment requirePublishedAssignment(Integer courseId, Integer assignmentId, Integer userId) {
        Assignment assignment = assignmentMapper.selectByCourseIdAndId(courseId, assignmentId);
        if (assignment == null || !AssignmentAccessService.STATE_PUBLISHED.equals(assignment.getState())) {
            throw AssignmentErrors.fail(log, courseId, assignmentId, userId, ErrorType.ASSIGNMENT_NOT_FOUND, null);
        }
        return assignment;
    }
}
