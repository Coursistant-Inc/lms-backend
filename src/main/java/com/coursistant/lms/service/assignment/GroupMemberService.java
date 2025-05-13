package com.coursistant.lms.service.assignment;
 
import com.coursistant.lms.entity.GroupMember;
import com.coursistant.lms.mapper.assignment.GroupMemberMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import com.coursistant.lms.entity.AssignmentGroup;
import com.coursistant.lms.entity.Assignment;

@Service
public class GroupMemberService {

    @Resource
    private GroupMemberMapper groupMemberMapper;

    @Resource
    private com.coursistant.lms.mapper.assignment.AssignmentGroupMapper assignmentGroupMapper;

    @Resource
    private com.coursistant.lms.mapper.assignment.AssignmentMapper assignmentMapper;

    @Resource
    private com.coursistant.lms.mapper.user.UserMapper userMapper;
    /**
     * 添加成员到小组
    */
    public void add(GroupMember member) {
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

    // public void addMemberByEmail(Integer groupId, String email) {
    //     com.coursistant.lms.entity.User user = userMapper.selectByEmail(email);
    //     if (user == null || !"STUDENT".equals(user.getLevel())) {
    //         throw new RuntimeException("User not found or not a student");
    //     }

    //     // 查 assignmentGroup → assignmentId
    //     AssignmentGroup group = assignmentGroupMapper.selectById(groupId);
    //     if (group == null) {
    //         throw new RuntimeException("Group not found");
    //     }

    //     Integer assignmentId = group.getAssignmentId();
    //     Integer courseId = group.getCourseId();

    //     // 查 assignment → groupSize
    //     Assignment assignment = assignmentMapper.selectById(assignmentId);

    //     if (Boolean.FALSE.equals(assignment.getIsGroup())) {
    //         throw new RuntimeException("Not a group assignment");
    //     }
    //     if (!"free".equalsIgnoreCase(assignment.getGroupMode())) {
    //         throw new RuntimeException("This assignment does not allow free grouping");
    //     }




    //     int groupSize = assignment.getGroupSize() != null ? assignment.getGroupSize() : 0;

    //     // 判断是否已在该 assignment 的任意 group 中
    //     List<AssignmentGroup> allGroups = assignmentGroupMapper.selectByAssignmentId(assignmentId);
    //     for (AssignmentGroup g : allGroups) {
    //         GroupMember exists = groupMemberMapper.selectByGroupIdAndUserId(g.getId(), user.getId());
    //         if (exists != null) {
    //             throw new RuntimeException("User is already in another group");
    //         }
    //     }

    //     // 判断当前 group 是否已满
    //     List<GroupMember> members = groupMemberMapper.selectByGroupId(groupId);
    //     if (members.size() >= groupSize) {
    //         throw new RuntimeException("Group is full");
    //     }

    //     // 添加成员
    //     GroupMember newMember = new GroupMember();
    //     newMember.setGroupId(groupId);
    //     newMember.setCourseId(courseId);
    //     newMember.setUserId(user.getId());
    //     groupMemberMapper.insert(newMember);
    // }

    public void deleteByGroupIdAndUserId(Integer groupId, Integer userId) {
        List<GroupMember> list = groupMemberMapper.selectByGroupId(groupId);
        for (GroupMember member : list) {
            if (member.getUserId().equals(userId)) {
                groupMemberMapper.deleteById(member.getId());
                return;
            }
        }
    }


}