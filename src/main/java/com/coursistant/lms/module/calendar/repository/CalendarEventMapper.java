package com.coursistant.lms.module.calendar.repository;

import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.calendar.entity.CalendarEvent;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import com.coursistant.lms.module.chat.entity.Query;

/**
 * 操作 calendar_event 表的接口
 * Data access interface for calendar event operations
 */
public interface CalendarEventMapper {

    /**
     * 新增日历事件
     * Insert a new calendar event
     */
    int insert(CalendarEvent calendarEvent);

    /**
     * 根据 ID 删除事件
     * Delete a calendar event by ID
     */
    int deleteById(Integer id);

    /**
     * 根据 ID 修改事件
     * Update a calendar event by ID
     */
    int updateById(CalendarEvent calendarEvent);

    /**
     * 根据 ID 查询事件
     * Query a calendar event by ID
     */
    CalendarEvent selectById(Integer id);

    /**
     * 查询所有事件
     * Query all calendar events
     */
    List<CalendarEvent> selectAll(CalendarEvent calendarEvent);

    /**
     * 查询某用户在某时间段内的事件
     * Query calendar events by user ID and time range
     */
    List<CalendarEvent> selectByUserAndRange(
            @Param("userId") Integer userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
