package com.coursistant.lms.v2.repository;

import com.coursistant.lms.v2.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourseRepository extends JpaRepository<CourseEntity, Long>,
        JpaSpecificationExecutor<CourseEntity> {
}
