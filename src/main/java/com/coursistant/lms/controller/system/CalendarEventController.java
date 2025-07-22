package com.coursistant.lms.controller.system;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.CalendarEvent;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.service.system.CalendarEventService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.logging.Logger;

/**
 * 日历事件前端操作接口
 * Calendar event frontend operation API
 */
@RestController
@RequestMapping("/calendarEvent")
public class CalendarEventController {

    @Resource
    private CalendarEventService calendarEventService;

    private static final Logger logger = Logger.getLogger(CalendarEventController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增事件
     * Add a new calendar event
     */
    @PostMapping("/add")
    public Result add(@RequestBody CalendarEvent calendarEvent,
                      @RequestHeader(value = "X-Timezone", required = true) String timezone) {
        logRequest("add", calendarEvent.toString());
        ZoneId zone;
        try {
            zone = ZoneId.of(timezone);  // IANA 格式，例如 America/New_York
        } catch (DateTimeException e) {
            throw new CustomException(ResultCodeEnum.INVALID_TIMEZONE);
        }
        calendarEventService.add(calendarEvent, zone);
        logResponse("add", "Success");
        return Result.success();
    }


    /**
     * 根据 ID 删除事件
     * Delete a calendar event by ID
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        calendarEventService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    /**
     * 批量删除事件
     * Batch delete calendar events
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        calendarEventService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    /**
     * 修改事件
     * Update a calendar event
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody CalendarEvent calendarEvent,
                             @RequestHeader(value = "X-Timezone", required = true) String timezone) {
        logRequest("updateById", calendarEvent.toString());
        ZoneId zone;
        try {
            zone = ZoneId.of(timezone);  // IANA 格式，例如 America/New_York
        } catch (DateTimeException e) {
            throw new CustomException(ResultCodeEnum.INVALID_TIMEZONE);
        }
        calendarEventService.updateById(calendarEvent, zone);
        logResponse("updateById", "Success");
        return Result.success();
    }


    /**
     * 根据 ID 查询事件
     * Query a calendar event by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id,
                             @RequestHeader(value = "X-Timezone", required = true) String timezone) {
        logRequest("selectById", id.toString());
        ZoneId zone;
        try {
            zone = ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new CustomException(ResultCodeEnum.INVALID_TIMEZONE);
        }
        CalendarEvent calendarEvent = calendarEventService.selectById(id, zone);
        logResponse("selectById", calendarEvent.toString());
        return Result.success(calendarEvent);
    }


    /**
     * 查询所有事件
     * Query all calendar events
     */
    @GetMapping("/selectAll")
    public Result selectAll(CalendarEvent condition,
                            @RequestHeader(value = "X-Timezone", required = true) String timezone) {
        logRequest("selectAll", condition != null ? condition.toString() : "null");
        ZoneId zone;
        try {
            zone = ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new CustomException(ResultCodeEnum.INVALID_TIMEZONE);
        }
        List<CalendarEvent> list = calendarEventService.selectAll(condition, zone);
        logResponse("selectAll", null);
        return Result.success(list);
    }

}
