package com.coursistant.lms.module.interaction.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(name = "NotificationPageResponse", description = "Paged notification list")
public class NotificationPageResponse {

    @Schema(description = "Page items")
    private List<NotificationResponse> items = new ArrayList<>();
    @Schema(description = "1-based page number", example = "1")
    private int page;
    @Schema(description = "Page size (max 100)", example = "20")
    private int size;
    @Schema(description = "Total matching rows", example = "3")
    private long total;

    public List<NotificationResponse> getItems() {
        return items;
    }

    public void setItems(List<NotificationResponse> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
