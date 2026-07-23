package com.coursistant.lms.module.quiz.dto;

import java.util.List;
import com.coursistant.lms.module.assignment.entity.Assignment;

/**
 * 公告实体类
 * Assignment DTO
 */
public class AttemptItemBatchReq {
    private List<AttemptItemCreateReq> items;

    public List<AttemptItemCreateReq> getItems() {
        return items;
    }

    public void setItems(List<AttemptItemCreateReq> items) {
        this.items = items;
    }
}
