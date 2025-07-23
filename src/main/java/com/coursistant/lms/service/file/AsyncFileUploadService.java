package com.coursistant.lms.service.file;



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
    public void asyncUploadFile(Integer courseId, String fullpath) {
        // 1. 检查本地文件是否存在
        File localFile = new File(fullpath);
        if (!localFile.exists() || !localFile.isFile()) {
            logger.info("本地文件不存在或不是文件: " + fullpath);
            return;
        }

        // 2. 使用 HttpClient 进行文件上传
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 构造请求
            HttpPost httpPost = new HttpPost("http://dev.xlearnedu.com:5100/file");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            // 添加文本参数 course_id
            builder.addTextBody("course_id", courseId.toString(), ContentType.TEXT_PLAIN);

            // 添加文件参数 file（直接传 File 对象）
            builder.addBinaryBody("file", localFile, ContentType.MULTIPART_FORM_DATA, localFile.getName());

            // 拼装请求体
            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);

            // 执行请求
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
        }
    }
}
