package com.coursistant.lms.controller.quiz;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.QuizAttempt;
import com.coursistant.lms.service.quiz.QuizAttemptService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/quizAttempt")
public class QuizAttemptController {

    @Resource
    private QuizAttemptService quizAttemptService;

    private static final Logger logger = Logger.getLogger(QuizAttemptController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }
    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    @PostMapping("/add")
    public Result add(@RequestBody QuizAttempt quizAttempt) {
        logRequest("add", quizAttempt.toString());
        quizAttemptService.add(quizAttempt);
        logResponse("add", "Success");
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        quizAttemptService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        quizAttemptService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    @PutMapping("/update")
    public Result updateById(@RequestBody QuizAttempt quizAttempt) {
        logRequest("updateById", quizAttempt.toString());
        quizAttemptService.updateById(quizAttempt);
        logResponse("updateById", "Success");
        return Result.success();
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        QuizAttempt quizAttempt = quizAttemptService.selectById(id);
        logResponse("selectById", quizAttempt.toString());
        return Result.success(quizAttempt);
    }

    @GetMapping("/selectAll")
    public Result selectAll(QuizAttempt filter) {
        logRequest("selectAll", filter != null ? filter.toString() : "null");
        List<QuizAttempt> list = quizAttemptService.selectAll(filter);
        logResponse("selectAll", "Success");
        return Result.success(list);
    }
}