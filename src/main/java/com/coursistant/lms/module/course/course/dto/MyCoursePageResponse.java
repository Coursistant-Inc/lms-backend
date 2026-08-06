package com.coursistant.lms.module.course.course.dto;

import java.util.ArrayList;
import java.util.List;

public class MyCoursePageResponse {

    private List<MyCourseResponse> items = new ArrayList<>();
    private int page;
    private int size;
    private long total;

    public List<MyCourseResponse> getItems() {
        return items;
    }

    public void setItems(List<MyCourseResponse> items) {
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
