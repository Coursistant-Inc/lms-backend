package com.coursistant.lms.module.course.group.dto;

import java.util.ArrayList;
import java.util.List;

public class GroupResponse {
    private Integer id;
    private Integer groupSetId;
    private String name;
    private Integer capacity;
    private Integer capacityOverride;
    private Integer memberCount;
    private List<MembershipResponse> members = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getGroupSetId() {
        return groupSetId;
    }

    public void setGroupSetId(Integer groupSetId) {
        this.groupSetId = groupSetId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getCapacityOverride() {
        return capacityOverride;
    }

    public void setCapacityOverride(Integer capacityOverride) {
        this.capacityOverride = capacityOverride;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public List<MembershipResponse> getMembers() {
        return members;
    }

    public void setMembers(List<MembershipResponse> members) {
        this.members = members != null ? members : new ArrayList<>();
    }
}
