package com.coursistant.lms.module.quiz.repository;

import com.coursistant.lms.module.quiz.entity.QuizAttemptAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuizAttemptAnswerMapper {

    int upsert(QuizAttemptAnswer answer);

    QuizAttemptAnswer selectByAttemptIdAndQuestionId(@Param("attemptId") Integer attemptId,
                                                     @Param("questionId") Integer questionId);

    List<QuizAttemptAnswer> selectByAttemptId(@Param("attemptId") Integer attemptId);

    List<QuizAttemptAnswer> selectShortAnswersByQuestionId(@Param("questionId") Integer questionId);

    List<QuizAttemptAnswer> selectByQuestionId(@Param("questionId") Integer questionId);

    int updateScore(QuizAttemptAnswer answer);

    int countPendingManualByAttemptId(@Param("attemptId") Integer attemptId);

    int countPendingManualByQuizId(@Param("quizId") Integer quizId);
}
