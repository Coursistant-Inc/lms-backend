package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.entity.AssignmentGroup;
import com.coursistant.lms.module.assignment.repository.AssignmentGroupMapper;

import cn.hutool.core.util.ObjectUtil;

import com.coursistant.lms.module.assignment.entity.GroupMember;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.shared.exception.CustomException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import com.coursistant.lms.module.assignment.entity.GroupMemberDetail;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.assignment.repository.GroupMemberMapper;
import com.coursistant.lms.module.user.account.repository.UserMapper;

@Service
public class AssignmentGroupService {

    @Resource
    private AssignmentGroupMapper assignmentGroupMapper;

    @Resource
    private com.coursistant.lms.module.assignment.repository.GroupMemberMapper groupMemberMapper;

    @Resource
    private com.coursistant.lms.module.assignment.repository.AssignmentMapper assignmentMapper;

    @Resource
    private com.coursistant.lms.module.user.account.repository.UserMapper userMapper;

    @Resource
    private GroupJoinRequestService groupJoinRequestService;

    /**
     * 新增小组
     */
    public void add(AssignmentGroup group) {
        assignmentGroupMapper.insert(group);
    }

    /**
     * 更新小组
     */
    public void updateById(AssignmentGroup group) {
        assignmentGroupMapper.updateById(group);
    }

    /**
     * 删除小组
    */
    public void deleteById(Integer id) {
        // 先删除小组成员
        groupMemberMapper.deleteByGroupId(id);
        // 再删除小组
        assignmentGroupMapper.deleteById(id);
    }

    /**
     * 查询小组
    */
    public AssignmentGroup selectById(Integer id) {
        return assignmentGroupMapper.selectById(id);
    }

    /**
     * 查询所有（可带条件）
    */
    public List<AssignmentGroup> selectAll(AssignmentGroup group) {
        return assignmentGroupMapper.selectAll(group);
    }

    /**
     * 查询某作业下的所有小组
    */
    public List<AssignmentGroup> selectByAssignmentId(Integer assignmentId) {
        return assignmentGroupMapper.selectByAssignmentId(assignmentId);
    }

    /**
     * 查询某课程下的所有小组
    */
    public List<AssignmentGroup> selectByCourseId(Integer courseId) {
        return assignmentGroupMapper.selectByCourseId(courseId);
    }

    /**
     * 查询某课程某作业下的所有小组
    */
    public List<AssignmentGroup> selectByCourseIdAndAssignmentId(Integer courseId, Integer assignmentId) {
        return assignmentGroupMapper.selectByCourseIdAndAssignmentId(courseId, assignmentId);
    }

    /**
     * 获取可加入的小组
    */
    public List<AssignmentGroup> getAvailableGroups(Integer courseId, Integer assignmentId) {
        return assignmentGroupMapper.selectAvailableGroups(courseId, assignmentId);
    }

    /**
     * 获取指定课程作业下的所有成员（包括没有小组的成员）
     */
    public List<GroupMemberDetail> getAllMembersByCourseAndAssignment(Integer courseId, Integer assignmentId) {
        List<GroupMemberDetail> allMembers = new ArrayList<>();
        
        // 1. 获取有小组的成员
        List<AssignmentGroup> groups = assignmentGroupMapper.selectByCourseIdAndAssignmentId(courseId, assignmentId);
        Set<Integer> groupedUserIds = new HashSet<>(); // 记录已有小组的用户ID
        
        for (AssignmentGroup group : groups) {
            List<GroupMember> members = groupMemberMapper.selectByGroupId(group.getId());
            for (GroupMember member : members) {
                User user = userMapper.selectById(member.getUserId());
                if (user != null) {
                    GroupMemberDetail memberDetail = new GroupMemberDetail(member, user);
                    allMembers.add(memberDetail);
                    groupedUserIds.add(member.getUserId()); // 记录已有小组的用户
                }
            }
        }
        
        // 2. 获取没有小组的成员
        List<User> allStudents = userMapper.selectStudentsByCourseId(courseId);
        for (User student : allStudents) {
            if (!groupedUserIds.contains(student.getId())) {
                // 创建没有小组的成员记录
                GroupMemberDetail memberDetail = new GroupMemberDetail();
                memberDetail.setId(null); // 没有GroupMember记录，所以ID为null
                memberDetail.setGroupId(null); // 没有小组，所以group_id为null
                memberDetail.setCourseId(courseId);
                memberDetail.setUserId(student.getId());
                memberDetail.setUsername(student.getUsername());
                memberDetail.setName(student.getName());
                memberDetail.setEmail(student.getEmail());
                memberDetail.setAvatar(com.coursistant.lms.module.user.profile.AvatarUrlBuilder.buildStatic(
                        student.getId(), student.getAvatar()));
                
                allMembers.add(memberDetail);
            }
        }
        
        return allMembers;
    }

    /**
     * 获取指定课程作业下所有小组成员（原有方法，保持兼容性）
     */
    public List<GroupMemberDetail> getAllGroupMembersByCourseAndAssignment(Integer courseId, Integer assignmentId) {
        List<GroupMemberDetail> allMembers = new ArrayList<>();
        
        // 获取有小组的成员
        List<AssignmentGroup> groups = assignmentGroupMapper.selectByCourseIdAndAssignmentId(courseId, assignmentId);
        for (AssignmentGroup group : groups) {
            List<GroupMember> members = groupMemberMapper.selectByGroupId(group.getId());
            for (GroupMember member : members) {
                User user = userMapper.selectById(member.getUserId());
                if (user != null) {
                    GroupMemberDetail memberDetail = new GroupMemberDetail(member, user);
                    allMembers.add(memberDetail);
                }
            }
        }
        
        return allMembers;
    }

    /**
     * 获取单个小组的所有成员
     */
    public List<GroupMemberDetail> getGroupMembersById(Integer groupId) {
        List<GroupMember> members = groupMemberMapper.selectByGroupId(groupId);
        List<GroupMemberDetail> memberDetails = new ArrayList<>();
        
        for (GroupMember member : members) {
            User user = userMapper.selectById(member.getUserId());
            GroupMemberDetail memberDetail = new GroupMemberDetail(member, user);
            memberDetails.add(memberDetail);
        }
        
        return memberDetails;
    }

    /**
     * 学员创建小组
    */
    @Transactional
    public void createGroupByStudent(Integer assignmentId, Integer courseId, Integer creatorId, String groupName, String joinMode) {
        // 检查作业是否存在
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (ObjectUtil.isNull(assignment)) {
            throw new CustomException(ResultCodeEnum.ASSIGNMENT_NOT_EXIST_ERROR);
        }

        // 检查是否允许学员创建小组
        if (!"free".equalsIgnoreCase(assignment.getGroupMode()) && !"mixed".equalsIgnoreCase(assignment.getGroupMode())) {
            throw new CustomException(ResultCodeEnum.NO_PERMISSION_ERROR);
        }

        // 检查用户是否已经在其他小组中
        List<AssignmentGroup> existingGroups = assignmentGroupMapper.selectByCourseIdAndAssignmentId(courseId, assignmentId);
        for (AssignmentGroup group : existingGroups) {
            GroupMember existingMember = groupMemberMapper.selectByGroupIdAndUserId(group.getId(), creatorId);
            if (existingMember != null) {
                throw new CustomException(ResultCodeEnum.DUPLICATED_GROUP_MEMBER_ERROR);
            }
        }

        // 创建小组
        AssignmentGroup newGroup = new AssignmentGroup(assignmentId, courseId);
        newGroup.setGroupName(groupName);
        newGroup.setJoinMode(joinMode);
        this.add(newGroup);

        // 将创建者添加为小组成员
        GroupMember creatorMember = new GroupMember();
        creatorMember.setGroupId(newGroup.getId());
        creatorMember.setCourseId(courseId);
        creatorMember.setUserId(creatorId);
        groupMemberMapper.insert(creatorMember);
    }

    /**
     * 学员加入小组
    */
    @Transactional
    public String joinGroup(Integer groupId, Integer userId, Integer courseId, Integer assignmentId) {
        // 检查小组是否存在
        AssignmentGroup group = assignmentGroupMapper.selectById(groupId);
        if (group == null) {
            throw new CustomException(ResultCodeEnum.GROUP_NOT_EXIST_ERROR);
        }

        // 检查小组状态
        if (!"active".equals(group.getGroupStatus())) {
            throw new CustomException(ResultCodeEnum.GROUP_JOIN_INVALID_ERROR);
        }

        // 检查用户是否已经在小组中
        GroupMember existingMember = groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if (existingMember != null) {
            throw new CustomException(ResultCodeEnum.DUPLICATED_GROUP_MEMBER_ERROR);
        }

        // 检查作业的小组人数限制
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment != null && assignment.getGroupSize() != null) {
            List<GroupMember> members = groupMemberMapper.selectByGroupId(groupId);
            if (members.size() >= assignment.getGroupSize()) {
                throw new CustomException(ResultCodeEnum.GROUP_JOIN_INVALID_ERROR);
            }
        }

        // 根据加入模式处理
        if ("free".equals(group.getJoinMode())) {
            // 直接加入
            GroupMember newMember = new GroupMember();
            newMember.setGroupId(groupId);
            newMember.setCourseId(courseId);
            newMember.setUserId(userId);
            groupMemberMapper.insert(newMember);
            return "JOINED_SUCCESSFULLY";
        } else if ("approval".equals(group.getJoinMode())) {
            // 创建加入请求
            groupJoinRequestService.createJoinRequest(groupId, userId, courseId, assignmentId);
            return "PENDING_APPROVAL";
        } else {
            throw new CustomException(ResultCodeEnum.GROUP_JOIN_INVALID_ERROR);
        }
    }

    /**
     * 学员退出小组
    */
    @Transactional
    public void leaveGroup(Integer groupId, Integer userId) {
        // 检查小组是否存在
        AssignmentGroup group = assignmentGroupMapper.selectById(groupId);
        if (group == null) {
            throw new CustomException(ResultCodeEnum.GROUP_NOT_EXIST_ERROR);
        }

        // 检查用户是否在小组中
        GroupMember member = groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if (member == null) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }

        // 由于没有组长概念，所有成员都可以退出
        // 这里可以添加其他退出限制逻辑

        // 退出小组
        groupMemberMapper.deleteById(member.getId());
    }

    /**
     * 自动分组功能
    */
    @Transactional
    public void autoGroup(Integer assignmentId, Integer groupSize) {

        // 清除旧分组数据
        List<AssignmentGroup> oldGroups = assignmentGroupMapper.selectByAssignmentId(assignmentId);
        for (AssignmentGroup group : oldGroups) {
            groupMemberMapper.deleteByGroupId(group.getId());     // 删除旧成员
            assignmentGroupMapper.deleteById(group.getId());       // 删除旧小组
        }

        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (ObjectUtil.isNull(assignment)) {
            throw new CustomException(ResultCodeEnum.ASSIGNMENT_NOT_EXIST_ERROR);
        }
        if (ObjectUtil.isEmpty(assignment.getCourseId())) {
            throw new CustomException(ResultCodeEnum.COURSE_NOT_EXIST_ERROR);
        }
        assignment.setGroupSize(groupSize);
        assignmentMapper.updateById(assignment);
        Integer courseId = assignment.getCourseId();

        // 构造条件：只查学生
        List<User> students = userMapper.selectStudentsByCourseId(courseId);

        java.util.Collections.shuffle(students); // 打乱顺序

        int index = 0;
        int total = students.size();
        int groupCount = (int) Math.ceil((double) total / groupSize);

        for (int i = 0; i < groupCount; i++) {
            AssignmentGroup group = new AssignmentGroup(assignmentId, courseId);
            group.setGroupName("Group " + (i + 1));
            // 系统创建的小组没有创建者概念
            group.setJoinMode("free"); // 自动分组的小组默认为自由加入
            this.add(group);  // 调用已有方法

            for (int j = 0; j < groupSize && index < total; j++, index++) {
                User student = students.get(index);
                GroupMember member = new GroupMember();
                member.setGroupId(group.getId());
                member.setCourseId(courseId);
                member.setUserId(student.getId());
                groupMemberMapper.insert(member);
            }
        }
    }

    /**
     * 老师创建小组（支持Period-based分组）
     * @param courseId 课程ID
     * @param assignmentId 周期ID（可以是作业ID、周期ID或-1表示通用周期）
     * @param title 小组标题
     * @param freetojoin 是否自由加入 (1=是, 0=否)
     * @param description 小组描述
     * @param groupName 小组名称
     * @param maxStudent 最大学生数量
     * @return 创建的小组信息
     */
    @Transactional
    public AssignmentGroup createGroupByTeacher(Integer courseId, Integer assignmentId, String title, 
                                             Integer freetojoin, String description, String groupName, 
                                             Integer maxStudent) {
        // 验证课程是否存在
        if (ObjectUtil.isNull(courseId)) {
            throw new CustomException(ResultCodeEnum.COURSE_NOT_EXIST_ERROR);
        }
        
        // 验证周期ID（assignmentId现在表示周期ID，支持-1作为通用周期）
        if (ObjectUtil.isNull(assignmentId)) {
            throw new CustomException("400", "周期ID不能为空");
        }
        
        // 验证最大学生数量
        if (ObjectUtil.isNull(maxStudent) || maxStudent <= 0) {
            throw new CustomException("400", "最大学生数量必须大于0");
        }
        
        // 创建小组对象
        AssignmentGroup group = new AssignmentGroup(assignmentId, courseId);
        group.setTitle(title);
        group.setGroupName(groupName);
        group.setDescription(description);
        group.setMaxStudent(maxStudent);
        group.setCurrStudentCount(0); // 默认当前学生数量为0
        group.setGroupStatus("active"); // 默认状态为active
        
        // 根据freetojoin设置joinMode
        if (freetojoin != null && freetojoin == 0) {
            group.setJoinMode("approval"); // 需要审批
        } else {
            group.setJoinMode("free"); // 自由加入
        }
        
        // 设置创建时间
        group.setCreateAt(LocalDateTime.now());
        
        // 保存到数据库
        assignmentGroupMapper.insert(group);
        
        return group;
    }

    /**
     * 老师添加学生到小组（支持Period-based分组）
     * @param groupId 小组ID
     * @param assignmentId 周期ID（可以是作业ID、周期ID或-1表示通用周期）
     * @param studentId 学生ID
     * @return 添加结果信息
     */
    @Transactional
    public Map<String, Object> addStudentToGroupByTeacher(Integer groupId, Integer assignmentId, Integer studentId) {
        // 验证小组是否存在
        AssignmentGroup group = assignmentGroupMapper.selectById(groupId);
        if (ObjectUtil.isNull(group)) {
            throw new CustomException("400", "小组不存在");
        }
        
        // 验证学生是否存在且是学生角色
        User student = userMapper.selectById(studentId);
        if (ObjectUtil.isNull(student) || !"STUDENT".equalsIgnoreCase(student.getLevel())) {
            throw new CustomException("400", "学生不存在或用户类型不正确");
        }
        
        // 验证小组是否属于该周期（支持-1作为通用周期）
        if (!assignmentId.equals(group.getAssignmentId())) {
            throw new CustomException("400", "小组不属于该周期");
        }
        
        // 检查学生是否已经在该小组中
        GroupMember existingMember = groupMemberMapper.selectByGroupIdAndUserId(groupId, studentId);
        if (existingMember != null) {
            throw new CustomException("400", "学生已经在该小组中");
        }
        
        // 检查学生是否已经在其他小组中（同一周期下，但-1作为通用周期时跳过此检查）
        if (assignmentId != -1) {
            List<AssignmentGroup> allGroups = assignmentGroupMapper.selectByAssignmentId(assignmentId);
            for (AssignmentGroup g : allGroups) {
                GroupMember exists = groupMemberMapper.selectByGroupIdAndUserId(g.getId(), studentId);
                if (exists != null) {
                    throw new CustomException("400", "学生已经在其他小组中");
                }
            }
        }
        
        // 检查当前学生人数是否超过最大上限
        if (group.getCurrStudentCount() >= group.getMaxStudent()) {
            throw new CustomException("400", "小组已达到最大人数上限，无法添加更多学生");
        }
        
        // 添加学生到小组
        GroupMember newMember = new GroupMember();
        newMember.setGroupId(groupId);
        newMember.setCourseId(group.getCourseId());
        newMember.setUserId(studentId);
        groupMemberMapper.insert(newMember);
        
        // 更新小组的当前学生数量
        group.setCurrStudentCount(group.getCurrStudentCount() + 1);
        assignmentGroupMapper.updateById(group);
        
        // 返回结果信息
        Map<String, Object> result = new HashMap<>();
        result.put("message", "学生添加成功");
        result.put("groupId", groupId);
        result.put("studentId", studentId);
        result.put("studentName", student.getName());
        result.put("newCurrStudentCount", group.getCurrStudentCount());
        result.put("maxStudent", group.getMaxStudent());
        
        return result;
        
        // 注意：如果是通用周期（assignmentId = -1），学生可以同时加入多个通用小组
        // 这为灵活的课程管理提供了可能
    }

    /**
     * 老师从小组中删除学生（支持Period-based分组）
     * @param groupId 小组ID
     * @param assignmentId 周期ID（可以是作业ID、周期ID或-1表示通用周期）
     * @param studentId 学生ID
     * @return 删除结果信息
     */
    @Transactional
    public Map<String, Object> deleteStudentFromGroupByTeacher(Integer groupId, Integer assignmentId, Integer studentId) {
        // 验证小组是否存在
        AssignmentGroup group = assignmentGroupMapper.selectById(groupId);
        if (ObjectUtil.isNull(group)) {
            throw new CustomException("400", "小组不存在");
        }
        
        // 验证小组是否属于该周期（支持-1作为通用周期）
        if (!assignmentId.equals(group.getAssignmentId())) {
            throw new CustomException("400", "小组不属于该周期");
        }
        
        // 检查学生是否在该小组中
        GroupMember existingMember = groupMemberMapper.selectByGroupIdAndUserId(groupId, studentId);
        if (existingMember == null) {
            throw new CustomException("400", "学生不在该小组中");
        }
        
        // 从小组中删除学生
        groupMemberMapper.deleteById(existingMember.getId());
        
        // 更新小组的当前学生数量
        group.setCurrStudentCount(group.getCurrStudentCount() - 1);
        assignmentGroupMapper.updateById(group);
        
        // 返回结果信息
        Map<String, Object> result = new HashMap<>();
        result.put("message", "学生删除成功");
        result.put("groupId", groupId);
        result.put("studentId", studentId);
        result.put("newCurrStudentCount", group.getCurrStudentCount());
        result.put("maxStudent", group.getMaxStudent());
        
        return result;
    }

    /**
     * 老师删除小组（支持Period-based分组）
     * @param groupId 小组ID
     * @param assignmentId 周期ID（可以是作业ID、周期ID或-1表示通用周期）
     * @return 删除结果信息
     */
    @Transactional
    public Map<String, Object> deleteGroupByTeacher(Integer groupId, Integer assignmentId) {
        // 验证小组是否存在
        AssignmentGroup group = assignmentGroupMapper.selectById(groupId);
        if (ObjectUtil.isNull(group)) {
            throw new CustomException("400", "小组不存在");
        }
        
        // 验证小组是否属于该周期（支持-1作为通用周期）
        if (!assignmentId.equals(group.getAssignmentId())) {
            throw new CustomException("400", "小组不属于该周期");
        }
        
        // 获取小组信息用于返回
        String groupName = group.getGroupName();
        Integer courseId = group.getCourseId();
        
        // 先删除小组成员
        groupMemberMapper.deleteByGroupId(groupId);
        
        // 再删除小组
        assignmentGroupMapper.deleteById(groupId);
        
        // 返回结果信息
        Map<String, Object> result = new HashMap<>();
        result.put("message", "小组删除成功");
        result.put("groupId", groupId);
        result.put("groupName", groupName);
        result.put("courseId", courseId);
        result.put("assignmentId", assignmentId);
        
        return result;
    }
}
