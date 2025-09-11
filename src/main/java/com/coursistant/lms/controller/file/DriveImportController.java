package com.coursistant.lms.controller.file;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.DTO.PathMultipartFile;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.service.file.DiskFilesService;
import com.coursistant.lms.utils.DriveBuilder;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.coursistant.lms.entity.FileSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import jakarta.annotation.Resource;

import java.util.logging.Logger;
/**
 * 磁盘文件前端操作接口
 * Disk files frontend operation API
 **/
@RestController
@RequestMapping("/drive")
public class DriveImportController {

    @Resource
    private DiskFilesService diskFilesService;

    @Resource
    private OAuth2AuthorizedClientService authorizedClientService;

    @Resource
    private DriveBuilder driveBuilder;

    private static final Logger logger = Logger.getLogger(DiskFilesController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 从 Google Drive 导入文件
     * Import file(s) from Google Drive
     */
    @PostMapping("/importFromGoogleDrive")
    public Result importFromGoogleDrive(@AuthenticationPrincipal(expression = "name") String principalName,
                                        @RequestParam("fileIds") String[] fileIds,
                                        @RequestParam("courseId") Integer courseId,
                                        @RequestParam("userId") Integer userId,
                                        @RequestParam("categories") String[] categories,
                                        @RequestParam("analysis") Integer analysis) throws Exception {

        if (fileIds.length != categories.length) {
            throw new CustomException(ResultCodeEnum.FILE_CATEGORY_MISMATCH);
        }

        // 获取当前用户 Google Drive 授权信息
        OAuth2AuthorizedClient client =
                authorizedClientService.loadAuthorizedClient("google-drive", principalName);
        if (client == null || client.getAccessToken() == null) {
            throw new CustomException(ResultCodeEnum.GOOGLE_DRIVE_NOT_AUTHORIZED);
        }

        Drive drive = driveBuilder.build(client.getAccessToken().getTokenValue());

        if (fileIds.length == 1) {
            // 单文件导入
            FileSummary summary = importSingleFileFromDrive(
                    drive, fileIds[0], courseId, userId, categories[0], analysis);

            return Result.success(summary);
        } else {
            // 多文件导入
            for (int i = 0; i < fileIds.length; i++) {
                importSingleFileFromDrive(
                        drive, fileIds[i], courseId, userId, categories[i], 0);
            }
            return Result.success();
        }
    }

    /**
     * 从 Google Drive 下载单个文件并调用原 add 方法
     */
    private FileSummary importSingleFileFromDrive(Drive drive,
                                                  String fileId,
                                                  Integer courseId,
                                                  Integer userId,
                                                  String category,
                                                  Integer analysis) throws Exception {
        // 获取文件元数据
        com.google.api.services.drive.model.File meta = drive.files()
                .get(fileId)
                .setFields("name,mimeType")
                .execute();

        boolean isGoogleDoc = meta.getMimeType() != null &&
                meta.getMimeType().startsWith("application/vnd.google-apps");

        String filename = meta.getName();
        String mime;
        InputStream in;

        if (isGoogleDoc) {
            mime = "application/pdf"; // 可改成需要的导出格式
            in = drive.files().export(fileId, mime).executeMediaAsInputStream();
            if (!filename.toLowerCase().endsWith(".pdf")) filename += ".pdf";
        } else {
            mime = meta.getMimeType();
            in = drive.files().get(fileId).executeMediaAsInputStream();
        }

        // 落地到临时文件
        Path tmp = Files.createTempFile("gdrive-", "-" + filename);
        try (var out = Files.newOutputStream(tmp)) {
            in.transferTo(out);
        }

        MultipartFile mf = new PathMultipartFile(tmp, "file", filename, mime);

        logRequest("importFromGoogleDrive", String.format(
                "fileName=%s, courseId=%d, userId=%d, category=%s, analysis=%d",
                filename, courseId, userId, category, analysis));

        FileSummary summary = diskFilesService.add(mf, courseId, userId, category, analysis,true);

        logResponse("importFromGoogleDrive", "Success");

        return summary;
    }
}
