package tz.elmkusoma.grading.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tz.elmkusoma.academic.repository.AcademicYearRepository;
import tz.elmkusoma.academic.repository.TermRepository;
import tz.elmkusoma.common.exception.ResourceNotFoundException;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.GlobalExceptionHandler;
import tz.elmkusoma.grading.dto.response.GradeBoundaryResponse;
import tz.elmkusoma.grading.dto.response.GradingScaleResponse;
import tz.elmkusoma.grading.dto.response.ReportCardResponse;
import tz.elmkusoma.grading.service.GradeBoundaryService;
import tz.elmkusoma.grading.service.GradebookService;
import tz.elmkusoma.grading.service.GradingScaleService;
import tz.elmkusoma.grading.service.ReportCardService;
import tz.elmkusoma.grading.service.RubricService;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP tenancy wiring of {@link GradingController}: scale/boundary endpoints
 * forward the institution + role to the institution-scoped services (denied via
 * {@link ForbiddenException} → 403), and the report-card status PATCH reuses the
 * existing report-card read idiom (403 {@code ApiResponse.error} on cross-institution
 * publish, platform ADMIN bypass, 404 without an existence oracle).
 * Uses standalone MockMvc (no Spring context) with the real GlobalExceptionHandler.
 */
class GradingScopeSecurityTest {

    private static final String GUARD_ERROR = "You do not have access to this resource";
    private static final String RC_STATUS_ERROR =
            "Access denied: report card belongs to another institution";

    private GradingScaleService gradingScaleService;
    private GradeBoundaryService gradeBoundaryService;
    private ReportCardService reportCardService;
    private StudentRepository studentRepository;
    private MockMvc mockMvc;

    private UUID scaleId;
    private UUID boundaryId;
    private UUID cardId;
    private UUID studentId;
    private UUID ownInstitutionId;
    private UUID foreignInstitutionId;

    @BeforeEach
    void setUp() {
        gradingScaleService = mock(GradingScaleService.class);
        gradeBoundaryService = mock(GradeBoundaryService.class);
        reportCardService = mock(ReportCardService.class);
        studentRepository = mock(StudentRepository.class);
        GradebookService gradebookService = mock(GradebookService.class);
        RubricService rubricService = mock(RubricService.class);
        TermRepository termRepository = mock(TermRepository.class);
        AcademicYearRepository academicYearRepository = mock(AcademicYearRepository.class);

        GradingController controller = new GradingController(
                gradingScaleService, gradeBoundaryService, reportCardService,
                gradebookService, rubricService, studentRepository,
                termRepository, academicYearRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        scaleId = UUID.randomUUID();
        boundaryId = UUID.randomUUID();
        cardId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        ownInstitutionId = UUID.randomUUID();
        foreignInstitutionId = UUID.randomUUID();
    }

    // ── grading scales: institution + role forwarded, service denial → 403 ──

    @Test
    void getGradingScale_crossInstitutionInstitutionAdmin_403() throws Exception {
        when(gradingScaleService.getById(scaleId, ownInstitutionId, "INSTITUTION_ADMIN"))
                .thenThrow(new ForbiddenException(GUARD_ERROR));

        mockMvc.perform(get("/v1/grading/scales/{id}", scaleId)
                        .header("X-Institution-Id", ownInstitutionId.toString())
                        .requestAttr("userRole", "INSTITUTION_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value(GUARD_ERROR));

        verify(gradingScaleService).getById(scaleId, ownInstitutionId, "INSTITUTION_ADMIN");
    }

    @Test
    void getGradingScale_platformAdmin_bypassesInstitutionCheck() throws Exception {
        when(gradingScaleService.getById(scaleId, ownInstitutionId, "ADMIN"))
                .thenReturn(GradingScaleResponse.builder().id(scaleId).name("O-Level").build());

        mockMvc.perform(get("/v1/grading/scales/{id}", scaleId)
                        .header("X-Institution-Id", ownInstitutionId.toString())
                        .requestAttr("userRole", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateGradingScale_crossInstitutionInstitutionAdmin_403_neverMutates() throws Exception {
        when(gradingScaleService.update(
                eq(scaleId), eq(ownInstitutionId), eq("INSTITUTION_ADMIN"), any()))
                .thenThrow(new ForbiddenException(GUARD_ERROR));

        mockMvc.perform(put("/v1/grading/scales/{id}", scaleId)
                        .header("X-Institution-Id", ownInstitutionId.toString())
                        .requestAttr("userRole", "INSTITUTION_ADMIN")
                        .contentType("application/json")
                        .content("{\"name\":\"Updated\",\"scaleType\":\"PERCENTAGE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(gradingScaleService).update(
                eq(scaleId), eq(ownInstitutionId), eq("INSTITUTION_ADMIN"), any());
    }

    @Test
    void deleteGradingScale_crossInstitutionInstitutionAdmin_403_neverDeletes() throws Exception {
        doThrow(new ForbiddenException(GUARD_ERROR))
                .when(gradingScaleService).delete(scaleId, ownInstitutionId, "INSTITUTION_ADMIN");

        mockMvc.perform(delete("/v1/grading/scales/{id}", scaleId)
                        .header("X-Institution-Id", ownInstitutionId.toString())
                        .requestAttr("userRole", "INSTITUTION_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(gradingScaleService).delete(scaleId, ownInstitutionId, "INSTITUTION_ADMIN");
    }

    // ── grade boundaries: institution + role forwarded, service denial → 403 ──

    @Test
    void getGradeBoundaries_crossInstitutionInstitutionAdmin_403() throws Exception {
        when(gradeBoundaryService.getByGradingScaleId(scaleId, ownInstitutionId, "INSTITUTION_ADMIN"))
                .thenThrow(new ForbiddenException(GUARD_ERROR));

        mockMvc.perform(get("/v1/grading/boundaries/scale/{scaleId}", scaleId)
                        .header("X-Institution-Id", ownInstitutionId.toString())
                        .requestAttr("userRole", "INSTITUTION_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value(GUARD_ERROR));
    }

    @Test
    void getGradeBoundary_crossInstitutionInstitutionAdmin_403() throws Exception {
        when(gradeBoundaryService.getById(boundaryId, ownInstitutionId, "INSTITUTION_ADMIN"))
                .thenThrow(new ForbiddenException(GUARD_ERROR));

        mockMvc.perform(get("/v1/grading/boundaries/{id}", boundaryId)
                        .header("X-Institution-Id", ownInstitutionId.toString())
                        .requestAttr("userRole", "INSTITUTION_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getGradeBoundary_platformAdmin_bypassesInstitutionCheck() throws Exception {
        when(gradeBoundaryService.getById(boundaryId, ownInstitutionId, "ADMIN"))
                .thenReturn(GradeBoundaryResponse.builder().id(boundaryId).gradeLabel("A").build());

        mockMvc.perform(get("/v1/grading/boundaries/{id}", boundaryId)
                        .header("X-Institution-Id", ownInstitutionId.toString())
                        .requestAttr("userRole", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateGradeBoundary_crossInstitutionInstitutionAdmin_403_neverMutates() throws Exception {
        when(gradeBoundaryService.update(
                eq(boundaryId), eq(ownInstitutionId), eq("INSTITUTION_ADMIN"), any()))
                .thenThrow(new ForbiddenException(GUARD_ERROR));

        mockMvc.perform(put("/v1/grading/boundaries/{id}", boundaryId)
                        .header("X-Institution-Id", ownInstitutionId.toString())
                        .requestAttr("userRole", "INSTITUTION_ADMIN")
                        .contentType("application/json")
                        .content("{\"gradingScaleId\":\"" + scaleId + "\",\"gradeLabel\":\"B\","
                                + "\"minPercentage\":50,\"maxPercentage\":74}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(gradeBoundaryService).update(
                eq(boundaryId), eq(ownInstitutionId), eq("INSTITUTION_ADMIN"), any());
    }

    @Test
    void deleteGradeBoundary_crossInstitutionInstitutionAdmin_403_neverDeletes() throws Exception {
        doThrow(new ForbiddenException(GUARD_ERROR))
                .when(gradeBoundaryService).delete(boundaryId, ownInstitutionId, "INSTITUTION_ADMIN");

        mockMvc.perform(delete("/v1/grading/boundaries/{id}", boundaryId)
                        .header("X-Institution-Id", ownInstitutionId.toString())
                        .requestAttr("userRole", "INSTITUTION_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(gradeBoundaryService).delete(boundaryId, ownInstitutionId, "INSTITUTION_ADMIN");
    }

    // ── report-card status PATCH: existing read idiom copied ──

    @Test
    void updateReportCardStatus_crossInstitutionInstitutionAdmin_403_neverPublishes() throws Exception {
        when(reportCardService.getById(cardId)).thenReturn(ReportCardResponse.builder()
                .id(cardId).studentId(studentId).status("DRAFT").build());
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(Student.builder()
                .id(studentId).institutionId(foreignInstitutionId).build()));

        mockMvc.perform(patch("/v1/grading/report-cards/{id}/status", cardId)
                        .param("status", "PUBLISHED")
                        .requestAttr("institutionId", ownInstitutionId)
                        .requestAttr("userEmail", "admin@test.com")
                        .requestAttr("userRole", "INSTITUTION_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value(RC_STATUS_ERROR));

        verify(reportCardService, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    void updateReportCardStatus_sameInstitution_200() throws Exception {
        when(reportCardService.getById(cardId)).thenReturn(ReportCardResponse.builder()
                .id(cardId).studentId(studentId).status("DRAFT").build());
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(Student.builder()
                .id(studentId).institutionId(ownInstitutionId).build()));
        when(reportCardService.updateStatus(cardId, "PUBLISHED", "admin@test.com", "INSTITUTION_ADMIN"))
                .thenReturn(ReportCardResponse.builder().id(cardId).status("PUBLISHED").build());

        mockMvc.perform(patch("/v1/grading/report-cards/{id}/status", cardId)
                        .param("status", "PUBLISHED")
                        .requestAttr("institutionId", ownInstitutionId)
                        .requestAttr("userEmail", "admin@test.com")
                        .requestAttr("userRole", "INSTITUTION_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void updateReportCardStatus_platformAdmin_bypassesInstitutionCheck() throws Exception {
        when(reportCardService.updateStatus(cardId, "PUBLISHED", "admin@test.com", "ADMIN"))
                .thenReturn(ReportCardResponse.builder().id(cardId).status("PUBLISHED").build());

        mockMvc.perform(patch("/v1/grading/report-cards/{id}/status", cardId)
                        .param("status", "PUBLISHED")
                        .requestAttr("institutionId", ownInstitutionId)
                        .requestAttr("userEmail", "admin@test.com")
                        .requestAttr("userRole", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // The ownership lookup must be skipped entirely for platform ADMIN.
        verify(reportCardService, never()).getById(any());
    }

    @Test
    void updateReportCardStatus_unknownCard_404_noOracle() throws Exception {
        when(reportCardService.getById(cardId))
                .thenThrow(new ResourceNotFoundException("Report card not found"));

        mockMvc.perform(patch("/v1/grading/report-cards/{id}/status", cardId)
                        .param("status", "PUBLISHED")
                        .requestAttr("institutionId", ownInstitutionId)
                        .requestAttr("userEmail", "admin@test.com")
                        .requestAttr("userRole", "INSTITUTION_ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verify(reportCardService, never()).updateStatus(any(), any(), any(), any());
    }
}
