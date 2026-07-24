package com.coursistant.lms.module.course.group.service;

import com.coursistant.lms.module.course.group.dto.GroupResponse;
import com.coursistant.lms.module.course.group.dto.GroupSetResponse;
import com.coursistant.lms.module.course.group.dto.MembershipResponse;
import com.coursistant.lms.module.course.group.entity.CourseGroup;
import com.coursistant.lms.module.course.group.entity.GroupMembership;
import com.coursistant.lms.module.course.group.entity.GroupSet;
import com.coursistant.lms.module.course.group.repository.CourseGroupMapper;
import com.coursistant.lms.module.course.group.repository.GroupMembershipMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
        GroupSetResponse response = new GroupSetResponse();
        response.setId(groupSet.getId());
        response.setCourseId(groupSet.getCourseId());
        response.setName(groupSet.getName());
        response.setDefaultCapacity(groupSet.getDefaultCapacity());
        response.setJoinOpensAt(groupSet.getJoinOpensAt());
        response.setJoinClosesAt(groupSet.getJoinClosesAt());
        response.setLocked(groupSet.getLocked());
        boolean open = groupAccessService.isStudentSelfServiceOpen(groupSet, LocalDateTime.now());
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
