package com.coursistant.lms.controller.file;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.DTO.FolderDTO;
import com.coursistant.lms.entity.FileSummary;
import com.coursistant.lms.entity.Folder;
import com.coursistant.lms.entity.FolderItem;
import com.coursistant.lms.service.file.DiskFilesService;
import com.coursistant.lms.service.file.FolderItemService;
import com.coursistant.lms.service.file.FolderService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;

/**
 * FolderItem 前端操作接口
 * FolderItem frontend operation API
 */
@RestController
@RequestMapping("/courseContent")
public class FolderItemController {

    @Resource
    private FolderItemService folderItemService;

    @Resource
    private DiskFilesService diskFilesService;

    private static final Logger logger = Logger.getLogger(FolderItemController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    @PostMapping("/add")
    public Result add(@RequestBody FolderItem folderItem) {
        logRequest("add", folderItem.toString());
        folderItemService.add(folderItem);
        logResponse("add", "Success");
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        folderItemService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        folderItemService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody FolderItem folderItem) {
        logRequest("update", folderItem.toString());
        folderItemService.updateById(folderItem);
        logResponse("update", "Success");
        return Result.success();
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        FolderItem folderItem = folderItemService.selectById(id);
        logResponse("selectById", folderItem.toString());
        return Result.success(folderItem);
    }

    @GetMapping("/selectAll")
    public Result selectAll(FolderItem folderItem) {
        logRequest("selectAll", folderItem != null ? folderItem.toString() : "null");
        List<FolderItem> list = folderItemService.selectAll(folderItem);
        logResponse("selectAll", null);
        return Result.success(list);
    }

    @GetMapping("/selectByFolderId/{folderId}")
    public Result selectByFolderId(@PathVariable Integer folderId) {
        logRequest("selectByFolderId", folderId.toString());
        List<FolderItem> list = folderItemService.selectByFolderId(folderId);
        logResponse("selectByFolderId", null);
        return Result.success(list);
    }

    @DeleteMapping("/deleteByFolderId/{folderId}")
    public Result deleteByFolderId(@PathVariable Integer folderId) {
        logRequest("deleteByFolderId", folderId.toString());
        folderItemService.deleteByFolderId(folderId);
        logResponse("deleteByFolderId", "Success");
        return Result.success();
    }


    /**
     * Upload a file and create a FolderItem entry (type = file)
     */
    @PostMapping("/addWithFile")
    public Result addWithFile(@RequestParam("file") MultipartFile file,
                              @RequestParam("folderId") Integer folderId,
                              @RequestParam("title") String title,
                              @RequestParam("category") String category,
                              @RequestParam("courseId") Integer courseId,
                              @RequestParam("userId") Integer userId,
                              @RequestParam("analysis") Integer analysis) {
        logRequest("addWithFile", file.getOriginalFilename());

        FileSummary summary = diskFilesService.add(file, courseId, userId, category, analysis);
        Integer fileId = summary.getId();

        FolderItem item = new FolderItem();
        item.setFolderId(folderId);
        item.setTitle(title != null && !title.isEmpty() ? title : file.getOriginalFilename());
        item.setType("file");
        item.setFileId(fileId);
        item.setUploadedBy(userId);

        folderItemService.add(item);
        logResponse("addWithFile", "Success");
        return Result.success();
    }

}