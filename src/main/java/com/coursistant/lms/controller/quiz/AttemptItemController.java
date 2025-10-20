package com.coursistant.lms.controller.quiz;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.AttemptItem;
import com.coursistant.lms.service.quiz.AttemptItemService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/attemptItem")
public class AttemptItemController {

    @Resource
    private AttemptItemService attemptItemService;

    private static final Logger logger = Logger.getLogger(AttemptItemController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }
    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    @PostMapping("/add")
    public Result add(@RequestBody AttemptItem attemptItem) {
        logRequest("add", attemptItem.toString());
        attemptItemService.add(attemptItem);
        logResponse("add", "Success");
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        attemptItemService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        attemptItemService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    @PutMapping("/update")
    public Result updateById(@RequestBody AttemptItem attemptItem) {
        logRequest("updateById", attemptItem.toString());
        attemptItemService.updateById(attemptItem);
        logResponse("updateById", "Success");
        return Result.success();
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        AttemptItem attemptItem = attemptItemService.selectById(id);
        logResponse("selectById", attemptItem.toString());
        return Result.success(attemptItem);
    }

    @GetMapping("/selectAll")
    public Result selectAll(AttemptItem filter) {
        logRequest("selectAll", filter != null ? filter.toString() : "null");
        List<AttemptItem> list = attemptItemService.selectAll(filter);
        logResponse("selectAll", "Success");
        return Result.success(list);
    }
}