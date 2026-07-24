package com.coursistant.lms.module.file.service;



import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.ParseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

@Service
public class AsyncFileUploadService {

    private static final Logger logger = Logger.getLogger(AsyncFileUploadService.class.getName());

    /**
     * 使用 Spring 的 @Async 注解标记为异步方法
     * 方法签名从 (String courseName, MultipartFile file) 改为 (String courseName, String fullpath)
     */
    @Async
    public void asyncUploadFile(Integer courseId, MultipartFile file) {
        try {
            // 1. 将 MultipartFile 保存为临时文件
            File tempFile = File.createTempFile("upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);

            // 2. 使用 HttpClient 进行文件上传
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost("http://dev.xlearnedu.com:5100/file");
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();

                // 添加文本参数 course_id
                builder.addTextBody("course_id", courseId.toString(), ContentType.TEXT_PLAIN);

                // 添加文件参数 file
                builder.addBinaryBody("file", tempFile, ContentType.MULTIPART_FORM_DATA, tempFile.getName());

                HttpEntity multipart = builder.build();
                httpPost.setEntity(multipart);

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getCode();
                    if (statusCode == 200) {
                        String result = EntityUtils.toString(response.getEntity());
                        logger.info("异步上传成功，返回结果: {}" + result);
                    } else {
                        logger.info("异步上传失败，HTTP Status: {}" + statusCode);
                    }
                }
            } catch (IOException | ParseException e) {
                logger.info("异步上传出现异常: " + e);
            } finally {
                // 删除临时文件
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        } catch (IOException e) {
            logger.info("保存 MultipartFile 到临时文件失败: " + e);
        }
    }

}
