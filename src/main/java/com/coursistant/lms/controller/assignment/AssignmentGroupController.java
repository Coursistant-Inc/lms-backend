package com.coursistant.lms.controller.assignment;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.AssignmentGroup;
import com.coursistant.lms.service.assignment.AssignmentGroupService;
import com.coursistant.lms.annotation.RequiresPermission;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 小组分组控制器（用于作业）
* Controller for assignment groups
*/
@RestController
@RequestMapping("/assignmentGroup")
public class AssignmentGroupController {

    @Resource
    private AssignmentGroupService assignmentGroupService;


    // private static final Logger logger = Logger.getLogger(AssignmentGroupController.class.getName());

    @RequiresPermission("assignment:manage")
    @PostMapping("/add")
    public Result add(@RequestBody AssignmentGroup group) {
        assignmentGroupService.add(group);
        return Result.success();
    }

    @RequiresPermission("assignment:manage")
    @PutMapping("/update")
    public Result update(@RequestBody AssignmentGroup group) {
        assignmentGroupService.updateById(group);
        return Result.success();
    }

    @RequiresPermission("assignment:manage")
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        assignmentGroupService.deleteById(id);
        return Result.success();
    }

    @RequiresPermission("assignment:view")
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        AssignmentGroup group = assignmentGroupService.selectById(id);
        return Result.success(group);
    }

    @RequiresPermission("assignment:view")
    @GetMapping("/selectAll")
    public Result selectAll(AssignmentGroup group) {
        List<AssignmentGroup> list = assignmentGroupService.selectAll(group);
        return Result.success(list);
    }

    @RequiresPermission("assignment:view")
    @GetMapping("/selectByAssignment/{assignmentId}")
    public Result selectByAssignment(@PathVariable Integer assignmentId) {
        List<AssignmentGroup> list = assignmentGroupService.selectByAssignmentId(assignmentId);
        return Result.success(list);
    }

    @RequiresPermission("assignment:manage")
    @PostMapping("/autoGenerate")
    public Result autoGenerate(@RequestBody Map<String, Object> params) {
        com.coursistant.lms.entity.Account loginUser = com.coursistant.lms.utils.TokenUtils.getCurrentUser();

        if (!"Teacher".equalsIgnoreCase(loginUser.getLevel())) {
            return Result.error(ResultCodeEnum.INVALID_ACCESS_ERROR);
        }

        Integer assignmentId = (Integer) params.get("assignmentId");
        Integer groupSize = (Integer) params.get("groupSize");
        assignmentGroupService.autoGroup(assignmentId, groupSize);
        return Result.success("Auto grouping completed.");
    }


}