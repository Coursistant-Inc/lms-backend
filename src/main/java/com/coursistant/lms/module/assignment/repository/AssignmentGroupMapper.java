package com.coursistant.lms.module.assignment.repository;

import com.coursistant.lms.module.assignment.entity.AssignmentGroup;
import org.apache.ibatis.annotations.Mapper;
 
import java.util.List;
 
@Mapper
public interface AssignmentGroupMapper {
 
    void insert(AssignmentGroup group);
 
    void updateById(AssignmentGroup group);
 
    void deleteById(Integer id);
 
    AssignmentGroup selectById(Integer id);
 
    List<AssignmentGroup> selectAll(AssignmentGroup group); // 可带筛选条件
 
    List<AssignmentGroup> selectByAssignmentId(Integer assignmentId);

    List<AssignmentGroup> selectByCourseId(Integer courseId);

    List<AssignmentGroup> selectByCourseIdAndAssignmentId(Integer courseId, Integer assignmentId);

    List<AssignmentGroup> selectAvailableGroups(Integer courseId, Integer assignmentId);
}