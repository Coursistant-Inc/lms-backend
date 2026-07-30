package com.coursistant.lms.module.course.group.service;

import com.coursistant.lms.module.assignment.service.AssignmentTimeSupport;
import com.coursistant.lms.module.course.group.dto.GroupResponse;
import com.coursistant.lms.module.course.group.dto.GroupSetResponse;
import com.coursistant.lms.module.course.group.dto.MembershipResponse;
import com.coursistant.lms.module.course.group.entity.CourseGroup;
import com.coursistant.lms.module.course.group.entity.GroupMembership;
import com.coursistant.lms.module.course.group.entity.GroupSet;
import com.coursistant.lms.module.course.group.repository.CourseGroupMapper;
import com.coursistant.lms.module.course.group.repository.GroupMembershipMapper;
import com.coursistant.lms.module.tenant.service.TenantTimezoneService;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GroupResponseAssembler {

    @Resource
    private CourseGroupMapper courseGroupMapper;

    @Resource
    private GroupMembershipMapper groupMembershipMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private GroupAccessService groupAccessService;

    @Resource
    private TenantTimezoneService tenantTimezoneService;

    @Resource
    private AssignmentTimeSupport assignmentTimeSupport;

    public int effectiveCapacity(GroupSet groupSet, CourseGroup group) {
        if (group.getCapacityOverride() != null) {
            return group.getCapacityOverride();
        }
        return groupSet.getDefaultCapacity();
    }

    public MembershipResponse toMembershipResponse(GroupMembership membership) {
        if (membership == null) {
            return null;
        }
        MembershipResponse response = new MembershipResponse();
        response.setGroupId(membership.getGroupId());
        response.setUserId(membership.getUserId());
        response.setJoinedAt(membership.getJoinedAt());
        response.setAddedByType(membership.getAddedByType());
        response.setAddedByUserId(membership.getAddedByUserId());
        User user = userMapper.selectById(membership.getUserId());
        if (user != null) {
            response.setDisplayName(user.getName());
        }
        return response;
    }

    public GroupResponse toGroupResponse(GroupSet groupSet, CourseGroup group, boolean includeMembers) {
        GroupResponse response = new GroupResponse();
        response.setId(group.getId());
        response.setGroupSetId(group.getGroupSetId());
        response.setName(group.getName());
        response.setCapacityOverride(group.getCapacityOverride());
        response.setCapacity(effectiveCapacity(groupSet, group));
        List<GroupMembership> memberships = groupMembershipMapper.selectByGroupId(group.getId());
        response.setMemberCount(memberships.size());
        if (includeMembers) {
            response.setMembers(memberships.stream().map(this::toMembershipResponse).collect(Collectors.toList()));
        } else {
            response.setMembers(new ArrayList<>());
        }
        return response;
    }

    public List<GroupResponse> toGroupResponses(GroupSet groupSet, boolean includeMembers) {
        List<CourseGroup> groups = courseGroupMapper.selectByGroupSetId(groupSet.getId());
        List<GroupResponse> responses = new ArrayList<>();
        for (CourseGroup group : groups) {
            responses.add(toGroupResponse(groupSet, group, includeMembers));
        }
        return responses;
    }

    /**
     * @param managerView staff sees full roster always
     * @param viewerUserId caller; used for myGroup and non-open student shaping
     */
    public GroupSetResponse toGroupSetResponse(GroupSet groupSet, Integer viewerUserId, boolean managerView) {
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(groupSet.getCourseId());
        GroupSetResponse response = new GroupSetResponse();
        response.setId(groupSet.getId());
        response.setCourseId(groupSet.getCourseId());
        response.setName(groupSet.getName());
        response.setDefaultCapacity(groupSet.getDefaultCapacity());
        response.setJoinOpensAtUtc(assignmentTimeSupport.toInstant(groupSet.getJoinOpensAt()));
        response.setJoinOpensAtLocal(assignmentTimeSupport.toZone(groupSet.getJoinOpensAt(), zone));
        response.setJoinClosesAtUtc(assignmentTimeSupport.toInstant(groupSet.getJoinClosesAt()));
        response.setJoinClosesAtLocal(assignmentTimeSupport.toZone(groupSet.getJoinClosesAt(), zone));
        response.setTimezone(zone.getId());
        response.setLocked(groupSet.getLocked());
        LocalDateTime nowUtc = assignmentTimeSupport.nowUtc();
        boolean open = groupAccessService.isStudentSelfServiceOpen(groupSet, nowUtc);
        response.setOpenForSelfService(open);

        GroupMembership mine = viewerUserId == null
                ? null
                : groupMembershipMapper.selectByGroupSetIdAndUserId(groupSet.getId(), viewerUserId);
        response.setMyGroup(toMembershipResponse(mine));

        if (managerView || open) {
            response.setGroups(toGroupResponses(groupSet, true));
        } else {
            // Non-open student: only myGroup; no other rosters
            response.setGroups(new ArrayList<>());
        }
        return response;
    }

    public Map<Integer, String> displayNames(List<Integer> userIds) {
        Map<Integer, String> map = new HashMap<>();
        if (userIds == null) {
            return map;
        }
        for (Integer userId : userIds) {
            if (userId == null || map.containsKey(userId)) {
                continue;
            }
            User user = userMapper.selectById(userId);
            if (user != null) {
                map.put(userId, user.getName());
            }
        }
        return map;
    }
}
