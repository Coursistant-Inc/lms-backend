package com.coursistant.lms.controller.interaction;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.Bookmark;
import com.coursistant.lms.service.interaction.BookmarkService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;

/**
 * 部门信息表前端操作接口
 * Bookmark frontend operation API
 **/
@RestController
@RequestMapping("/bookmark")
public class BookmarkController {

    @Resource
    private BookmarkService bookmarkService;

    private static final Logger logger = Logger.getLogger(BookmarkController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增书签
     * Add a new bookmark
     */
    @PostMapping("/add")
    public Result add(@RequestBody Bookmark bookmark) {
        logRequest("add", bookmark.toString());
        bookmarkService.add(bookmark);
        logResponse("add", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 删除书签
     * Delete a bookmark by ID
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        bookmarkService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    /**
     * 批量删除书签
     * Batch delete bookmarks
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        bookmarkService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    /**
     * 更新书签
     * Update a bookmark
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Bookmark bookmark) {
        logRequest("updateById", bookmark.toString());
        bookmarkService.updateById(bookmark);
        logResponse("updateById", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 查询书签
     * Query a bookmark by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        Bookmark bookmark = bookmarkService.selectById(id);
        logResponse("selectById", bookmark.toString());
        return Result.success(bookmark);
    }

    /**
     * 查询所有书签
     * Query all bookmarks
     */
    @GetMapping("/selectAll")
    public Result selectAll(Bookmark bookmark) {
        logRequest("selectAll", bookmark != null ? bookmark.toString() : "null");
        List<Bookmark> list = bookmarkService.selectAll(bookmark);
        logResponse("selectAll", null);
        return Result.success(list);
    }

}
