package com.coursistant.lms.service.file;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.SubmissionFile;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.file.SubmissionFileMapper;
import com.coursistant.lms.service.system.MinIOService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;


@Service
public class SubmissionFileService {

    @Resource
    private SubmissionFileMapper submissionFileMapper;

    @Resource
    private MinIOService minIOService;

    private static final String filePath = "submission/";
    private static final String bucket = "lms-uploads";

    public void add(MultipartFile file, Integer submissionId) {
        SubmissionFile submissionFile=new SubmissionFile();
        // 创建文件存储路径
        // Create file storage path
        String path = filePath + submissionId+ "/";
        // 获取文件信息
        // Get file information
        String filename = file.getOriginalFilename();
        String fileDest = path + filename;


        try {
            minIOService.uploadFile(fileDest, file, bucket);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(ResultCodeEnum.FILE_UPLOAD_ERROR);
        }

        submissionFile.setSubmissionId(submissionId);
        submissionFile.setName(filename);
        submissionFile.setPath(fileDest);
        submissionFileMapper.insert(submissionFile);
    }

    /**
     * 删除
     * Delete a submissionFile by ID
     */
    public void deleteById(Integer id) {
        SubmissionFile submissionFile=submissionFileMapper.selectById(id);
        if (ObjectUtil.isNull(submissionFile)){
            throw new CustomException(ResultCodeEnum.FILE_NOT_FOUND);
        }
        String fileDest=submissionFile.getPath();



        try {
            minIOService.deleteFile(fileDest, bucket);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(ResultCodeEnum.FILE_DELETION_ERROR);
        }

        submissionFileMapper.deleteById(id);
    }

    /**
     * 批量删除
     * Delete multiple submissionFiles by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    /**
     * 修改
     * Update a submissionFile by ID
     */
    public void updateById(SubmissionFile submissionFile) {
        submissionFileMapper.updateById(submissionFile);

    }

    /**
     * 根据ID查询
     * Query a submissionFile by ID
     */
    public SubmissionFile selectById(Integer id) {
        
        SubmissionFile submissionFile = submissionFileMapper.selectById(id);
        if (submissionFile == null) {
            throw new CustomException(ResultCodeEnum.FILE_NOT_FOUND);
        }

        return submissionFile;
    }


    public List<SubmissionFile> selectBySubmissionId(Integer id) {
        
        List<SubmissionFile> submissionFiles = submissionFileMapper.selectBySubmissionId(id);
        if (submissionFiles == null) {
            throw new CustomException(ResultCodeEnum.FILE_NOT_FOUND);
        }

        return submissionFiles;
    }

    /**
     * 查询所有
     * Query all submissionFiles
     */
    public List<SubmissionFile> selectAll(SubmissionFile submissionFile) {

        List<SubmissionFile> submissionFiles = submissionFileMapper.selectAll(submissionFile);

        return submissionFiles;
    }

}
