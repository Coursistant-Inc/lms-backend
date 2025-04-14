package com.coursistant.lms.controller.file;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * 读取文件接口
 * Read file API
 */
@RestController
@RequestMapping("/common")
public class ReadFileController {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 读取远程文件
     * Read a remote file
     *
     * @param filePath 文件路径 / File path
     * @return 文件内容 / File content as byte array
     */
    @GetMapping("/readFile")
    public ResponseEntity<byte[]> readFile(@RequestParam String filePath) {

        String baseUrl = "http://labserver101.ddns.net:6201/read_file/";
        String url = baseUrl + "?file_path=" + filePath;
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 发送 GET 请求并接收字节数组作为响应
        // Make GET request and receive response as byte array
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            // 返回文件作为响应
            // Return file as response
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.IMAGE_PNG); // 设置适当的内容类型 / Set appropriate content type
            responseHeaders.setContentDisposition(ContentDisposition.inline().filename("image.png").build()); // 建议文件名 / Suggest filename

            return new ResponseEntity<>(response.getBody(), responseHeaders, HttpStatus.OK);
        } else {
            return ResponseEntity.status(response.getStatusCode()).body(null);
        }
    }
}
