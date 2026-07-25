package com.coursistant.lms.module.quiz.repository;

import com.coursistant.lms.module.quiz.entity.QuizAttempt;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import com.coursistant.lms.module.chat.entity.Query;

public interface QuizAttemptMapper {

    /**
     * 新增
     * Insert a new quizAttempt record
     */
    int insert(QuizAttempt quizAttempt);

    /**
     * 删除
     * Delete a quizAttempt record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a quizAttempt record by ID
     */
    int updateById(QuizAttempt quizAttempt);

    /**
     * 根据 ID 查询
     * Query a quizAttempt record by ID
     */
    QuizAttempt selectById(Integer id);

    /**
     * 查询所有（可带条件）
     * Query all quizAttempt records (filterable)
     */
    List<QuizAttempt> selectAll(QuizAttempt filter);




    // 1) quizId 下按 student_id 去重：每个学生只取主键 id 最大的一条
    @Select("""
    SELECT qa.*
    FROM quiz_attempt qa
    INNER JOIN (
        SELECT student_id, MAX(id) AS max_id
        FROM quiz_attempt
        WHERE quiz_id = #{quizId}
        GROUP BY student_id
    ) t ON qa.id = t.max_id
    """)
    List<QuizAttempt> selectLatestByQuizIdDistinctStudent(@Param("quizId") Integer quizId);

    // 2) quizId + studentId：取该学生在该测验下主键 id 最大的一条
    @Select("""
    SELECT *
    FROM quiz_attempt
    WHERE quiz_id = #{quizId}
      AND student_id = #{studentId}
    ORDER BY id DESC
    LIMIT 1
    """)
    QuizAttempt selectLatestByQuizIdAndStudentId(@Param("quizId") Integer quizId,
                                                 @Param("studentId") Integer studentId);


}
