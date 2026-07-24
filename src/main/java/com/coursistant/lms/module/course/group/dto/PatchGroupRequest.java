package com.coursistant.lms.module.course.group.dto;

public class PatchGroupRequest {
    private String name;
    private Integer capacityOverride;
    private Boolean clearCapacityOverride;
    private Boolean confirmCapacityShorten;

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

    public Boolean getClearCapacityOverride() {
        return clearCapacityOverride;
    }

    public void setClearCapacityOverride(Boolean clearCapacityOverride) {
        this.clearCapacityOverride = clearCapacityOverride;
    }

    public Boolean getConfirmCapacityShorten() {
        return confirmCapacityShorten;
    }

    public void setConfirmCapacityShorten(Boolean confirmCapacityShorten) {
        this.confirmCapacityShorten = confirmCapacityShorten;
    }
}
