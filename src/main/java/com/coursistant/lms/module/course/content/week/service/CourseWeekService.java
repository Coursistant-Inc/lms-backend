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
import com.coursistant.lms.module.course.course.service.CourseAuditActions;
import com.coursistant.lms.module.course.course.service.CourseAuditService;
import com.coursistant.lms.module.file.service.MinIOService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.ActorContext;
import jakarta.annotation.Resource;
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

    @Resource
    private CourseAuditService courseAuditService;

    public List<WeekResponse> list(ActorContext actor, Integer courseId) {
        boolean draftView = courseContentAccessService.canViewDraftContent(actor, courseId);

        List<CourseWeek> weeks = courseWeekMapper.selectByCourseId(courseId).stream()
                .filter(w -> draftView || STATE_PUBLISHED.equals(w.getState()))
                .collect(Collectors.toList());
        return toResponses(weeks);
    }

    @Transactional
    public WeekResponse create(ActorContext actor, Integer courseId, CreateWeekRequest request) {
        var course = courseContentAccessService.requireCourseManagerWritable(actor, courseId);
        String title = requireTitle(request == null ? null : request.getTitle());

        Integer maxOrder = courseWeekMapper.selectMaxOrderPosition(courseId);
        CourseWeek week = new CourseWeek();
        week.setCourseId(courseId);
        week.setTitle(title);
        week.setOrderPosition(maxOrder == null ? 0 : maxOrder + 1);
        week.setState(STATE_DRAFT);
        courseWeekMapper.insert(week);

        CourseWeek created = courseWeekMapper.selectById(week.getId());
        courseAuditService.write(actor, courseId, course.getTenantId(), CourseAuditActions.WEEK_CREATED,
                CourseAuditActions.TARGET_WEEK, created.getId(), null, Map.of("title", title, "state", STATE_DRAFT), null);
        return toResponse(created);
    }

    @Transactional
    public WeekResponse rename(ActorContext actor, Integer courseId, Integer weekId, RenameWeekRequest request) {
        courseContentAccessService.requireWeekWritable(actor, courseId, weekId);
        String title = requireTitle(request == null ? null : request.getTitle());

        CourseWeek patch = new CourseWeek();
        patch.setId(weekId);
        patch.setTitle(title);
        courseWeekMapper.updateById(patch);

        return toResponse(courseWeekMapper.selectById(weekId));
    }

    @Transactional
    public List<WeekResponse> reorder(ActorContext actor, Integer courseId, ReorderWeeksRequest request) {
        courseContentAccessService.requireCourseManagerWritable(actor, courseId);
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

    @Transactional
    public WeekResponse publish(ActorContext actor, Integer courseId, Integer weekId) {
        CourseWeek week = courseContentAccessService.requireWeekWritable(actor, courseId, weekId);
        if (!STATE_PUBLISHED.equals(week.getState())) {
            courseWeekMapper.updateState(weekId, STATE_PUBLISHED);
        }
        return toResponse(courseWeekMapper.selectById(weekId));
    }

    @Transactional
    public WeekResponse unpublish(ActorContext actor, Integer courseId, Integer weekId) {
        CourseWeek week = courseContentAccessService.requireWeekWritable(actor, courseId, weekId);
        if (!STATE_DRAFT.equals(week.getState())) {
            courseWeekMapper.updateState(weekId, STATE_DRAFT);
        }
        return toResponse(courseWeekMapper.selectById(weekId));
    }

    @Transactional
    public void delete(ActorContext actor, Integer courseId, Integer weekId) {
        courseContentAccessService.requireWeekWritable(actor, courseId, weekId);
        int materialCount = courseMaterialMapper.countByWeekId(weekId);
        if (materialCount > 0) {
            throw new ApiException(ErrorType.WEEK_NOT_EMPTY);
        }
        courseWeekMapper.deleteById(weekId);
    }

    public ResponseEntity<StreamingResponseBody> downloadZip(ActorContext actor, Integer courseId, Integer weekId) {
        CourseWeek week = courseContentAccessService.requireWeekReadable(actor, courseId, weekId);
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
