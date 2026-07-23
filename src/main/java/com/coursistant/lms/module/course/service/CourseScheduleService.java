package com.coursistant.lms.module.course.service;
import com.coursistant.lms.module.calendar.entity.CalendarDisplayEvent;



import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.shared.exception.CustomException;

import com.coursistant.lms.module.course.repository.CourseScheduleMapper;

import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.coursistant.lms.module.course.entity.Course;
import com.coursistant.lms.module.course.entity.CourseSchedule;
import com.coursistant.lms.module.course.entity.Learn;
import com.coursistant.lms.module.course.entity.Teach;


/**
 * 课程排课服务类
 * Service for course schedule operations
 */
@Service
public class CourseScheduleService {

    @Resource
    private CourseScheduleMapper courseScheduleMapper;
    @Resource
    private CourseService courseService;
    @Resource
    private LearnService learnService;
    @Resource
    private TeachService teachService;

    /**
     * 新增
     * Insert a new course schedule
     */
    public void add(CourseSchedule courseSchedule) {
        courseScheduleMapper.insert(courseSchedule);
    }

    /**
     * 删除
     * Delete by ID
     */
    public void deleteById(Integer id) {
        courseScheduleMapper.deleteById(id);
    }

    /**
     * 批量删除
     * Delete multiple schedules by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            courseScheduleMapper.deleteById(id);
        }
    }

    /**
     * 修改
     * Update course schedule
     */
    public void updateById(CourseSchedule courseSchedule) {
        courseScheduleMapper.updateById(courseSchedule);
    }

    /**
     * 根据 ID 查询
     * Select by ID
     */
    public CourseSchedule selectById(Integer id) {
        CourseSchedule schedule = courseScheduleMapper.selectById(id);
        if (schedule == null) {
            throw new CustomException(ResultCodeEnum.COURSE_SCHEDULE_NOT_EXIST_ERROR);
        }
        return schedule;
    }

    /**
     * 查询所有
     * Select all schedules
     */
    public List<CourseSchedule> selectAll(CourseSchedule condition) {
        return courseScheduleMapper.selectAll(condition);
    }

    /**
     * 查询课程排课
     * Select by course ID
     */
    public List<CourseSchedule> selectByCourseId(Integer courseId) {

        return courseScheduleMapper.selectByCourseId(courseId);
    }

    public List<CalendarDisplayEvent> selectCourseOccurrencesByTeacherId(Integer teacherId, LocalDateTime start, LocalDateTime end){

        List<CalendarDisplayEvent> result = new ArrayList<>();

        List<Teach> dbteach=teachService.selectByTeacherId(teacherId);
        if (dbteach == null || dbteach.isEmpty()) {
            return result;
        }
        for (Teach singleLearn:dbteach){
            List<CalendarDisplayEvent> courses=selectCourseOccurrences(singleLearn.getCourseId(),start,end);
            result.addAll(courses);
        }

        return result;
    }

    public List<CalendarDisplayEvent> selectCourseOccurrencesByStudentId(Integer studentId, LocalDateTime start, LocalDateTime end){

        List<CalendarDisplayEvent> result = new ArrayList<>();

        List<Learn> dblearn=learnService.selectByStudentId(studentId);
        if (dblearn == null || dblearn.isEmpty()) {
            return result;
        }
        for (Learn singleLearn:dblearn){
            List<CalendarDisplayEvent> courses=selectCourseOccurrences(singleLearn.getCourseId(),start,end);
            result.addAll(courses);
        }

        return result;
    }

    public List<CalendarDisplayEvent> selectCourseOccurrences(Integer courseId, LocalDateTime start, LocalDateTime end) {
        List<CalendarDisplayEvent> result = new ArrayList<>();

        List<CourseSchedule> schedules = courseScheduleMapper.selectByCourseId(courseId);
        Course dbcourse=courseService.selectById(courseId);

        if (schedules == null || schedules.isEmpty()) {
            return result;
        }

        for (CourseSchedule schedule : schedules) {
            List<LocalDate> classDates = expandScheduleDates(schedule, start.toLocalDate(), end.toLocalDate());

            for (LocalDate date : classDates) {
                LocalDateTime classStart = date.atTime(schedule.getStartTime());
                LocalDateTime classEnd = date.atTime(schedule.getEndTime());

                // 过滤掉超出 start-end 区间的（精确到时间）
                if (classEnd.isBefore(start) || classStart.isAfter(end)) {
                    continue;
                }

                CalendarDisplayEvent event = new CalendarDisplayEvent();
                event.setTitle(dbcourse.getName());
                event.setStart(classStart);
                event.setEnd(classEnd);
                event.setAllDay(false);
                event.setType("course");
                event.setSourceId(schedule.getId());
                event.setTimezone(schedule.getTimezone());

                result.add(event);
            }
        }

        return result;
    }


    private List<LocalDate> expandScheduleDates(CourseSchedule schedule, LocalDate rangeStart, LocalDate rangeEnd) {
        List<LocalDate> result = new ArrayList<>();

        LocalDate start = schedule.getStartDate().isAfter(rangeStart) ? schedule.getStartDate() : rangeStart;
        LocalDate end = schedule.getEndDate().isBefore(rangeEnd) ? schedule.getEndDate() : rangeEnd;

        int targetWeekday = schedule.getWeekday(); // 1=Monday ~ 7=Sunday

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (date.getDayOfWeek().getValue() == targetWeekday) {
                result.add(date);
            }
        }

        return result;
    }


}
