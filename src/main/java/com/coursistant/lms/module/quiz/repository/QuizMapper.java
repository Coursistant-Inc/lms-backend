package com.coursistant.lms.module.quiz.repository;

import com.coursistant.lms.module.quiz.entity.Quiz;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuizMapper {

    int insert(Quiz quiz);

    Quiz selectById(@Param("id") Integer id);

    Quiz selectByCourseIdAndId(@Param("courseId") Integer courseId, @Param("id") Integer id);

    List<Quiz> selectByCourseId(@Param("courseId") Integer courseId);

    List<Quiz> selectByCourseIdAndState(@Param("courseId") Integer courseId, @Param("state") String state);

    int updateById(Quiz quiz);

    int updateState(@Param("id") Integer id, @Param("state") String state);

    int deleteById(@Param("id") Integer id);

    int countAttemptsByQuizId(@Param("quizId") Integer quizId);
}
