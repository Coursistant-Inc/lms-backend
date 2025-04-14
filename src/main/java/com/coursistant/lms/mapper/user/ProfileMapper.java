package com.coursistant.lms.mapper.user;

import com.coursistant.lms.entity.Profile;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

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
}
