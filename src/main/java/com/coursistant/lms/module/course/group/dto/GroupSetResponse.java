package com.coursistant.lms.module.course.group.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GroupSetResponse {
    private Integer id;
    private Integer courseId;
    private String name;
    private Integer defaultCapacity;
    private Instant joinOpensAtUtc;
    private LocalDateTime joinOpensAtLocal;
    private Instant joinClosesAtUtc;
    private LocalDateTime joinClosesAtLocal;
    private String timezone;
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

    public Instant getJoinOpensAtUtc() {
        return joinOpensAtUtc;
    }

    public void setJoinOpensAtUtc(Instant joinOpensAtUtc) {
        this.joinOpensAtUtc = joinOpensAtUtc;
    }

    public LocalDateTime getJoinOpensAtLocal() {
        return joinOpensAtLocal;
    }

    public void setJoinOpensAtLocal(LocalDateTime joinOpensAtLocal) {
        this.joinOpensAtLocal = joinOpensAtLocal;
    }

    public Instant getJoinClosesAtUtc() {
        return joinClosesAtUtc;
    }

    public void setJoinClosesAtUtc(Instant joinClosesAtUtc) {
        this.joinClosesAtUtc = joinClosesAtUtc;
    }

    public LocalDateTime getJoinClosesAtLocal() {
        return joinClosesAtLocal;
    }

    public void setJoinClosesAtLocal(LocalDateTime joinClosesAtLocal) {
        this.joinClosesAtLocal = joinClosesAtLocal;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
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
