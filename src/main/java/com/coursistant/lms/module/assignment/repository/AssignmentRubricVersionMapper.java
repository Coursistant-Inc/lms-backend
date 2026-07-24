package com.coursistant.lms.module.assignment.repository;

import com.coursistant.lms.module.assignment.entity.AssignmentRubricVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssignmentRubricVersionMapper {

    int insert(AssignmentRubricVersion rubricVersion);

    AssignmentRubricVersion selectById(@Param("id") Integer id);

    List<AssignmentRubricVersion> selectByAssignmentIdOrderByVersionDesc(@Param("assignmentId") Integer assignmentId);

    Integer selectMaxVersionNo(@Param("assignmentId") Integer assignmentId);
}
