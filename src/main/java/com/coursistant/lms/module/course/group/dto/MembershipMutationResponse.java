package com.coursistant.lms.module.course.group.dto;

import java.util.ArrayList;
import java.util.List;

public class MembershipMutationResponse {
    private MembershipResponse membership;
    private GroupResponse group;
    private List<GroupResponse> groups = new ArrayList<>();

    public MembershipResponse getMembership() {
        return membership;
    }

    public void setMembership(MembershipResponse membership) {
        this.membership = membership;
    }

    public GroupResponse getGroup() {
        return group;
    }

    public void setGroup(GroupResponse group) {
        this.group = group;
    }

    public List<GroupResponse> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupResponse> groups) {
        this.groups = groups != null ? groups : new ArrayList<>();
    }
}
