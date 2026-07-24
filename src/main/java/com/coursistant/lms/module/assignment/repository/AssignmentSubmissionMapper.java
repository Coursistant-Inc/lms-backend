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

    /**
     * The single submission head a group owns on one Group assignment.
     */
    AssignmentSubmission selectByAssignmentIdAndGroupId(@Param("assignmentId") Integer assignmentId,
                                                        @Param("groupId") Integer groupId);

    int updateCurrentVersionId(@Param("id") Integer id, @Param("currentVersionId") Integer currentVersionId);

    int countByAssignmentId(@Param("assignmentId") Integer assignmentId);

    /**
     * Submission heads owned by one group across all assignments. A non-empty result is what
     * blocks deleting the group.
     */
    int countByGroupId(@Param("groupId") Integer groupId);

    /**
     * Submitted versions owned by one group across all assignments — the academic-hold signal.
     * Counting versions rather than heads keeps "has submitted" and "has a head row" from
     * drifting apart.
     */
    int countVersionsByGroupId(@Param("groupId") Integer groupId);
}
