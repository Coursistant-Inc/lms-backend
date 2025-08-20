package com.coursistant.lms.service.file;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.AssignmentFile;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.file.AssignmentFileMapper;
import com.coursistant.lms.service.system.MinIOService;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;


@Service
public class AssignmentFileService {

    @Resource
    private AssignmentFileMapper assignmentFileMapper;

    @Resource
    private MinIOService minIOService;

    private static final String filePath = "assignment/";
    private static final String bucket = "lms-uploads";

    public void add(MultipartFile file, Integer assignmentId) {
        AssignmentFile assignmentFile=new AssignmentFile();
        // 创建文件存储路径
        // Create file storage path
        String path = filePath + assignmentId+ "/";
        // if (!FileUtil.exist(path)) {
        //     FileUtil.mkdir(path);
        // }

        // 获取文件信息
        // Get file information
        String filename = file.getOriginalFilename();
        String fileDest = path + filename;


        // try {
        //     file.transferTo(new File(fullpath));
        // } catch (IOException e) {
        //     throw new CustomException(ResultCodeEnum.FILE_UPLOAD_ERROR);
        // }
        try {
            minIOService.uploadFile(fileDest, file, bucket);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(ResultCodeEnum.FILE_UPLOAD_ERROR);
        }

        assignmentFile.setAssignmentId(assignmentId);
        assignmentFile.setName(filename);
        assignmentFile.setPath(fileDest);
        assignmentFileMapper.insert(assignmentFile);
    }

    /**
     * 删除
     * Delete a assignmentFile by ID
     */
    public void deleteById(Integer id) {
        AssignmentFile assignmentFile=assignmentFileMapper.selectById(id);
        if (ObjectUtil.isNull(assignmentFile)){
            throw new CustomException(ResultCodeEnum.FILE_NOT_FOUND);
        }
        String fileDest=assignmentFile.getPath();


        // File file = new File(filepath);
        // if (file.exists()) {
        //     boolean deleted = file.delete();
        //     if (!deleted) {
        //         throw new CustomException(ResultCodeEnum.FILE_DELETION_ERROR);
        //     }
        // } else {
        //     throw new CustomException(ResultCodeEnum.FILE_NOT_FOUND);
        // }
        try {
            minIOService.deleteFile(fileDest, bucket);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(ResultCodeEnum.FILE_DELETION_ERROR);
        }

        assignmentFileMapper.deleteById(id);

    }

    /**
     * 批量删除
     * Delete multiple assignmentFiles by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    /**
     * 修改
     * Update a assignmentFile by ID
     */
    public void updateById(AssignmentFile assignmentFile) {
        assignmentFileMapper.updateById(assignmentFile);

    }

    /**
     * 根据ID查询
     * Query a assignmentFile by ID
     */
    public AssignmentFile selectById(Integer id) {

        AssignmentFile assignmentFile = assignmentFileMapper.selectById(id);
        if (assignmentFile == null) {
            throw new CustomException(ResultCodeEnum.FILE_NOT_FOUND);
        }

        return assignmentFile;
    }


    public List<AssignmentFile> selectByAssignmentId(Integer id) {

        List<AssignmentFile> assignmentFiles = assignmentFileMapper.selectByAssignmentId(id);
        if (assignmentFiles == null) {
            throw new CustomException(ResultCodeEnum.FILE_NOT_FOUND);
        }

        return assignmentFiles;
    }

    /**
     * 查询所有
     * Query all assignmentFiles
     */
    public List<AssignmentFile> selectAll(AssignmentFile assignmentFile) {

        List<AssignmentFile> assignmentFiles = assignmentFileMapper.selectAll(assignmentFile);

        return assignmentFiles;
    }

}
