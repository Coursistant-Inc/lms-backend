package com.coursistant.lms.module.assignment.repository;

import com.coursistant.lms.module.assignment.entity.AssignmentAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssignmentAttachmentMapper {

    int insert(AssignmentAttachment attachment);

    AssignmentAttachment selectById(@Param("id") Integer id);

    List<AssignmentAttachment> selectByAssignmentId(@Param("assignmentId") Integer assignmentId);

    int deleteById(@Param("id") Integer id);
}
