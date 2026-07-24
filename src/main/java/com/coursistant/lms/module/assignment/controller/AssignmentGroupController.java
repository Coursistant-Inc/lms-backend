package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.module.assignment.entity.AssignmentGroup;
import com.coursistant.lms.module.assignment.service.AssignmentGroupService;
import com.coursistant.lms.shared.security.TokenUtils;
import com.coursistant.lms.module.user.account.entity.Account;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import com.coursistant.lms.module.assignment.entity.GroupMemberDetail;
import java.util.HashMap;
import com.coursistant.lms.shared.exception.CustomException;

/**
 * 小组分组控制器（用于作业）
* Controller for assignment groups
*/
@RestController
@RequestMapping("/grouping")
public class AssignmentGroupController {

    @Resource
    private AssignmentGroupService assignmentGroupService;

    // private static final Logger logger = Logger.getLogger(AssignmentGroupController.class.getName());

    @PostMapping("/add")
    public Result add(@RequestBody AssignmentGroup group) {
        assignmentGroupService.add(group);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody AssignmentGroup group) {
        assignmentGroupService.updateById(group);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        assignmentGroupService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        AssignmentGroup group = assignmentGroupService.selectById(id);
        return Result.success(group);
    }

    @GetMapping("/selectAll")
    public Result selectAll(AssignmentGroup group) {
        List<AssignmentGroup> list = assignmentGroupService.selectAll(group);
        return Result.success(list);
    }

    @GetMapping("/selectByAssignment/{assignmentId}")
    public Result selectByAssignment(@PathVariable Integer assignmentId) {
        List<AssignmentGroup> list = assignmentGroupService.selectByAssignmentId(assignmentId);
        return Result.success(list);
    }

    @GetMapping("/selectByCourse/{courseId}")
    public Result selectByCourse(@PathVariable Integer courseId) {
        List<AssignmentGroup> list = assignmentGroupService.selectByCourseId(courseId);
        return Result.success(list);
    }

    @GetMapping("/selectByCourseAndAssignment")
    public Result selectByCourseAndAssignment(@RequestParam Integer courseId, @RequestParam Integer assignmentId) {
        List<AssignmentGroup> list = assignmentGroupService.selectByCourseIdAndAssignmentId(courseId, assignmentId);
        return Result.success(list);
    }

    @GetMapping("/membersByCourseAndAssignment")
    public Result getMembersByCourseAndAssignment(@RequestParam Integer courseId, @RequestParam Integer assignmentId) {
        List<GroupMemberDetail> members = assignmentGroupService.getAllGroupMembersByCourseAndAssignment(courseId, assignmentId);
        return Result.success(members);
    }

    @GetMapping("/allMembersByCourseAndAssignment")
    public Result getAllMembersByCourseAndAssignment(@RequestParam Integer courseId, @RequestParam Integer assignmentId) {
        List<GroupMemberDetail> allMembers = assignmentGroupService.getAllMembersByCourseAndAssignment(courseId, assignmentId);
        return Result.success(allMembers);
    }

    @GetMapping("/membersByGroupId/{groupId}")
    public Result getMembersByGroupId(@PathVariable Integer groupId) {
        List<GroupMemberDetail> members = assignmentGroupService.getGroupMembersById(groupId);
        return Result.success(members);
    }

    @GetMapping("/groupsWithDetails")
    public Result getGroupsWithDetails(@RequestParam Integer courseId, @RequestParam Integer assignmentId) {
        List<AssignmentGroup> groups = assignmentGroupService.selectByCourseIdAndAssignmentId(courseId, assignmentId);
        
        // 为每个小组添加成员信息和待审批请求状态
        for (AssignmentGroup group : groups) {
            // 这里可以添加额外的逻辑来丰富返回数据
            // 比如成员数量、是否有待审批请求等
        }
        
        return Result.success(groups);
    }

    @GetMapping("/availableGroups")
    public Result getAvailableGroups(@RequestParam Integer courseId, @RequestParam Integer assignmentId) {
        List<AssignmentGroup> list = assignmentGroupService.getAvailableGroups(courseId, assignmentId);
        return Result.success(list);
    }

    @PostMapping("/autoGenerate")
    public Result autoGenerate(@RequestBody Map<String, Object> params) {
        Account loginUser = TokenUtils.getCurrentUser();

        if (!"Teacher".equalsIgnoreCase(loginUser.getLevel())) {
            return Result.error(ResultCodeEnum.INVALID_ACCESS_ERROR);
        }

        Integer assignmentId = (Integer) params.get("assignmentId");
        Integer groupSize = (Integer) params.get("groupSize");
        assignmentGroupService.autoGroup(assignmentId, groupSize);
        return Result.success("Auto grouping completed.");
    }

    @PostMapping("/createByStudent")
    public Result createByStudent(@RequestBody Map<String, Object> params) {
        Account loginUser = TokenUtils.getCurrentUser();
        
        if (!"Student".equalsIgnoreCase(loginUser.getLevel())) {
            return Result.error(ResultCodeEnum.INVALID_ACCESS_ERROR);
        }

        Integer assignmentId = (Integer) params.get("assignmentId");
        Integer courseId = (Integer) params.get("courseId");
        String groupName = (String) params.get("groupName");
        String joinMode = (String) params.get("joinMode");

        if (assignmentId == null || courseId == null || groupName == null || joinMode == null) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }

        assignmentGroupService.createGroupByStudent(assignmentId, courseId, loginUser.getId(), groupName, joinMode);
        return Result.success("Group created successfully.");
    }

    @PostMapping("/join")
    public Result joinGroup(@RequestBody Map<String, Object> params) {
        Account loginUser = TokenUtils.getCurrentUser();
        
        if (!"Student".equalsIgnoreCase(loginUser.getLevel())) {
            return Result.error(ResultCodeEnum.INVALID_ACCESS_ERROR);
        }

        Integer groupId = (Integer) params.get("groupId");
        Integer courseId = (Integer) params.get("courseId");
        Integer assignmentId = (Integer) params.get("assignmentId");

        if (groupId == null || courseId == null || assignmentId == null) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }

        String result = assignmentGroupService.joinGroup(groupId, loginUser.getId(), courseId, assignmentId);
        
        if ("JOINED_SUCCESSFULLY".equals(result)) {
            return Result.success("Joined group successfully.");
        } else if ("PENDING_APPROVAL".equals(result)) {
            return Result.success("Join request submitted. Please wait for approval.");
        } else {
            return Result.success("Operation completed.");
        }
    }

    @PostMapping("/leave")
    public Result leaveGroup(@RequestBody Map<String, Object> params) {
        Account loginUser = TokenUtils.getCurrentUser();
        
        if (!"Student".equalsIgnoreCase(loginUser.getLevel())) {
            return Result.error(ResultCodeEnum.INVALID_ACCESS_ERROR);
        }

        Integer groupId = (Integer) params.get("groupId");

        if (groupId == null) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }

        assignmentGroupService.leaveGroup(groupId, loginUser.getId());
        return Result.success("Left group successfully.");
    }

    @PostMapping("/debug/createTestData")
    public Result createTestData(@RequestBody Map<String, Object> params) {
        Integer courseId = (Integer) params.get("courseId");
        Integer assignmentId = (Integer) params.get("assignmentId");
        String groupName = (String) params.get("groupName");
        
        if (courseId == null || assignmentId == null || groupName == null) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        
        try {
            // 创建测试小组
            AssignmentGroup testGroup = new AssignmentGroup(assignmentId, courseId);
            testGroup.setGroupName(groupName);
            testGroup.setGroupStatus("active");
            testGroup.setJoinMode("free");
            
            assignmentGroupService.add(testGroup);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Test group created successfully");
            result.put("groupId", testGroup.getId());
            result.put("courseId", courseId);
            result.put("assignmentId", assignmentId);
            result.put("groupName", groupName);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(ResultCodeEnum.SYSTEM_ERROR, "Failed to create test data: " + e.getMessage());
        }
    }

    @GetMapping("/debug/checkDatabase")
    public Result checkDatabase(@RequestParam Integer courseId, @RequestParam Integer assignmentId) {
        try {
            // 1. 检查AssignmentGroup表中的所有数据
            List<AssignmentGroup> allGroups = assignmentGroupService.selectAll(null);
            
            // 2. 检查指定条件的数据
            List<AssignmentGroup> targetGroups = assignmentGroupService.selectByCourseIdAndAssignmentId(courseId, assignmentId);
            
            // 3. 检查参数类型和值
            Map<String, Object> result = new HashMap<>();
            result.put("courseId", courseId);
            result.put("assignmentId", assignmentId);
            result.put("courseIdType", courseId != null ? courseId.getClass().getSimpleName() : "null");
            result.put("assignmentIdType", assignmentId != null ? assignmentId.getClass().getSimpleName() : "null");
            result.put("totalGroupsInTable", allGroups.size());
            result.put("targetGroupsFound", targetGroups.size());
            result.put("allGroups", allGroups);
            result.put("targetGroups", targetGroups);
            
            return Result.success(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("errorType", e.getClass().getSimpleName());
            errorResult.put("courseId", courseId);
            errorResult.put("assignmentId", assignmentId);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR, errorResult);
        }
    }

    /**
     * 老师创建小组（支持Period-based分组）
     * API: /api/grouping/teacher/add
     * assignmentId: 可以是作业ID、周期ID或-1表示通用周期
     */
    @PostMapping("/teacher/add")
    public Result createGroupByTeacher(@RequestBody Map<String, Object> params) {
        Account loginUser = TokenUtils.getCurrentUser();
        
        // 验证用户是否为老师
        if (!"Teacher".equalsIgnoreCase(loginUser.getLevel())) {
            return Result.error(ResultCodeEnum.INVALID_ACCESS_ERROR);
        }
        
        // 获取参数
        Integer courseId = (Integer) params.get("courseId");
        Integer assignmentId = (Integer) params.get("assignmentId");
        String title = (String) params.get("title");
        Integer freetojoin = (Integer) params.get("freetojoin");
        String description = (String) params.get("description");
        String groupName = (String) params.get("group_name");
        Integer maxStudent = (Integer) params.get("max_student");
        
        // 验证必填参数
        if (courseId == null || assignmentId == null || groupName == null || maxStudent == null) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        
        try {
            // 调用服务层创建小组
            AssignmentGroup createdGroup = assignmentGroupService.createGroupByTeacher(
                courseId, assignmentId, title, freetojoin, description, groupName, maxStudent
            );
            
            // 返回创建成功的小组信息
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Group created successfully");
            result.put("groupId", createdGroup.getId());
            result.put("group", createdGroup);
            
            return Result.success(result);
        } catch (CustomException e) {
            return Result.error(e.getCode(), e.getMsg());
        } catch (Exception e) {
            return Result.error(ResultCodeEnum.SYSTEM_ERROR, "Failed to create group: " + e.getMessage());
        }
    }

    /**
     * 测试端点 - 验证路径映射是否工作
     */
    @GetMapping("/teacher/test")
    public Result testTeacherEndpoint() {
        return Result.success("Teacher endpoint is working!");
    }

    /**
     * 老师添加学生到小组（支持Period-based分组）
     * API: /api/grouping/teacher/addStudent
     * assignmentId: 可以是作业ID、周期ID或-1表示通用周期
     */
    @PostMapping("/teacher/addStudent")
    public Result addStudentToGroup(@RequestBody Map<String, Object> params) {
        Account loginUser = TokenUtils.getCurrentUser();
        
        // 验证用户是否为老师
        if (!"Teacher".equalsIgnoreCase(loginUser.getLevel())) {
            return Result.error(ResultCodeEnum.INVALID_ACCESS_ERROR);
        }
        
        // 获取参数
        Integer groupId = (Integer) params.get("groupId");
        Integer assignmentId = (Integer) params.get("assignmentId");
        Integer studentId = (Integer) params.get("studentId");
        
        // 验证必填参数
        if (groupId == null || assignmentId == null || studentId == null) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        
        try {
            // 调用服务层添加学生到小组
            Map<String, Object> result = assignmentGroupService.addStudentToGroupByTeacher(
                groupId, assignmentId, studentId
            );
            
            return Result.success(result);
        } catch (CustomException e) {
            return Result.error(e.getCode(), e.getMsg());
        } catch (Exception e) {
            return Result.error(ResultCodeEnum.SYSTEM_ERROR, "添加学生失败: " + e.getMessage());
        }
    }

    /**
     * 老师从小组中删除学生（支持Period-based分组）
     * API: /api/grouping/teacher/deleteStudent
     * assignmentId: 可以是作业ID、周期ID或-1表示通用周期
     */
    @DeleteMapping("/teacher/deleteStudent")
    public Result deleteStudentFromGroup(@RequestBody Map<String, Object> params) {
        Account loginUser = TokenUtils.getCurrentUser();
        
        // 验证用户是否为老师
        if (!"Teacher".equalsIgnoreCase(loginUser.getLevel())) {
            return Result.error(ResultCodeEnum.INVALID_ACCESS_ERROR);
        }
        
        // 获取参数
        Integer groupId = (Integer) params.get("groupId");
        Integer assignmentId = (Integer) params.get("assignmentId");
        Integer studentId = (Integer) params.get("studentId");
        
        // 验证必填参数
        if (groupId == null || assignmentId == null || studentId == null) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        
        try {
            // 调用服务层删除学生
            Map<String, Object> result = assignmentGroupService.deleteStudentFromGroupByTeacher(
                groupId, assignmentId, studentId
            );
            
            return Result.success(result);
        } catch (CustomException e) {
            return Result.error(e.getCode(), e.getMsg());
        } catch (Exception e) {
            return Result.error(ResultCodeEnum.SYSTEM_ERROR, "删除学生失败: " + e.getMessage());
        }
    }

    /**
     * 老师删除小组（支持Period-based分组）
     * API: /api/grouping/teacher/delete
     * assignmentId: 可以是作业ID、周期ID或-1表示通用周期
     */
    @DeleteMapping("/teacher/delete")
    public Result deleteGroup(@RequestBody Map<String, Object> params) {
        Account loginUser = TokenUtils.getCurrentUser();
        
        // 验证用户是否为老师
        if (!"Teacher".equalsIgnoreCase(loginUser.getLevel())) {
            return Result.error(ResultCodeEnum.INVALID_ACCESS_ERROR);
        }
        
        // 获取参数
        Integer groupId = (Integer) params.get("groupId");
        Integer assignmentId = (Integer) params.get("assignmentId");
        
        // 验证必填参数
        if (groupId == null || assignmentId == null) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        
        try {
            // 调用服务层删除小组
            Map<String, Object> result = assignmentGroupService.deleteGroupByTeacher(
                groupId, assignmentId
            );
            
            return Result.success(result);
        } catch (CustomException e) {
            return Result.error(e.getCode(), e.getMsg());
        } catch (Exception e) {
            return Result.error(ResultCodeEnum.SYSTEM_ERROR, "删除小组失败: " + e.getMessage());
        }
    }
}