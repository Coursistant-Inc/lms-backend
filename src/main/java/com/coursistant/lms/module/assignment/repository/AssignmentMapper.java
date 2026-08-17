package com.coursistant.lms.module.assignment.repository;

import com.coursistant.lms.module.assignment.dto.UpcomingAssignmentQueryRow;
import com.coursistant.lms.module.assignment.entity.Assignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AssignmentMapper {

    int insert(Assignment assignment);

    Assignment selectById(@Param("id") Integer id);

    List<Assignment> selectByCourseId(@Param("courseId") Integer courseId);

    List<Assignment> selectByCourseIdAndState(@Param("courseId") Integer courseId, @Param("state") String state);

    Assignment selectByCourseIdAndId(@Param("courseId") Integer courseId, @Param("id") Integer id);

    int updateById(Assignment assignment);

    /**
     * Always writes late_until, including {@code null} (the dynamic updateById skips nulls).
     */
    int updateLateUntil(@Param("id") Integer id, @Param("lateUntil") java.time.LocalDateTime lateUntil);

    int deleteById(@Param("id") Integer id);

    int countSubmissionsByAssignmentId(@Param("assignmentId") Integer assignmentId);

    /**
     * Submitted versions, not submission heads: the delete / unpublish guards must react to
     * "someone handed something in", which is a version.
     */
    int countSubmissionVersionsByAssignmentId(@Param("assignmentId") Integer assignmentId);

    int countByGroupSetId(@Param("groupSetId") Integer groupSetId);

    int countGradesByAssignmentId(@Param("assignmentId") Integer assignmentId);

    int updateCurrentRubricVersionId(@Param("id") Integer id,
                                     @Param("currentRubricVersionId") Integer currentRubricVersionId);

    int updateState(@Param("id") Integer id, @Param("state") String state);

    /**
     * Sets Published and increments publication_version only when currently unpublished.
     * Must not be written by {@link #updateById}.
     */
    int publishAndIncrementPublicationVersion(@Param("id") Integer id);

    /**
     * Atomic schedule_version increment. Must not be written by {@link #updateById}.
     */
    int incrementScheduleVersion(@Param("id") Integer id);

    /** Always writes group_set_id, including {@code null} when switching to Individual. */
    int updateGroupSetId(@Param("id") Integer id, @Param("groupSetId") Integer groupSetId);

    /**
     * Published assignments for the user's active enrollments with due_at in
     * {@code [fromUtc, toUtc]} (inclusive), ordered by due then course then id.
     */
    List<UpcomingAssignmentQueryRow> selectPublishedUpcomingForUser(@Param("userId") Integer userId,
                                                                    @Param("fromUtc") LocalDateTime fromUtc,
                                                                    @Param("toUtc") LocalDateTime toUtc);
}
