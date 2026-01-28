package com.coursistant.lms.v2.service;

import com.coursistant.lms.v2.common.EntityType;
import com.coursistant.lms.v2.dto.AssignmentAttachmentUploadRequest;
import com.coursistant.lms.v2.dto.AssignmentForEditResponse;
import com.coursistant.lms.v2.dto.LocalFileUploadDTO;
import com.coursistant.lms.v2.dto.UpdateAssignmentDetailRequest;
import com.coursistant.lms.v2.entity.AssignmentEntity;
import com.coursistant.lms.v2.entity.QAssignmentEntity;
import com.coursistant.lms.v2.repository.AssignmentRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssignmentV2Service {
    private final JPAQueryFactory queryFactory;
    private final FileV2Service fileService;
    private final AssignmentRepository assignmentRepository;

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
                        assignment.settings
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
    public void updateAssignmentDetail(Long assignmentId, UpdateAssignmentDetailRequest update) {
        if (!update.hasUpdates()) return;

        var clause = queryFactory.update(assignment);

        if (update.title() != null) clause.set(assignment.title, update.title());
        if (update.description() != null) clause.set(assignment.description, update.description());
        if (update.type() != null) clause.set(assignment.type, update.type());
        if (update.dueTime() != null) clause.set(assignment.dueTime, update.dueTime());
        if (update.settings() != null) {
            var newSettings = new AssignmentEntity.AssignmentSettings(
                    update.settings().allowLateSubmission(),
                    update.settings().allowedResubmissionCount()
            );
            clause.set(assignment.settings, newSettings);
        }

        clause.where(assignment.id.eq(assignmentId)).execute();
    }

    @Transactional
    public Long uploadAttachment(AssignmentAttachmentUploadRequest request, Integer userId) {
        var uploadDto = LocalFileUploadDTO.builder()
                .file(request.getFile())
                .entityType(EntityType.ASSIGNMENT)
                .entityId(request.getAssignmentId())
                .userId(userId)
                .directory(String.format("assignment_%d/", request.getAssignmentId()))
                .build();
        var file = fileService.uploadAndLink(uploadDto);
        return file.getId();
    }

    @Transactional
    public void deleteAttachment(Long fileId) {

    }

    private static final QAssignmentEntity assignment = QAssignmentEntity.assignmentEntity;
}
