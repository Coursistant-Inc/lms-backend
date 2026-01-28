package com.coursistant.lms.v2.service;

import com.coursistant.lms.v2.common.EntityRepositoryMapping;
import com.coursistant.lms.v2.common.EntityType;
import com.coursistant.lms.v2.dto.FileResponse;
import com.coursistant.lms.v2.dto.LocalFileUploadDTO;
import com.coursistant.lms.v2.entity.*;
import com.coursistant.lms.v2.repository.AssignmentRepository;
import com.coursistant.lms.v2.repository.FileReferenceRepository;
import com.coursistant.lms.v2.repository.UserRepository;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileV2Service {
    private final JPAQueryFactory queryFactory;
    private final FileStorageService storageService;
    private final FileReferenceRepository fileReferenceRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;

    private Map<String, EntityRepositoryMapping<?>> repositoryMapping;

    @PostConstruct
    public void init() {
        repositoryMapping = Map.of(
                EntityType.ASSIGNMENT.getCode(),
                new EntityRepositoryMapping<>(
                        assignmentRepository,
                        QAssignmentEntity.assignmentEntity,
                        q -> ((QAssignmentEntity) q).id
                )
        );
    }

    @Transactional
    public FileReferenceEntity uploadAndLink(LocalFileUploadDTO dto) {
        var storageResult = storageService.upload(dto.getFile(), "v2/" + dto.getDirectory());

        validateEntityExists(dto.getEntityType(), dto.getEntityId());

        var userRef = userRepository.getReferenceById(dto.getUserId());

        var fileRef = FileReferenceEntity.builder()
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .fileName(dto.getFile().getOriginalFilename())
                .fileSize(dto.getFile().getSize())
                .mimeType(dto.getFile().getContentType())
                .filePath(storageResult.getFilePath())
                .entityType(dto.getEntityType())
                .entityId(dto.getEntityId())
                .uploadUser(userRef)
                .build();

        return fileReferenceRepository.save(fileRef);
    }

    private void validateEntityExists(String entityType, Long entityId) {
        var mapping = repositoryMapping.get(entityType);
        if (mapping == null) {
            throw new IllegalArgumentException("Unknown entity type: " + entityType);
        }

        var exists = queryFactory.selectOne()
                .from(mapping.getQEntity())
                .where(mapping.getIdPath().eq(entityId))
                .limit(1)
                .fetchFirst() != null;

        if (!exists) {
            throw new EntityNotFoundException(
                    String.format("Entity %s with ID %s not found", entityType, entityId));
        }
    }

    @Transactional(readOnly = true)
    public Page<FileResponse> getFileReferencesByEntityPageable(
            String entityType, Long entityId, Pageable pageable) {
        var query = queryFactory.select(
                        Projections.constructor(FileResponse.class,
                                file.id,
                                file.createdAt,
                                file.updatedAt,
                                file.fileName,
                                file.fileSize,
                                file.mimeType,
                                file.filePath
                        ))
                .from(file)
                .where(file.entityType.eq(entityType)
                        .and(file.entityId.eq(entityId)));

        long total = query.fetch().size();

        var content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(pageable.getSort().stream()
                        .map(order -> switch (order.getProperty()) {
                            case "fileName" -> order.isAscending() ?
                                    file.fileName.asc() : file.fileName.desc();
                            case "fileSize" -> order.isAscending() ?
                                    file.fileSize.asc() : file.fileSize.desc();
                            default -> order.isAscending() ?
                                    file.createdAt.asc() : file.createdAt.desc();
                        })
                        .toArray(OrderSpecifier[]::new))
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    private static final QFileReferenceEntity file = QFileReferenceEntity.fileReferenceEntity;
}