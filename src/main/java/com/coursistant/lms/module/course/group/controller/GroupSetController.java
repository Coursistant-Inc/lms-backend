package com.coursistant.lms.module.course.group.controller;

import com.coursistant.lms.module.course.group.dto.AssignMemberRequest;
import com.coursistant.lms.module.course.group.dto.BatchCreateGroupsRequest;
import com.coursistant.lms.module.course.group.dto.CreateGroupRequest;
import com.coursistant.lms.module.course.group.dto.CreateGroupSetRequest;
import com.coursistant.lms.module.course.group.dto.GroupResponse;
import com.coursistant.lms.module.course.group.dto.GroupSetResponse;
import com.coursistant.lms.module.course.group.dto.MembershipMutationResponse;
import com.coursistant.lms.module.course.group.dto.MembershipResponse;
import com.coursistant.lms.module.course.group.dto.MoveMemberRequest;
import com.coursistant.lms.module.course.group.dto.PatchGroupRequest;
import com.coursistant.lms.module.course.group.dto.PatchGroupSetRequest;
import com.coursistant.lms.module.course.group.dto.SwitchGroupRequest;
import com.coursistant.lms.module.course.group.dto.UngroupedStudentResponse;
import com.coursistant.lms.module.course.group.service.GroupMembershipService;
import com.coursistant.lms.module.course.group.service.GroupSetService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v2/courses/{courseId}/group-sets")
@Tag(name = "Groups", description = "Group sets, groups, and membership")
public class GroupSetController {

    @Resource
    private GroupSetService groupSetService;

    @Resource
    private GroupMembershipService groupMembershipService;

    @PostMapping
    @Operation(operationId = "courseGroupSetCreate", summary = "Create a group set")
    public ApiResponse<GroupSetResponse> create(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @RequestBody CreateGroupSetRequest body) {
        return ApiResponse.success(groupSetService.createGroupSet(courseId, currentUserId(request), body));
    }

    @GetMapping
    @Operation(operationId = "courseGroupSetList", summary = "List group sets for a course")
    public ApiResponse<List<GroupSetResponse>> list(HttpServletRequest request,
                                                    @PathVariable Integer courseId) {
        return ApiResponse.success(groupSetService.listGroupSets(courseId, currentUserId(request)));
    }

    @GetMapping("/{groupSetId}")
    @Operation(operationId = "courseGroupSetGet", summary = "Get a group set by id")
    public ApiResponse<GroupSetResponse> get(HttpServletRequest request,
                                             @PathVariable Integer courseId,
                                             @PathVariable Integer groupSetId) {
        return ApiResponse.success(groupSetService.getGroupSet(courseId, groupSetId, currentUserId(request)));
    }

    @PatchMapping("/{groupSetId}")
    @Operation(operationId = "courseGroupSetPatch", summary = "Patch a group set")
    public ApiResponse<GroupSetResponse> patch(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @PathVariable Integer groupSetId,
                                               @RequestBody PatchGroupSetRequest body) {
        return ApiResponse.success(groupSetService.patchGroupSet(courseId, groupSetId, currentUserId(request), body));
    }

    @DeleteMapping("/{groupSetId}")
    @Operation(operationId = "courseGroupSetDelete", summary = "Delete a group set")
    public ApiResponse<Void> delete(HttpServletRequest request,
                                    @PathVariable Integer courseId,
                                    @PathVariable Integer groupSetId) {
        groupSetService.deleteGroupSet(courseId, groupSetId, currentUserId(request));
        return ApiResponse.success();
    }

    @PostMapping("/{groupSetId}/groups")
    @Operation(operationId = "courseGroupCreate", summary = "Create a group in a set")
    public ApiResponse<GroupResponse> createGroup(HttpServletRequest request,
                                                  @PathVariable Integer courseId,
                                                  @PathVariable Integer groupSetId,
                                                  @RequestBody CreateGroupRequest body) {
        return ApiResponse.success(groupSetService.createGroup(courseId, groupSetId, currentUserId(request), body));
    }

    @PostMapping("/{groupSetId}/groups/batch")
    @Operation(operationId = "courseGroupBatchCreate", summary = "Batch create groups in a set")
    public ApiResponse<List<GroupResponse>> batchCreateGroups(HttpServletRequest request,
                                                              @PathVariable Integer courseId,
                                                              @PathVariable Integer groupSetId,
                                                              @RequestBody BatchCreateGroupsRequest body) {
        return ApiResponse.success(groupSetService.batchCreateGroups(courseId, groupSetId, currentUserId(request), body));
    }

    @PatchMapping("/{groupSetId}/groups/{groupId}")
    @Operation(operationId = "courseGroupPatch", summary = "Patch a group")
    public ApiResponse<GroupResponse> patchGroup(HttpServletRequest request,
                                                 @PathVariable Integer courseId,
                                                 @PathVariable Integer groupSetId,
                                                 @PathVariable Integer groupId,
                                                 @RequestBody PatchGroupRequest body) {
        return ApiResponse.success(
                groupSetService.patchGroup(courseId, groupSetId, groupId, currentUserId(request), body));
    }

    @DeleteMapping("/{groupSetId}/groups/{groupId}")
    @Operation(operationId = "courseGroupDelete", summary = "Delete a group")
    public ApiResponse<Void> deleteGroup(HttpServletRequest request,
                                         @PathVariable Integer courseId,
                                         @PathVariable Integer groupSetId,
                                         @PathVariable Integer groupId) {
        groupSetService.deleteGroup(courseId, groupSetId, groupId, currentUserId(request));
        return ApiResponse.success();
    }

    @PostMapping("/{groupSetId}/groups/{groupId}/join")
    @Operation(operationId = "courseGroupJoin", summary = "Join a group")
    public ApiResponse<MembershipMutationResponse> join(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer groupSetId,
                                                        @PathVariable Integer groupId) {
        return ApiResponse.success(
                groupMembershipService.join(courseId, groupSetId, groupId, currentUserId(request)));
    }

    @PostMapping("/{groupSetId}/groups/{groupId}/leave")
    @Operation(operationId = "courseGroupLeave", summary = "Leave a group")
    public ApiResponse<MembershipMutationResponse> leave(HttpServletRequest request,
                                                         @PathVariable Integer courseId,
                                                         @PathVariable Integer groupSetId,
                                                         @PathVariable Integer groupId) {
        return ApiResponse.success(
                groupMembershipService.leave(courseId, groupSetId, groupId, currentUserId(request)));
    }

    @PostMapping("/{groupSetId}/switch")
    @Operation(operationId = "courseGroupSwitch", summary = "Switch to another group in the set")
    public ApiResponse<MembershipMutationResponse> switchGroup(HttpServletRequest request,
                                                               @PathVariable Integer courseId,
                                                               @PathVariable Integer groupSetId,
                                                               @RequestBody SwitchGroupRequest body) {
        return ApiResponse.success(
                groupMembershipService.switchGroup(courseId, groupSetId, currentUserId(request), body));
    }

    @GetMapping("/{groupSetId}/ungrouped-students")
    @Operation(operationId = "courseGroupUngroupedStudents", summary = "List ungrouped students in a set")
    public ApiResponse<List<UngroupedStudentResponse>> ungrouped(HttpServletRequest request,
                                                                 @PathVariable Integer courseId,
                                                                 @PathVariable Integer groupSetId) {
        return ApiResponse.success(
                groupMembershipService.listUngrouped(courseId, groupSetId, currentUserId(request)));
    }

    @PostMapping("/{groupSetId}/groups/{groupId}/members")
    @Operation(operationId = "courseGroupMemberAssign", summary = "Assign a member to a group")
    public ApiResponse<MembershipMutationResponse> assign(HttpServletRequest request,
                                                          @PathVariable Integer courseId,
                                                          @PathVariable Integer groupSetId,
                                                          @PathVariable Integer groupId,
                                                          @RequestBody AssignMemberRequest body) {
        return ApiResponse.success(
                groupMembershipService.assign(courseId, groupSetId, groupId, currentUserId(request), body));
    }

    @PostMapping("/{groupSetId}/members/{userId}/move")
    @Operation(operationId = "courseGroupMemberMove", summary = "Move a member between groups")
    public ApiResponse<MembershipMutationResponse> move(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer groupSetId,
                                                        @PathVariable Integer userId,
                                                        @RequestBody MoveMemberRequest body) {
        return ApiResponse.success(
                groupMembershipService.move(courseId, groupSetId, userId, currentUserId(request), body));
    }

    @DeleteMapping("/{groupSetId}/groups/{groupId}/members/{userId}")
    @Operation(operationId = "courseGroupMemberRemove", summary = "Remove a member from a group")
    public ApiResponse<MembershipMutationResponse> remove(HttpServletRequest request,
                                                          @PathVariable Integer courseId,
                                                          @PathVariable Integer groupSetId,
                                                          @PathVariable Integer groupId,
                                                          @PathVariable Integer userId,
                                                          @RequestParam(required = false) Boolean confirmAcademicImpact) {
        return ApiResponse.success(groupMembershipService.remove(
                courseId, groupSetId, groupId, userId, currentUserId(request), confirmAcademicImpact));
    }

    @PostMapping("/{groupSetId}/distribute-random")
    @Operation(operationId = "courseGroupDistributeRandom", summary = "Randomly distribute ungrouped students")
    public ApiResponse<List<MembershipResponse>> distributeRandom(HttpServletRequest request,
                                                                  @PathVariable Integer courseId,
                                                                  @PathVariable Integer groupSetId) {
        return ApiResponse.success(
                groupMembershipService.distributeRandom(courseId, groupSetId, currentUserId(request)));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return (Integer) userId;
    }
}
