package tz.elmkusoma.nursery.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.GlobalExceptionHandler;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nursery.service.NurseryExtendedService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code NurseryExtendedController} must forward the server-validated
 * {@code X-Institution-Id} scope header into every scoped service call, and
 * surface cross-tenant denials as 403 / missing resources as 404 / a missing
 * scope header as 400. Uses standalone MockMvc (no Spring context) with the
 * real GlobalExceptionHandler.
 */
class NurseryExtendedScopeSecurityTest {

    private NurseryExtendedService service;
    private MockMvc mockMvc;

    private UUID institutionId;
    private UUID classId;
    private UUID studentId;
    private UUID storyId;
    private UUID feelingsId;

    @BeforeEach
    void setUp() {
        service = mock(NurseryExtendedService.class);
        NurseryExtendedController controller = new NurseryExtendedController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        institutionId = UUID.randomUUID();
        classId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        storyId = UUID.randomUUID();
        feelingsId = UUID.randomUUID();
    }

    // ── story reads and writes ──

    @Test
    void getStory_forwardsInstitutionHeaderToService() throws Exception {
        when(service.getStory(institutionId, storyId)).thenReturn(Map.of("id", storyId.toString()));

        mockMvc.perform(get("/v1/nursery/extended/stories/{id}", storyId)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(service).getStory(institutionId, storyId);
    }

    @Test
    void getStory_missingInstitutionHeaderIsRejectedWith400() throws Exception {
        mockMvc.perform(get("/v1/nursery/extended/stories/{id}", storyId))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void getStory_crossInstitutionDenialIsMappedTo403() throws Exception {
        when(service.getStory(institutionId, storyId))
                .thenThrow(new ForbiddenException("You do not have access to this resource"));

        mockMvc.perform(get("/v1/nursery/extended/stories/{id}", storyId)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You do not have access to this resource"));
    }

    @Test
    void getStory_unknownResourceIsMappedTo404() throws Exception {
        when(service.getStory(institutionId, storyId))
                .thenThrow(new ResourceNotFoundException("Story not found"));

        mockMvc.perform(get("/v1/nursery/extended/stories/{id}", storyId)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Story not found"));
    }

    @Test
    void getStoriesByClass_forwardsInstitutionHeaderToService() throws Exception {
        when(service.getStoriesByClass(institutionId, classId)).thenReturn(List.of());

        mockMvc.perform(get("/v1/nursery/extended/stories")
                        .param("classId", classId.toString())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk());

        verify(service).getStoriesByClass(institutionId, classId);
    }

    // ── student-keyed reads ──

    @Test
    void getDailyQuestsByStudent_forwardsInstitutionHeaderToService() throws Exception {
        when(service.getDailyQuestsByStudent(institutionId, studentId)).thenReturn(List.of());

        mockMvc.perform(get("/v1/nursery/extended/daily-quests/student/{studentId}", studentId)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk());

        verify(service).getDailyQuestsByStudent(institutionId, studentId);
    }

    @Test
    void getParentLearningByStudent_forwardsInstitutionHeaderToService() throws Exception {
        when(service.getParentLearningByStudent(institutionId, studentId)).thenReturn(List.of());

        mockMvc.perform(get("/v1/nursery/extended/parent-learning/student/{studentId}", studentId)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk());

        verify(service).getParentLearningByStudent(institutionId, studentId);
    }

    // ── Tanzania reads ──

    @Test
    void getTanzaniaByClass_forwardsInstitutionHeaderToService() throws Exception {
        when(service.getTanzaniaTopics(institutionId, classId)).thenReturn(List.of());

        mockMvc.perform(get("/v1/nursery/extended/tanzania")
                        .param("classId", classId.toString())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk());

        verify(service).getTanzaniaTopics(institutionId, classId);
    }

    @Test
    void getTanzaniaByCategory_forwardsInstitutionHeaderToService() throws Exception {
        when(service.getTanzaniaByCategory(institutionId, "LANDMARKS")).thenReturn(List.of());

        mockMvc.perform(get("/v1/nursery/extended/tanzania/category/{category}", "LANDMARKS")
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk());

        verify(service).getTanzaniaByCategory(institutionId, "LANDMARKS");
    }

    // ── deletes ──

    @Test
    void deleteFeelingsCheckin_forwardsHeaderAndMapsCrossInstitutionDenialTo403() throws Exception {
        doThrow(new ForbiddenException("You do not have access to this resource"))
                .when(service).deleteFeelingsCheckin(institutionId, feelingsId);

        mockMvc.perform(delete("/v1/nursery/extended/feelings/{id}", feelingsId)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden());

        verify(service).deleteFeelingsCheckin(institutionId, feelingsId);
    }

    @Test
    void deleteFeelingsCheckin_missingInstitutionHeaderIsRejectedWith400() throws Exception {
        mockMvc.perform(delete("/v1/nursery/extended/feelings/{id}", feelingsId))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void anyScopedRead_neverReachesTheServiceWithoutAnInstitutionHeader() throws Exception {
        mockMvc.perform(get("/v1/nursery/extended/stories")
                        .param("classId", classId.toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/nursery/extended/daily-quests/student/{studentId}", studentId))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/nursery/extended/tanzania/category/{category}", "LANDMARKS"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
