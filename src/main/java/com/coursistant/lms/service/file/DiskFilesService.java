package com.coursistant.lms.service.file;

import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.entity.DiskFiles;
import com.coursistant.lms.entity.FileSummary;
import com.coursistant.lms.mapper.file.DiskFilesMapper;
import com.coursistant.lms.service.system.MinIOService;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.coursistant.lms.common.enums.ResultCodeEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * 网盘文件信息表业务处理
 * Cloud disk file service processing
 **/
@Service
public class DiskFilesService {

    @Resource
    private DiskFilesMapper diskFilesMapper;
    @Resource
    private AsyncFileUploadService asyncFileUploadService;

    @Resource
    private MinIOService minIOService;

    private static final String filePath = "disk/";
    private static final String bucket = "lms-uploads";

    private static final Logger log = LoggerFactory.getLogger(DiskFilesService.class);

    /**
     * 新增文件
     * Add a new file
     */
    public FileSummary add(MultipartFile file, Integer courseId, Integer userId, String category, Integer analysis, Boolean upload2RAG) {
        FileSummary summary = new FileSummary();

        // 创建文件存储路径
        // Create file storage path
        String path = filePath + courseId + "/";

        // 获取文件信息
        // Get file information
        String filename = file.getOriginalFilename();
        String extName = FileUtil.extName(filename);
        String fileDest = path + filename;

        // 检查文件是否已存在 duplicate?
        // Check if the file already exists
        List<DiskFiles> exist = diskFilesMapper.selectByCourseName(courseId);
        if (ObjectUtil.isNotEmpty(exist)) {
            // 获取不包含扩展名的文件名
            String baseName = filename.substring(0, filename.lastIndexOf("."));
            String extension = extName.isEmpty() ? "" : "." + extName;

            int counter = 1;
            boolean duplicateFound = true;

            while (duplicateFound) {
                duplicateFound = false;
                for (DiskFiles df : exist) {
                    if (df.getName().equals(filename)) {
                        // 发现重复 -> 改名
                        filename = baseName + "_" + counter + extension;
                        counter++;
                        duplicateFound = true;
                        break;
                    }
                }
            }
        }

// 重新构建最终路径
        fileDest = path + filename;

        DiskFiles diskFiles = new DiskFiles();
        String now = DateUtil.now();
        diskFiles.setCreateTime(now);
        diskFiles.setName(filename);
        diskFiles.setType(extName);
        diskFiles.setCourseId(courseId);
        diskFiles.setPath(fileDest);
        diskFiles.setUserId(userId);
        diskFiles.setCategory(category);

        try {
            byte[] bytes = file.getBytes();
            double size = BigDecimal.valueOf(bytes.length).divide(BigDecimal.valueOf(1024), 3, RoundingMode.HALF_UP).doubleValue();
            diskFiles.setSize(size);

            // 文件上传
            // File upload
            minIOService.uploadFile(fileDest, file, bucket);

            // 进行文件分析
            // Perform file analysis
            if (analysis != 0) {
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    HttpPost analyzePost = new HttpPost("http://dev.xlearnedu.com:5004/analyze");
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.addBinaryBody("file", bytes, ContentType.MULTIPART_FORM_DATA, filename);
                    builder.addTextBody("category", category, ContentType.TEXT_PLAIN);
                    analyzePost.setEntity(builder.build());

                    try (CloseableHttpResponse analyzeResponse = httpClient.execute(analyzePost)) {
                        if (analyzeResponse.getCode() == 200) {
                            String analyzeResponseJson = EntityUtils.toString(analyzeResponse.getEntity());
                            ObjectMapper objectMapper = new ObjectMapper();
                            JsonNode analyzeRootNode = objectMapper.readTree(analyzeResponseJson);
                            String result = analyzeRootNode.path("result").asText();
                            summary.setSummary(result);
                        } else {
                            throw new IOException("Failed to call analyze API. HTTP Status: " + analyzeResponse.getCode());
                        }
                    }
                }
            }

            diskFilesMapper.insert(diskFiles);
            summary.setId(diskFiles.getId());
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(ResultCodeEnum.FILE_UPLOAD_ERROR);
        }

        if (upload2RAG) {
            // ============ 异步调用部分 ==============
            // 在这里触发异步上传到 5100/file 的逻辑，不阻塞 add 的返回
            asyncFileUploadService.asyncUploadFile(courseId, file);
        }

        return summary;
    }

    /**
     * 覆盖文件
     * Overwrite file
     */
    public void overwrite(MultipartFile file, Integer courseId, Integer userId) {
        // 创建存储路径
        // Create storage path
        String path=filePath+courseId+"/";
        if (!FileUtil.exist(filePath)) {
            FileUtil.mkdir(path);
        }

        //file info
        String filename=file.getOriginalFilename();
        String fullpath=path+filename;


        // 查找重复文件
        // Find duplicate files
        List<DiskFiles> exist=new ArrayList<>();
        DiskFiles oldone=new DiskFiles();
        exist=diskFilesMapper.selectByCourseName(courseId);
        Boolean duplicate=false;
        for (int i=0;i<exist.size();i++){
            if (exist.get(i).getName().equals(filename)){
                duplicate=true;
                oldone=exist.get(i);
            }
        }
        if (!duplicate){
            throw new CustomException(ResultCodeEnum.FILE_NOT_FOUND);
        }
        String now = DateUtil.now();
        oldone.setCreateTime(now);
        oldone.setUserId(userId);

        // 删除旧文件
        // Delete the old file
        FileUtil.del(oldone.getPath());

        try {
            byte[] bytes = file.getBytes();  // byte
            double size = BigDecimal.valueOf(bytes.length).divide(BigDecimal.valueOf(1024), 3, RoundingMode.HALF_UP).doubleValue();
            oldone.setSize(size);
            // 文件上传 file upload
            file.transferTo(new File(fullpath));
            diskFilesMapper.updateById(oldone);
        } catch (Exception e) {
            log.error("upload failed", e);
        }
    }

    /**
     * 新增
     */



    /**
     * 根据 ID 删除文件
     * Delete file by ID
     */
    public void deleteById(Integer id) {
        diskFilesMapper.deleteById(id);
    }
    public void deepDelete(Integer id) {
        DiskFiles diskFiles = diskFilesMapper.selectById(id);
        if (diskFiles == null) {
            throw new CustomException(ResultCodeEnum.FILE_NOT_FOUND);
        }

        diskFilesMapper.deleteById(id);  // 删除当前的文件记录


    }


    /**
     * 批量删除文件
     * Batch delete files
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            diskFilesMapper.deleteById(id);
        }
    }

    /**
     * 修改文件信息
     * Update file information
     */
    public void updateById(DiskFiles diskFiles) {
        diskFilesMapper.updateById(diskFiles);
    }

    /**
     * 根据 ID 查询文件
     * Select file by ID
     */
    public DiskFiles selectById(Integer id) {
        return diskFilesMapper.selectById(id);
    }

    /**
     * 查询所有文件
     * Select all files
     */
    public List<DiskFiles> selectAll(DiskFiles diskFiles) {
        return diskFilesMapper.selectAll(diskFiles);
    }


}
