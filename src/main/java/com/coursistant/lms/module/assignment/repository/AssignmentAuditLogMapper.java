package com.coursistant.lms.module.assignment.repository;

import com.coursistant.lms.module.assignment.entity.AssignmentAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AssignmentAuditLogMapper {

    int insert(AssignmentAuditLog auditLog);
}
