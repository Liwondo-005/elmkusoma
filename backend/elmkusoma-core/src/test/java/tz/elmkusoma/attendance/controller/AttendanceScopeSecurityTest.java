package tz.elmkusoma.attendance.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tz.elmkusoma.attendance.service.AttendanceService;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.GlobalExceptionHandler;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code AttendanceController} must forward the server-validated
 * {@code X-Institution-Id} scope header into every read call, and surface
 * cross-tenant denials as 403 / missing resources as 404 / a missing scope
 * header as 400. Uses standalone MockMvc (no Spring context) with the real
 * GlobalExceptionHandler.
 */
class AttendanceScopeSecurityTest {

    private AttendanceService attendanceService;
    private MockMvc mockMvc;

    private UUID institutionId;
    private UUID classId;
    private UUID studentId;
    private UUID termId;

    @BeforeEach
    void setUp() {
        attendanceService = mock(AttendanceService.class);
        AttendanceController controller = new AttendanceController(
                attendanceService,
                mock(StudentRepository.class),
                mock(ParentStudentLinkRepository.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        institutionId = UUID.randomUUID();
        classId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        termId = UUID.randomUUID();
    }

    // ── GET /v1/attendance (class + date) ──

    @Test
    void getAttendance_forwardsInstitutionHeaderToService() throws Exception {
        LocalDate date = LocalDate.of(2026, 1, 5);
        when(attendanceService.getByClassAndDate(institutionId, classId, date)).thenReturn(List.of());

        mockMvc.perform(get("/v1/attendance")
                        .param("classId", classId.toString())
                        .param("date", "2026-01-05")
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(attendanceService).getByClassAndDate(institutionId, classId, date);
    }

    @Test
    void getAttendance_missingInstitutionHeaderIsRejectedWith400() throws Exception {
        mockMvc.perform(get("/v1/attendance")
                        .param("classId", classId.toString())
                        .param("date", "2026-01-05"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(attendanceService);
    }

    @Test
    void getAttendance_crossInstitutionDenialIsMappedTo403() throws Exception {
        when(attendanceService.getByClassAndDate(eq(institutionId), eq(classId), any(LocalDate.class)))
                .thenThrow(new ForbiddenException("You do not have access to this resource"));

        mockMvc.perform(get("/v1/attendance")
                        .param("classId", classId.toString())
                        .param("date", "2026-01-05")
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("You do not have access to this resource"));
    }

    // ── GET /v1/attendance/student/{studentId} ──

    @Test
    void getStudentAttendance_forwardsHeaderAndKeepsServiceScopeCheck() throws Exception {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(attendanceService.getByStudentAndDateRange(institutionId, studentId, start, end))
                .thenReturn(List.of());

        mockMvc.perform(get("/v1/attendance/student/{studentId}", studentId)
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31")
                        .requestAttr("userRole", "TEACHER")
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk());

        verify(attendanceService).getByStudentAndDateRange(institutionId, studentId, start, end);
    }

    @Test
    void getStudentAttendance_missingInstitutionHeaderIsRejectedWith400() throws Exception {
        mockMvc.perform(get("/v1/attendance/student/{studentId}", studentId)
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(attendanceService);
    }

    // ── GET /v1/attendance/summary ──

    @Test
    void getAttendanceSummary_forwardsInstitutionHeaderToService() throws Exception {
        when(attendanceService.getSummary(institutionId, studentId, termId)).thenReturn(null);

        mockMvc.perform(get("/v1/attendance/summary")
                        .param("studentId", studentId.toString())
                        .param("termId", termId.toString())
                        .requestAttr("userRole", "TEACHER")
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(attendanceService).getSummary(institutionId, studentId, termId);
    }

    @Test
    void getAttendanceSummary_crossInstitutionDenialIsMappedTo403() throws Exception {
        when(attendanceService.getSummary(eq(institutionId), eq(studentId), eq(termId)))
                .thenThrow(new ForbiddenException("You do not have access to this resource"));

        mockMvc.perform(get("/v1/attendance/summary")
                        .param("studentId", studentId.toString())
                        .param("termId", termId.toString())
                        .requestAttr("userRole", "TEACHER")
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You do not have access to this resource"));
    }

    @Test
    void getAttendanceSummary_unknownStudentIsMappedTo404() throws Exception {
        when(attendanceService.getSummary(institutionId, studentId, termId))
                .thenThrow(new ResourceNotFoundException("Student not found"));

        mockMvc.perform(get("/v1/attendance/summary")
                        .param("studentId", studentId.toString())
                        .param("termId", termId.toString())
                        .requestAttr("userRole", "TEACHER")
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Student not found"));
    }

    // ── GET /v1/attendance/summary/class/{classId} ──

    @Test
    void getClassAttendanceSummary_forwardsInstitutionHeaderToService() throws Exception {
        when(attendanceService.getByClassAndTerm(institutionId, classId, termId)).thenReturn(List.of());

        mockMvc.perform(get("/v1/attendance/summary/class/{classId}", classId)
                        .param("termId", termId.toString())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk());

        verify(attendanceService).getByClassAndTerm(institutionId, classId, termId);
    }

    @Test
    void getClassAttendanceSummary_crossInstitutionDenialIsMappedTo403() throws Exception {
        when(attendanceService.getByClassAndTerm(eq(institutionId), eq(classId), eq(termId)))
                .thenThrow(new ForbiddenException("You do not have access to this resource"));

        mockMvc.perform(get("/v1/attendance/summary/class/{classId}", classId)
                        .param("termId", termId.toString())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You do not have access to this resource"));
    }
}
