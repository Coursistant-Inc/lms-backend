package com.coursistant.lms.module.quiz.repository;

import com.coursistant.lms.module.quiz.entity.QuizQuestionOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuizQuestionOptionMapper {

    int insert(QuizQuestionOption option);

    int insertBatch(@Param("options") List<QuizQuestionOption> options);

    QuizQuestionOption selectById(@Param("id") Integer id);

    List<QuizQuestionOption> selectInstructorByQuestionId(@Param("questionId") Integer questionId);

    List<QuizQuestionOption> selectStudentSafeByQuestionId(@Param("questionId") Integer questionId);

    List<QuizQuestionOption> selectStudentSafeByQuestionIds(@Param("questionIds") List<Integer> questionIds);

    int updateLabel(@Param("id") Integer id, @Param("label") String label, @Param("position") Integer position);

    int updateIsCorrect(@Param("id") Integer id, @Param("isCorrect") Boolean isCorrect);

    int deleteByQuestionId(@Param("questionId") Integer questionId);

    int deleteById(@Param("id") Integer id);
}
