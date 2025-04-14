package com.coursistant.individual.service.file;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import com.coursistant.individual.common.enums.ResultCodeEnum;
import com.coursistant.individual.entity.AssignmentFile;
import com.coursistant.individual.exception.CustomException;
import com.coursistant.individual.mapper.file.AssignmentFileMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;


@Service
public class AssignmentFileService {

    @Resource
    private AssignmentFileMapper assignmentFileMapper;

    private static final String filePath = System.getProperty("user.dir") + "/disk/assignment/";

    public void add(MultipartFile file, Integer assignmentId) {
        AssignmentFile assignmentFile=new AssignmentFile();
        // 创建文件存储路径
        // Create file storage path
        String path = filePath + assignmentId+ "/";
        if (!FileUtil.exist(path)) {
            FileUtil.mkdir(path);
        }

        // 获取文件信息
        // Get file information
        String filename = file.getOriginalFilename();
        String fullpath = path + filename;


        try {
            file.transferTo(new File(fullpath));
        } catch (IOException e) {
            throw new CustomException(ResultCodeEnum.FILE_UPLOAD_ERROR);
        }

        assignmentFile.setAssignmentId(assignmentId);
        assignmentFile.setName(filename);
        assignmentFile.setPath(fullpath);
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
        String filepath=assignmentFile.getPath();

        assignmentFileMapper.deleteById(id);

        File file = new File(filepath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                throw new CustomException(ResultCodeEnum.FILE_DELETION_ERROR);
            }
        } else {
            throw new CustomException(ResultCodeEnum.FILE_NOT_FOUND);
        }

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
