package com.coursistant.lms.module.quiz.controller;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.module.quiz.entity.QuizItem;
import com.coursistant.lms.module.quiz.service.QuizItemService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/quizItem")
public class QuizItemController {

    @Resource
    private QuizItemService quizItemService;

    private static final Logger logger = Logger.getLogger(QuizItemController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }
    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    @PostMapping("/add")
    public Result add(@RequestBody QuizItem quizItem) {
        logRequest("add", quizItem.toString());
        quizItemService.add(quizItem);
        logResponse("add", "Success");
        return Result.success();
    }

    @PostMapping("/addQuestion")
    public Result addQuestion(@RequestBody QuizItem req) {
        logRequest("addQuestion", req.toString());
        quizItemService.addQuestion(req.getQuizId(), req.getQuestionId(), req.getPoints(), req.getOrderIndex());
        logResponse("addQuestion", "Success");
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        quizItemService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        quizItemService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    @PutMapping("/update")
    public Result updateById(@RequestBody QuizItem quizItem) {
        logRequest("updateById", quizItem.toString());
        quizItemService.updateById(quizItem);
        logResponse("updateById", "Success");
        return Result.success();
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        QuizItem quizItem = quizItemService.selectById(id);
        logResponse("selectById", quizItem.toString());
        return Result.success(quizItem);
    }

    @GetMapping("/selectAll")
    public Result selectAll(QuizItem filter) {
        logRequest("selectAll", filter != null ? filter.toString() : "null");
        List<QuizItem> list = quizItemService.selectAll(filter);
        logResponse("selectAll", "Success");
        return Result.success(list);
    }

    @GetMapping("/selectByQuestionId/{questionId}")
    public Result selectByQuestionId(@PathVariable Integer questionId) {
        logRequest("selectByQuestionId", String.valueOf(questionId));
        List<QuizItem> list = quizItemService.selectByQuestionId(questionId);
        logResponse("selectByQuestionId", "Success");
        return Result.success(list);
    }

    @GetMapping("/selectByQuizId/{quizId}")
    public Result selectByQuizId(@PathVariable Integer quizId) {
        logRequest("selectByQuizId", String.valueOf(quizId));
        List<QuizItem> list = quizItemService.selectByQuizId(quizId);
        logResponse("selectByQuizId", "Success");
        return Result.success(list);
    }


}