package com.coursistant.lms.module.file.dto;

import com.coursistant.lms.module.file.entity.Folder;
import com.coursistant.lms.module.file.entity.FolderItem;

import java.util.List;

/**
 * FolderDTO：用于返回某课程下的所有文件夹及其资源项
 */
public class FolderDTO extends Folder {

    /** 文件夹下的所有资源项 */
    private List<FolderItem> items;

    public List<FolderItem> getItems() {
        return items;
    }

    public void setItems(List<FolderItem> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "FolderDTO{" +
                "items=" + items +
                "} " + super.toString();
    }
}