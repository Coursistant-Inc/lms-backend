package com.coursistant.lms.service.file;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.AssignmentItem;
import com.coursistant.lms.entity.SubmissionFile;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.file.SubmissionFileMapper;
import com.coursistant.lms.service.system.MinIOService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


@Service
public class SubmissionFileService {

    @Resource
    private SubmissionFileMapper submissionFileMapper;

    // 如需使用，可根据项目结构调整存储路径
    private static final String filePath = System.getProperty("user.dir") + "/disk/submission/";

    /**
     * 新增
     * Add new submission file
     */
    public Integer add(SubmissionFile submissionFile) {
        // 自动分配顺序：按 submission_id 下已有项的最大 order_index + 1
        if (ObjectUtil.isNull(submissionFile.getOrderIndex())) {
            List<SubmissionFile> existing;
            if (submissionFile.getSubmissionId() != null) {
                existing = submissionFileMapper.selectBySubmissionId(submissionFile.getSubmissionId());
            } else {
                // 没有 submissionId 时无法分组计算，给空集合（结果 nextIndex = 0）
                existing = Collections.emptyList();
            }

            int nextIndex = existing.stream()
                    .map(SubmissionFile::getOrderIndex)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(-1) + 1;

            submissionFile.setOrderIndex(nextIndex);
        }

        submissionFileMapper.insert(submissionFile);
        return submissionFile.getId();
    }

    /**
     * 删除
     * Delete by ID
     */
    public void deleteById(Integer id) {
        submissionFileMapper.deleteById(id);
    }

    /**
     * 批量删除
     * Delete multiple submission files by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            submissionFileMapper.deleteById(id);
        }
    }

    /**
     * 修改
     * Update submission file
     */
    public void updateById(SubmissionFile submissionFile) {
        submissionFileMapper.updateById(submissionFile);
    }

    /**
     * 根据 ID 查询
     * Select by ID
     */
    public SubmissionFile selectById(Integer id) {
        return submissionFileMapper.selectById(id);
    }

    /**
     * 根据 submission_id 删除所有文件
     * Delete all files under a submission
     */
    public void deleteBySubmissionId(Integer submissionId) {
        submissionFileMapper.deleteBySubmissionId(submissionId);
    }

    /**
     * 查询所有
     * Select all submission files (multi-criteria)
     */
    public List<SubmissionFile> selectAll(SubmissionFile submissionFile) {
        return submissionFileMapper.selectAll(submissionFile);
    }

    /**
     * 查询 submission 下所有文件
     * Select submission files by submission ID
     */
    public List<SubmissionFile> selectBySubmissionId(Integer submissionId) {
        return submissionFileMapper.selectBySubmissionId(submissionId);
    }

    /**
     * （可选）如果你在 XML 中保留了 selectSubmissionInfo，则提供同名方法
     */
    public List<SubmissionFile> selectSubmissionInfo(Integer submissionId) {
        return submissionFileMapper.selectBySubmissionId(submissionId);
        // 若 XML 中确实有 <select id="selectSubmissionInfo">，改成：
        // return submissionFileMapper.selectSubmissionInfo(submissionId);
    }
}
