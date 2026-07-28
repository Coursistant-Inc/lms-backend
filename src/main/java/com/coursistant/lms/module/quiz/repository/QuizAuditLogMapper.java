package com.coursistant.lms.module.quiz.repository;

import com.coursistant.lms.module.quiz.entity.QuizAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuizAuditLogMapper {

    int insert(QuizAuditLog log);
}
