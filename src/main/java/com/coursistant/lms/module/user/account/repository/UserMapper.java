package com.coursistant.lms.module.user.account.repository;

import java.util.List;

import org.apache.ibatis.annotations.Select;

import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.chat.entity.Query;

/**
 * 操作 user 相关数据接口
 * Data access interface for user-related operations
 */
public interface UserMapper {

    /**
     * 新增
     * Insert a new User record
     */
    int insert(User user);

    /**
     * 删除
     * Delete a User record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a User record by ID
     */
    int updateById(User user);

    /**
     * 根据 ID 查询
     * Query a User record by ID
     */
    User selectById(Integer id);

    /**
     * 查询所有
     * Query all User records
     */
    List<User> selectAll(User user);

    /**
     * 根据用户名查询
     * Query a User record by username
     */
    @Select("select * from User where username = #{username}")
    User selectByUsername(String username);

    /**
     * 根据邮箱查询
     * Query a User record by email
     */
    @Select("select * from User where email = #{email}")
    User selectByEmail(String email);

    /**
     * 查询所有教师
     * Query all teachers
     */
    @Select("select * from User where level = 'INSTRUCTOR'")
    List<User> selectTeachers();

    /**
     * Query all students by course ID
     */
    List<User> selectStudentsByCourseId(Integer courseId);

    // @Select("SELECT level FROM User WHERE id = #{id}")
    String selectUserLevelById(Integer id);

    List<User> selectUsersByIds(List<Integer> userIds);

    void updateMustChangePassword( Integer id, boolean mustChangePassword);

    void clearAvatar(Integer id);
}
