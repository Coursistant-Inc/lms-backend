package com.coursistant.lms.module.course.course.repository;

import com.coursistant.lms.module.course.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CourseMapper {

    int insert(Course course);

    Course selectById(Integer id);

    Course selectByIdForUpdate(@Param("id") Integer id);

    int updateById(Course course);

    int patchById(@Param("course") Course course,
                  @Param("clearDescription") boolean clearDescription,
                  @Param("clearLocation") boolean clearLocation);

    int deleteById(Integer id);

    long countByTenantId(@Param("tenantId") Integer tenantId);

    long countByInstructorOrCreator(@Param("userId") Integer userId);

    int archiveById(@Param("id") Integer id,
                    @Param("archivedAt") LocalDateTime archivedAt,
                    @Param("archivedByActorType") String archivedByActorType,
                    @Param("archivedByActorId") Integer archivedByActorId);

    int unarchiveById(@Param("id") Integer id);

    long countForBrowse(@Param("q") String q,
                        @Param("state") String state,
                        @Param("tenantId") Integer tenantId,
                        @Param("instructorUserId") Integer instructorUserId);

    List<Course> selectForBrowse(@Param("q") String q,
                                 @Param("state") String state,
                                 @Param("tenantId") Integer tenantId,
                                 @Param("instructorUserId") Integer instructorUserId,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);
}
