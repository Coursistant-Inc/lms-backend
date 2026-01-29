package com.coursistant.lms.v2.service;

import com.coursistant.lms.v2.common.EntityType;
import com.coursistant.lms.v2.dto.*;
import com.coursistant.lms.v2.entity.*;
import com.coursistant.lms.v2.repository.AssignmentRepository;
import com.coursistant.lms.v2.repository.ReviewRepository;
import com.coursistant.lms.v2.repository.SubmissionRepository;
import com.coursistant.lms.v2.repository.UserRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssignmentV2Service {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final JPAQueryFactory queryFactory;
    private final FileV2Service fileService;

    @Transactional(readOnly = true)
    public AssignmentForEditResponse getAssignmentForEdit(Long assignmentId) {
        var result = queryFactory
                .select(Projections.constructor(
                        AssignmentForEditResponse.class,
                        assignment.id,
                        assignment.createdAt,
                        assignment.updatedAt,
                        assignment.title,
                        assignment.description,
                        assignment.type,
                        assignment.dueTime,
                        assignment.settings,
                        null
                ))
                .from(assignment)
                .where(assignment.id.eq(assignmentId))
                .fetchOne();
        if (result == null) throw new RuntimeException();

        var attachments = fileService.getFileReferencesByEntity(EntityType.ASSIGNMENT, assignmentId);
        result.setAttachments(attachments);

        return result;
    }

    @Transactional
    public void editAssignment(Long assignmentId, EditAssignmentRequest request) {
        if (!request.hasUpdates()) return;

        var clause = queryFactory.update(assignment);

        if (request.title() != null) clause.set(assignment.title, request.title());
        if (request.description() != null) clause.set(assignment.description, request.description());
        if (request.type() != null) clause.set(assignment.type, request.type());
        if (request.dueTime() != null) clause.set(assignment.dueTime, request.dueTime());
        if (request.settings() != null) {
            var newSettings = new AssignmentEntity.AssignmentSettings(
                    request.settings().allowLateSubmission(),
                    request.settings().allowedResubmissionCount()
            );
            clause.set(assignment.settings, newSettings);
        }

        clause.where(assignment.id.eq(assignmentId)).execute();
    }

    @Transactional
    public Long uploadAttachment(Long assignmentId, MultipartFile attachment, Integer userId) {
        var uploadDto = LocalFileUploadDTO.builder()
                .file(attachment)
                .entityType(EntityType.ASSIGNMENT)
                .entityId(assignmentId)
                .userId(userId)
                .directory(String.format("assignment_%d/", assignmentId))
                .build();
        var file = fileService.uploadAndLink(uploadDto);
        return file.getId();
    }

    @Transactional
    public void deleteAttachment(Long fileId) {
        // TODO: No validation
        fileService.deleteFile(fileId);
    }

    @Transactional(readOnly = true)
    public AssignmentForSubmissionResponse getAssignmentForSubmission(Long assignmentId, Integer userId) {
        var result = queryFactory
                .select(
                        assignment.id,
                        assignment.createdAt,
                        assignment.updatedAt,
                        assignment.title,
                        assignment.description,
                        assignment.type,
                        assignment.dueTime,
                        assignment.settings,

                        submission.id,
                        submission.createdAt,
                        submission.updatedAt,
                        submission.submissionCount,
                        submission.submissionContent
                )
                .from(assignment)
                .leftJoin(submission).on(
                        submission.assignment.id.eq(assignment.id)
                                .and(submission.student.id.eq(userId))
                )
                .where(assignment.id.eq(assignmentId))
                .fetchOne();

        if (result == null) throw new EntityNotFoundException();

        var attachments = fileService.getFileReferencesByEntity(EntityType.ASSIGNMENT, assignmentId);
        var assignmentDTO = new AssignmentForEditResponse(
                result.get(assignment.id),
                result.get(assignment.createdAt),
                result.get(assignment.updatedAt),
                result.get(assignment.title),
                result.get(assignment.description),
                result.get(assignment.type),
                result.get(assignment.dueTime),
                result.get(assignment.settings),
                attachments
        );

        SubmissionResponse submissionDTO = null;
        var submissionId = result.get(submission.id);
        if (submissionId != null) {
            var files = fileService.getFileReferencesByEntity(EntityType.SUBMISSION, submissionId);
            submissionDTO = new SubmissionResponse(
                    submissionId,
                    result.get(submission.createdAt),
                    result.get(submission.updatedAt),
                    result.get(submission.submissionCount),
                    result.get(submission.submissionContent),
                    files
            );
        }

        return new AssignmentForSubmissionResponse(assignmentDTO, submissionDTO);
    }

    @Transactional
    public Long createSubmission(Long assignmentId, Integer userId,
                                 AssignmentSubmissionRequest request) {
        var result = queryFactory
                .select(
                        assignment.dueTime,
                        assignment.settings,

                        submission.id
                )
                .from(assignment)
                .leftJoin(submission).on(
                        submission.assignment.id.eq(assignment.id)
                                .and(submission.student.id.eq(userId))
                )
                .where(assignment.id.eq(assignmentId))
                .fetchOne();
        if (result == null) throw new EntityNotFoundException();

        var settings = result.get(assignment.settings);
        var dueTime = result.get(assignment.dueTime);
        if (settings == null || dueTime == null) throw new EntityNotFoundException();

        if (result.get(submission.id) != null) {
            throw new RuntimeException("Submission already exists, use resubmit instead");
        }
        if (!settings.getAllowLateSubmission() && Instant.now().isAfter(dueTime)) {
            throw new RuntimeException("Submission overdue");
        }

        var userRef = userRepository.getReferenceById(userId);
        var assignmentRef = assignmentRepository.getReferenceById(assignmentId);
        var submission = SubmissionEntity.builder()
                .submissionCount(1)
                .submissionContent(request.submissionContent())
                .student(userRef)
                .assignment(assignmentRef)
                .build();

        return submissionRepository.save(submission).getId();
    }

    @Transactional
    public Boolean resubmitSubmission(Long assignmentId, Long submissionId, Integer userId,
                                      AssignmentSubmissionRequest request) {
        var result = queryFactory
                .select(
                        assignment.dueTime,
                        assignment.settings,

                        submission.submissionCount,
                        submission.submissionContent
                )
                .from(assignment)
                .leftJoin(submission).on(
                        submission.assignment.id.eq(assignment.id)
                                .and(submission.student.id.eq(userId))
                )
                .where(assignment.id.eq(assignmentId))
                .fetchOne();
        if (result == null) throw new EntityNotFoundException();

        var dueTime = result.get(assignment.dueTime);
        var settings = result.get(assignment.settings);
        var submissionCount = result.get(submission.submissionCount);
        if (dueTime == null || settings == null || submissionCount == null) throw new EntityNotFoundException();

        if ((!settings.getAllowLateSubmission() && Instant.now().isAfter(dueTime))
                || submissionCount >= settings.getAllowedResubmissionCount()) {
            return false;
        }

        queryFactory.update(submission)
                .set(submission.submissionCount, submissionCount + 1)
                .set(submission.submissionContent, request.submissionContent())
                .where(submission.id.eq(submissionId))
                .execute();

        return true;
    }

    @Transactional
    public Long uploadSubmissionFile(Long submissionId, MultipartFile file, Integer userId) {
        var uploadDto = LocalFileUploadDTO.builder()
                .file(file)
                .entityType(EntityType.SUBMISSION)
                .entityId(submissionId)
                .userId(userId)
                .directory(String.format("submission_%d/", submissionId))
                .build();
        var fileResult = fileService.uploadAndLink(uploadDto);
        return fileResult.getId();
    }

    @Transactional
    public void deleteSubmissionFile(Long fileId) {
        // TODO: No validation
        fileService.deleteFile(fileId);
    }

    @Transactional(readOnly = true)
    public AssignmentForReviewResponse getAssignmentForReview(Long assignmentId) {
        var assignmentPart = queryFactory
                .select(
                        assignment.createdAt,
                        assignment.updatedAt,
                        assignment.title,
                        assignment.description,
                        assignment.type,
                        assignment.dueTime,
                        assignment.settings
                )
                .from(assignment)
                .where(assignment.id.eq(assignmentId))
                .fetchOne();
        if (assignmentPart == null) throw new EntityNotFoundException();

        var result = queryFactory
                .select(
                        submission.id,
                        submission.createdAt,
                        submission.updatedAt,
                        submission.submissionCount,
                        submission.submissionContent,
                        review.id,
                        review.createdAt,
                        review.updatedAt,
                        review.grade,
                        review.teacherComment
                )
                .from(submission)
                .leftJoin(review).on(review.submission.id.eq(submission.id))
                .where(submission.assignment.id.eq(assignmentId))
                .fetch();

        var submissionIds = new ArrayList<Long>();
        var submissions = new HashMap<Long, AssignmentForReviewResponse.Submission>();
        var reviews = new HashMap<Long, AssignmentForReviewResponse.Review>();

        for (var entry : result) {
            var submissionId = entry.get(submission.id);
            if (submissionId == null) throw new EntityNotFoundException();
            submissionIds.add(submissionId);
            submissions.put(submissionId, AssignmentForReviewResponse.Submission.builder()
                    .createdAt(entry.get(submission.createdAt))
                    .updatedAt(entry.get(submission.updatedAt))
                    .submissionCount(entry.get(submission.submissionCount))
                    .submissionContent(entry.get(submission.submissionContent))
                    .build());

            var reviewId = entry.get(review.id);
            if (reviewId == null) continue;
            reviews.put(reviewId, AssignmentForReviewResponse.Review.builder()
                    .submissionId(submissionId)
                    .createdAt(entry.get(review.createdAt))
                    .updatedAt(entry.get(review.updatedAt))
                    .grade(entry.get(review.grade))
                    .teacherComment(entry.get(review.teacherComment))
                    .build());
        }

        var submissionType = EntityType.SUBMISSION.getCode();
        var filesQuery = queryFactory.select(
                        file.id,
                        file.createdAt,
                        file.updatedAt,
                        file.fileName,
                        file.fileSize,
                        file.mimeType,
                        file.filePath,
                        file.entityId
                )
                .from(file)
                .where(file.entityType.eq(submissionType)
                        .and(file.entityId.in(submissionIds)))
                .fetch();

        var files = new HashMap<Long, FlatFile>();
        for (var entry : filesQuery) {
            var fileId = entry.get(file.id);
            if (fileId == null) throw new EntityNotFoundException();

            files.put(fileId, FlatFile.builder()
                    .createdAt(entry.get(file.createdAt))
                    .updatedAt(entry.get(file.updatedAt))
                    .parentEntityType(EntityType.SUBMISSION.getCode())
                    .parentEntityId(entry.get(file.entityId))
                    .fileName(entry.get(file.fileName))
                    .fileSize(entry.get(file.fileSize))
                    .mimeType(entry.get(file.mimeType))
                    .filePath(entry.get(file.filePath))
                    .build());
        }

        return new AssignmentForReviewResponse(AssignmentForReviewResponse.Assignment.builder()
                .createdAt(assignmentPart.get(assignment.createdAt))
                .updatedAt(assignmentPart.get(assignment.updatedAt))
                .title(assignmentPart.get(assignment.title))
                .description(assignmentPart.get(assignment.description))
                .type(assignmentPart.get(assignment.type))
                .dueTime(assignmentPart.get(assignment.dueTime))
                .settings(assignmentPart.get(assignment.settings))
                .build(),
                submissions, reviews, files);
    }

    @Transactional
    public Long createSubmissionReview(Long submissionId, CreateSubmissionReviewRequest request) {
        // TODO: Validation (ESSENTIAL)
        var submissionRef = submissionRepository.getReferenceById(submissionId);
        var newReview = ReviewEntity.builder()
                .grade(request.getGrade())
                .teacherComment(request.getTeacherComment())
                .submission(submissionRef)
                .build();

        reviewRepository.save(newReview);
        return newReview.getId();
    }

    @Transactional
    public void updateSubmissionReview(Long submissionId, Map<Long, UpdateSubmissionReviewRequest> requests) {
        for (var request : requests.entrySet()) {
            var reviewId = request.getKey();
            var update = request.getValue();

            if (!update.hasUpdate()) return;
            var clause = queryFactory.update(review);

            if (update.grade() != null) clause.set(review.grade, update.grade());
            if (update.teacherComment() != null) clause.set(review.teacherComment, update.teacherComment());

            clause.where(review.id.eq(reviewId)).execute();
        }
    }

    private static final QAssignmentEntity assignment = QAssignmentEntity.assignmentEntity;
    private static final QSubmissionEntity submission = QSubmissionEntity.submissionEntity;
    private static final QFileReferenceEntity file = QFileReferenceEntity.fileReferenceEntity;
    private static final QReviewEntity review = QReviewEntity.reviewEntity;
}
