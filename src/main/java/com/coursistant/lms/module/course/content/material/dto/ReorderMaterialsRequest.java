package com.coursistant.lms.module.course.content.material.dto;

import java.util.List;

public class ReorderMaterialsRequest {
    private List<Integer> materialIds;

    public List<Integer> getMaterialIds() {
        return materialIds;
    }

    public void setMaterialIds(List<Integer> materialIds) {
        this.materialIds = materialIds;
    }
}
