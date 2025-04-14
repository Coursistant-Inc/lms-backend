package com.coursistant.lms.mapper.user;

import com.coursistant.lms.entity.Status;

import java.util.List;

/**
 * 操作 status 相关数据接口
 * Data access interface for status-related operations
 */
public interface StatusMapper {

    /**
     * 新增
     * Insert a new status record
     */
    int insert(Status status);

    /**
     * 删除
     * Delete a status record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a status record by ID
     */
    int updateById(Status status);

    /**
     * 根据 ID 查询
     * Query a status record by ID
     */
    Status selectById(Integer id);

    /**
     * 查询所有
     * Query all status records
     */
    List<Status> selectAll(Status status);
}
