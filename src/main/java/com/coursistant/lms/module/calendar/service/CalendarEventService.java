package com.coursistant.lms.module.calendar.service;



import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.module.calendar.entity.CalendarDisplayEvent;
import com.coursistant.lms.module.calendar.entity.CalendarEvent;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.module.calendar.repository.CalendarEventMapper;


import com.coursistant.lms.shared.util.TimeZoneUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.chat.entity.Query;

/**
 * 日历事件服务类
 * Service for calendar event operations
 */
@Service
public class CalendarEventService {

    @Resource
    private CalendarEventMapper calendarEventMapper;

    /**
     * 新增
     * Insert a new calendar event
     */
    public void add(CalendarEvent calendarEvent, ZoneId timezone) {
        calendarEvent.setStartTime(TimeZoneUtils.toUtcLocalDateTime(calendarEvent.getStartTime(),timezone));
        calendarEvent.setEndTime(TimeZoneUtils.toUtcLocalDateTime(calendarEvent.getEndTime(),timezone));
        calendarEventMapper.insert(calendarEvent);
    }

    /**
     * 删除
     * Delete a calendar event by ID
     */
    public void deleteById(Integer id) {
        calendarEventMapper.deleteById(id);
    }

    /**
     * 批量删除
     * Delete multiple calendar events by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            calendarEventMapper.deleteById(id);
        }
    }

    /**
     * 修改
     * Update a calendar event
     */
    public void updateById(CalendarEvent calendarEvent, ZoneId timezone) {
        calendarEvent.setStartTime(TimeZoneUtils.toUtcLocalDateTime(calendarEvent.getStartTime(),timezone));
        calendarEvent.setEndTime(TimeZoneUtils.toUtcLocalDateTime(calendarEvent.getEndTime(),timezone));
        calendarEventMapper.updateById(calendarEvent);
    }

    /**
     * 根据 ID 查询
     * Select by ID
     */
    public CalendarEvent selectById(Integer id, ZoneId timezone) {
        CalendarEvent calendarEvent = calendarEventMapper.selectById(id);
        if (calendarEvent == null) {
            throw new CustomException(ResultCodeEnum.EVENT_NOT_EXIST_ERROR);
        }
        calendarEvent.setStartTime(TimeZoneUtils.fromUtcLocalDateTime(calendarEvent.getStartTime(),timezone));
        calendarEvent.setEndTime(TimeZoneUtils.fromUtcLocalDateTime(calendarEvent.getEndTime(),timezone));
        return calendarEvent;
    }

    /**
     * 查询所有
     * Select all events
     */
    public List<CalendarEvent> selectAll(CalendarEvent calendarEvent1, ZoneId timezone) {
        List<CalendarEvent> lists=calendarEventMapper.selectAll(calendarEvent1);
        for (CalendarEvent calendarEvent:lists){
            calendarEvent.setStartTime(TimeZoneUtils.fromUtcLocalDateTime(calendarEvent.getStartTime(),timezone));
            calendarEvent.setEndTime(TimeZoneUtils.fromUtcLocalDateTime(calendarEvent.getEndTime(),timezone));
        }
        return lists;
    }

    /**
     * 查询某用户在某时间段的事件
     * Select events by user and time range
     */
    public List<CalendarEvent> selectByUserAndTimeRange(Integer userId, LocalDateTime start, LocalDateTime end,ZoneId timezone) {
        start=TimeZoneUtils.toUtcLocalDateTime(start,timezone);
        end=TimeZoneUtils.toUtcLocalDateTime(end,timezone);
        List<CalendarEvent> lists=calendarEventMapper.selectByUserAndRange(userId, start, end);
        for (CalendarEvent calendarEvent:lists){
            calendarEvent.setStartTime(TimeZoneUtils.fromUtcLocalDateTime(calendarEvent.getStartTime(),timezone));
            calendarEvent.setEndTime(TimeZoneUtils.fromUtcLocalDateTime(calendarEvent.getEndTime(),timezone));
        }
        return lists;
    }

    /**
     * 查询某用户在指定时间范围内的个人事件，并转换为日历展示格式
     * Query personal calendar events and convert to display events
     */
    public List<CalendarDisplayEvent> selectDisplayEventsByUserAndTimeRange(Integer userId, LocalDateTime start, LocalDateTime end, ZoneId timezone) {

        start=TimeZoneUtils.toUtcLocalDateTime(start,timezone);
        end=TimeZoneUtils.toUtcLocalDateTime(end,timezone);

        List<CalendarEvent> personalEvents = calendarEventMapper.selectByUserAndRange(userId, start, end);

        for (CalendarEvent calendarEvent:personalEvents){
            calendarEvent.setStartTime(TimeZoneUtils.fromUtcLocalDateTime(calendarEvent.getStartTime(),timezone));
            calendarEvent.setEndTime(TimeZoneUtils.fromUtcLocalDateTime(calendarEvent.getEndTime(),timezone));
        }

        return personalEvents.stream().map(e -> {
            CalendarDisplayEvent display = new CalendarDisplayEvent();
            display.setTitle(e.getTitle());
            display.setStart(e.getStartTime());
            display.setEnd(e.getEndTime());
            display.setAllDay(e.getAllDay());
            display.setType("personal");
            display.setSourceId(e.getId());
            display.setTimezone(timezone.toString());
            return display;
        }).collect(Collectors.toList());
    }

    public String generateGoogleCalendarLink(CalendarDisplayEvent event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

        String start = formatter.format(event.getStart().atZone(ZoneId.of(event.getTimezone())));
        String end = formatter.format(event.getEnd().atZone(ZoneId.of(event.getTimezone())));

        String details = "";

        switch (event.getType()) {
            case "assignment":
                details = "Reminder: Assignment deadline is approaching.";
                break;
            case "personal":
                details = "Reminder: Upcoming personal event.";
                break;
            case "course":
                details = "Reminder: Scheduled course session.";
                break;
            default:
                details = "Reminder: Upcoming calendar event.";
        }


        return String.format("https://www.google.com/calendar/render?action=TEMPLATE" +
                        "&text=%s&dates=%s/%s&details=%s&ctz=%s",
                URLEncoder.encode(event.getTitle(), StandardCharsets.UTF_8),
                start,
                end,
                URLEncoder.encode(details, StandardCharsets.UTF_8),
                URLEncoder.encode(event.getTimezone(), StandardCharsets.UTF_8));

    }








}
