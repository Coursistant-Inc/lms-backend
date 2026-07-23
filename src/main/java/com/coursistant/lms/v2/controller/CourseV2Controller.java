package com.coursistant.lms.v2.controller;

import com.coursistant.lms.v2.common.ApiResponse;
import com.coursistant.lms.v2.dto.*;
import com.coursistant.lms.v2.entity.FileReferenceEntity;
import com.coursistant.lms.v2.service.CourseV2Service;
import com.coursistant.lms.v2.service.FileV2Service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;



import java.io.IOException;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/v2/courses")
@Slf4j
@RequiredArgsConstructor
public class CourseV2Controller {
    private final CourseV2Service courseService;
    private final FileV2Service fileService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CoursePreviewResponse>>> getCourses(
            @RequestAttribute("userId") Integer userId
    ) {
        var results = courseService.getCourses(userId);
        return ResponseEntity.ok(
                ApiResponse.success("Success querying courses", results)
        );
    }

//     @GetMapping
//     public ResponseEntity

    @GetMapping("/{courseId}/detail")
    public ResponseEntity<ApiResponse<CourseDetailV2DTO>> getCourseDetail(
            @PathVariable Long courseId, @RequestParam Integer studentId
    ) {
        // TODO: Structured logging (already annotated with Slf4j)
        try {
            CourseDetailV2DTO detail = courseService.getCourseDetail(courseId, studentId);
            return ResponseEntity.ok(
                    ApiResponse.success("Querying course detail success", detail)
            );
        } catch (RuntimeException e) {
            // TODO: Domain exception encapsulation here
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "Course doesn't exist"));
        }
    }

    @PostMapping("/new")
    public ResponseEntity<ApiResponse<Long>> createCourse(
            @RequestAttribute("userId") Integer userId,
            @RequestBody CreateCourseRequest request
    ) {
        var course = courseService.createCourse(userId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Creating course success", course.getId())
        );
    }

    @PostMapping("/{courseId}/units/new")
    public ResponseEntity<ApiResponse<Long>> createCourseUnit(
            @PathVariable Long courseId,
            @RequestBody CreateCourseUnitRequest request
    ) {
        var unit = courseService.createCourseUnit(courseId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Creating course unit success", unit.getId())
        );
    }

    @PostMapping("/{courseId}/units/{courseUnitId}/assignments/new")
    public ResponseEntity<ApiResponse<Long>> createAssignment(
            @SuppressWarnings("unused") @PathVariable Long courseId,
            @PathVariable Long courseUnitId,
            @RequestBody CreateAssignmentRequest request
    ) {
        // TODO: Also validate relation here
        var assignment = courseService.createAssignment(courseUnitId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Creating assignment success", assignment.getId())
        );
    }

    @PostMapping("/{courseId}/update")
    public ResponseEntity<ApiResponse<Long>> updateCourse(
            @PathVariable Long courseId,
            @RequestBody @Valid UpdateCourseRequest request
    ) {
        courseService.updateCourse(courseId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Updating course success", courseId)
        );
    }

    @PostMapping("/{courseId}/delete")
    public ResponseEntity<ApiResponse<Boolean>> deleteCourse(
            @PathVariable Long courseId
    ) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.ok(
                ApiResponse.success("Deleting course success", true)
        );
    }

    @PostMapping("/{courseId}/units/{courseUnitId}/delete")
    public ResponseEntity<ApiResponse<Boolean>> deleteCourseUnit(
            @SuppressWarnings("unused") @PathVariable Long courseId,
            @PathVariable Long courseUnitId
    ) {
        courseService.deleteCourseUnit(courseUnitId);
        return ResponseEntity.ok(
                ApiResponse.success("Deleting course unit success", true)
        );
    }

    @DeleteMapping("/{courseId}/units/{courseUnitId}/assignments/{assignmentId}/delete")
    public ResponseEntity<ApiResponse<Boolean>> deleteAssignment(
            @SuppressWarnings("unused") @PathVariable Long courseId,
            @SuppressWarnings("unused") @PathVariable Long courseUnitId,
            @PathVariable Long assignmentId
    ) {
        try{

            courseService.deleteAssignment(assignmentId);
            return ResponseEntity.ok(
                    ApiResponse.success("Deleting assignment success", true)
            );

        } catch (Exception e) {
            System.out.println("Could not delete assignment: "+e.getMessage());
            return ResponseEntity.internalServerError().build();
        }

    }

    @PostMapping("/{courseId}/units/{courseUnitId}/edit")
    public ResponseEntity <ApiResponse<Boolean>> editCourseUnit(
        @SuppressWarnings("unused") @PathVariable Long courseId,
        @SuppressWarnings("unused") @PathVariable Long courseUnitId,
        @RequestBody Map<Long, UpdateCourseRequest.CourseUnitUpdate> courseUnitUpdate
    ) {
        courseService.batchUpdateCourseUnits(courseId,courseUnitUpdate);
        return ResponseEntity.ok(
                ApiResponse.success("Unit(s) updated",true)
        );
    }

    @GetMapping("/courseDetailsForUser")
    public UserEnrolledAndCreatedCoursesDTO getUserCourses(@RequestParam Integer studentId) {
        UserEnrolledAndCreatedCoursesDTO userCourses = courseService.getUserCourses(studentId);
        return userCourses;
    }

    @PostMapping(value = "/upload/{courseId}/addFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(responses = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
        description = "upload success, returns file id", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))

    })
    public ResponseEntity<ApiResponse<Long>> uploadCourseFile(
        @RequestAttribute("userId") Integer userId,
        @PathVariable("courseId") Long courseId,
        @RequestPart("attachment") MultipartFile attachment
    ) {
        Long fileId = courseService.addCourseFile(attachment, courseId, userId);
        return ResponseEntity.ok(ApiResponse.success("Add course file success",fileId));

    }
    
    @GetMapping("/download/courseFile")
    public ResponseEntity<Resource> downloadFile(@RequestParam Long fileId, HttpServletRequest request) {
        Resource file = fileService.downloadFile(fileId);

        String contentType = null;

        try {
                contentType = request.getServletContext()
                .getMimeType(file.getFile().getAbsolutePath());
        } catch(IOException ex) {
                contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);     
    }

    @DeleteMapping("/deleteCourseFile")
    public ResponseEntity<ApiResponse<Boolean>> deleteCourseFile(@RequestParam("fileId") Long fileId) {

        courseService.deleteCourseFile(fileId);
        return ResponseEntity.ok(
                ApiResponse.success("Deleting attachment success", true)
        );
    }

    @PostMapping(value = "/upload/courseUnit/addFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(responses = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
        description = "upload success, returns file id", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))})

    public ResponseEntity<ApiResponse<Long>> uploadCourseUnitFile(
        @RequestAttribute("userId") Integer userId,
        @RequestPart("attachment") MultipartFile attachment,
        @RequestParam("courseUnitId") Long courseUnitId
    ) {
        Long fileId = courseService.addCourseUnitFile(attachment, courseUnitId, userId);
        return ResponseEntity.ok(ApiResponse.success("Add course unit file success", fileId));
    }

    @GetMapping(value = "/{courseUnitId}/getFile")
    public ResponseEntity<ApiResponse<List<FileResponse>>> getCourseUnitFiles(@PathVariable("courseUnitId") Long courseUnitId) {
        List<FileResponse> courseUnitFiles = courseService.getCourseUnitFiles(courseUnitId);
        return ResponseEntity.ok(ApiResponse.success("get course unit files success", courseUnitFiles));

    }

    @GetMapping("/download/courseUnitFile")
    public ResponseEntity<Resource> downloadCourseUnitFile(@RequestParam Long fileId, HttpServletRequest request) {
        Resource file = fileService.downloadFile(fileId);

        String contentType = null;

        try {
                contentType = request.getServletContext()
                .getMimeType(file.getFile().getAbsolutePath());
        } catch(IOException ex) {
                contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);     
    }

    @DeleteMapping("/deletecourseUnitFile")
    public ResponseEntity<ApiResponse<Boolean>> deleteCourseUnitFile(@RequestParam("fileId") Long fileId) {
        courseService.deleteCourseUnitFile(fileId);
        return ResponseEntity.ok(ApiResponse.success("Deleting course unit file success",true));
    }
    
}
