package com.coursistant.lms.module.quiz.repository;

import com.coursistant.lms.module.quiz.entity.QuizScoreAudit;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuizScoreAuditMapper {

    int insert(QuizScoreAudit audit);
}
