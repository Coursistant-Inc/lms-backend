package com.coursistant.lms.module.course.group.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.course.group.dto.AssignMemberRequest;
import com.coursistant.lms.module.course.group.dto.GroupResponse;
import com.coursistant.lms.module.course.group.dto.MembershipMutationResponse;
import com.coursistant.lms.module.course.group.dto.MembershipResponse;
import com.coursistant.lms.module.course.group.dto.MoveMemberRequest;
import com.coursistant.lms.module.course.group.dto.SwitchGroupRequest;
import com.coursistant.lms.module.course.group.dto.UngroupedStudentResponse;
import com.coursistant.lms.module.course.group.entity.CourseGroup;
import com.coursistant.lms.module.course.group.entity.GroupMembership;
import com.coursistant.lms.module.course.group.entity.GroupMembershipAudit;
import com.coursistant.lms.module.course.group.entity.GroupSet;
import com.coursistant.lms.module.course.group.repository.CourseGroupMapper;
import com.coursistant.lms.module.course.group.repository.GroupMembershipMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GroupMembershipService {

    @Resource
    private GroupAccessService groupAccessService;

    @Resource
    private GroupResponseAssembler groupResponseAssembler;

    @Resource
    private GroupAuditService groupAuditService;

    @Resource
    private GroupNotificationService groupNotificationService;

    @Resource
    private CourseGroupMapper courseGroupMapper;

    @Resource
    private GroupMembershipMapper groupMembershipMapper;

    @Resource
    private EnrollmentMapper enrollmentMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private CoursePermissionService coursePermissionService;

    @Transactional
    public MembershipMutationResponse join(Integer courseId, Integer groupSetId, Integer groupId, Integer userId) {
        Course course = groupAccessService.requireCourseMember(courseId, userId);
        groupAccessService.requireNotArchived(course);
        if (!coursePermissionService.isStudent(courseId, userId)) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Only students may join groups");
        }
        GroupSet groupSet = groupAccessService.requireGroupSetInCourse(courseId, groupSetId);
        groupAccessService.assertStudentSelfServiceAllowed(groupSet);
        CourseGroup group = groupAccessService.requireGroupInSet(courseId, groupSetId, groupId);

        GroupMembership existing = groupMembershipMapper.selectByGroupSetIdAndUserId(groupSetId, userId);
        if (existing != null) {
            if (groupId.equals(existing.getGroupId())) {
                return mutationResponse(groupSet, existing, userId, false);
            }
            throw new ApiException(ErrorType.GROUP_ALREADY_IN_SET);
        }

        groupAccessService.assertNoAcademicHold(courseId, groupId);
        lockAndAssertStudentCapacity(course, groupSet, group, userId);

        GroupMembership membership = newMembership(courseId, groupSetId, groupId, userId,
                GroupMembership.ADDED_BY_SELF, userId);
        try {
            groupMembershipMapper.insert(membership);
        } catch (DuplicateKeyException e) {
            GroupMembership raced = groupMembershipMapper.selectByGroupSetIdAndUserId(groupSetId, userId);
            if (raced != null && groupId.equals(raced.getGroupId())) {
                return mutationResponse(groupSet, raced, userId, false);
            }
            throw new ApiException(ErrorType.GROUP_ALREADY_IN_SET);
        }

        writeAudit(course, membership, null, membership, GroupMembershipAudit.ACTOR_USER, userId,
                GroupMembershipAudit.JOIN_SELF, null);
        groupNotificationService.notifyMembershipChanged(course, GroupMembershipAudit.JOIN_SELF, userId, groupId);
        return mutationResponse(groupSet, membership, userId, false);
    }

    @Transactional
    public MembershipMutationResponse leave(Integer courseId, Integer groupSetId, Integer groupId, Integer userId) {
        Course course = groupAccessService.requireCourseMember(courseId, userId);
        groupAccessService.requireNotArchived(course);
        GroupSet groupSet = groupAccessService.requireGroupSetInCourse(courseId, groupSetId);
        groupAccessService.requireGroupInSet(courseId, groupSetId, groupId);

        GroupMembership existing = groupMembershipMapper.selectByGroupSetIdAndUserId(groupSetId, userId);
        if (existing == null || !groupId.equals(existing.getGroupId())) {
            // Idempotent leave: target state satisfied
            MembershipMutationResponse response = new MembershipMutationResponse();
            response.setMembership(null);
            response.setGroups(studentShapedGroups(groupSet, userId));
            return response;
        }

        groupAccessService.assertStudentSelfServiceAllowed(groupSet);
        groupAccessService.assertNoAcademicHold(courseId, groupId);

        GroupMembership before = copy(existing);
        groupMembershipMapper.deleteById(existing.getId());
        writeAudit(course, before, before, null, GroupMembershipAudit.ACTOR_USER, userId,
                GroupMembershipAudit.LEAVE_SELF, null);
        groupNotificationService.notifyMembershipChanged(course, GroupMembershipAudit.LEAVE_SELF, userId, groupId);

        MembershipMutationResponse response = new MembershipMutationResponse();
        response.setMembership(null);
        response.setGroups(studentShapedGroups(groupSet, userId));
        return response;
    }

    @Transactional
    public MembershipMutationResponse switchGroup(Integer courseId, Integer groupSetId, Integer userId,
                                                  SwitchGroupRequest request) {
        Course course = groupAccessService.requireCourseMember(courseId, userId);
        groupAccessService.requireNotArchived(course);
        if (!coursePermissionService.isStudent(courseId, userId)) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Only students may switch groups");
        }
        if (request == null || request.getTargetGroupId() == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "targetGroupId is required");
        }
        GroupSet groupSet = groupAccessService.requireGroupSetInCourse(courseId, groupSetId);
        groupAccessService.assertStudentSelfServiceAllowed(groupSet);
        CourseGroup target = groupAccessService.requireGroupInSet(courseId, groupSetId, request.getTargetGroupId());

        GroupMembership existing = groupMembershipMapper.selectByGroupSetIdAndUserId(groupSetId, userId);
        if (existing != null && request.getTargetGroupId().equals(existing.getGroupId())) {
            return mutationResponse(groupSet, existing, userId, false);
        }
        if (existing == null) {
            return join(courseId, groupSetId, request.getTargetGroupId(), userId);
        }

        groupAccessService.assertNoAcademicHold(courseId, existing.getGroupId());
        groupAccessService.assertNoAcademicHold(courseId, target.getId());
        lockAndAssertStudentCapacity(course, groupSet, target, userId);

        GroupMembership before = copy(existing);
        existing.setGroupId(target.getId());
        existing.setJoinedAt(LocalDateTime.now());
        existing.setAddedByType(GroupMembership.ADDED_BY_SELF);
        existing.setAddedByUserId(userId);
        groupMembershipMapper.updateById(existing);

        writeAudit(course, existing, before, existing, GroupMembershipAudit.ACTOR_USER, userId,
                GroupMembershipAudit.SWITCH_SELF, null);
        groupNotificationService.notifyMembershipChanged(course, GroupMembershipAudit.SWITCH_SELF, userId, target.getId());
        return mutationResponse(groupSet, existing, userId, false);
    }

    public List<UngroupedStudentResponse> listUngrouped(Integer courseId, Integer groupSetId, Integer actorUserId) {
        groupAccessService.requireCanManageGroups(courseId, actorUserId);
        groupAccessService.requireGroupSetInCourse(courseId, groupSetId);

        Set<Integer> grouped = groupMembershipMapper.selectByGroupSetId(groupSetId).stream()
                .map(GroupMembership::getUserId)
                .collect(Collectors.toSet());
        List<UngroupedStudentResponse> result = new ArrayList<>();
        for (Enrollment enrollment : enrollmentMapper.selectActiveStudentsByCourseId(courseId)) {
            if (grouped.contains(enrollment.getUserId())) {
                continue;
            }
            UngroupedStudentResponse item = new UngroupedStudentResponse();
            item.setUserId(enrollment.getUserId());
            User user = userMapper.selectById(enrollment.getUserId());
            if (user != null) {
                item.setDisplayName(user.getName());
            }
            result.add(item);
        }
        return result;
    }

    @Transactional
    public MembershipMutationResponse assign(Integer courseId, Integer groupSetId, Integer groupId,
                                             Integer actorUserId, AssignMemberRequest request) {
        Course course = groupAccessService.requireCanManageGroupsWritable(courseId, actorUserId);
        GroupSet groupSet = groupAccessService.requireGroupSetInCourse(courseId, groupSetId);
        CourseGroup group = groupAccessService.requireGroupInSet(courseId, groupSetId, groupId);
        if (request == null || request.getUserId() == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "userId is required");
        }
        requireActiveStudent(courseId, request.getUserId());

        GroupMembership existing = groupMembershipMapper.selectByGroupSetIdAndUserId(groupSetId, request.getUserId());
        if (existing != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("groupId", existing.getGroupId());
            throw new ApiException(ErrorType.GROUP_ALREADY_IN_SET,
                    ErrorType.GROUP_ALREADY_IN_SET.getDefaultMessage(), data);
        }

        assertStaffConfirms(courseId, groupId, groupSet, group, request.getConfirmCapacityOverfill(),
                request.getConfirmAcademicImpact(), true);

        GroupMembership membership = newMembership(courseId, groupSetId, groupId, request.getUserId(),
                GroupMembership.ADDED_BY_STAFF, actorUserId);
        try {
            groupMembershipMapper.insert(membership);
        } catch (DuplicateKeyException e) {
            GroupMembership raced = groupMembershipMapper.selectByGroupSetIdAndUserId(groupSetId, request.getUserId());
            Map<String, Object> data = new HashMap<>();
            if (raced != null) {
                data.put("groupId", raced.getGroupId());
            }
            throw new ApiException(ErrorType.GROUP_ALREADY_IN_SET,
                    ErrorType.GROUP_ALREADY_IN_SET.getDefaultMessage(), data);
        }

        writeAudit(course, membership, null, membership, GroupMembershipAudit.ACTOR_USER, actorUserId,
                GroupMembershipAudit.ASSIGN_STAFF,
                Map.of("confirmCapacityOverfill", Boolean.TRUE.equals(request.getConfirmCapacityOverfill())));
        groupNotificationService.notifyMembershipChanged(course, GroupMembershipAudit.ASSIGN_STAFF,
                request.getUserId(), groupId);
        return mutationResponse(groupSet, membership, actorUserId, true);
    }

    @Transactional
    public MembershipMutationResponse move(Integer courseId, Integer groupSetId, Integer userId,
                                           Integer actorUserId, MoveMemberRequest request) {
        Course course = groupAccessService.requireCanManageGroupsWritable(courseId, actorUserId);
        GroupSet groupSet = groupAccessService.requireGroupSetInCourse(courseId, groupSetId);
        if (request == null || request.getTargetGroupId() == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "targetGroupId is required");
        }
        CourseGroup target = groupAccessService.requireGroupInSet(courseId, groupSetId, request.getTargetGroupId());
        requireActiveStudent(courseId, userId);

        GroupMembership existing = groupMembershipMapper.selectByGroupSetIdAndUserId(groupSetId, userId);
        if (existing == null) {
            AssignMemberRequest assign = new AssignMemberRequest();
            assign.setUserId(userId);
            assign.setConfirmCapacityOverfill(request.getConfirmCapacityOverfill());
            assign.setConfirmAcademicImpact(request.getConfirmAcademicImpact());
            return assign(courseId, groupSetId, request.getTargetGroupId(), actorUserId, assign);
        }
        if (request.getTargetGroupId().equals(existing.getGroupId())) {
            return mutationResponse(groupSet, existing, actorUserId, true);
        }

        boolean academicHold = groupAccessService.hasAcademicHold(courseId, existing.getGroupId())
                || groupAccessService.hasAcademicHold(courseId, target.getId());
        if (academicHold && !Boolean.TRUE.equals(request.getConfirmAcademicImpact())) {
            throw new ApiException(ErrorType.GROUP_ACADEMIC_CONFIRM_REQUIRED);
        }
        assertStaffConfirms(courseId, target.getId(), groupSet, target, request.getConfirmCapacityOverfill(),
                true, true);

        GroupMembership before = copy(existing);
        existing.setGroupId(target.getId());
        existing.setJoinedAt(LocalDateTime.now());
        existing.setAddedByType(GroupMembership.ADDED_BY_STAFF);
        existing.setAddedByUserId(actorUserId);
        groupMembershipMapper.updateById(existing);

        writeAudit(course, existing, before, existing, GroupMembershipAudit.ACTOR_USER, actorUserId,
                GroupMembershipAudit.MOVE_STAFF, null);
        groupNotificationService.notifyMembershipChanged(course, GroupMembershipAudit.MOVE_STAFF, userId, target.getId());
        return mutationResponse(groupSet, existing, actorUserId, true);
    }

    @Transactional
    public MembershipMutationResponse remove(Integer courseId, Integer groupSetId, Integer groupId,
                                             Integer targetUserId, Integer actorUserId,
                                             Boolean confirmAcademicImpact) {
        Course course = groupAccessService.requireCanManageGroupsWritable(courseId, actorUserId);
        GroupSet groupSet = groupAccessService.requireGroupSetInCourse(courseId, groupSetId);
        groupAccessService.requireGroupInSet(courseId, groupSetId, groupId);

        GroupMembership existing = groupMembershipMapper.selectByGroupSetIdAndUserId(groupSetId, targetUserId);
        if (existing == null || !groupId.equals(existing.getGroupId())) {
            MembershipMutationResponse response = new MembershipMutationResponse();
            response.setMembership(null);
            response.setGroups(groupResponseAssembler.toGroupResponses(groupSet, true));
            return response;
        }

        if (groupAccessService.hasAcademicHold(courseId, groupId)
                && !Boolean.TRUE.equals(confirmAcademicImpact)) {
            throw new ApiException(ErrorType.GROUP_ACADEMIC_CONFIRM_REQUIRED);
        }

        GroupMembership before = copy(existing);
        groupMembershipMapper.deleteById(existing.getId());
        writeAudit(course, before, before, null, GroupMembershipAudit.ACTOR_USER, actorUserId,
                GroupMembershipAudit.REMOVE_STAFF, null);
        groupNotificationService.notifyMembershipChanged(course, GroupMembershipAudit.REMOVE_STAFF,
                targetUserId, groupId);

        MembershipMutationResponse response = new MembershipMutationResponse();
        response.setMembership(null);
        response.setGroups(groupResponseAssembler.toGroupResponses(groupSet, true));
        return response;
    }

    @Transactional
    public List<MembershipResponse> distributeRandom(Integer courseId, Integer groupSetId, Integer actorUserId) {
        Course course = groupAccessService.requireCanManageGroupsWritable(courseId, actorUserId);
        GroupSet groupSet = groupAccessService.requireGroupSetInCourse(courseId, groupSetId);

        List<UngroupedStudentResponse> ungrouped = listUngrouped(courseId, groupSetId, actorUserId);
        List<Integer> studentIds = ungrouped.stream().map(UngroupedStudentResponse::getUserId).collect(Collectors.toList());
        Collections.shuffle(studentIds);

        List<CourseGroup> groups = courseGroupMapper.selectByGroupSetId(groupSetId);
        if (groups.isEmpty()) {
            throw new ApiException(ErrorType.BAD_REQUEST, "No groups available for distribution");
        }

        String batchId = UUID.randomUUID().toString();
        List<MembershipResponse> assigned = new ArrayList<>();
        int groupIndex = 0;
        for (Integer studentId : studentIds) {
            CourseGroup placed = null;
            for (int attempt = 0; attempt < groups.size(); attempt++) {
                CourseGroup candidate = groups.get((groupIndex + attempt) % groups.size());
                courseGroupMapper.selectByIdForUpdate(candidate.getId());
                int count = groupMembershipMapper.countByGroupId(candidate.getId());
                int capacity = groupResponseAssembler.effectiveCapacity(groupSet, candidate);
                if (count < capacity) {
                    placed = candidate;
                    groupIndex = (groupIndex + attempt + 1) % groups.size();
                    break;
                }
            }
            if (placed == null) {
                break;
            }
            GroupMembership membership = newMembership(courseId, groupSetId, placed.getId(), studentId,
                    GroupMembership.ADDED_BY_STAFF, actorUserId);
            groupMembershipMapper.insert(membership);
            writeAudit(course, membership, null, membership, GroupMembershipAudit.ACTOR_USER, actorUserId,
                    GroupMembershipAudit.DISTRIBUTE_RANDOM, Map.of("batchId", batchId));
            groupNotificationService.notifyMembershipChanged(course, GroupMembershipAudit.DISTRIBUTE_RANDOM,
                    studentId, placed.getId());
            assigned.add(groupResponseAssembler.toMembershipResponse(membership));
        }
        return assigned;
    }

    /**
     * Ends all memberships for a user in a course after enrollment deactivation (same transaction).
     */
    @Transactional
    public void endGroupMembershipsOnEnrollmentDeactivated(Integer courseId, Integer userId,
                                                           String actorType, Integer actorId) {
        Course course = groupAccessService.requireCourse(courseId);
        List<GroupMembership> memberships = groupMembershipMapper.selectByCourseIdAndUserId(courseId, userId);
        if (memberships.isEmpty()) {
            return;
        }
        for (GroupMembership membership : memberships) {
            GroupMembership before = copy(membership);
            groupMembershipMapper.deleteById(membership.getId());
            writeAudit(course, before, before, null, actorType, actorId,
                    GroupMembershipAudit.END_ON_DROP, null);
        }
    }

    private void lockAndAssertStudentCapacity(Course course, GroupSet groupSet, CourseGroup group,
                                              Integer viewerUserId) {
        CourseGroup locked = courseGroupMapper.selectByIdForUpdate(group.getId());
        if (locked == null) {
            throw new ApiException(ErrorType.GROUP_NOT_FOUND);
        }
        int count = groupMembershipMapper.countByGroupId(locked.getId());
        int capacity = groupResponseAssembler.effectiveCapacity(groupSet, locked);
        if (count >= capacity) {
            Map<String, Object> data = new HashMap<>();
            data.put("groups", studentShapedGroups(groupSet, viewerUserId));
            throw new ApiException(ErrorType.GROUP_CAPACITY_FULL, "Group just filled up", data);
        }
    }

    private void assertStaffConfirms(Integer courseId, Integer groupId, GroupSet groupSet, CourseGroup group,
                                     Boolean confirmCapacityOverfill, Boolean confirmAcademicImpact,
                                     boolean lockGroup) {
        if (groupAccessService.hasAcademicHold(courseId, groupId)
                && !Boolean.TRUE.equals(confirmAcademicImpact)) {
            throw new ApiException(ErrorType.GROUP_ACADEMIC_CONFIRM_REQUIRED);
        }
        if (lockGroup) {
            courseGroupMapper.selectByIdForUpdate(group.getId());
        }
        int count = groupMembershipMapper.countByGroupId(group.getId());
        int capacity = groupResponseAssembler.effectiveCapacity(groupSet, group);
        if (count >= capacity && !Boolean.TRUE.equals(confirmCapacityOverfill)) {
            throw new ApiException(ErrorType.GROUP_CAPACITY_CONFIRM_REQUIRED);
        }
    }

    private void requireActiveStudent(Integer courseId, Integer userId) {
        Enrollment enrollment = enrollmentMapper.selectByCourseIdAndUserId(courseId, userId);
        if (enrollment == null || !Boolean.TRUE.equals(enrollment.getActive())) {
            throw new ApiException(ErrorType.ENROLLMENT_NOT_FOUND);
        }
        if (!CoursePermissionService.ROLE_STUDENT.equals(enrollment.getCourseRole())) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Only active students can be assigned to groups");
        }
    }

    private GroupMembership newMembership(Integer courseId, Integer groupSetId, Integer groupId,
                                          Integer userId, String addedByType, Integer addedByUserId) {
        GroupMembership membership = new GroupMembership();
        membership.setCourseId(courseId);
        membership.setGroupSetId(groupSetId);
        membership.setGroupId(groupId);
        membership.setUserId(userId);
        membership.setJoinedAt(LocalDateTime.now());
        membership.setAddedByType(addedByType);
        membership.setAddedByUserId(addedByUserId);
        return membership;
    }

    private MembershipMutationResponse mutationResponse(GroupSet groupSet, GroupMembership membership,
                                                        Integer viewerUserId, boolean managerView) {
        MembershipMutationResponse response = new MembershipMutationResponse();
        response.setMembership(groupResponseAssembler.toMembershipResponse(membership));
        CourseGroup group = courseGroupMapper.selectById(membership.getGroupId());
        if (group != null) {
            response.setGroup(groupResponseAssembler.toGroupResponse(groupSet, group, managerView || true));
        }
        if (managerView) {
            response.setGroups(groupResponseAssembler.toGroupResponses(groupSet, true));
        } else {
            response.setGroups(studentShapedGroups(groupSet, viewerUserId));
        }
        return response;
    }

    private List<GroupResponse> studentShapedGroups(GroupSet groupSet, Integer viewerUserId) {
        boolean open = groupAccessService.isStudentSelfServiceOpen(groupSet, LocalDateTime.now());
        if (open) {
            return groupResponseAssembler.toGroupResponses(groupSet, true);
        }
        return new ArrayList<>();
    }

    private void writeAudit(Course course, GroupMembership entityRef, GroupMembership before, GroupMembership after,
                            String actorType, Integer actorUserId, String action, Map<String, ?> detail) {
        Integer groupId = after != null ? after.getGroupId() : (before != null ? before.getGroupId() : null);
        Integer groupSetId = after != null ? after.getGroupSetId() : (before != null ? before.getGroupSetId() : null);
        Integer targetUserId = after != null ? after.getUserId() : (before != null ? before.getUserId() : null);
        groupAuditService.write(
                course.getTenantId(),
                course.getId(),
                groupSetId,
                groupId,
                targetUserId,
                actorType,
                actorUserId,
                action,
                before,
                after,
                detail);
    }

    private GroupMembership copy(GroupMembership source) {
        GroupMembership copy = new GroupMembership();
        copy.setId(source.getId());
        copy.setGroupId(source.getGroupId());
        copy.setGroupSetId(source.getGroupSetId());
        copy.setCourseId(source.getCourseId());
        copy.setUserId(source.getUserId());
        copy.setJoinedAt(source.getJoinedAt());
        copy.setAddedByType(source.getAddedByType());
        copy.setAddedByUserId(source.getAddedByUserId());
        return copy;
    }
}
