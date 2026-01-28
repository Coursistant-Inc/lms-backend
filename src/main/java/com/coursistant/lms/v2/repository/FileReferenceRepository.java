package com.coursistant.lms.v2.repository;

import com.coursistant.lms.v2.entity.FileReferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FileReferenceRepository extends JpaRepository<FileReferenceEntity, Long>,
        JpaSpecificationExecutor<FileReferenceEntity> {
}
