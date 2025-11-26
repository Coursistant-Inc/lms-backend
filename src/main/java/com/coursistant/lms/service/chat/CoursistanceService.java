package com.coursistant.lms.service.chat;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.io.FileUtil;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Chat;
import com.coursistant.lms.entity.Dialogue;
import com.coursistant.lms.entity.Learn;
import com.coursistant.lms.entity.Query;
import com.coursistant.lms.entity.Teach;
import com.coursistant.lms.entity.User;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.chat.DialogueMapper;
import com.coursistant.lms.service.course.LearnService;
import com.coursistant.lms.service.course.TeachService;
import com.coursistant.lms.service.user.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class CoursistanceService {

    @Resource
    private ChatService chatService;
    @Resource
    private UserService userService;
    @Resource
    private TeachService teachService;
    @Resource
    private LearnService learnService;
    @Resource
    private DialogueService dialogueService;
    @Resource
    private DialogueMapper dialogueMapper;

    private static final Logger logger = Logger.getLogger(CoursistanceService.class.getName());

    /**
     * 处理查询请求
     * Process query request
     */
    public Query query(File file, Integer courseId, String query, Integer dialogueId, Integer userId, ZoneId timezone) {
        Query returnQuery = new Query();

        // 去掉所有控制字符（除了常见的 \r、\n、\t 可保留或替换为空格）
        query = query.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ");
        query = query.replace("\r", " ").replace("\n", " ");


        // 检查对话是否存在 / Check if dialogue exists
        Dialogue dialogue= dialogueMapper.selectById(dialogueId);
        Boolean initial=false;
        String jsonString="";
        if (ObjectUtil.isNotNull(dialogue)) {
            //get last 5 chats and put to json
            ObjectMapper objectMapper = new ObjectMapper();

            Map<String, String> chatMap = new LinkedHashMap<>();
            List<Chat> last5chats=chatService.getTop5ChatsByDialogueId(dialogueId);
            for (int i=0;i<last5chats.size();i++){
                String questionKey = "question" + (i + 1);
                String answerKey = "answer" + (i + 1);
                chatMap.put(questionKey, last5chats.get(i).getQueryText());
                chatMap.put(answerKey, last5chats.get(i).getAnswerText());
            }
            try {
                jsonString = objectMapper.writeValueAsString(chatMap);
                System.out.println(jsonString);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            // 更新对话 / Update dialogue
            dialogue.setUpdateTime(LocalDateTime.now());
            dialogueService.updateById(dialogue, timezone);
        } else {
            // 创建新的对话 / Create a new dialogue
            dialogue=new Dialogue();
            dialogue.setCourseId(courseId);
            dialogue.setUserId(userId);
            dialogue.setUpdateTime(LocalDateTime.now());
            dialogueService.add(dialogue, timezone);
            dialogueId=dialogue.getId();

            initial=true;
        }

        // 保存聊天记录 / Save chat history
        Chat chat = new Chat();
        chat.setQueryText(query);
        chat.setQueryImage(file != null ? file.getAbsolutePath() : null);
        chat.setTime(LocalDateTime.now());
        chat.setDialogueId(dialogueId);

        //get user info
        String course_list="";
        List<String> courseList = new ArrayList<>();
        if (courseId==0) {

            User currentUser =userService.selectById(userId);
            if (ObjectUtil.isEmpty(currentUser)) {
                throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
            }
            if ("TEACHER".equals(currentUser.getLevel())) {
                Teach serTeach=new Teach();
                serTeach.setUserId(userId);
                List<Teach> teaches=teachService.selectAll(serTeach);
                for (int i = 0; i < teaches.size(); i++) {
                    courseList.add(String.valueOf(teaches.get(i).getCourseId()));
                }
                course_list = String.join(",", courseList);

            }
            else{
                Learn serLearn=new Learn();
                serLearn.setUserId(userId);
                List<Learn> learns=learnService.selectAll(serLearn);
                for (int i = 0; i < learns.size(); i++) {
                    courseList.add(String.valueOf(learns.get(i).getCourseId()));
                }
                course_list = String.join(",", courseList);

            }
        }







        // 定义两个 API 地址 / Define two API endpoints
        String queryApiUrl = "https://dev.xlearnedu.com/query";
        String analyzeImageUrl = "http://dev.xlearnedu.com:5001/analyze-image";
        String analyzeFileUrl   = "http://dev.xlearnedu.com:5005/analyze-file";
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String analyzedResult = null;

            // 如果文件存在且不为空，调用 analyze-image/file API
            // If the file exists and is not empty, call the analyze-image/file API
            if (file != null && file.exists()) {
                String ext = FileUtil.extName(file.getName()).toLowerCase();
                String apiUrl = (List.of("png","jpg","jpeg","bmp","gif").contains(ext))
                        ? analyzeImageUrl : analyzeFileUrl;
                HttpPost post = new HttpPost(apiUrl);
                MultipartEntityBuilder mb = MultipartEntityBuilder.create();
                if (apiUrl.equals(analyzeImageUrl)) {
                    mb.addBinaryBody(
                            "image",
                            file,
                            ContentType.MULTIPART_FORM_DATA,
                            file.getName());
                } else {
                    mb.addBinaryBody(
                            "file",
                            file,
                            ContentType.DEFAULT_BINARY,
                            file.getName());
                }
                post.setEntity(mb.build());

                try (CloseableHttpResponse resp = httpClient.execute(post)) {
                    int code = resp.getCode();
                    if (code == 200) {
                        String analyzeResponseJson = EntityUtils.toString(resp.getEntity());

                        // 解析 JSON 响应 / Parse JSON response
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode analyzeRootNode = objectMapper.readTree(analyzeResponseJson);

                        // 提取 result 和 status / Extract result and status
                        String status = analyzeRootNode.path("status").asText();
                        if ("success".equalsIgnoreCase(status)) {
                            analyzedResult = analyzeRootNode.path("result").asText();
                        } else {
                            throw new IOException("File analysis failed with status: " + status);
                        }
                    } else {
                        throw new IOException("Failed to call analyze API. HTTP Status " + code);
                    }
                }
            } else {
                System.out.println("No file provided. Skipping analyze-file/image API call.");
            }

            // 创建 POST 请求到 query API / Create POST request to query API
            HttpPost queryPost = new HttpPost(queryApiUrl);

            queryPost.setHeader("Content-Type", "application/json");
            // 如果图片分析成功，将 analyzedResult 拼接到 query 后，并加上英文说明
            // If image analysis is successful, append analyzedResult to query with English explanation
            if (analyzedResult != null) {
                logger.info("File analysis: " + analyzedResult);
                query += " The question includes a file: " + analyzedResult;
            }

            // 合并版本：包含course_id参数
            String jsonBody = String.format(
                    "{ \"question\": \"%s\", \"top_k\": 2, \"course_id\": %d }",
                    query, courseId
            );

            ObjectMapper mapper = new ObjectMapper();
            Object jsonObj = mapper.readValue(jsonBody, Object.class);
            String prettyBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj);

            logger.info("Query Request Body:\n" + prettyBody);

            queryPost.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            /*MultipartEntityBuilder queryBuilder = MultipartEntityBuilder.create();
            queryBuilder.addTextBody("course_id", courseId.toString(), ContentType.TEXT_PLAIN);
            // 添加 course_list
            if (courseId==0) {
                queryBuilder.addTextBody("course_list", course_list, ContentType.TEXT_PLAIN);
            }

            // 如果图片分析成功，将 analyzedResult 拼接到 query 后，并加上英文说明
            // If image analysis is successful, append analyzedResult to query with English explanation
            if (analyzedResult != null) {
                logger.info("File analysis: " + analyzedResult);
                query += " The question includes a file: " + analyzedResult;
            }
            queryBuilder.addTextBody("question", query, ContentType.TEXT_PLAIN);
            queryBuilder.addTextBody("top_k", "2", ContentType.TEXT_PLAIN);*/

/*
            if (!initial) {
                queryBuilder.addTextBody("past_chat", jsonString, ContentType.APPLICATION_JSON);
            }

            // 添加初始标志 / Add initial flag
            if (initial) {
                queryBuilder.addTextBody("initial", initial ? "1" : "0", ContentType.TEXT_PLAIN);
            }*/
/*
            HttpEntity queryEntity = queryBuilder.build();
            queryPost.setEntity(queryEntity);*/

            // 发送 query 请求并接收响应 / Send query request and receive response
            try (CloseableHttpResponse queryResponse = httpClient.execute(queryPost)) {
                if (queryResponse.getCode() == 200) {
                    String queryResponseJson = EntityUtils.toString(queryResponse.getEntity());

                    // 解析 JSON 响应 / Parse JSON response
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode queryRootNode = objectMapper.readTree(queryResponseJson);

                    // 从 JSON 中提取字符串和图片内容 / Extract text and image from JSON
                    String answer = queryRootNode.path("large_model_result").path("answer").asText();

                    String imageValue = queryRootNode.path("image_url").asText();
                    if (initial) {
                        String summary = queryRootNode.path("summary").asText();
                        dialogue.setSummary(summary);
                    }
                    dialogue.setRecentMessage(answer);
                    dialogueService.updateById(dialogue, timezone);
                    //String imageBase64 = "Image not found".equals(imageValue) ? null : imageValue;

                    logger.info("Full Query Response: " + queryResponseJson);

                    logger.info("Query analysis result: " + answer);

                    // 设置到返回对象中 / Set values in return object
                    returnQuery.setAnswer(answer);
                    returnQuery.setImageURL(imageValue);
                    returnQuery.setQueryId(dialogueId);

                    // 保存聊天记录 / Save chat history
                    chat.setAnswerText(answer);
                    chat.setAnswerImage(imageValue);

                } else {
                    throw new IOException("Failed to query API. HTTP Status: " +
                            queryResponse.getCode());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            returnQuery.setAnswer("Error: " + e.getMessage());
        }

        // 上传聊天记录 / Upload chat history
        chatService.add(chat, timezone);

        return returnQuery;
    }

    /**
     * 将 Base64 编码的字符串转换为文件
     * Convert Base64 encoded string to file
     */
    public static File base64ToFile(String base64Str, String filePath) throws IOException {
        // 如果 Base64 字符串包含 "data:image/png;base64," 这样的前缀，先去掉它
        // If Base64 string contains prefix like "data:image/png;base64,", remove it first
        if (base64Str.contains(",")) {
            base64Str = base64Str.split(",")[1];
        }

        // 解码 Base64 / Decode Base64
        byte[] decodedBytes = Base64.getDecoder().decode(base64Str);

        // 生成文件对象 / Generate file object
        File file = new File(filePath);

        // 创建输出流并写入数据 / Create output stream and write data
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(decodedBytes);
        }

        return file; // 返回 File 对象 / Return File object
    }
}