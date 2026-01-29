package com.coursistant.lms.v2.service;

import com.coursistant.lms.v2.common.EntityType;
import com.coursistant.lms.v2.dto.*;
import com.coursistant.lms.v2.entity.*;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssignmentV2Service {
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

    private static final QAssignmentEntity assignment = QAssignmentEntity.assignmentEntity;
    private static final QSubmissionEntity submission = QSubmissionEntity.submissionEntity;
    private static final QUserEntity user = QUserEntity.userEntity;
    private static final QReviewEntity review = QReviewEntity.reviewEntity;
}
