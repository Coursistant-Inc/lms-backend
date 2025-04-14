package com.coursistant.individual.controller.file;

import com.coursistant.individual.common.Result;
import com.coursistant.individual.common.enums.ResultCodeEnum;
import com.coursistant.individual.entity.DiskFiles;
import com.coursistant.individual.entity.FileSummary;
import com.coursistant.individual.exception.CustomException;
import com.coursistant.individual.service.file.DiskFilesService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;

/**
 * 磁盘文件前端操作接口
 * Disk files frontend operation API
 **/
@RestController
@RequestMapping("/diskFiles")
public class DiskFilesController {

    @Resource
    private DiskFilesService diskFilesService;

    private static final Logger logger = Logger.getLogger(DiskFilesController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 上传文件
     * Upload a file
     */
    @PostMapping("/add")
    public Result add(@RequestParam("files") MultipartFile[] files,
                   @RequestParam("courseName") String courseName,
                   @RequestParam("userId") Integer userId,
                   @RequestParam("categories") String[] categories,
                   @RequestParam("analysis") Integer analysis) {

        if (files.length != categories.length) {
            throw new CustomException(ResultCodeEnum.FILE_CATEGORY_MISMATCH);
        }
        if (files.length==1){
            logRequest("add", String.format("fileName=%s, courseName=%s, userId=%d, category=%s, analysis=%d",
                files[0].getOriginalFilename(), courseName, userId, categories[0],analysis));
            FileSummary summary=diskFilesService.add(files[0], courseName, userId, categories[0],analysis);

            logResponse("add", "Success");
        
            return Result.success(summary);           
        }
        else{
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                String category = categories[i];
                       
                logRequest("add", String.format("fileName=%s, courseName=%s, userId=%d, category=%s, analysis=%d",
                        file.getOriginalFilename(), courseName, userId, category, analysis));
        
                diskFilesService.add(file, courseName, userId, category, 0);

                logResponse("add", "Success");
            }

            return Result.success(); 
        }    
    }

    /**
     * 覆盖上传文件
     * Overwrite an existing file
     */
    @PostMapping("/overwrite")
    public Result overwrite(MultipartFile file, String courseName, Integer userId) {
        logRequest("overwrite", String.format("fileName=%s, courseName=%s, userId=%d",
                file.getOriginalFilename(), courseName, userId));
        diskFilesService.overwrite(file, courseName, userId);
        logResponse("overwrite", "Success");
        return Result.success();
    }

    /**
     * Hadoop 处理文件
     * Process a file with Hadoop
     */
    @PostMapping("/hadooped")
    public Result hadooped(String path, String hadoopPath, String time) {
        logRequest("hadooped", String.format("path=%s, hadoopPath=%s, time=%s", path, hadoopPath, time));
        diskFilesService.hadooped(path, hadoopPath, time);
        logResponse("hadooped", "Success");
        return Result.success();
    }

    /**
     * Qdrant 处理文件
     * Process a file with Qdrant
     */
    @PostMapping("/qdranted")
    public Result qdranted(String path, String time) {
        logRequest("qdranted", String.format("path=%s, time=%s", path, time));
        diskFilesService.qdranted(path, time);
        logResponse("qdranted", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 删除文件
     * Delete a file by ID
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        diskFilesService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 深度删除文件
     * Deep delete a file by ID
     */
    @DeleteMapping("/deepDelete/{id}")
    public Result deepDelete(@PathVariable Integer id) {
        logRequest("deepDelete", id.toString());
        diskFilesService.deepDelete(id);
        logResponse("deepDelete", "Success");
        return Result.success();
    }

    /**
     * 批量删除文件
     * Batch delete files
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        diskFilesService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    /**
     * 更新文件信息
     * Update file information
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody DiskFiles diskFiles) {
        logRequest("updateById", diskFiles.toString());
        diskFilesService.updateById(diskFiles);
        logResponse("updateById", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 查询文件
     * Query a file by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        DiskFiles diskFiles = diskFilesService.selectById(id);
        logResponse("selectById", diskFiles.toString());
        return Result.success(diskFiles);
    }

    /**
     * 查询所有文件
     * Query all files
     */
    @GetMapping("/selectAll")
    public Result selectAll(DiskFiles diskFiles) {
        logRequest("selectAll", diskFiles != null ? diskFiles.toString() : "null");
        List<DiskFiles> list = diskFilesService.selectAll(diskFiles);
        logResponse("selectAll", null);
        return Result.success(list);
    }

    /**
     * 分页查询文件
     * Paginated query for files
     */
    @GetMapping("/selectPage")
    public Result selectPage(DiskFiles diskFiles,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        logRequest("selectPage", String.format("diskFiles=%s, pageNum=%d, pageSize=%d",
                diskFiles, pageNum, pageSize));
        PageInfo<DiskFiles> page = diskFilesService.selectPage(diskFiles, pageNum, pageSize);
        logResponse("selectPage", null);
        return Result.success(page);
    }
}
