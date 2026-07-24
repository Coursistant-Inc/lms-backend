package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.entity.AssignmentGroup;
import com.coursistant.lms.module.assignment.entity.GroupJoinRequest;
import com.coursistant.lms.module.assignment.entity.GroupMember;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.module.assignment.repository.AssignmentGroupMapper;
import com.coursistant.lms.module.assignment.repository.GroupJoinRequestMapper;
import com.coursistant.lms.module.assignment.repository.GroupMemberMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.module.user.account.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroupJoinRequestService {

    @Resource
    private GroupJoinRequestMapper groupJoinRequestMapper;

    @Resource
    private AssignmentGroupMapper assignmentGroupMapper;

    @Resource
    private GroupMemberMapper groupMemberMapper;

    @Resource
    private AssignmentMapper assignmentMapper;

    @Resource
    private UserMapper userMapper;

    /**
     * 创建加入请求
     */
    @Transactional
    public void createJoinRequest(Integer groupId, Integer userId, Integer courseId, Integer assignmentId) {
        // 检查小组是否存在
        AssignmentGroup group = assignmentGroupMapper.selectById(groupId);
        if (group == null) {
            throw new CustomException(ResultCodeEnum.GROUP_NOT_EXIST_ERROR);
        }

        // 检查用户是否已经在小组中
        GroupMember existingMember = groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if (existingMember != null) {
            throw new CustomException(ResultCodeEnum.DUPLICATED_GROUP_MEMBER_ERROR);
        }

        // 检查是否已有待处理的请求
        List<GroupJoinRequest> existingRequests = groupJoinRequestMapper.selectByGroupIdAndUserId(groupId, userId);
        for (GroupJoinRequest request : existingRequests) {
            if ("pending".equals(request.getStatus())) {
                throw new CustomException(ResultCodeEnum.DUPLICATED_JOIN_REQUEST_ERROR);
            }
        }

        // 创建加入请求
        GroupJoinRequest request = new GroupJoinRequest(groupId, userId, courseId, assignmentId);
        groupJoinRequestMapper.insert(request);
    }

    /**
     * 审批加入请求
     */
    @Transactional
    public void approveJoinRequest(Integer requestId, Integer approverId) {
        GroupJoinRequest request = groupJoinRequestMapper.selectById(requestId);
        if (request == null) {
            throw new CustomException(ResultCodeEnum.REQUEST_NOT_EXIST_ERROR);
        }

        if (!"pending".equals(request.getStatus())) {
            throw new CustomException(ResultCodeEnum.REQUEST_ALREADY_PROCESSED_ERROR);
        }

        // 检查审批者是否是该组的组员（任意组员都可以审批）
        GroupMember approver = groupMemberMapper.selectByGroupIdAndUserId(request.getGroupId(), approverId);
        if (approver == null) {
            throw new CustomException(ResultCodeEnum.NO_PERMISSION_ERROR);
        }

        // 更新请求状态
        request.setStatus("approved");
        request.setApproverId(approverId);
        request.setApproveTime(LocalDateTime.now());
        groupJoinRequestMapper.updateById(request);

        // 将用户添加到小组
        GroupMember newMember = new GroupMember();
        newMember.setGroupId(request.getGroupId());
        newMember.setCourseId(request.getCourseId());
        newMember.setUserId(request.getUserId());
        groupMemberMapper.insert(newMember);
    }

    /**
     * 拒绝加入请求
     */
    @Transactional
    public void rejectJoinRequest(Integer requestId, Integer approverId) {
        GroupJoinRequest request = groupJoinRequestMapper.selectById(requestId);
        if (request == null) {
            throw new CustomException(ResultCodeEnum.REQUEST_NOT_EXIST_ERROR);
        }

        if (!"pending".equals(request.getStatus())) {
            throw new CustomException(ResultCodeEnum.REQUEST_ALREADY_PROCESSED_ERROR);
        }

        // 检查审批者是否是该组的组员（任意组员都可以审批）
        GroupMember approver = groupMemberMapper.selectByGroupIdAndUserId(request.getGroupId(), approverId);
        if (approver == null) {
            throw new CustomException(ResultCodeEnum.NO_PERMISSION_ERROR);
        }

        // 更新请求状态
        request.setStatus("rejected");
        request.setApproverId(approverId);
        request.setApproveTime(LocalDateTime.now());
        groupJoinRequestMapper.updateById(request);
    }

    /**
     * 获取小组的待审批请求
     */
    public List<GroupJoinRequest> getPendingRequestsByGroupId(Integer groupId) {
        return groupJoinRequestMapper.selectByGroupId(groupId).stream()
                .filter(request -> "pending".equals(request.getStatus()))
                .toList();
    }

    /**
     * 获取用户的加入请求
     */
    public List<GroupJoinRequest> getRequestsByUserId(Integer userId) {
        return groupJoinRequestMapper.selectByUserId(userId);
    }

    /**
     * 获取作业的所有加入请求
     */
    public List<GroupJoinRequest> getRequestsByAssignmentId(Integer assignmentId) {
        return groupJoinRequestMapper.selectByAssignmentId(assignmentId);
    }

    /**
     * 删除加入请求
     */
    public void deleteRequest(Integer requestId, Integer userId) {
        GroupJoinRequest request = groupJoinRequestMapper.selectById(requestId);
        if (request == null) {
            throw new CustomException(ResultCodeEnum.REQUEST_NOT_EXIST_ERROR);
        }

        // 检查用户是否是该组的组员（只有组员才能删除该组的请求）
        GroupMember member = groupMemberMapper.selectByGroupIdAndUserId(request.getGroupId(), userId);
        if (member == null) {
            throw new CustomException(ResultCodeEnum.NO_PERMISSION_ERROR);
        }

        groupJoinRequestMapper.deleteById(requestId);
    }

    /**
     * 检查是否需要所有成员同意
     */
    public boolean requiresAllMembersApproval(Integer groupId) {
        AssignmentGroup group = assignmentGroupMapper.selectById(groupId);
        return group != null && "approval".equals(group.getJoinMode());
    }

    /**
     * 获取小组的所有成员数量
     */
    public int getGroupMemberCount(Integer groupId) {
        List<GroupMember> members = groupMemberMapper.selectByGroupId(groupId);
        return members.size();
    }

    /**
     * 根据ID获取请求
     */
    public GroupJoinRequest getRequestById(Integer requestId) {
        return groupJoinRequestMapper.selectById(requestId);
    }

    /**
     * 获取所有请求（可带筛选条件）
     */
    public List<GroupJoinRequest> getAllRequests(GroupJoinRequest request) {
        return groupJoinRequestMapper.selectAll(request);
    }

    /**
     * 获取小组的所有申请记录（包括所有状态）
     */
    public List<GroupJoinRequest> getAllRequestsByGroupId(Integer groupId) {
        return groupJoinRequestMapper.selectByGroupId(groupId);
    }

    /**
     * 获取小组的已审批请求
     */
    public List<GroupJoinRequest> getApprovedRequestsByGroupId(Integer groupId) {
        return groupJoinRequestMapper.selectByGroupId(groupId).stream()
                .filter(request -> "approved".equals(request.getStatus()))
                .toList();
    }

    /**
     * 获取小组的已拒绝请求
     */
    public List<GroupJoinRequest> getRejectedRequestsByGroupId(Integer groupId) {
        return groupJoinRequestMapper.selectByGroupId(groupId).stream()
                .filter(request -> "rejected".equals(request.getStatus()))
                .toList();
    }
}
