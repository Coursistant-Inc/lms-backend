package com.coursistant.lms.controller.system;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.CalendarDisplayEvent;
import com.coursistant.lms.entity.CalendarEvent;
import com.coursistant.lms.entity.CourseSchedule;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.service.assignment.AssignmentService;
import com.coursistant.lms.service.course.CourseScheduleService;
import com.coursistant.lms.service.system.CalendarEventService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 日历事件前端操作接口
 * Calendar event frontend operation API
 */
@RestController
@RequestMapping("/calendar")
public class CalendarController {

    @Resource
    private CalendarEventService calendarEventService;

    @Resource
    private CourseScheduleService courseScheduleService;

    @Resource
    private AssignmentService assignmentService;

    private static final Logger logger = Logger.getLogger(CalendarController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 查询某人在时间范围内的所有课程日历安排
     * Query all course schedule occurrences for a person within a time range
     */
    @GetMapping("/selectCourseOccurrencesById")
    public Result selectCourseOccurrencesById(
            @RequestParam Integer id,
            @RequestParam String type,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {

        logRequest("selectCourseOccurrencesByStudentId", "Id=" + id + ", start=" + start + ", end=" + end);

        List<CalendarDisplayEvent> list = new ArrayList<>();

        if (type.equals("student")){
            list = courseScheduleService.selectCourseOccurrencesByStudentId(id, start, end);
        }
        else if (type.equals("teacher")){
            list = courseScheduleService.selectCourseOccurrencesByTeacherId(id, start, end);
        }
        else{
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        logResponse("selectCourseOccurrencesById", "size=" + list.size());

        return Result.success(list);
    }



    /**
     * 查询用户在指定时间范围内的日历事件
     * Query calendar events for a user within a time range
     */
    @GetMapping("/selectEventsByUserId")
    public Result selectByUserAndTimeRange(
            @RequestParam Integer id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {

        logRequest("selectByUserAndTimeRange", "userId=" + id + ", start=" + start + ", end=" + end);

        List<CalendarDisplayEvent> list = calendarEventService.selectDisplayEventsByUserAndTimeRange(id, start, end);

        logResponse("selectByUserAndTimeRange", "size=" + list.size());

        return Result.success(list);
    }


    /**
     * 查询某学生在时间范围内的所有日历事件（课程 + 私人事件）
     * Query all calendar events for a person (course + personal) in a time range
     */
    @GetMapping("/selectUnifiedById")
    public Result selectUnifiedById(
            @RequestParam Integer id,
            @RequestParam String type,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {

        logRequest("selectUnifiedById", "Id=" + id + ", start=" + start + ", end=" + end);

        // 查询课程安排
        List<CalendarDisplayEvent> courseEvents = new ArrayList<>();
        List<CalendarDisplayEvent> list=new ArrayList<>();

        List<CalendarDisplayEvent> personalEvents = calendarEventService.selectDisplayEventsByUserAndTimeRange(id, start, end);
        // 查询私人日历事件（假设 studentId 即 userId）
        if (type.equals("student")){
            courseEvents = courseScheduleService.selectCourseOccurrencesByStudentId(id, start, end);
            list = assignmentService.selectAssignmentByUserAndTimeRange(id, start, end);
        }
        else if (type.equals("teacher")){
            courseEvents = courseScheduleService.selectCourseOccurrencesByTeacherId(id, start, end);
        }
        else{
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }



        // 合并所有事件
        List<CalendarDisplayEvent> allEvents = new ArrayList<>();
        allEvents.addAll(courseEvents);
        allEvents.addAll(personalEvents);
        allEvents.addAll(list);

        logResponse("selectUnifiedById", "size=" + allEvents.size());

        return Result.success(allEvents);
    }

    /**
     * 查询某学生在时间范围内的所有作业（用于日历展示）
     * Query assignment deadlines for a student within a time range
     */
    @GetMapping("/selectAssignmentsByUserAndTimeRange")
    public Result selectAssignmentsByUserAndTimeRange(
            @RequestParam Integer id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {

        logRequest("selectAssignmentsByUserAndTimeRange", "userId=" + id + ", start=" + start + ", end=" + end);

        List<CalendarDisplayEvent> list = assignmentService.selectAssignmentByUserAndTimeRange(id, start, end);

        logResponse("selectAssignmentsByUserAndTimeRange", "size=" + list.size());

        return Result.success(list);
    }

    /**
     * 生成 Google Calendar 链接
     * Generate Google Calendar link for a given event
     */
    @PostMapping("/generateSingleGoogleCalendarLink")
    public Result generateGoogleCalendarLink(@RequestBody CalendarDisplayEvent event) {
        logRequest("generateGoogleCalendarLink", event.toString());

        String link = calendarEventService.generateGoogleCalendarLink(event);

        logResponse("generateGoogleCalendarLink", link);
        return Result.success(link);
    }








}
