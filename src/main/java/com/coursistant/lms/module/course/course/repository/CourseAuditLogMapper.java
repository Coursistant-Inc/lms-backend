package com.coursistant.lms.module.course.course.repository;

import com.coursistant.lms.module.course.course.entity.CourseAuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface CourseAuditLogMapper {

    @Insert("INSERT INTO course_audit_log (course_id, tenant_id, actor_type, actor_id, actor_role, action, "
            + "target_type, target_id, before_json, after_json, request_id) "
            + "VALUES (#{courseId}, #{tenantId}, #{actorType}, #{actorId}, #{actorRole}, #{action}, "
            + "#{targetType}, #{targetId}, #{beforeJson}, #{afterJson}, #{requestId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CourseAuditLog row);
}
