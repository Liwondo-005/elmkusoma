package tz.elmkusoma.nursery.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.GlobalExceptionHandler;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nursery.dto.response.NurseryActivityResponse;
import tz.elmkusoma.nursery.service.NurseryActivityService;
import tz.elmkusoma.nursery.service.NurseryMilestoneService;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code NurseryController} must forward the server-validated
 * {@code X-Institution-Id} scope header into every service call, and surface
 * cross-tenant denials as 403 / missing resources as 404 / a missing scope
 * header as 400. Uses standalone MockMvc (no Spring context) with the real
 * GlobalExceptionHandler.
 */
class NurseryScopeSecurityTest {

    private NurseryActivityService activityService;
    private NurseryMilestoneService milestoneService;
    private MockMvc mockMvc;

    private UUID institutionId;
    private UUID activityId;
    private UUID studentId;
    private UUID milestoneId;

    @BeforeEach
    void setUp() {
        activityService = mock(NurseryActivityService.class);
        milestoneService = mock(NurseryMilestoneService.class);
        NurseryController controller = new NurseryController(activityService, milestoneService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        institutionId = UUID.randomUUID();
        activityId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        milestoneId = UUID.randomUUID();
    }

    // ── activity reads ──

    @Test
    void getActivity_forwardsInstitutionHeaderToService() throws Exception {
        when(activityService.getById(institutionId, activityId))
                .thenReturn(NurseryActivityResponse.builder().id(activityId).build());

        mockMvc.perform(get("/v1/nursery/activities/{id}", activityId)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(activityService).getById(institutionId, activityId);
    }

    @Test
    void getActivity_missingInstitutionHeaderIsRejectedWith400() throws Exception {
        mockMvc.perform(get("/v1/nursery/activities/{id}", activityId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(activityService);
    }

    @Test
    void getActivity_crossInstitutionDenialIsMappedTo403() throws Exception {
        when(activityService.getById(institutionId, activityId))
                .thenThrow(new ForbiddenException("You do not have access to this resource"));

        mockMvc.perform(get("/v1/nursery/activities/{id}", activityId)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("You do not have access to this resource"));
    }

    @Test
    void getActivity_unknownResourceIsMappedTo404() throws Exception {
        when(activityService.getById(institutionId, activityId))
                .thenThrow(new ResourceNotFoundException("Nursery activity not found"));

        mockMvc.perform(get("/v1/nursery/activities/{id}", activityId)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Nursery activity not found"));
    }

    @Test
    void getActivitiesByClass_forwardsInstitutionHeaderToService() throws Exception {
        UUID classId = UUID.randomUUID();
        when(activityService.getByClassGroupId(institutionId, classId)).thenReturn(List.of());

        mockMvc.perform(get("/v1/nursery/activities")
                        .param("classId", classId.toString())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk());

        verify(activityService).getByClassGroupId(institutionId, classId);
    }

    @Test
    void getActivitiesByType_forwardsInstitutionHeaderToService() throws Exception {
        when(activityService.getByType(institutionId, "EDUCATIONAL")).thenReturn(List.of());

        mockMvc.perform(get("/v1/nursery/activities/type/{type}", "EDUCATIONAL")
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk());

        verify(activityService).getByType(institutionId, "EDUCATIONAL");
    }

    // ── milestone reads and writes ──

    @Test
    void getMilestonesByStudent_forwardsInstitutionHeaderToService() throws Exception {
        when(milestoneService.getByStudentId(institutionId, studentId)).thenReturn(List.of());

        mockMvc.perform(get("/v1/nursery/milestones/student/{studentId}", studentId)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk());

        verify(milestoneService).getByStudentId(institutionId, studentId);
    }

    @Test
    void getMilestonesByStudent_missingInstitutionHeaderIsRejectedWith400() throws Exception {
        mockMvc.perform(get("/v1/nursery/milestones/student/{studentId}", studentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(milestoneService);
    }

    @Test
    void getMilestone_crossInstitutionDenialIsMappedTo403() throws Exception {
        when(milestoneService.getById(institutionId, milestoneId))
                .thenThrow(new ForbiddenException("You do not have access to this resource"));

        mockMvc.perform(get("/v1/nursery/milestones/{id}", milestoneId)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You do not have access to this resource"));
    }

    @Test
    void deleteMilestone_forwardsHeaderAndMapsCrossInstitutionDenialTo403() throws Exception {
        doThrow(new ForbiddenException("You do not have access to this resource"))
                .when(milestoneService).delete(institutionId, milestoneId);

        mockMvc.perform(delete("/v1/nursery/milestones/{id}", milestoneId)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden());

        verify(milestoneService).delete(institutionId, milestoneId);
    }

    @Test
    void deleteMilestone_missingInstitutionHeaderIsRejectedWith400() throws Exception {
        mockMvc.perform(delete("/v1/nursery/milestones/{id}", milestoneId))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(milestoneService);
    }

    @Test
    void anyScopedRead_neverReachesTheServiceWithoutAnInstitutionHeader() throws Exception {
        mockMvc.perform(get("/v1/nursery/activities")
                        .param("classId", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/nursery/activities/type/{type}", "SONG"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(activityService);
        verifyNoInteractions(milestoneService);
    }
}
