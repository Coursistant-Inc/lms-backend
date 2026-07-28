package com.coursistant.lms.module.quiz.repository;

import com.coursistant.lms.module.quiz.entity.QuizQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface QuizQuestionMapper {

    int insert(QuizQuestion question);

    QuizQuestion selectById(@Param("id") Integer id);

    QuizQuestion selectByQuizIdAndId(@Param("quizId") Integer quizId, @Param("id") Integer id);

    QuizQuestion selectByIdForUpdate(@Param("id") Integer id);

    List<QuizQuestion> selectByQuizId(@Param("quizId") Integer quizId);

    int updateById(QuizQuestion question);

    int updatePosition(@Param("id") Integer id, @Param("position") Integer position);

    int deleteById(@Param("id") Integer id);

    int countByQuizId(@Param("quizId") Integer quizId);

    BigDecimal sumPointsByQuizId(@Param("quizId") Integer quizId);

    int maxPositionByQuizId(@Param("quizId") Integer quizId);
}
