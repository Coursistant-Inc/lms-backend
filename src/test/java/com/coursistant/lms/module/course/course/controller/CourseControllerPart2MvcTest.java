package com.coursistant.lms.module.course.course.controller;

import com.coursistant.lms.module.course.course.dto.CourseResponse;
import com.coursistant.lms.module.course.course.dto.CreateCourseRequest;
import com.coursistant.lms.module.course.course.dto.PatchCourseRequest;
import com.coursistant.lms.module.course.course.dto.ReassignPrimaryInstructorRequest;
import com.coursistant.lms.module.course.course.service.CourseAuthorizationService;
import com.coursistant.lms.module.course.course.service.CourseService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiExceptionHandler;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.security.ActorContext;
import com.coursistant.lms.shared.security.ActorContextResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CourseControllerPart2MvcTest {

    @Mock private CourseService courseService;
    @Mock private CourseAuthorizationService courseAuthorizationService;
    @Mock private ActorContextResolver actorContextResolver;

    @InjectMocks
    private CourseController courseController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(courseController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void create_usesActorContext() throws Exception {
        ActorContext actor = user(7);
        when(actorContextResolver.resolve(any())).thenReturn(actor);
        CourseResponse resp = new CourseResponse();
        resp.setId(1);
        when(courseService.create(eq(actor), any(CreateCourseRequest.class), any())).thenReturn(resp);

        CreateCourseRequest body = new CreateCourseRequest();
        body.setCourseCode("CS");
        body.setTitle("T");
        mockMvc.perform(post("/v2/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "k1")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void patch_archived_returnsCourseArchived() throws Exception {
        ActorContext actor = user(7);
        when(actorContextResolver.resolve(any())).thenReturn(actor);
        when(courseService.patch(eq(actor), eq(3), any(PatchCourseRequest.class), any()))
                .thenThrow(new ApiException(ErrorType.COURSE_ARCHIVED));

        mockMvc.perform(patch("/v2/courses/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "k2")
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COURSE_ARCHIVED"));
    }

    @Test
    void delete_nonEmpty_409() throws Exception {
        ActorContext actor = user(7);
        when(actorContextResolver.resolve(any())).thenReturn(actor);
        doThrow(new ApiException(ErrorType.CONFLICT, "Course is not empty; archive instead of delete"))
                .when(courseService).delete(eq(actor), eq(3), any());

        mockMvc.perform(delete("/v2/courses/3"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void reassign_forbiddenForInstructor() throws Exception {
        ActorContext actor = user(7);
        when(actorContextResolver.resolve(any())).thenReturn(actor);
        when(courseService.reassignPrimaryInstructor(eq(actor), eq(3), any(ReassignPrimaryInstructorRequest.class), any()))
                .thenThrow(new ApiException(ErrorType.FORBIDDEN));

        mockMvc.perform(post("/v2/courses/3/primary-instructor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "k3")
                        .content("{\"primaryInstructorUserId\":9}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void transferInstructor_routeRemoved() throws Exception {
        // Standalone MockMvc surfaces missing mappings as 500 NoHandlerFoundException.
        mockMvc.perform(post("/v2/courses/3/transfer-instructor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newInstructorId\":9}"))
                .andExpect(status().isInternalServerError());
    }

    private ActorContext user(int id) {
        return new ActorContext(ActorContext.ACTOR_USER, id, RoleEnum.USER.name(), 1, "INSTRUCTOR", "ACTIVE");
    }
}
