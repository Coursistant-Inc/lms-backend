package com.coursistant.individual.service.chat;

import cn.hutool.core.util.ObjectUtil;

import com.coursistant.individual.common.enums.ResultCodeEnum;
import com.coursistant.individual.entity.Chat;
import com.coursistant.individual.entity.Dialogue;
import com.coursistant.individual.entity.Learn;
import com.coursistant.individual.entity.Query;
import com.coursistant.individual.entity.Teach;
import com.coursistant.individual.entity.User;
import com.coursistant.individual.exception.CustomException;
import com.coursistant.individual.mapper.chat.DialogueMapper;
import com.coursistant.individual.service.course.LearnService;
import com.coursistant.individual.service.course.TeachService;
import com.coursistant.individual.service.user.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    public Query query(File file, Integer courseId, String query, Integer dialogueId, Integer userId) {
        Query returnQuery = new Query();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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
            dialogue.setUpdateTime(LocalDateTime.now().format(formatter));
            dialogueService.updateById(dialogue);
        } else {
            // 创建新的对话 / Create a new dialogue
            dialogue=new Dialogue();
            dialogue.setCourseId(courseId);
            dialogue.setUserId(userId);
            dialogue.setUpdateTime(LocalDateTime.now().format(formatter));
            dialogueService.add(dialogue);
            dialogueId=dialogue.getId();

            initial=true;
        }

        // 保存聊天记录 / Save chat history
        Chat chat = new Chat();
        chat.setQueryText(query);
        chat.setQueryImage(file != null ? file.getAbsolutePath() : null);
        chat.setTime(LocalDateTime.now().format(formatter));
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
        String queryApiUrl = "http://labserver101.ddns.net:5000/chat";
        String analyzeImageApiUrl = "http://labserver101.ddns.net:5001/analyze-image";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String analyzedResult = null;

            // 如果文件存在且不为空，调用 analyze-image API
            // If the file exists and is not empty, call the analyze-image API
            if (file != null && file.exists()) {
                HttpPost analyzePost = new HttpPost(analyzeImageApiUrl);
                MultipartEntityBuilder fileBuilder = MultipartEntityBuilder.create();
                fileBuilder.addBinaryBody(
                        "image",
                        file,
                        ContentType.MULTIPART_FORM_DATA,
                        file.getName()
                );

                HttpEntity fileEntity = fileBuilder.build();
                analyzePost.setEntity(fileEntity);

                try (CloseableHttpResponse analyzeResponse = httpClient.execute(analyzePost)) {
                    if (analyzeResponse.getStatusLine().getStatusCode() == 200) {
                        String analyzeResponseJson = EntityUtils.toString(analyzeResponse.getEntity());

                        // 解析 JSON 响应 / Parse JSON response
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode analyzeRootNode = objectMapper.readTree(analyzeResponseJson);

                        // 提取 result 和 status / Extract result and status
                        String status = analyzeRootNode.path("status").asText();
                        if ("success".equalsIgnoreCase(status)) {
                            analyzedResult = analyzeRootNode.path("result").asText();
                        } else {
                            throw new IOException("Image analysis failed with status: " + status);
                        }
                    } else {
                        throw new IOException("Failed to call analyze-image API. HTTP Status: " +
                                analyzeResponse.getStatusLine().getStatusCode());
                    }
                }
            } else {
                System.out.println("No image file provided. Skipping analyze-image API call.");
            }

            // 创建 POST 请求到 query API / Create POST request to query API
            HttpPost queryPost = new HttpPost(queryApiUrl);
            MultipartEntityBuilder queryBuilder = MultipartEntityBuilder.create();
            queryBuilder.addTextBody("course_id", courseId.toString(), ContentType.TEXT_PLAIN);
            // 添加 course_list
            if (courseId==0) {
                queryBuilder.addTextBody("course_list", course_list, ContentType.TEXT_PLAIN);
            }
            // 如果图片分析成功，将 analyzedResult 拼接到 query 后，并加上英文说明
            // If image analysis is successful, append analyzedResult to query with English explanation
            if (analyzedResult != null) {
                logger.info("Image analysis result: " + analyzedResult);
                query = query + " The question includes an image: " + analyzedResult;
            }
            queryBuilder.addTextBody("text", query, ContentType.TEXT_PLAIN);

            if (!initial) {
                queryBuilder.addTextBody("past_chat", jsonString, ContentType.APPLICATION_JSON);
            }

            // 添加初始标志 / Add initial flag
            if (initial) {
                queryBuilder.addTextBody("initial", initial ? "1" : "0", ContentType.TEXT_PLAIN);
            }

            HttpEntity queryEntity = queryBuilder.build();
            queryPost.setEntity(queryEntity);

            // 发送 query 请求并接收响应 / Send query request and receive response
            try (CloseableHttpResponse queryResponse = httpClient.execute(queryPost)) {
                if (queryResponse.getStatusLine().getStatusCode() == 200) {
                    String queryResponseJson = EntityUtils.toString(queryResponse.getEntity());

                    // 解析 JSON 响应 / Parse JSON response
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode queryRootNode = objectMapper.readTree(queryResponseJson);

                    // 从 JSON 中提取字符串和图片内容 / Extract text and image from JSON
                    String answer = queryRootNode.path("text").asText();
                    String imageValue = queryRootNode.path("image_bytes").asText();
                    if (initial) {
                        String summary = queryRootNode.path("summary").asText();
                        dialogue.setSummary(summary);
                    }
                    dialogue.setRecentMessage(answer);
                    dialogueService.updateById(dialogue);
                    //String imageBase64 = "Image not found".equals(imageValue) ? null : imageValue;

                    logger.info("Query analysis result: " + answer);

                    // 设置到返回对象中 / Set values in return object
                    returnQuery.setAnswer(answer);
                    returnQuery.setImageBase64(imageValue);
                    returnQuery.setQueryId(dialogueId);

                    // 保存聊天记录 / Save chat history
                    chat.setAnswerText(answer);
                    if (!"null".equals(imageValue)) {
                        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
                        String baseDir = "/home/admir/SpringBoot/saved_images/answer_images"; // 设定存储路径 / Set storage path
                        //String baseDir = "C:\\Users\\Charlottejas\\Desktop\\Jerry\\项目脚手架\\manager\\"; // 设定存储路径
                        String uploadDir = baseDir + datePath + "/";
                        File dir = new File(uploadDir);
                        if (!dir.exists() && !dir.mkdirs()) {
                            throw new RuntimeException("Failed to create directory: " + uploadDir);
                        }

                        // 创建唯一文件名 / Create unique filename
                        String fileName = System.currentTimeMillis() + "_" + dialogueId + ".png";
                        String filePath = uploadDir + fileName;

                        try {
                            // 存储文件 / Save file
                            File imageFile = base64ToFile(imageValue, filePath);
                            System.out.println("Save answer image：" + imageFile.getAbsolutePath());
                            // 保存文件路径 / Save file path
                            chat.setAnswerImage(filePath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    throw new IOException("Failed to query API. HTTP Status: " +
                            queryResponse.getStatusLine().getStatusCode());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            returnQuery.setAnswer("Error: " + e.getMessage());
        }

        // 上传聊天记录 / Upload chat history
        chatService.add(chat);

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
