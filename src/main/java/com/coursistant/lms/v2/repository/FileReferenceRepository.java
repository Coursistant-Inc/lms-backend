package com.coursistant.lms.v2.repository;

import com.coursistant.lms.v2.entity.FileReferenceEntity;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import java.util.List;


public interface FileReferenceRepository extends JpaRepository<FileReferenceEntity, Long>,
        JpaSpecificationExecutor<FileReferenceEntity> {

                @Transactional
                @Modifying
                void deleteByEntityId(Long entityId);

                List<FileReferenceEntity> findByEntityId(Long entityId);
}
