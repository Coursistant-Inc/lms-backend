package com.coursistant.lms.module.course.course.controller;

import com.coursistant.lms.module.course.course.dto.CoursePageResponse;
import com.coursistant.lms.module.course.course.dto.CourseResponse;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.service.CourseAuthorizationService;
import com.coursistant.lms.module.course.course.service.CourseService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiExceptionHandler;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.security.ActorContext;
import com.coursistant.lms.shared.security.ActorContextResolver;
import com.coursistant.lms.shared.security.AuthzService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CourseControllerSecurityTest {

    @Mock
    private CourseService courseService;
    @Mock
    private CourseAuthorizationService courseAuthorizationService;
    @Mock
    private ActorContextResolver actorContextResolver;
    @Mock
    private AuthzService authzService;

    @InjectMocks
    private CourseController courseController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(courseController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void getById_unauthenticated_401() throws Exception {
        when(actorContextResolver.resolve(any())).thenThrow(new ApiException(ErrorType.UNAUTHORIZED));
        mockMvc.perform(get("/v2/courses/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void getById_crossTenant_404_courseNotFound() throws Exception {
        ActorContext actor = new ActorContext(ActorContext.ACTOR_USER, 5, RoleEnum.TENANT_ADMIN.name(),
                1, "NOT_APPLICABLE", "ACTIVE");
        when(actorContextResolver.resolve(any())).thenReturn(actor);
        when(courseAuthorizationService.requireVisibleCourse(eq(actor), eq(99)))
                .thenThrow(new ApiException(ErrorType.COURSE_NOT_FOUND));

        mockMvc.perform(get("/v2/courses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("@"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SELECT"))));
    }

    @Test
    void getById_visible_200() throws Exception {
        ActorContext actor = new ActorContext(ActorContext.ACTOR_USER, 7, RoleEnum.USER.name(),
                1, "INSTRUCTOR", "ACTIVE");
        when(actorContextResolver.resolve(any())).thenReturn(actor);
        when(courseAuthorizationService.requireVisibleCourse(eq(actor), eq(10))).thenReturn(new Course());
        CourseResponse resp = new CourseResponse();
        resp.setId(10);
        resp.setTitle("Intro");
        when(courseService.getById(10)).thenReturn(resp);

        mockMvc.perform(get("/v2/courses/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void list_systemAdmin_canFilterTenantId() throws Exception {
        ActorContext actor = new ActorContext(ActorContext.ACTOR_ADMIN, 1, RoleEnum.SYSTEM_ADMIN.name(),
                null, null, "ACTIVE");
        when(actorContextResolver.resolve(any())).thenReturn(actor);
        CoursePageResponse page = new CoursePageResponse();
        page.setItems(java.util.List.of());
        page.setPage(0);
        page.setSize(20);
        page.setTotal(0);
        when(courseService.listForBrowse(eq(actor), isNull(), isNull(), eq(999), isNull(), isNull())).thenReturn(page);

        mockMvc.perform(get("/v2/courses").param("tenantId", "999"))
                .andExpect(status().isOk());
    }
}

