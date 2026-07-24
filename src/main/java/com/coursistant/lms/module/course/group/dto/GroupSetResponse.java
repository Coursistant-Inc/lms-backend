package com.coursistant.lms.module.course.group.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GroupSetResponse {
    private Integer id;
    private Integer courseId;
    private String name;
    private Integer defaultCapacity;
    private LocalDateTime joinOpensAt;
    private LocalDateTime joinClosesAt;
    private Boolean locked;
    private Boolean openForSelfService;
    private Boolean capacityShortenWarning;
    private Boolean windowShortenWarning;
    private MembershipResponse myGroup;
    private List<GroupResponse> groups = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDefaultCapacity() {
        return defaultCapacity;
    }

    public void setDefaultCapacity(Integer defaultCapacity) {
        this.defaultCapacity = defaultCapacity;
    }

    public LocalDateTime getJoinOpensAt() {
        return joinOpensAt;
    }

    public void setJoinOpensAt(LocalDateTime joinOpensAt) {
        this.joinOpensAt = joinOpensAt;
    }

    public LocalDateTime getJoinClosesAt() {
        return joinClosesAt;
    }

    public void setJoinClosesAt(LocalDateTime joinClosesAt) {
        this.joinClosesAt = joinClosesAt;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public Boolean getOpenForSelfService() {
        return openForSelfService;
    }

    public void setOpenForSelfService(Boolean openForSelfService) {
        this.openForSelfService = openForSelfService;
    }

    public Boolean getCapacityShortenWarning() {
        return capacityShortenWarning;
    }

    public void setCapacityShortenWarning(Boolean capacityShortenWarning) {
        this.capacityShortenWarning = capacityShortenWarning;
    }

    public Boolean getWindowShortenWarning() {
        return windowShortenWarning;
    }

    public void setWindowShortenWarning(Boolean windowShortenWarning) {
        this.windowShortenWarning = windowShortenWarning;
    }

    public MembershipResponse getMyGroup() {
        return myGroup;
    }

    public void setMyGroup(MembershipResponse myGroup) {
        this.myGroup = myGroup;
    }

    public List<GroupResponse> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupResponse> groups) {
        this.groups = groups != null ? groups : new ArrayList<>();
    }
}
