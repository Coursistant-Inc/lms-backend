package com.coursistant.lms.mapper.assignment;

import com.coursistant.lms.entity.AssignmentGroup;
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
}