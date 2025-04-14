package com.coursistant.individual.mapper.course;

import com.coursistant.individual.entity.Learn;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

}
