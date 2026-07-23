package com.coursistant.lms.module.quiz.controller;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.module.quiz.entity.QuizAttempt;
import com.coursistant.lms.module.quiz.service.QuizAttemptService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        Integer id=quizAttemptService.add(quizAttempt);
        Map<String, Object> data = new HashMap<>();
        data.put("quizAttemptId", id);
        logResponse("add", "Success");
        return Result.success(data);
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

    @PostMapping("/updateGradebyId/{id}")
    public Result updateGradebyId(@PathVariable Integer id) {
        logRequest("updateGradebyId", id.toString());

        int finalScore = quizAttemptService.updateGrade(id);

        Map<String, Object> data = new HashMap<>();
        data.put("quizAttemptId", id);
        data.put("finalScore", finalScore);

        logResponse("updateGradebyId", "finalScore=" + finalScore);
        return Result.success(data);
    }

    @GetMapping("/latestByQuiz")
    public Result latestByQuiz(@RequestParam("quizId") Integer quizId) {
        logRequest("latestByQuiz", quizId == null ? "null" : quizId.toString());

        List<QuizAttempt> list = quizAttemptService.getLatestAttemptsDistinctStudentByQuizId(quizId);

        logResponse("latestByQuiz", list == null ? "null" : list.toString());
        return Result.success(list);
    }

    /**
     * 【功能2】按 quizId + studentId 查询：该学生在该测验下最新一次尝试（主键 id 最大）
     * GET /quizAttempt/latest?quizId=123&studentId=456
     */
    @GetMapping("/latest")
    public Result latest(@RequestParam("quizId") Integer quizId,
                         @RequestParam("studentId") Integer studentId) {
        logRequest("latest",
                (quizId == null ? "null" : quizId.toString()) + "," + (studentId == null ? "null" : studentId.toString()));

        QuizAttempt quizAttempt = quizAttemptService.getLatestAttemptByQuizIdAndStudentId(quizId, studentId);

        logResponse("latest", quizAttempt == null ? "null" : quizAttempt.toString());
        return Result.success(quizAttempt);
    }


}