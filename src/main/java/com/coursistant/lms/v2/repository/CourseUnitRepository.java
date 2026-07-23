package com.coursistant.lms.v2.repository;

import com.coursistant.lms.v2.entity.CourseUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourseUnitRepository extends JpaRepository<CourseUnitEntity, Long>,
        JpaSpecificationExecutor<CourseUnitEntity> {
}
