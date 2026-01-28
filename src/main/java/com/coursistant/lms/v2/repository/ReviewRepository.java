package com.coursistant.lms.v2.repository;

import com.coursistant.lms.v2.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long>,
        JpaSpecificationExecutor<ReviewEntity> {
}
