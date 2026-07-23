package com.coursistant.lms.module.quiz.controller;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.module.quiz.entity.AttemptItem;
import com.coursistant.lms.module.quiz.dto.AttemptItemBatchReq;
import com.coursistant.lms.module.quiz.dto.AttemptItemCreateReq;
import com.coursistant.lms.module.quiz.service.AttemptItemService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/attemptItem")
public class AttemptItemController {

    @Resource
    private AttemptItemService attemptItemService;

    @Resource
    private ObjectMapper objectMapper;

    private static final Logger logger = Logger.getLogger(AttemptItemController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }
    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    @PostMapping("/add")
    public Result add(@RequestBody AttemptItemCreateReq req) throws JsonProcessingException {
        logRequest("add", req.toString());
        AttemptItem item = new AttemptItem();
        item.setAttemptId(req.getAttemptId());
        item.setQuizItemId(req.getQuizItemId());
        item.setQuestionId(req.getQuestionId());
        item.setNeedsGrading(req.getNeedsGrading());

        // 序列化为字符串再存库
        item.setAnswerPayload(
                req.getAnswerPayload() == null ? null : objectMapper.writeValueAsString(req.getAnswerPayload())
        );

        attemptItemService.add(item);
        logResponse("add", "Success");
        return Result.success();
    }


    @PostMapping("/addBatch")
    public Result addBatch(@RequestBody List<AttemptItemCreateReq> items) throws com.fasterxml.jackson.core.JsonProcessingException {
        logRequest("addBatch", items == null ? "null" : "size=" + items.size());

        List<AttemptItem> list = new java.util.ArrayList<>();
        for (AttemptItemCreateReq req : items) {
            AttemptItem it = new AttemptItem();
            it.setAttemptId(req.getAttemptId());
            it.setQuizItemId(req.getQuizItemId());
            it.setQuestionId(req.getQuestionId());
            it.setNeedsGrading(req.getNeedsGrading());
            it.setAnswerPayload(req.getAnswerPayload() == null ? null : objectMapper.writeValueAsString(req.getAnswerPayload()));
            list.add(it);
        }

        attemptItemService.addBatch(list);
        logResponse("addBatch", "Success");
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