package com.coursistant.lms.module.quiz.repository;

import com.coursistant.lms.module.quiz.entity.Quiz;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import com.coursistant.lms.module.chat.entity.Query;

public interface QuizMapper {

    /**
     * 新增
     * Insert a new quiz record
     */
    int insert(Quiz quiz);

    /**
     * 删除
     * Delete a quiz record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a quiz record by ID
     */
    int updateById(Quiz quiz);

    /**
     * 根据 ID 查询
     * Query a quiz record by ID
     */
    Quiz selectById(Integer id);

    /**
     * 查询所有（可带条件）
     * Query all quiz records (filterable)
     */
    List<Quiz> selectAll(Quiz filter);


    @Select("SELECT * FROM quiz WHERE course_id = #{courseId}")
    List<Quiz> selectByCourseId(Integer courseId);


}
