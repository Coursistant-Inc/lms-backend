package com.coursistant.lms.module.course.content.material.repository;

import com.coursistant.lms.module.course.content.material.entity.CourseMaterial;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseMaterialMapper {
    int insert(CourseMaterial material);

    int updateById(CourseMaterial material);

    int deleteById(@Param("id") Integer id);

    CourseMaterial selectById(@Param("id") Integer id);

    List<CourseMaterial> selectByWeekId(@Param("weekId") Integer weekId);

    List<CourseMaterial> selectByWeekIdAndType(@Param("weekId") Integer weekId, @Param("materialType") String materialType);

    List<CourseMaterial> selectByCourseId(@Param("courseId") Integer courseId);

    int countByWeekId(@Param("weekId") Integer weekId);

    Integer selectMaxOrderPosition(@Param("weekId") Integer weekId);

    int updateOrderPosition(@Param("id") Integer id, @Param("orderPosition") Integer orderPosition);

    int updateWeekId(@Param("id") Integer id, @Param("weekId") Integer weekId, @Param("orderPosition") Integer orderPosition);
}
