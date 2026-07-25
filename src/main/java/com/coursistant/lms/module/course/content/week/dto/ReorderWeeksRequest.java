package com.coursistant.lms.module.course.content.week.dto;

import java.util.List;

public class ReorderWeeksRequest {
    private List<Integer> weekIds;

    public List<Integer> getWeekIds() {
        return weekIds;
    }

    public void setWeekIds(List<Integer> weekIds) {
        this.weekIds = weekIds;
    }
}
