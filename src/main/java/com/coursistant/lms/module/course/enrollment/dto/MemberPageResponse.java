package com.coursistant.lms.module.course.enrollment.dto;

import java.util.ArrayList;
import java.util.List;

public class MemberPageResponse {

    private List<MemberResponse> items = new ArrayList<>();
    private int page;
    private int size;
    private long total;

    public List<MemberResponse> getItems() {
        return items;
    }

    public void setItems(List<MemberResponse> items) {
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
