package com.coursistant.lms.module.assignment.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GradeCorrectionAuditMapper {
    @Insert("INSERT INTO grade_correction_audit (actor_id, assignment_id, student_user_id, course_id, tenant_id, reason, before_json, after_json) " +
            "VALUES (#{actorId}, #{assignmentId}, #{studentUserId}, #{courseId}, #{tenantId}, #{reason}, #{beforeJson}, #{afterJson})")
    int insert(@Param("actorId") Integer actorId,
               @Param("assignmentId") Integer assignmentId,
               @Param("studentUserId") Integer studentUserId,
               @Param("courseId") Integer courseId,
               @Param("tenantId") Integer tenantId,
               @Param("reason") String reason,
               @Param("beforeJson") String beforeJson,
               @Param("afterJson") String afterJson);
}
