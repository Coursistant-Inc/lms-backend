package com.coursistant.lms.v2.repository;

import com.coursistant.lms.v2.entity.CourseContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourseContentRepository extends JpaRepository<CourseContentEntity, Long>,
JpaSpecificationExecutor<CourseContentEntity> {

}
