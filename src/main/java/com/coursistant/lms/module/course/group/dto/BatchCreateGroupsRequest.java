package com.coursistant.lms.module.course.group.dto;

public class BatchCreateGroupsRequest {
    private Integer count;
    private String namePrefix;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getNamePrefix() {
        return namePrefix;
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }
}
