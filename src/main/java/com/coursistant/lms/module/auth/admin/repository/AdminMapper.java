package com.coursistant.lms.module.auth.admin.repository;

import com.coursistant.lms.module.auth.admin.entity.Admin;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import com.coursistant.lms.module.chat.entity.Query;

/**
 * 操作 admin 相关数据接口
 * Data access interface for admin-related operations
 */
public interface AdminMapper {

    /**
     * 新增
     * Insert a new admin record
     */
    int insert(Admin admin);

    /**
     * 删除
     * Delete an admin record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update an admin record by ID
     */
    int updateById(Admin admin);

    /**
     * 根据 ID 查询
     * Query an admin record by ID
     */
    Admin selectById(Integer id);

    /**
     * 查询所有
     * Query all admin records
     */
    List<Admin> selectAll(Admin admin);

    @Select("select * from Admin where username = #{username}")
    Admin selectByUsername(String username);

    @Select("select * from Admin where email = #{email}")
    Admin selectByEmail(String email);
}
