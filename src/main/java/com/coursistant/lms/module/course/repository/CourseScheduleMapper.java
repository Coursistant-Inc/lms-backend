package com.coursistant.lms.module.course.repository;


import com.coursistant.lms.module.course.entity.CourseSchedule;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import com.coursistant.lms.module.chat.entity.Query;

/**
 * 操作 course_schedule 表的接口
 * Data access interface for course schedule operations
 */
public interface CourseScheduleMapper {

    /**
     * 新增课程排课记录
     * Insert a new course schedule record
     */
    int insert(CourseSchedule courseSchedule);

    /**
     * 根据 ID 删除课程排课记录
     * Delete a course schedule record by ID
     */
    int deleteById(Integer id);

    /**
     * 根据 ID 修改课程排课记录
     * Update a course schedule record by ID
     */
    int updateById(CourseSchedule courseSchedule);

    /**
     * 根据 ID 查询课程排课记录
     * Query a course schedule record by ID
     */
    CourseSchedule selectById(Integer id);

    /**
     * 查询所有课程排课记录
     * Query all course schedule records
     */
    List<CourseSchedule> selectAll(CourseSchedule courseSchedule);

    /**
     * 查询某个课程的所有排课记录（按 course_id 查询）
     * Query schedules by course ID
     */
    List<CourseSchedule> selectByCourseId(Integer courseId);
}
