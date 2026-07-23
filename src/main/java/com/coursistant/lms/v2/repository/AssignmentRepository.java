package com.coursistant.lms.v2.repository;

import com.coursistant.lms.v2.entity.AssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AssignmentRepository extends JpaRepository<AssignmentEntity, Long>,
        JpaSpecificationExecutor<AssignmentEntity> {
}
