package com.coursistant.lms.entity;

import java.io.Serializable;
import java.util.List;

/**
 * 作业小组详情DTO（包含成员信息）
 * DTO for assignment group with member details
 */
public class AssignmentGroupDetailDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 小组基本信息 */
    private AssignmentGroup group;
    
    /** 小组成员列表 */
    private List<GroupMemberDetail> members;
    
    /** 成员数量 */
    private Integer memberCount;
    
    /** 是否有待审批的加入请求 */
    private Boolean hasPendingRequests;

    public AssignmentGroupDetailDTO() {}

    public AssignmentGroupDetailDTO(AssignmentGroup group, List<GroupMemberDetail> members) {
        this.group = group;
        this.members = members;
        this.memberCount = members != null ? members.size() : 0;
    }

    public AssignmentGroup getGroup() {
        return group;
    }

    public void setGroup(AssignmentGroup group) {
        this.group = group;
    }

    public List<GroupMemberDetail> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMemberDetail> members) {
        this.members = members;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public Boolean getHasPendingRequests() {
        return hasPendingRequests;
    }

    public void setHasPendingRequests(Boolean hasPendingRequests) {
        this.hasPendingRequests = hasPendingRequests;
    }

    @Override
    public String toString() {
        return "AssignmentGroupDetailDTO{" +
                "group=" + group +
                ", members=" + members +
                ", memberCount=" + memberCount +
                ", hasPendingRequests=" + hasPendingRequests +
                '}';
    }
}
