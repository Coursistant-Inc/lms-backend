package com.coursistant.lms.module.assignment.service;
 
import com.coursistant.lms.module.assignment.entity.GroupMember;
import com.coursistant.lms.module.user.entity.User;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.module.assignment.repository.GroupMemberMapper;

import cn.hutool.core.util.ObjectUtil;

import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import com.coursistant.lms.module.assignment.entity.AssignmentGroup;
import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.repository.AssignmentGroupMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.user.repository.UserMapper;

@Service
public class GroupMemberService {

    @Resource
    private GroupMemberMapper groupMemberMapper;

    @Resource
    private com.coursistant.lms.module.assignment.repository.AssignmentGroupMapper assignmentGroupMapper;

    @Resource
    private com.coursistant.lms.module.assignment.repository.AssignmentMapper assignmentMapper;

    @Resource
    private com.coursistant.lms.module.user.repository.UserMapper userMapper;
    /**
     * 添加成员到小组
    */
    public void add(GroupMember member) {
        Integer groupId = member.getGroupId();
        AssignmentGroup group = assignmentGroupMapper.selectById(groupId);
        if (ObjectUtil.isNull(group)) {
            throw new CustomException(ResultCodeEnum.GROUP_NOT_EXIST_ERROR);
        }
        Integer assignmentId = group.getAssignmentId();
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (ObjectUtil.isNull(assignment)) {
            throw new CustomException(ResultCodeEnum.ASSIGNMENT_NOT_EXIST_ERROR);
        }
        checkGroupMemberLimit(groupId, assignment);
        groupMemberMapper.insert(member);
    }

    /**
     * 移除单个成员
    */
    public void deleteById(Integer id) {
        groupMemberMapper.deleteById(id);
    }

    /**
     * 移除整组成员
    */
    public void deleteByGroupId(Integer groupId) {
        groupMemberMapper.deleteByGroupId(groupId);
    }

    /**
     * 查某个小组的所有成员
    */
    public List<GroupMember> selectByGroupId(Integer groupId) {
        return groupMemberMapper.selectByGroupId(groupId);
    }

    /**
     * 查某小组是否已有该成员
    */
    public GroupMember selectByGroupIdAndUserId(Integer groupId, Integer userId) {
        return groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
    }

    public List<GroupMember> selectAll(GroupMember member) {
        // 如果缓存不存在，从数据库查询
        // If cache does not exist, query from database
        List<GroupMember> members = groupMemberMapper.selectAll(member);

        return members;
    }

    public void addMemberById(Integer groupId, Integer userId) {
        User user = userMapper.selectById(userId);
        if (ObjectUtil.isNull(user) || !"STUDENT".equals(user.getLevel())) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }

        // 查 assignmentGroup → assignmentId
        AssignmentGroup group = assignmentGroupMapper.selectById(groupId);
        if (ObjectUtil.isNull(group)) {
            throw new CustomException(ResultCodeEnum.GROUP_NOT_EXIST_ERROR);
        }

        Integer assignmentId = group.getAssignmentId();
        Integer courseId = group.getCourseId();

        // 查 assignment → groupSize
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (ObjectUtil.isNull(assignment)) {
            throw new CustomException(ResultCodeEnum.ASSIGNMENT_NOT_EXIST_ERROR);
        }

        if (!"free".equalsIgnoreCase(assignment.getGroupMode())) {
            throw new CustomException(ResultCodeEnum.GROUP_JOIN_INVALID_ERROR);
        }


        // 判断是否已在该 assignment 的任意 group 中
        List<AssignmentGroup> allGroups = assignmentGroupMapper.selectByAssignmentId(assignmentId);
        for (AssignmentGroup g : allGroups) {
            GroupMember exists = groupMemberMapper.selectByGroupIdAndUserId(g.getId(), user.getId());
            if (exists != null) {
                throw new CustomException(ResultCodeEnum.DUPLICATED_GROUP_MEMBER_ERROR);
            }
        }

        checkGroupMemberLimit(groupId, assignment);

        // 添加成员
        GroupMember newMember = new GroupMember();
        newMember.setGroupId(groupId);
        newMember.setCourseId(courseId);
        newMember.setUserId(user.getId());
        groupMemberMapper.insert(newMember);
    }

    public void deleteByGroupIdAndUserId(Integer groupId, Integer userId) {
        List<GroupMember> list = groupMemberMapper.selectByGroupId(groupId);
        for (GroupMember member : list) {
            if (member.getUserId().equals(userId)) {
                groupMemberMapper.deleteById(member.getId());
                return;
            }
        }
    }

    private void checkGroupMemberLimit(Integer groupId, Assignment assignment){
        int groupSize = assignment.getGroupSize();
        if (ObjectUtil.isEmpty(groupSize)){
            groupSize = 0;
        }
        // 判断当前 group 是否已满
        List<GroupMember> members = groupMemberMapper.selectByGroupId(groupId);
        if (members.size() >= groupSize) {
            throw new CustomException(ResultCodeEnum.GROUP_JOIN_INVALID_ERROR);
        }
    }


}