package com.coursistant.lms.module.assignment.repository;

import com.coursistant.lms.module.assignment.entity.AssignmentSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssignmentSubmissionMapper {

    int insert(AssignmentSubmission submission);

    AssignmentSubmission selectById(@Param("id") Integer id);

    List<AssignmentSubmission> selectByAssignmentId(@Param("assignmentId") Integer assignmentId);

    AssignmentSubmission selectByAssignmentIdAndOwnerUserId(@Param("assignmentId") Integer assignmentId,
                                                            @Param("ownerUserId") Integer ownerUserId);

    int updateCurrentVersionId(@Param("id") Integer id, @Param("currentVersionId") Integer currentVersionId);

    int countByAssignmentId(@Param("assignmentId") Integer assignmentId);
}
