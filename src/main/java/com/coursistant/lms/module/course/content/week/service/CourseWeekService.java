package com.coursistant.lms.module.course.content.week.service;

import com.coursistant.lms.module.course.content.CourseContentAccessService;
import com.coursistant.lms.module.course.content.CourseContentFilePolicy;
import com.coursistant.lms.module.course.content.material.entity.CourseMaterial;
import com.coursistant.lms.module.course.content.material.repository.CourseMaterialMapper;
import com.coursistant.lms.module.course.content.material.service.MaterialResponseAssembler;
import com.coursistant.lms.module.course.content.week.dto.CreateWeekRequest;
import com.coursistant.lms.module.course.content.week.dto.RenameWeekRequest;
import com.coursistant.lms.module.course.content.week.dto.ReorderWeeksRequest;
import com.coursistant.lms.module.course.content.week.dto.WeekResponse;
import com.coursistant.lms.module.course.content.week.entity.CourseWeek;
import com.coursistant.lms.module.course.content.week.repository.CourseWeekMapper;
import com.coursistant.lms.module.file.service.MinIOService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class CourseWeekService {

    private static final Logger log = LoggerFactory.getLogger(CourseWeekService.class);

    private static final String STATE_DRAFT = CourseContentAccessService.WEEK_STATE_DRAFT;
    private static final String STATE_PUBLISHED = CourseContentAccessService.WEEK_STATE_PUBLISHED;
    private static final String TYPE_FILE = "FILE";

    @Resource
    private CourseWeekMapper courseWeekMapper;

    @Resource
    private CourseMaterialMapper courseMaterialMapper;

    @Resource
    private CourseContentAccessService courseContentAccessService;

    @Resource
    private CourseContentFilePolicy courseContentFilePolicy;

    @Resource
    private MinIOService minIOService;

    @Resource
    private MaterialResponseAssembler materialResponseAssembler;

    public List<WeekResponse> list(HttpServletRequest request, Integer courseId, Integer userId) {
        courseContentAccessService.requireCourse(courseId);
        boolean instructorView = courseContentAccessService.resolveInstructorView(request, courseId, userId);

        List<CourseWeek> weeks = courseWeekMapper.selectByCourseId(courseId).stream()
                .filter(w -> instructorView || STATE_PUBLISHED.equals(w.getState()))
                .collect(Collectors.toList());
        return toResponses(weeks);
    }

    @Transactional
    public WeekResponse create(Integer courseId, Integer userId, CreateWeekRequest request) {
        courseContentAccessService.requireCourseWritable(courseId, userId);
        String title = requireTitle(request == null ? null : request.getTitle());

        Integer maxOrder = courseWeekMapper.selectMaxOrderPosition(courseId);
        CourseWeek week = new CourseWeek();
        week.setCourseId(courseId);
        week.setTitle(title);
        week.setOrderPosition(maxOrder == null ? 0 : maxOrder + 1);
        week.setState(STATE_DRAFT);
        courseWeekMapper.insert(week);

        return toResponse(courseWeekMapper.selectById(week.getId()));
    }

    public WeekResponse rename(Integer courseId, Integer weekId, Integer userId, RenameWeekRequest request) {
        courseContentAccessService.requireWeekWritable(courseId, weekId, userId);
        String title = requireTitle(request == null ? null : request.getTitle());

        CourseWeek patch = new CourseWeek();
        patch.setId(weekId);
        patch.setTitle(title);
        courseWeekMapper.updateById(patch);

        return toResponse(courseWeekMapper.selectById(weekId));
    }

    @Transactional
    public List<WeekResponse> reorder(Integer courseId, Integer userId, ReorderWeeksRequest request) {
        courseContentAccessService.requireCourseWritable(courseId, userId);
        if (request == null || request.getWeekIds() == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "weekIds is required");
        }

        List<CourseWeek> existing = courseWeekMapper.selectByCourseId(courseId);
        Set<Integer> existingIds = existing.stream().map(CourseWeek::getId).collect(Collectors.toSet());
        Set<Integer> requestedIds = new HashSet<>(request.getWeekIds());

        if (!existingIds.equals(requestedIds) || existingIds.size() != request.getWeekIds().size()) {
            throw new ApiException(ErrorType.BAD_REQUEST, "weekIds must contain exactly the course's current weeks");
        }

        int position = 0;
        for (Integer weekId : request.getWeekIds()) {
            courseWeekMapper.updateOrderPosition(weekId, position++);
        }

        return toResponses(courseWeekMapper.selectByCourseId(courseId));
    }

    public WeekResponse publish(Integer courseId, Integer weekId, Integer userId) {
        CourseWeek week = courseContentAccessService.requireWeekWritable(courseId, weekId, userId);
        if (!STATE_PUBLISHED.equals(week.getState())) {
            courseWeekMapper.updateState(weekId, STATE_PUBLISHED);
        }
        return toResponse(courseWeekMapper.selectById(weekId));
    }

    public WeekResponse unpublish(Integer courseId, Integer weekId, Integer userId) {
        CourseWeek week = courseContentAccessService.requireWeekWritable(courseId, weekId, userId);
        if (!STATE_DRAFT.equals(week.getState())) {
            courseWeekMapper.updateState(weekId, STATE_DRAFT);
        }
        return toResponse(courseWeekMapper.selectById(weekId));
    }

    public void delete(Integer courseId, Integer weekId, Integer userId) {
        courseContentAccessService.requireWeekWritable(courseId, weekId, userId);
        int materialCount = courseMaterialMapper.countByWeekId(weekId);
        if (materialCount > 0) {
            throw new ApiException(ErrorType.WEEK_NOT_EMPTY);
        }
        courseWeekMapper.deleteById(weekId);
    }

    public ResponseEntity<StreamingResponseBody> downloadZip(HttpServletRequest request, Integer courseId,
                                                              Integer weekId, Integer userId) {
        CourseWeek week = courseContentAccessService.requireWeekReadable(request, courseId, weekId, userId);
        List<CourseMaterial> fileMaterials = courseMaterialMapper.selectByWeekIdAndType(weekId, TYPE_FILE);
        if (fileMaterials.isEmpty()) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Week has no downloadable files");
        }

        StreamingResponseBody body = outputStream -> writeZip(fileMaterials, outputStream);
        String zipName = safeEntryName(week.getTitle().isBlank() ? "week-" + week.getId() : week.getTitle()) + ".zip";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipName + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(body);
    }

    private void writeZip(List<CourseMaterial> fileMaterials, OutputStream outputStream) throws java.io.IOException {
        Map<String, Integer> usedNames = new HashMap<>();
        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            for (CourseMaterial material : fileMaterials) {
                String baseName = material.getOriginalFilename() != null && !material.getOriginalFilename().isBlank()
                        ? material.getOriginalFilename()
                        : material.getDisplayName();
                String entryName = uniqueEntryName(safeEntryName(baseName), usedNames);

                zipOut.putNextEntry(new ZipEntry(entryName));
                try {
                    InputStream in = minIOService.downloadFile(material.getObjectKey(), courseContentFilePolicy.bucket());
                    try {
                        in.transferTo(zipOut);
                    } finally {
                        in.close();
                    }
                } catch (Exception e) {
                    log.warn("Failed to add material {} to week zip", material.getId(), e);
                } finally {
                    zipOut.closeEntry();
                }
            }
        }
    }

    private String uniqueEntryName(String name, Map<String, Integer> usedNames) {
        Integer count = usedNames.merge(name, 1, Integer::sum);
        if (count == 1) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        return base + "_" + (count - 1) + ext;
    }

    private String safeEntryName(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }
        return name.replace("/", "_").replace("\\", "_").trim();
    }

    private String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ApiException(ErrorType.PARAM_MISSING, "title is required");
        }
        String trimmed = title.trim();
        if (trimmed.length() > 255) {
            throw new ApiException(ErrorType.BAD_REQUEST, "title must be at most 255 characters");
        }
        return trimmed;
    }

    private List<WeekResponse> toResponses(List<CourseWeek> weeks) {
        List<WeekResponse> responses = new ArrayList<>();
        for (CourseWeek week : weeks) {
            responses.add(toResponse(week));
        }
        return responses;
    }

    private WeekResponse toResponse(CourseWeek week) {
        WeekResponse response = new WeekResponse();
        response.setId(week.getId());
        response.setCourseId(week.getCourseId());
        response.setTitle(week.getTitle());
        response.setOrderPosition(week.getOrderPosition());
        response.setState(week.getState());
        response.setCreatedAt(week.getCreatedAt());
        response.setUpdatedAt(week.getUpdatedAt());
        List<CourseMaterial> materials = courseMaterialMapper.selectByWeekId(week.getId());
        response.setMaterials(materialResponseAssembler.toResponses(materials));
        return response;
    }
}
