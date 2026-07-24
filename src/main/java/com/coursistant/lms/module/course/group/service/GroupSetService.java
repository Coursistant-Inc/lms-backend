package com.coursistant.lms.module.course.group.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.group.dto.BatchCreateGroupsRequest;
import com.coursistant.lms.module.course.group.dto.CreateGroupRequest;
import com.coursistant.lms.module.course.group.dto.CreateGroupSetRequest;
import com.coursistant.lms.module.course.group.dto.GroupResponse;
import com.coursistant.lms.module.course.group.dto.GroupSetResponse;
import com.coursistant.lms.module.course.group.dto.PatchGroupRequest;
import com.coursistant.lms.module.course.group.dto.PatchGroupSetRequest;
import com.coursistant.lms.module.course.group.entity.CourseGroup;
import com.coursistant.lms.module.course.group.entity.GroupSet;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionMapper;
import com.coursistant.lms.module.course.group.repository.CourseGroupMapper;
import com.coursistant.lms.module.course.group.repository.GroupMembershipMapper;
import com.coursistant.lms.module.course.group.repository.GroupSetMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class GroupSetService {

    @Resource
    private GroupAccessService groupAccessService;

    @Resource
    private GroupSetMapper groupSetMapper;

    @Resource
    private CourseGroupMapper courseGroupMapper;

    @Resource
    private GroupMembershipMapper groupMembershipMapper;

    @Resource
    private GroupResponseAssembler groupResponseAssembler;

    @Resource
    private AssignmentMapper assignmentMapper;

    @Resource
    private AssignmentSubmissionMapper assignmentSubmissionMapper;

    @Transactional
    public GroupSetResponse createGroupSet(Integer courseId, Integer actorUserId, CreateGroupSetRequest request) {
        groupAccessService.requireCanManageGroupsWritable(courseId, actorUserId);
        validateCreate(request);

        GroupSet groupSet = new GroupSet();
        groupSet.setCourseId(courseId);
        groupSet.setName(request.getName().trim());
        groupSet.setDefaultCapacity(request.getDefaultCapacity());
        groupSet.setJoinOpensAt(request.getJoinOpensAt());
        groupSet.setJoinClosesAt(request.getJoinClosesAt());
        groupSet.setLocked(Boolean.TRUE.equals(request.getLocked()));
        groupSetMapper.insert(groupSet);
        return getGroupSet(courseId, groupSet.getId(), actorUserId);
    }

    public List<GroupSetResponse> listGroupSets(Integer courseId, Integer viewerUserId) {
        groupAccessService.requireCourseMember(courseId, viewerUserId);
        boolean manager = groupAccessService.isManager(courseId, viewerUserId);
        List<GroupSetResponse> responses = new ArrayList<>();
        for (GroupSet groupSet : groupSetMapper.selectByCourseId(courseId)) {
            responses.add(groupResponseAssembler.toGroupSetResponse(groupSet, viewerUserId, manager));
        }
        return responses;
    }

    public GroupSetResponse getGroupSet(Integer courseId, Integer groupSetId, Integer viewerUserId) {
        groupAccessService.requireCourseMember(courseId, viewerUserId);
        GroupSet groupSet = groupAccessService.requireGroupSetInCourse(courseId, groupSetId);
        boolean manager = groupAccessService.isManager(courseId, viewerUserId);
        return groupResponseAssembler.toGroupSetResponse(groupSet, viewerUserId, manager);
    }

    @Transactional
    public GroupSetResponse patchGroupSet(Integer courseId, Integer groupSetId, Integer actorUserId,
                                          PatchGroupSetRequest request) {
        groupAccessService.requireCanManageGroupsWritable(courseId, actorUserId);
        GroupSet groupSet = groupAccessService.requireGroupSetInCourse(courseId, groupSetId);
        if (request == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Request body is required");
        }

        if (StringUtils.hasText(request.getName())) {
            groupSet.setName(request.getName().trim());
        }
        if (request.getLocked() != null) {
            groupSet.setLocked(request.getLocked());
        }

        if (request.getDefaultCapacity() != null) {
            validateCapacity(request.getDefaultCapacity());
            if (request.getDefaultCapacity() < groupSet.getDefaultCapacity()) {
                int maxMembersUsingDefault = maxMembersOnDefaultCapacityGroups(groupSet);
                if (request.getDefaultCapacity() < maxMembersUsingDefault
                        && !Boolean.TRUE.equals(request.getConfirmCapacityShorten())) {
                    throw new ApiException(ErrorType.CONFLICT,
                            "Confirmation required to shorten default capacity below current membership");
                }
            }
            groupSet.setDefaultCapacity(request.getDefaultCapacity());
        }

        if (Boolean.TRUE.equals(request.getClearJoinOpensAt())) {
            groupSet.setJoinOpensAt(null);
        } else if (request.getJoinOpensAt() != null) {
            groupSet.setJoinOpensAt(request.getJoinOpensAt());
        }
        if (Boolean.TRUE.equals(request.getClearJoinClosesAt())) {
            if (groupSet.getJoinClosesAt() != null
                    && !Boolean.TRUE.equals(request.getConfirmWindowShorten())) {
                // clearing close is lengthening; ok without confirm
            }
            groupSet.setJoinClosesAt(null);
        } else if (request.getJoinClosesAt() != null) {
            if (groupSet.getJoinClosesAt() != null
                    && request.getJoinClosesAt().isBefore(groupSet.getJoinClosesAt())
                    && !Boolean.TRUE.equals(request.getConfirmWindowShorten())) {
                throw new ApiException(ErrorType.CONFLICT,
                        "Confirmation required to shorten join window");
            }
            groupSet.setJoinClosesAt(request.getJoinClosesAt());
        }

        validateWindowOrder(groupSet.getJoinOpensAt(), groupSet.getJoinClosesAt());
        groupSetMapper.updateAll(groupSet);
        GroupSetResponse response = getGroupSet(courseId, groupSetId, actorUserId);
        response.setCapacityShortenWarning(false);
        response.setWindowShortenWarning(false);
        return response;
    }

    @Transactional
    public void deleteGroupSet(Integer courseId, Integer groupSetId, Integer actorUserId) {
        groupAccessService.requireCanManageGroupsWritable(courseId, actorUserId);
        groupAccessService.requireGroupSetInCourse(courseId, groupSetId);
        if (assignmentMapper.countByGroupSetId(groupSetId) > 0) {
            throw new ApiException(ErrorType.GROUP_SET_IN_USE);
        }
        if (groupMembershipMapper.countByGroupSetId(groupSetId) > 0
                || courseGroupMapper.countByGroupSetId(groupSetId) > 0) {
            throw new ApiException(ErrorType.GROUP_SET_NOT_EMPTY);
        }
        groupSetMapper.deleteById(groupSetId);
    }

    @Transactional
    public GroupResponse createGroup(Integer courseId, Integer groupSetId, Integer actorUserId,
                                     CreateGroupRequest request) {
        groupAccessService.requireCanManageGroupsWritable(courseId, actorUserId);
        GroupSet groupSet = groupAccessService.requireGroupSetInCourse(courseId, groupSetId);
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Group name is required");
        }
        if (request.getCapacityOverride() != null) {
            validateCapacity(request.getCapacityOverride());
        }
        CourseGroup group = new CourseGroup();
        group.setGroupSetId(groupSetId);
        group.setCourseId(courseId);
        group.setName(request.getName().trim());
        group.setCapacityOverride(request.getCapacityOverride());
        courseGroupMapper.insert(group);
        return groupResponseAssembler.toGroupResponse(groupSet, courseGroupMapper.selectById(group.getId()), true);
    }

    @Transactional
    public List<GroupResponse> batchCreateGroups(Integer courseId, Integer groupSetId, Integer actorUserId,
                                                 BatchCreateGroupsRequest request) {
        groupAccessService.requireCanManageGroupsWritable(courseId, actorUserId);
        GroupSet groupSet = groupAccessService.requireGroupSetInCourse(courseId, groupSetId);
        if (request == null || request.getCount() == null || request.getCount() < 1 || request.getCount() > 200) {
            throw new ApiException(ErrorType.BAD_REQUEST, "count must be between 1 and 200");
        }
        String prefix = StringUtils.hasText(request.getNamePrefix()) ? request.getNamePrefix().trim() : "Group";
        int existing = courseGroupMapper.countByGroupSetId(groupSetId);
        List<GroupResponse> created = new ArrayList<>();
        for (int i = 1; i <= request.getCount(); i++) {
            CourseGroup group = new CourseGroup();
            group.setGroupSetId(groupSetId);
            group.setCourseId(courseId);
            group.setName(prefix + " " + (existing + i));
            group.setCapacityOverride(null);
            courseGroupMapper.insert(group);
            created.add(groupResponseAssembler.toGroupResponse(groupSet, group, true));
        }
        return created;
    }

    @Transactional
    public GroupResponse patchGroup(Integer courseId, Integer groupSetId, Integer groupId, Integer actorUserId,
                                    PatchGroupRequest request) {
        groupAccessService.requireCanManageGroupsWritable(courseId, actorUserId);
        GroupSet groupSet = groupAccessService.requireGroupSetInCourse(courseId, groupSetId);
        CourseGroup group = groupAccessService.requireGroupInSet(courseId, groupSetId, groupId);
        if (request == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Request body is required");
        }
        if (StringUtils.hasText(request.getName())) {
            group.setName(request.getName().trim());
        }
        Integer newOverride = group.getCapacityOverride();
        if (Boolean.TRUE.equals(request.getClearCapacityOverride())) {
            newOverride = null;
        } else if (request.getCapacityOverride() != null) {
            validateCapacity(request.getCapacityOverride());
            newOverride = request.getCapacityOverride();
        }
        int oldCap = groupResponseAssembler.effectiveCapacity(groupSet, group);
        group.setCapacityOverride(newOverride);
        int newCap = groupResponseAssembler.effectiveCapacity(groupSet, group);
        if (newCap < oldCap) {
            int members = groupMembershipMapper.countByGroupId(groupId);
            if (newCap < members && !Boolean.TRUE.equals(request.getConfirmCapacityShorten())) {
                throw new ApiException(ErrorType.CONFLICT,
                        "Confirmation required to shorten group capacity below current membership");
            }
        }
        courseGroupMapper.updateById(group);
        return groupResponseAssembler.toGroupResponse(groupSet, courseGroupMapper.selectById(groupId), true);
    }

    @Transactional
    public void deleteGroup(Integer courseId, Integer groupSetId, Integer groupId, Integer actorUserId) {
        groupAccessService.requireCanManageGroupsWritable(courseId, actorUserId);
        groupAccessService.requireGroupSetInCourse(courseId, groupSetId);
        groupAccessService.requireGroupInSet(courseId, groupSetId, groupId);
        if (assignmentSubmissionMapper.countByGroupId(groupId) > 0) {
            throw new ApiException(ErrorType.GROUP_HAS_SUBMISSIONS);
        }
        if (groupMembershipMapper.countByGroupId(groupId) > 0) {
            throw new ApiException(ErrorType.GROUP_NOT_EMPTY);
        }
        courseGroupMapper.deleteById(groupId);
    }

    private int maxMembersOnDefaultCapacityGroups(GroupSet groupSet) {
        int max = 0;
        for (CourseGroup group : courseGroupMapper.selectByGroupSetId(groupSet.getId())) {
            if (group.getCapacityOverride() != null) {
                continue;
            }
            max = Math.max(max, groupMembershipMapper.countByGroupId(group.getId()));
        }
        return max;
    }

    private void validateCreate(CreateGroupSetRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Group set name is required");
        }
        if (request.getDefaultCapacity() == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "defaultCapacity is required");
        }
        validateCapacity(request.getDefaultCapacity());
        validateWindowOrder(request.getJoinOpensAt(), request.getJoinClosesAt());
    }

    private void validateCapacity(int capacity) {
        if (capacity < 1 || capacity > 200) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Capacity must be between 1 and 200");
        }
    }

    private void validateWindowOrder(java.time.LocalDateTime opens, java.time.LocalDateTime closes) {
        if (opens != null && closes != null && closes.isBefore(opens)) {
            throw new ApiException(ErrorType.BAD_REQUEST, "joinClosesAt must be >= joinOpensAt");
        }
    }
}
