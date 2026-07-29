package com.coursistant.lms.module.tenant.repository;

import com.coursistant.lms.module.tenant.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TenantMapper {

    int insert(Tenant tenant);

    Tenant selectById(@Param("id") Integer id);

    Tenant selectByName(@Param("name") String name);

    List<Tenant> selectAllOrderByNameAsc();

    int updateById(Tenant tenant);

    int deleteById(@Param("id") Integer id);
}
