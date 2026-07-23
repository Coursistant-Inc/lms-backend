package com.coursistant.lms.module.chat.entity;

import java.io.Serializable;

/**
 * Query
 * 包含一个字符串和一张图片（Base64 编码形式）
 * Contains a string and an image (Base64 encoded)
 */
public class Query implements Serializable {
    private static final long serialVersionUID = 1L;

    // 用于存储字符串内容
    // Stores the string content
    private String answer;

    // 用于存储图片的 Base64 编码
    // Stores the image in Base64 encoding
    private String imageURL;

    private Integer queryId;

    // 无参构造器
    // No-argument constructor
    public Query() {
    }

    @Override
    public String toString() {
        return "Query{" +
                "answer='" + answer + '\'' +
                '}';
    }

    // 全参构造器
    // Full-argument constructor
    public Query(String answer, String imageURL) {
        this.answer = answer;
        this.imageURL = imageURL;
    }

    // Getter 和 Setter 方法
    // Getter and Setter methods
    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public Integer getQueryId() {
        return queryId;
    }

    public void setQueryId(Integer queryId) {
        this.queryId = queryId;
    }
}
