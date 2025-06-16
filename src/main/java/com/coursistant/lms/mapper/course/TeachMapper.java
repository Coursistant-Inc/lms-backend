package com.coursistant.lms.mapper.course;

import com.coursistant.lms.entity.Learn;
import com.coursistant.lms.entity.Teach;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作 teach 相关数据接口
 * Data access interface for teach-related operations
 */
public interface TeachMapper {

    /**
     * 新增
     * Insert a new Teach record
     */
    int insert(Teach teach);

    /**
     * 删除
     * Delete a Teach record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a Teach record by ID
     */
    int updateById(Teach teach);

    /**
     * 根据 ID 查询
     * Query a Teach record by ID
     */
    Teach selectById(Integer id);

    /**
     * 查询所有
     * Query all Teach records
     */
    List<Teach> selectAll(Teach teach);

    @Select("select * from Teach where user_id = #{userId}")
    List<Teach> selectByUserId(Integer userId);
}
