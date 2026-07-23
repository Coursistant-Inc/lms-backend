package com.coursistant.lms.v2.service;

import com.coursistant.lms.v2.common.EntityRepositoryMapping;
import com.coursistant.lms.v2.common.EntityType;
import com.coursistant.lms.v2.dto.FileResponse;
import com.coursistant.lms.v2.dto.LocalFileUploadDTO;
import com.coursistant.lms.v2.entity.*;
import com.coursistant.lms.v2.repository.AssignmentRepository;
import com.coursistant.lms.v2.repository.FileReferenceRepository;
import com.coursistant.lms.v2.repository.SubmissionRepository;
import com.coursistant.lms.v2.repository.UserRepository;
import com.coursistant.lms.v2.repository.CourseRepository;
import com.coursistant.lms.v2.repository.CourseUnitRepository;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileV2Service {
    private final JPAQueryFactory queryFactory;
    private final FileStorageService storageService;
    private final FileReferenceRepository fileReferenceRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseRepository courseRepository;
    private final CourseUnitRepository courseUnitRepository;


    private Map<String, EntityRepositoryMapping<?>> repositoryMapping;

    @PostConstruct
    public void init() {
        repositoryMapping = Map.of(
                EntityType.ASSIGNMENT.getCode(),
                new EntityRepositoryMapping<>(
                        assignmentRepository,
                        QAssignmentEntity.assignmentEntity,
                        q -> ((QAssignmentEntity) q).id
                ),
                EntityType.SUBMISSION.getCode(),
                new EntityRepositoryMapping<>(
                        submissionRepository,
                        QSubmissionEntity.submissionEntity,
                        q -> ((QSubmissionEntity) q).id
                ),
                EntityType.COURSE.getCode(),
                new EntityRepositoryMapping<>(
                        courseRepository,
                        QCourseEntity.courseEntity,
                        q -> ((QCourseEntity) q).id
                ),
                EntityType.COURSEUNIT.getCode(),
                new EntityRepositoryMapping<>(
                        courseUnitRepository,
                        QCourseUnitEntity.courseUnitEntity,
                        q -> ((QCourseUnitEntity) q).id
                )
        );
    }

    @Transactional
    public FileReferenceEntity uploadAndLink(LocalFileUploadDTO dto) {

        var storageResult = storageService.upload(dto.getFile(), "v2/" + dto.getDirectory());

        validateEntityExists(dto.getEntityType(), dto.getEntityId());

        var userRef = userRepository.getReferenceById(dto.getUserId());

        var fileRef = FileReferenceEntity.builder()
                .fileName(dto.getFile().getOriginalFilename())
                .fileSize(dto.getFile().getSize())
                .mimeType(dto.getFile().getContentType())
                .filePath(storageResult.getFilePath())
                .entityType(dto.getEntityType().getCode())
                .entityId(dto.getEntityId())
                .uploadUser(userRef)
                .build();

        return fileReferenceRepository.save(fileRef);
    }

    private void validateEntityExists(EntityType entityType, Long entityId) {
        var mapping = repositoryMapping.get(entityType.getCode());
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
    public List<FileResponse> getFileReferencesByEntity(EntityType entityType, Long entityId) {
        var type = entityType.getCode();
        return queryFactory.select(
                        Projections.constructor(FileResponse.class,
                                file.id,
                                file.createdAt,
                                file.updatedAt,
                                file.entityId,
                                file.entityType,
                                file.fileName,
                                file.fileSize,
                                file.mimeType,
                                file.filePath
                        ))
                .from(file)
                .where(file.entityType.eq(type)
                        .and(file.entityId.eq(entityId)))
                .fetch();
    }

    @Transactional(readOnly = true)
    public Page<FileResponse> getFileReferencesByEntityPageable(
            EntityType entityType, Long entityId, Pageable pageable) {
        var type = entityType.getCode();
        var query = queryFactory.select(
                        Projections.constructor(FileResponse.class,
                                file.id,
                                file.createdAt,
                                file.updatedAt,
                                file.entityId,
                                file.entityType,
                                file.fileName,
                                file.fileSize,
                                file.mimeType,
                                file.filePath
                        ))
                .from(file)
                .where(file.entityType.eq(type)
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

    @Transactional
    public void deleteFile(Long fileId) {
        // TODO: This is not atomic, also no error handling
        var path = queryFactory.select(file.filePath)
                .from(file)
                .where(file.id.eq(fileId))
                .limit(1)
                .fetchFirst();
        if (path == null) throw new EntityNotFoundException("File with ID " + fileId + " not found");
        fileReferenceRepository.deleteById(fileId);
        storageService.delete(path);
    }

    private String getFilePath(Long fileId){
        String filePath = queryFactory.select(file.filePath)
        .from(file)
        .where(file.id.eq(fileId))
        .limit(1)
        .fetchFirst();
        if (filePath == null){
                throw new EntityNotFoundException("File path of fileId: "+fileId+" not found");
        }

        return filePath;
    }

    public Resource downloadFile(Long fileId){

        String filePath = getFilePath(fileId);
        try {
                Path path = Paths.get(filePath).normalize();
                Resource resource = new UrlResource(path.toUri());

                if(resource.exists() && resource.isReadable()) {
                        return resource;
                } else {
                        throw new RuntimeException("File not found or not readable: "+filePath);
                }

        } catch(MalformedURLException e) {
                throw new RuntimeException("Invalid file path: "+filePath);
        }

    }

    private static final QFileReferenceEntity file = QFileReferenceEntity.fileReferenceEntity;


}