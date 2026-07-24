package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.module.assignment.entity.GroupJoinRequest;
import com.coursistant.lms.module.assignment.service.GroupJoinRequestService;
import com.coursistant.lms.shared.security.TokenUtils;
import com.coursistant.lms.module.user.account.entity.Account;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 小组加入请求控制器
 * Controller for group join requests
 */
@RestController
@RequestMapping("/groupJoinRequest")
public class GroupJoinRequestController {

    @Resource
    private GroupJoinRequestService groupJoinRequestService;

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        GroupJoinRequest request = groupJoinRequestService.getRequestById(id);
        return Result.success(request);
    }

    @GetMapping("/selectByGroup/{groupId}")
    public Result selectByGroup(@PathVariable Integer groupId) {
        List<GroupJoinRequest> list = groupJoinRequestService.getPendingRequestsByGroupId(groupId);
        return Result.success(list);
    }

    @GetMapping("/selectByUser/{userId}")
    public Result selectByUser(@PathVariable Integer userId) {
        List<GroupJoinRequest> list = groupJoinRequestService.getRequestsByUserId(userId);
        return Result.success(list);
    }

    @GetMapping("/selectByAssignment/{assignmentId}")
    public Result selectByAssignment(@PathVariable Integer assignmentId) {
        List<GroupJoinRequest> list = groupJoinRequestService.getRequestsByAssignmentId(assignmentId);
        return Result.success(list);
    }

    @GetMapping("/selectAll")
    public Result selectAll(GroupJoinRequest request) {
        List<GroupJoinRequest> list = groupJoinRequestService.getAllRequests(request);
        return Result.success(list);
    }

    @PostMapping("/approve")
    public Result approveRequest(@RequestBody Map<String, Object> params) {
        Account loginUser = TokenUtils.getCurrentUser();

        Integer requestId = (Integer) params.get("requestId");

        if (requestId == null) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }

        groupJoinRequestService.approveJoinRequest(requestId, loginUser.getId());
        return Result.success("Join request approved successfully.");
    }

    @PostMapping("/reject")
    public Result rejectRequest(@RequestBody Map<String, Object> params) {
        Account loginUser = TokenUtils.getCurrentUser();

        Integer requestId = (Integer) params.get("requestId");

        if (requestId == null) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }

        groupJoinRequestService.rejectJoinRequest(requestId, loginUser.getId());
        return Result.success("Join request rejected successfully.");
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteRequest(@PathVariable Integer id) {
        Account loginUser = TokenUtils.getCurrentUser();
        groupJoinRequestService.deleteRequest(id, loginUser.getId());
        return Result.success("Request deleted successfully.");
    }

    @GetMapping("/pendingByGroup/{groupId}")
    public Result getPendingRequestsByGroup(@PathVariable Integer groupId) {
        List<GroupJoinRequest> list = groupJoinRequestService.getPendingRequestsByGroupId(groupId);
        return Result.success(list);
    }

    @GetMapping("/approvedByGroup/{groupId}")
    public Result getApprovedRequestsByGroup(@PathVariable Integer groupId) {
        List<GroupJoinRequest> list = groupJoinRequestService.getApprovedRequestsByGroupId(groupId);
        return Result.success(list);
    }

    @GetMapping("/rejectedByGroup/{groupId}")
    public Result getRejectedRequestsByGroup(@PathVariable Integer groupId) {
        List<GroupJoinRequest> list = groupJoinRequestService.getRejectedRequestsByGroupId(groupId);
        return Result.success(list);
    }

    @GetMapping("/allByGroup/{groupId}")
    public Result getAllRequestsByGroup(@PathVariable Integer groupId) {
        List<GroupJoinRequest> list = groupJoinRequestService.getAllRequestsByGroupId(groupId);
        return Result.success(list);
    }

    @GetMapping("/myRequests")
    public Result getMyRequests() {
        Account loginUser = TokenUtils.getCurrentUser();
        return Result.success(groupJoinRequestService.getRequestsByUserId(loginUser.getId()));
    }
}
