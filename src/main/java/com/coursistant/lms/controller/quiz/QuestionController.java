package com.coursistant.lms.controller.quiz;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.Bookmark;
import com.coursistant.lms.entity.DTO.QuestionCreateReq;
import com.coursistant.lms.entity.Question;
import com.coursistant.lms.service.interaction.BookmarkService;
import com.coursistant.lms.service.quiz.QuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/question")
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private ObjectMapper objectMapper;

    private static final Logger logger = Logger.getLogger(QuestionController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }
    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    @PostMapping("/add")
    public Result add(@RequestBody QuestionCreateReq req) throws com.fasterxml.jackson.core.JsonProcessingException{
        logRequest("add", req.toString());

        Question q = new Question();
        q.setType(req.getType());
        q.setStem(req.getStem());
        // 将结构化 JSON 序列化为字符串存库
        q.setChoices(req.getChoices() == null ? null : objectMapper.writeValueAsString(req.getChoices()));
        q.setAnswerKey(req.getAnswerKey() == null ? null : objectMapper.writeValueAsString(req.getAnswerKey()));
        q.setStatus(req.getStatus());

        Integer id=questionService.add(q);
        Map<String, Object> data = new HashMap<>();
        data.put("questionId", id);
        logResponse("add", "Success");
        return Result.success(data);
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        questionService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        questionService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    @PutMapping("/update")
    public Result updateById(@RequestBody Question question) {
        logRequest("updateById", question.toString());
        questionService.updateById(question);
        logResponse("updateById", "Success");
        return Result.success();
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        Question question = questionService.selectById(id);
        logResponse("selectById", question.toString());
        return Result.success(question);
    }

    @GetMapping("/selectAll")
    public Result selectAll(Question filter) {
        logRequest("selectAll", filter != null ? filter.toString() : "null");
        List<Question> list = questionService.selectAll(filter);
        logResponse("selectAll", "Success");
        return Result.success(list);
    }
}