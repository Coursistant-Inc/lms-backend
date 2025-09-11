package com.coursistant.lms.mapper.course;

import java.util.List;

import org.apache.ibatis.annotations.Select;

import com.coursistant.lms.entity.Learn;

/**
 * 操作 learn 相关数据接口
 * Data access interface for learn-related operations
 */
public interface LearnMapper {

    /**
     * 新增
     * Insert a new Learn record
     */
    int insert(Learn learn);

    /**
     * 删除
     * Delete a Learn record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a Learn record by ID
     */
    int updateById(Learn learn);

    /**
     * 根据 ID 查询
     * Query a Learn record by ID
     */
    Learn selectById(Integer id);

    /**
     * 根据 Course ID 查询
     * Query all Learn records associated with a course id
     */
    List<Learn> selectByCourseId(Integer courseId);

    /**
     * 查询所有
     * Query all Learn records
     */
    List<Learn> selectAll(Learn learn);

    /**
     * 根据用户名查询
     * Query a Learn record by username
     */
    @Select("select * from Learn where username = #{username}")
    Learn selectByUsername(String username);

    @Select("select * from Learn where user_id = #{userId}")
    List<Learn> selectByUserId(Integer userId);

    Learn selectByUserIdAndCourseId(Integer userId, Integer courseId);

    void updateLearnStatusById(Integer userId, Integer courseId, String courseStatus);

    String selectLearnStatusById(Integer userId, Integer courseId);

    void updateGradeById(Integer userId, Integer courseId, String grade);

    String selectGradeById(Integer userId, Integer courseId);



    

    /**
     * 根据 Course ID 查询用户邮箱列表
     * Query all user emails associated with a course id
     */
    List<String> selectEmailsByCourseId(Integer courseId);

    /**
     * 根据 Course ID 查询记录数量
     * Count how many Learn records are associated with a course id
     */
    @Select("SELECT COUNT(*) FROM Learn WHERE course_id = #{courseId}")
    int countByCourseId(Integer courseId);



}
