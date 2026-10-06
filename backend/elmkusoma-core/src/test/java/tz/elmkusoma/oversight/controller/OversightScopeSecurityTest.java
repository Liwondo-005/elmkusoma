package tz.elmkusoma.oversight.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.GlobalExceptionHandler;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.oversight.domain.District;
import tz.elmkusoma.oversight.dto.DistrictResponse;
import tz.elmkusoma.oversight.dto.OversightDashboardResponse;
import tz.elmkusoma.oversight.dto.RegionResponse;
import tz.elmkusoma.oversight.repository.DistrictRepository;
import tz.elmkusoma.oversight.repository.RegionRepository;
import tz.elmkusoma.oversight.service.OversightGovernanceService;
import tz.elmkusoma.oversight.service.OversightScopeResolver;
import tz.elmkusoma.oversight.service.OversightService;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Nationaladmin.md §5/§8 scope authority:
 * {@code OversightScopeResolver} is the single rule — NATIONAL drills anywhere,
 * REGIONAL only its own region, DISTRICT only its own district, every other
 * role is rejected. Standalone MockMvc (no Spring context) with the real
 * resolver and GlobalExceptionHandler; services are mocked so a rejected
 * request must never reach business logic (IDOR proof).
 */
class OversightScopeSecurityTest {

    private static final UUID R1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID R2 = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID D1 = UUID.fromString("00000000-0000-0000-0000-000000000111");
    private static final UUID D2 = UUID.fromString("00000000-0000-0000-0000-000000000112");

    private static final UUID NATIONAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000121");
    private static final UUID REGIONAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000122");
    private static final UUID DISTRICT_ID = UUID.fromString("00000000-0000-0000-0000-000000000123");
    private static final UUID TEACHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000124");
    private static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000125");
    private static final UUID INSTITUTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000131");

    private OversightService oversightService;
    private OversightGovernanceService governanceService;
    private InstitutionRepository institutionRepository;
    private UserRepository userRepository;
    private RegionRepository regionRepository;
    private DistrictRepository districtRepository;
    private OversightScopeResolver scopeResolver;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        oversightService = mock(OversightService.class);
        governanceService = mock(OversightGovernanceService.class);
        institutionRepository = mock(InstitutionRepository.class);
        userRepository = mock(UserRepository.class);
        regionRepository = mock(RegionRepository.class);
        districtRepository = mock(DistrictRepository.class);
        scopeResolver = new OversightScopeResolver(userRepository, regionRepository, districtRepository);

        OversightController controller = new OversightController(
                oversightService, governanceService, scopeResolver, institutionRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        seedUser(NATIONAL_ID, User.Role.NATIONAL_ADMIN, null, null);
        seedUser(REGIONAL_ID, User.Role.REGIONAL_ADMIN, R1, null);
        seedUser(DISTRICT_ID, User.Role.DISTRICT_ADMIN, R1, D1);
        seedUser(TEACHER_ID, User.Role.TEACHER, R1, null);
        when(regionRepository.existsById(any())).thenReturn(true);
    }

    private void seedUser(UUID id, User.Role role, UUID regionId, UUID districtId) {
        User user = mock(User.class);
        when(user.getRole()).thenReturn(role);
        when(user.getRegionId()).thenReturn(regionId);
        when(user.getDistrictId()).thenReturn(districtId);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
    }

    private void seedDistrict(UUID id, UUID regionId) {
        District district = mock(District.class);
        when(district.getId()).thenReturn(id);
        when(district.getRegionId()).thenReturn(regionId);
        when(districtRepository.findById(id)).thenReturn(Optional.of(district));
    }

    // ── resolver: national ──

    @Test
    void nationalWithoutParams_isCountryWide() {
        OversightScopeResolver.Scope scope = scopeResolver.resolve(NATIONAL_ID, null, null);
        assertNull(scope.regionId());
        assertNull(scope.districtId());
    }

    @Test
    void nationalMayDrillIntoAnyRegion() {
        OversightScopeResolver.Scope scope = scopeResolver.resolve(NATIONAL_ID, R2, null);
        assertEquals(R2, scope.regionId());
        assertNull(scope.districtId());
    }

    @Test
    void nationalUnknownRegion_rejectedWith404() {
        UUID missing = UUID.fromString("00000000-0000-0000-0000-000000000199");
        when(regionRepository.existsById(missing)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class,
                () -> scopeResolver.resolve(NATIONAL_ID, missing, null));
    }

    @Test
    void nationalDistrictDrill_returnsDistrictsRegion() {
        seedDistrict(D2, R2);
        OversightScopeResolver.Scope scope = scopeResolver.resolve(NATIONAL_ID, R2, D2);
        assertEquals(R2, scope.regionId());
        assertEquals(D2, scope.districtId());
    }

    @Test
    void nationalDistrictOutsideRequestedRegion_rejected() {
        seedDistrict(D2, R2);
        assertThrows(ForbiddenException.class,
                () -> scopeResolver.resolve(NATIONAL_ID, R1, D2));
    }

    @Test
    void nationalUnknownDistrict_rejectedWith404() {
        UUID missing = UUID.fromString("00000000-0000-0000-0000-000000000198");
        assertThrows(ResourceNotFoundException.class,
                () -> scopeResolver.resolve(NATIONAL_ID, null, missing));
    }

    // ── resolver: regional ──

    @Test
    void regionalPinnedToOwnRegionWithoutParams() {
        OversightScopeResolver.Scope scope = scopeResolver.resolve(REGIONAL_ID, null, null);
        assertEquals(R1, scope.regionId());
        assertNull(scope.districtId());
    }

    @Test
    void regionalOtherRegion_rejected() {
        assertThrows(ForbiddenException.class,
                () -> scopeResolver.resolve(REGIONAL_ID, R2, null));
    }

    @Test
    void regionalOwnRegionDistrict_narrows() {
        seedDistrict(D1, R1);
        OversightScopeResolver.Scope scope = scopeResolver.resolve(REGIONAL_ID, null, D1);
        assertEquals(R1, scope.regionId());
        assertEquals(D1, scope.districtId());
    }

    @Test
    void regionalDistrictOutsideOwnRegion_rejected() {
        seedDistrict(D2, R2);
        assertThrows(ForbiddenException.class,
                () -> scopeResolver.resolve(REGIONAL_ID, null, D2));
    }

    @Test
    void regionalWithoutOwnRegion_rejected() {
        UUID homelessRegional = UUID.fromString("00000000-0000-0000-0000-000000000126");
        seedUser(homelessRegional, User.Role.REGIONAL_ADMIN, null, null);
        assertThrows(ForbiddenException.class,
                () -> scopeResolver.resolve(homelessRegional, null, null));
    }

    // ── resolver: district ──

    @Test
    void districtPinnedToOwnJurisdictionWithoutParams() {
        OversightScopeResolver.Scope scope = scopeResolver.resolve(DISTRICT_ID, null, null);
        assertEquals(R1, scope.regionId());
        assertEquals(D1, scope.districtId());
    }

    @Test
    void districtOtherDistrict_rejected() {
        assertThrows(ForbiddenException.class,
                () -> scopeResolver.resolve(DISTRICT_ID, null, D2));
    }

    @Test
    void districtOtherRegion_rejected() {
        assertThrows(ForbiddenException.class,
                () -> scopeResolver.resolve(DISTRICT_ID, R2, null));
    }

    @Test
    void districtWithoutOwnDistrict_rejected() {
        UUID homelessDistrict = UUID.fromString("00000000-0000-0000-0000-000000000127");
        seedUser(homelessDistrict, User.Role.DISTRICT_ADMIN, R1, null);
        assertThrows(ForbiddenException.class,
                () -> scopeResolver.resolve(homelessDistrict, null, null));
    }

    // ── resolver: other roles ──

    @Test
    void nonOversightRole_rejected() {
        assertThrows(ForbiddenException.class,
                () -> scopeResolver.resolve(TEACHER_ID, null, null));
    }

    @Test
    void unknownUser_rejected() {
        assertThrows(ForbiddenException.class,
                () -> scopeResolver.resolve(UNKNOWN_ID, null, null));
    }

    // ── HTTP: jurisdiction IDOR ──

    @Test
    void regionalDashboard_ownRegionAllowed() throws Exception {
        when(oversightService.getDashboard(eq(R1), isNull())).thenReturn(new OversightDashboardResponse());

        mockMvc.perform(get("/v1/oversight/dashboard")
                        .requestAttr("userId", REGIONAL_ID)
                        .param("regionId", R1.toString()))
                .andExpect(status().isOk());

        verify(oversightService).getDashboard(R1, null);
    }

    @Test
    void regionalDashboard_otherRegionForbidden_beforeService() throws Exception {
        mockMvc.perform(get("/v1/oversight/dashboard")
                        .requestAttr("userId", REGIONAL_ID)
                        .param("regionId", R2.toString()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(oversightService);
    }

    @Test
    void regionalDashboard_otherRegionDistrictForbidden() throws Exception {
        seedDistrict(D2, R2);
        mockMvc.perform(get("/v1/oversight/dashboard")
                        .requestAttr("userId", REGIONAL_ID)
                        .param("districtId", D2.toString()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(oversightService);
    }

    @Test
    void districtDashboard_otherDistrictForbidden() throws Exception {
        mockMvc.perform(get("/v1/oversight/performance")
                        .requestAttr("userId", DISTRICT_ID)
                        .param("districtId", D2.toString()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(oversightService);
    }

    @Test
    void nationalDashboard_drillDownAnywhereAllowed() throws Exception {
        when(oversightService.getDashboard(eq(R2), isNull())).thenReturn(new OversightDashboardResponse());

        mockMvc.perform(get("/v1/oversight/dashboard")
                        .requestAttr("userId", NATIONAL_ID)
                        .param("regionId", R2.toString()))
                .andExpect(status().isOk());

        verify(oversightService).getDashboard(R2, null);
    }

    @Test
    void teacherDashboard_rejected() throws Exception {
        mockMvc.perform(get("/v1/oversight/dashboard")
                        .requestAttr("userId", TEACHER_ID))
                .andExpect(status().isForbidden());

        verifyNoInteractions(oversightService);
    }

    @Test
    void unknownUserDashboard_rejected() throws Exception {
        mockMvc.perform(get("/v1/oversight/dashboard")
                        .requestAttr("userId", UNKNOWN_ID))
                .andExpect(status().isForbidden());

        verifyNoInteractions(oversightService);
    }

    // ── HTTP: region list narrowing ──

    @Test
    void regionalRegionList_filteredToOwnRegion() throws Exception {
        when(oversightService.getAllRegions()).thenReturn(List.of(
                RegionResponse.builder().id(R1).build(),
                RegionResponse.builder().id(R2).build()));

        mockMvc.perform(get("/v1/oversight/regions")
                        .requestAttr("userId", REGIONAL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(R1.toString()));
    }

    @Test
    void nationalRegionList_unfiltered() throws Exception {
        when(oversightService.getAllRegions()).thenReturn(List.of(
                RegionResponse.builder().id(R1).build(),
                RegionResponse.builder().id(R2).build()));

        mockMvc.perform(get("/v1/oversight/regions")
                        .requestAttr("userId", NATIONAL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── HTTP: institution detail scope ──

    private void seedInstitution(UUID regionId, UUID districtId) {
        Institution institution = mock(Institution.class);
        when(institution.getRegionId()).thenReturn(regionId);
        when(institution.getDistrictId()).thenReturn(districtId);
        when(institutionRepository.findById(INSTITUTION_ID)).thenReturn(Optional.of(institution));
    }

    @Test
    void regionalInstitutionDetail_ownRegionAllowed() throws Exception {
        seedInstitution(R1, null);
        when(oversightService.getInstitutionDetail(INSTITUTION_ID)).thenReturn(null);

        mockMvc.perform(get("/v1/oversight/institutions/{id}", INSTITUTION_ID)
                        .requestAttr("userId", REGIONAL_ID))
                .andExpect(status().isOk());

        verify(oversightService).getInstitutionDetail(INSTITUTION_ID);
    }

    @Test
    void regionalInstitutionDetail_otherRegionForbidden() throws Exception {
        seedInstitution(R2, null);

        mockMvc.perform(get("/v1/oversight/institutions/{id}", INSTITUTION_ID)
                        .requestAttr("userId", REGIONAL_ID))
                .andExpect(status().isForbidden());

        verifyNoInteractions(oversightService);
    }

    @Test
    void districtInstitutionDetail_otherDistrictForbidden() throws Exception {
        seedInstitution(R1, D2);

        mockMvc.perform(get("/v1/oversight/institutions/{id}", INSTITUTION_ID)
                        .requestAttr("userId", DISTRICT_ID))
                .andExpect(status().isForbidden());

        verifyNoInteractions(oversightService);
    }

    @Test
    void nationalInstitutionDetail_anyInstitutionAllowed() throws Exception {
        seedInstitution(R2, D2);
        when(oversightService.getInstitutionDetail(INSTITUTION_ID)).thenReturn(null);

        mockMvc.perform(get("/v1/oversight/institutions/{id}", INSTITUTION_ID)
                        .requestAttr("userId", NATIONAL_ID))
                .andExpect(status().isOk());
    }

    @Test
    void institutionDetail_missingInstitution404() throws Exception {
        mockMvc.perform(get("/v1/oversight/institutions/{id}", INSTITUTION_ID)
                        .requestAttr("userId", NATIONAL_ID))
                .andExpect(status().isNotFound());
    }

    // ── HTTP: governance endpoints ──

    @Test
    void regionalDataQuality_otherRegionForbidden() throws Exception {
        mockMvc.perform(get("/v1/oversight/data-quality")
                        .requestAttr("userId", REGIONAL_ID)
                        .param("regionId", R2.toString()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(governanceService);
    }

    @Test
    void regionalSearch_pinnedToOwnRegion() throws Exception {
        when(governanceService.search(any(), any(), eq(R1), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/v1/oversight/search")
                        .requestAttr("userId", REGIONAL_ID)
                        .param("q", "school"))
                .andExpect(status().isOk());

        verify(governanceService).search(eq("school"), isNull(), eq(R1), isNull());
    }

    @Test
    void nationalSearch_usesRequestedRegion() throws Exception {
        when(governanceService.search(any(), any(), eq(R2), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/v1/oversight/search")
                        .requestAttr("userId", NATIONAL_ID)
                        .param("q", "school")
                        .param("regionId", R2.toString()))
                .andExpect(status().isOk());

        verify(governanceService).search(eq("school"), isNull(), eq(R2), isNull());
    }

    @Test
    void export_regionalOtherRegionForbidden_beforeReportGeneration() throws Exception {
        mockMvc.perform(get("/v1/oversight/reports/{id}/export", "school-performance")
                        .requestAttr("userId", REGIONAL_ID)
                        .param("regionId", R2.toString()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(governanceService);
    }

    @Test
    void export_national_returnsCsvAttachment() throws Exception {
        when(governanceService.exportReport(eq("attendance-report"), isNull(), isNull()))
                .thenReturn("institution,present\nTest School,42\n");

        mockMvc.perform(get("/v1/oversight/reports/{id}/export", "attendance-report")
                        .requestAttr("userId", NATIONAL_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".csv")));
    }

    // ── HTTP: path-variable jurisdiction IDOR ──

    @Test
    void regionalDistrictsPath_otherRegionForbidden() throws Exception {
        mockMvc.perform(get("/v1/oversight/regions/{regionId}/districts", R2)
                        .requestAttr("userId", REGIONAL_ID))
                .andExpect(status().isForbidden());

        verifyNoInteractions(oversightService);
    }

    @Test
    void regionalInstitutionsPath_otherRegionForbidden() throws Exception {
        mockMvc.perform(get("/v1/oversight/regions/{regionId}/institutions", R2)
                        .requestAttr("userId", REGIONAL_ID))
                .andExpect(status().isForbidden());

        verifyNoInteractions(oversightService);
    }

    @Test
    void districtInstitutionsPath_otherDistrictForbidden() throws Exception {
        mockMvc.perform(get("/v1/oversight/districts/{districtId}/institutions", D2)
                        .requestAttr("userId", DISTRICT_ID))
                .andExpect(status().isForbidden());

        verifyNoInteractions(oversightService);
    }

    @Test
    void regionalDistrictsPath_ownRegionAllowed() throws Exception {
        when(oversightService.getDistrictsByRegion(R1)).thenReturn(List.of());

        mockMvc.perform(get("/v1/oversight/regions/{regionId}/districts", R1)
                        .requestAttr("userId", REGIONAL_ID))
                .andExpect(status().isOk());

        verify(oversightService).getDistrictsByRegion(R1);
    }

    @Test
    void districtDistrictsPath_limitedToOwnDistrict() throws Exception {
        when(oversightService.getDistrictsByRegion(R1)).thenReturn(List.of(
                DistrictResponse.builder().id(D1).build(),
                DistrictResponse.builder().id(D2).build()));

        mockMvc.perform(get("/v1/oversight/regions/{regionId}/districts", R1)
                        .requestAttr("userId", DISTRICT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(D1.toString()));

        verify(oversightService).getDistrictsByRegion(R1);
    }

    // ── resolver: every endpoint shares one authority ──

    @Test
    void allJurisdictionEndpoints_rejectCrossScopeIdentically() throws Exception {
        String[] endpoints = {
                "/v1/oversight/dashboard",
                "/v1/oversight/performance",
                "/v1/oversight/attendance",
                "/v1/oversight/curriculum",
                "/v1/oversight/assessments",
                "/v1/oversight/live-classes",
                "/v1/oversight/alerts",
                "/v1/oversight/attention",
                "/v1/oversight/data-quality",
                "/v1/oversight/search",
                "/v1/oversight/reports/school-performance/export",
                "/v1/oversight/schools"
        };
        for (String endpoint : endpoints) {
            mockMvc.perform(get(endpoint)
                            .requestAttr("userId", REGIONAL_ID)
                            .param("regionId", R2.toString()))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(oversightService, governanceService);
    }
}
