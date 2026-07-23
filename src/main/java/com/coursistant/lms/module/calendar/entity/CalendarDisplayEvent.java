package com.coursistant.lms.module.calendar.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门信息表
 * Department Information Sheet
*/
public class CalendarDisplayEvent implements Serializable {
    private static final long serialVersionUID = 1L;


    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private Boolean allDay;
    private String type;      // "course" or "personal"
    private String timezone;
    private Integer sourceId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public Boolean getAllDay() {
        return allDay;
    }

    public void setAllDay(Boolean allDay) {
        this.allDay = allDay;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }



    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}