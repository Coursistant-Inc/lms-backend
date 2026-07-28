package com.coursistant.lms.module.quiz.repository;

import com.coursistant.lms.module.quiz.entity.QuizGrade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuizGradeMapper {

    int upsertOnSubmit(QuizGrade grade);

    QuizGrade selectByQuizIdAndUserId(@Param("quizId") Integer quizId, @Param("userId") Integer userId);

    List<QuizGrade> selectByQuizId(@Param("quizId") Integer quizId);

    int release(@Param("quizId") Integer quizId,
                @Param("userIds") List<Integer> userIds,
                @Param("releasedBy") Integer releasedBy);

    int retract(@Param("quizId") Integer quizId, @Param("userIds") List<Integer> userIds);

    int incrementVersion(@Param("id") Integer id);

    int countReleasedByQuizId(@Param("quizId") Integer quizId);
}
