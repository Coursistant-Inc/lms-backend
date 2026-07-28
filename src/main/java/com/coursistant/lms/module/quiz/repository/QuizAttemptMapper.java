package com.coursistant.lms.module.quiz.repository;

import com.coursistant.lms.module.quiz.entity.QuizAttempt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface QuizAttemptMapper {

    int insert(QuizAttempt attempt);

    QuizAttempt selectById(@Param("id") Integer id);

    QuizAttempt selectByQuizIdAndId(@Param("quizId") Integer quizId, @Param("id") Integer id);

    QuizAttempt selectInProgressByQuizIdAndUserId(@Param("quizId") Integer quizId, @Param("userId") Integer userId);

    int countByQuizIdAndUserId(@Param("quizId") Integer quizId, @Param("userId") Integer userId);

    int countByQuizIdAndUserIdSubmitted(@Param("quizId") Integer quizId, @Param("userId") Integer userId);

    int countByQuizIdAndUserIdAny(@Param("quizId") Integer quizId, @Param("userId") Integer userId);

    List<QuizAttempt> selectByQuizIdAndUserId(@Param("quizId") Integer quizId, @Param("userId") Integer userId);

    List<QuizAttempt> selectByQuizId(@Param("quizId") Integer quizId,
                                     @Param("userId") Integer userId,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    int casToFinalizing(@Param("id") Integer id);

    int updateSubmitted(QuizAttempt attempt);

    List<Integer> selectIdsNeedingFinalize(@Param("nowUtc") LocalDateTime nowUtc, @Param("limit") int limit);

    List<Integer> selectInProgressIdsByCourseId(@Param("courseId") Integer courseId, @Param("limit") int limit);

    List<Integer> selectInProgressIdsByCourseIdAndUserId(@Param("courseId") Integer courseId,
                                                         @Param("userId") Integer userId,
                                                         @Param("limit") int limit);

    int countSubmittedByQuizId(@Param("quizId") Integer quizId);
}
