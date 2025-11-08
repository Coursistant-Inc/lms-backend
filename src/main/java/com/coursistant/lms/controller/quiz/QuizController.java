package com.coursistant.lms.controller.quiz;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.Quiz;
import com.coursistant.lms.service.quiz.QuizService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    @Resource
    private QuizService quizService;

    private static final Logger logger = Logger.getLogger(QuizController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }
    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    @PostMapping("/add")
    public Result add(@RequestBody Quiz quiz) {
        logRequest("add", quiz.toString());
        quizService.add(quiz);
        logResponse("add", "Success");
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        quizService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        quizService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    @PutMapping("/update")
    public Result updateById(@RequestBody Quiz quiz) {
        logRequest("updateById", quiz.toString());
        quizService.updateById(quiz);
        logResponse("updateById", "Success");
        return Result.success();
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        Quiz quiz = quizService.selectById(id);
        logResponse("selectById", quiz.toString());
        return Result.success(quiz);
    }

    @GetMapping("/selectAll")
    public Result selectAll(Quiz filter) {
        logRequest("selectAll", filter != null ? filter.toString() : "null");
        List<Quiz> list = quizService.selectAll(filter);
        logResponse("selectAll", "Success");
        return Result.success(list);
    }
}