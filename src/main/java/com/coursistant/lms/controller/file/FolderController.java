package com.coursistant.lms.controller.file;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.Announcement;
import com.coursistant.lms.entity.DTO.FolderDTO;
import com.coursistant.lms.entity.Folder;
import com.coursistant.lms.service.file.FolderService;
import com.coursistant.lms.service.interaction.AnnouncementService;
import com.coursistant.lms.utils.TimeZoneUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.util.List;
import java.util.logging.Logger;

/**
 * Folder 前端操作接口
 * Folder frontend operation API
 */
@RestController
@RequestMapping("/folder")
public class FolderController {

    @Resource
    private FolderService folderService;

    private static final Logger logger = Logger.getLogger(FolderController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    @PostMapping("/add")
    public Result add(@RequestBody Folder folder) {
        logRequest("add", folder.toString());
        folderService.add(folder);
        logResponse("add", "Success");
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        folderService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        folderService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody Folder folder) {
        logRequest("update", folder.toString());
        folderService.updateById(folder);
        logResponse("update", "Success");
        return Result.success();
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        Folder folder = folderService.selectById(id);
        logResponse("selectById", folder.toString());
        return Result.success(folder);
    }

    @GetMapping("/selectAll")
    public Result selectAll(Folder folder) {
        logRequest("selectAll", folder != null ? folder.toString() : "null");
        List<Folder> list = folderService.selectAll(folder);
        logResponse("selectAll", null);
        return Result.success(list);
    }

    @GetMapping("/selectByCourseIdWithItems/{courseId}")
    public Result selectByCourseIdWithItems(@PathVariable Integer courseId) {
        logRequest("selectByCourseIdWithItems", courseId.toString());
        List<FolderDTO> list = folderService.getFoldersWithItemsByCourseId(courseId);
        logResponse("selectByCourseIdWithItems", null);
        return Result.success(list);
    }

}