package com.coursistant.lms.module.course.teaching.repository;

import com.coursistant.lms.module.course.teaching.dto.TeachingActivityResponse;
import com.coursistant.lms.module.course.teaching.dto.TeachingCourseRow;
import com.coursistant.lms.module.course.teaching.dto.TeachingDeadlineRow;
import com.coursistant.lms.module.course.teaching.dto.TeachingGradingQueueRow;
import com.coursistant.lms.module.course.teaching.dto.TeachingRecentActivityRow;
import com.coursistant.lms.module.course.teaching.dto.TeachingSessionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TeachingDashboardMapper {

    List<TeachingCourseRow> selectTeachingCourses(@Param("userId") Integer userId);

    List<TeachingGradingQueueRow> selectAssignmentUngradedIndividual(@Param("courseIds") List<Integer> courseIds);

    List<TeachingGradingQueueRow> selectAssignmentUngradedGroup(@Param("courseIds") List<Integer> courseIds);

    List<TeachingGradingQueueRow> selectAssignmentAwaitingRelease(@Param("courseIds") List<Integer> courseIds);

    List<TeachingGradingQueueRow> selectQuizManualPending(@Param("courseIds") List<Integer> courseIds);

    List<TeachingGradingQueueRow> selectQuizAwaitingRelease(@Param("courseIds") List<Integer> courseIds);

    List<TeachingSessionRow> selectSessionsByCourseIds(@Param("courseIds") List<Integer> courseIds);

    List<TeachingActivityResponse> selectEventsInWindow(@Param("courseIds") List<Integer> courseIds,
                                                        @Param("fromDate") LocalDate fromDate,
                                                        @Param("toDate") LocalDate toDate);

    List<TeachingDeadlineRow> selectAssignmentDeadlines(@Param("courseIds") List<Integer> courseIds,
                                                        @Param("fromUtc") LocalDateTime fromUtc,
                                                        @Param("toUtc") LocalDateTime toUtc);

    List<TeachingDeadlineRow> selectQuizDeadlines(@Param("courseIds") List<Integer> courseIds,
                                                  @Param("fromUtc") LocalDateTime fromUtc,
                                                  @Param("toUtc") LocalDateTime toUtc);

    List<TeachingRecentActivityRow> selectGroupMembershipChanges(@Param("courseIds") List<Integer> courseIds,
                                                                 @Param("limit") int limit);

    List<TeachingRecentActivityRow> selectLateSubmissions(@Param("courseIds") List<Integer> courseIds,
                                                          @Param("limit") int limit);
}
