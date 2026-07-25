package com.coursistant.lms.module.course.enrollment.repository;

import com.coursistant.lms.module.course.enrollment.entity.EnrollmentAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EnrollmentAuditLogMapper {

    int insert(EnrollmentAuditLog auditLog);

    List<EnrollmentAuditLog> selectByCourseId(@Param("courseId") Integer courseId);
}
