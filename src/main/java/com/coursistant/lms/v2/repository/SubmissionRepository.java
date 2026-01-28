package com.coursistant.lms.v2.repository;

import com.coursistant.lms.v2.entity.SubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SubmissionRepository extends JpaRepository<SubmissionEntity, Long>,
        JpaSpecificationExecutor<SubmissionEntity> {
}
