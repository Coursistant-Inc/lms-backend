package com.coursistant.lms.v2.repository;

import com.coursistant.lms.v2.entity.UserCourseRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserCourseRelationRepository extends JpaRepository<UserCourseRelationEntity, Long>,
        JpaSpecificationExecutor<UserCourseRelationEntity> {
}
