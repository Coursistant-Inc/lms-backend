package com.coursistant.lms.mapper.user;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.coursistant.lms.entity.Profile;
/**
 * 操作 Profile 相关数据接口
 * Data access interface for Profile-related operations
 */
@Mapper
public interface ProfileMapper {

    /**
     * 新增
     * Insert a new Profile record
     */
    int insert(Profile profile);

    /**
     * 删除
     * Delete a Profile record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a Profile record by ID
     */
    int updateById(Profile profile);

    /**
     * 根据用户 ID 查询
     * Query a Profile record by user ID
     */
    Profile selectByUserId(Integer userId);

    /**
     * 查询所有
     * Query all Profile records
     */
    List<Profile> selectAll();

    void updateUserPrivacyById(String privacy, Integer userId);

    String selectUserPrivacyById(Integer userId);

    List<Map<String,Object>> selectGradesById(Integer userId);

    List<Map<String,Object>> selectCourseGradesById(Integer userId, Integer courseId);

    String selectAvatarPathById(Integer userId);
}
