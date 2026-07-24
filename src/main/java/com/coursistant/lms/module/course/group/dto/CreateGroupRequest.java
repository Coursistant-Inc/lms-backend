package com.coursistant.lms.module.course.group.dto;

public class CreateGroupRequest {
    private String name;
    private Integer capacityOverride;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacityOverride() {
        return capacityOverride;
    }

    public void setCapacityOverride(Integer capacityOverride) {
        this.capacityOverride = capacityOverride;
    }
}
