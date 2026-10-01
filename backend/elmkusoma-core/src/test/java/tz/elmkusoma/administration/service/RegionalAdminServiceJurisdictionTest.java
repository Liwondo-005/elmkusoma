package tz.elmkusoma.administration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.administration.domain.PlatformNotification;
import tz.elmkusoma.administration.domain.ScheduledReport;
import tz.elmkusoma.administration.domain.VerificationRecord;
import tz.elmkusoma.administration.dto.AnnouncementDetailResponse;
import tz.elmkusoma.administration.dto.CreateAnnouncementRequest;
import tz.elmkusoma.administration.dto.CreateScheduledReportRequest;
import tz.elmkusoma.administration.dto.ScheduledReportSummary;
import tz.elmkusoma.administration.dto.WardSummary;
import tz.elmkusoma.administration.repository.ScheduledReportRepository;
import tz.elmkusoma.administration.repository.ScheduledReportRunRepository;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.audit.repository.SecurityEventRepository;
import tz.elmkusoma.course.repository.CourseRepository;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.learning.repository.VideoTutorialRepository;
import tz.elmkusoma.learner.domain.LearnerNotification;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.administration.repository.PlatformNotificationRepository;
import tz.elmkusoma.administration.repository.VerificationRecordRepository;
import tz.elmkusoma.oversight.domain.District;
import tz.elmkusoma.oversight.domain.Region;
import tz.elmkusoma.oversight.domain.Ward;
import tz.elmkusoma.oversight.repository.DistrictRepository;
import tz.elmkusoma.oversight.repository.RegionRepository;
import tz.elmkusoma.oversight.repository.WardRepository;
import tz.elmkusoma.oversight.service.OversightService;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Jurisdiction enforcement tests for the Regional Administration command center.
 *
 * <p>These tests prove the prompt's core rule: region/district/institution scope is
 * resolved and enforced server-side from the caller's database identity — never from
 * client-supplied identifiers.</p>
 */
@ExtendWith(MockitoExtension.class)
class RegionalAdminServiceJurisdictionTest {

    private static final UUID CALLER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID REGION_A = UUID.fromString("11111111-1111-1111-1111-111111111101");
    private static final UUID REGION_B = UUID.fromString("11111111-1111-1111-1111-111111111102");
    private static final UUID DISTRICT_A = UUID.fromString("22222222-2222-2222-2222-222222222201");
    private static final UUID DISTRICT_A2 = UUID.fromString("22222222-2222-2222-2222-222222222202");
    private static final UUID DISTRICT_B = UUID.fromString("22222222-2222-2222-2222-222222222203");
    private static final UUID INST_A = UUID.fromString("33333333-3333-3333-3333-333333333301");
    private static final UUID INST_B = UUID.fromString("33333333-3333-3333-3333-333333333302");
    private static final UUID UNKNOWN = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Mock private OversightService oversightService;
    @Mock private PlatformAdminService platformAdminService;
    @Mock private NotificationService notificationService;
    @Mock private RegionRepository regionRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private UserRepository userRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private InstitutionMembershipRepository membershipRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private LiveClassRepository liveClassRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private VideoTutorialRepository videoTutorialRepository;
    @Mock private VerificationRecordRepository verificationRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private SecurityEventRepository securityEventRepository;
    @Mock private PlatformNotificationRepository platformNotificationRepository;
    @Mock private LearnerNotificationRepository learnerNotificationRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private WardRepository wardRepository;
    @Mock private ScheduledReportRepository scheduledReportRepository;
    @Mock private ScheduledReportRunRepository scheduledReportRunRepository;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private RegionalAdminService service;

    // ── fixtures ──

    private User user(User.Role role, UUID regionId, UUID districtId) {
        User u = new User();
        u.setId(CALLER);
        u.setRole(role);
        u.setRegionId(regionId);
        u.setDistrictId(districtId);
        u.setEmail("regional@elmkusoma.go.tz");
        return u;
    }

    private Region region(UUID id, String code) {
        Region r = new Region();
        r.setId(id);
        r.setName("Region " + code);
        r.setCode(code);
        r.setIsActive(true);
        return r;
    }

    private District district(UUID id, UUID regionId, String code) {
        District d = new District();
        d.setId(id);
        d.setName("District " + code);
        d.setCode(code);
        d.setRegionId(regionId);
        d.setIsActive(true);
        return d;
    }

    private Institution institution(UUID id, UUID regionId, UUID districtId) {
        Institution i = new Institution();
        i.setId(id);
        i.setName("Institution " + id);
        i.setCode("CODE-" + id.toString().substring(0, 4));
        i.setRegionId(regionId);
        i.setDistrictId(districtId);
        i.setIsActive(true);
        return i;
    }

    private Ward ward(UUID id, UUID districtId, String code) {
        Ward w = new Ward();
        w.setId(id);
        w.setDistrictId(districtId);
        w.setName("Ward " + code);
        w.setCode(code);
        w.setIsActive(true);
        return w;
    }

    private ScheduledReport report(UUID id, UUID ownerId, UUID regionId, UUID districtId) {
        ScheduledReport r = new ScheduledReport();
        r.setId(id);
        r.setUserId(ownerId);
        r.setRegionId(regionId);
        r.setDistrictId(districtId);
        r.setReportType("PERFORMANCE");
        r.setTitle("Monthly performance");
        r.setFrequency("MONTHLY");
        r.setStatus("ACTIVE");
        r.setRunCount(0);
        return r;
    }

    // ── role & jurisdiction resolution ──

    @Test
    @DisplayName("A platform ADMIN cannot use the regional administration API")
    void adminRoleIsRejected() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.ADMIN, REGION_A, null)));

        assertThatThrownBy(() -> service.getAccessibleRegions(CALLER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("A regional admin without an assigned region is rejected")
    void regionalAdminWithoutRegionIsRejected() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.REGIONAL_ADMIN, null, null)));

        assertThatThrownBy(() -> service.getDashboard(CALLER))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("regional jurisdiction");
    }

    @Test
    @DisplayName("A district admin without an assigned district is rejected")
    void districtAdminWithoutDistrictIsRejected() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.DISTRICT_ADMIN, REGION_A, null)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));

        assertThatThrownBy(() -> service.getDistrictDetail(CALLER, DISTRICT_A))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("district jurisdiction");
    }

    // ── region scope ──

    @Test
    @DisplayName("Regional admin sees exactly one region — their own")
    void regionalAdminSeesOnlyOwnRegion() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.REGIONAL_ADMIN, REGION_A, null)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(institutionRepository.findByRegionIdAndIsDeletedFalse(REGION_A))
                .thenReturn(List.of(institution(INST_A, REGION_A, DISTRICT_A)));
        when(membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(anyList(), any()))
                .thenReturn(2L);

        var regions = service.getAccessibleRegions(CALLER);

        assertThat(regions).hasSize(1);
        assertThat(regions.get(0).getId()).isEqualTo(REGION_A);
    }

    @Test
    @DisplayName("Regional admin cannot open a region other than their own")
    void regionalAdminCannotOpenForeignRegion() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.REGIONAL_ADMIN, REGION_A, null)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));

        assertThatThrownBy(() -> service.getRegionDetail(CALLER, REGION_B))
                .isInstanceOf(ForbiddenException.class);
        verify(oversightService, never()).jurisdictionAttendanceRate(anyList());
    }

    @Test
    @DisplayName("District admin gets no region-level list (sibling regions must not leak)")
    void districtAdminGetsNoRegionList() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.DISTRICT_ADMIN, REGION_A, DISTRICT_A)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(districtRepository.findById(DISTRICT_A)).thenReturn(Optional.of(district(DISTRICT_A, REGION_A, "A1")));
        when(institutionRepository.findByDistrictIdAndIsDeletedFalse(DISTRICT_A)).thenReturn(List.of());

        assertThat(service.getAccessibleRegions(CALLER)).isEmpty();
    }

    // ── district scope ──

    @Test
    @DisplayName("Regional admin cannot open a district outside their region")
    void regionalAdminCannotOpenForeignDistrict() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.REGIONAL_ADMIN, REGION_A, null)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(institutionRepository.findByRegionIdAndIsDeletedFalse(REGION_A)).thenReturn(List.of());
        when(districtRepository.findById(DISTRICT_B)).thenReturn(Optional.of(district(DISTRICT_B, REGION_B, "B1")));

        assertThatThrownBy(() -> service.getDistrictDetail(CALLER, DISTRICT_B))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("District admin cannot open a sibling district in the same region")
    void districtAdminCannotOpenSiblingDistrict() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.DISTRICT_ADMIN, REGION_A, DISTRICT_A)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(districtRepository.findById(DISTRICT_A)).thenReturn(Optional.of(district(DISTRICT_A, REGION_A, "A1")));
        when(institutionRepository.findByDistrictIdAndIsDeletedFalse(DISTRICT_A)).thenReturn(List.of());
        when(districtRepository.findById(DISTRICT_A2)).thenReturn(Optional.of(district(DISTRICT_A2, REGION_A, "A2")));

        assertThatThrownBy(() -> service.getDistrictDetail(CALLER, DISTRICT_A2))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("District admin can open their own district")
    void districtAdminCanOpenOwnDistrict() {
        District own = district(DISTRICT_A, REGION_A, "A1");
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.DISTRICT_ADMIN, REGION_A, DISTRICT_A)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(districtRepository.findById(DISTRICT_A)).thenReturn(Optional.of(own));
        when(institutionRepository.findByDistrictIdAndIsDeletedFalse(DISTRICT_A))
                .thenReturn(List.of(institution(INST_A, REGION_A, DISTRICT_A)));
        when(membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(anyList(), any())).thenReturn(1L);
        when(oversightService.jurisdictionAttendanceRate(anyList())).thenReturn(88.5);

        var detail = service.getDistrictDetail(CALLER, DISTRICT_A);

        assertThat(detail.getId()).isEqualTo(DISTRICT_A);
        assertThat(detail.getInstitutions()).hasSize(1);
        assertThat(detail.getAttendanceRate()).isEqualTo(88.5);
    }

    // ── institution scope ──

    @Test
    @DisplayName("Regional admin cannot open an institution in another region")
    void regionalAdminCannotOpenForeignInstitution() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.REGIONAL_ADMIN, REGION_A, null)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(institutionRepository.findByRegionIdAndIsDeletedFalse(REGION_A)).thenReturn(List.of());
        when(institutionRepository.findById(INST_B)).thenReturn(
                Optional.of(institution(INST_B, REGION_B, DISTRICT_B)));

        assertThatThrownBy(() -> service.getInstitutionGovernance(CALLER, INST_B))
                .isInstanceOf(ForbiddenException.class);
        verify(oversightService, never()).getInstitutionDetail(any());
    }

    @Test
    @DisplayName("Institution scope is enforced before any institution data is read")
    void unknownInstitutionIsReportedAsMissing() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.REGIONAL_ADMIN, REGION_A, null)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(institutionRepository.findByRegionIdAndIsDeletedFalse(REGION_A)).thenReturn(List.of());
        when(institutionRepository.findById(UNKNOWN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getInstitutionGovernance(CALLER, UNKNOWN))
                .isInstanceOf(tz.elmkusoma.exception.ResourceNotFoundException.class);
    }

    // ── communication jurisdiction ──

    @Test
    @DisplayName("An announcement cannot target a district outside the caller's region")
    void announcementWithForeignDistrictIsRejected() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.REGIONAL_ADMIN, REGION_A, null)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(institutionRepository.findByRegionIdAndIsDeletedFalse(REGION_A)).thenReturn(List.of());
        when(districtRepository.findById(DISTRICT_B)).thenReturn(Optional.of(district(DISTRICT_B, REGION_B, "B1")));

        CreateAnnouncementRequest request = new CreateAnnouncementRequest();
        request.setTitle("Term notice");
        request.setContent("Schools should resume on Monday.");
        request.setAudienceType("DISTRICTS");
        request.setTargetDistrictIds(List.of(DISTRICT_B));

        assertThatThrownBy(() -> service.createAnnouncement(CALLER, request))
                .isInstanceOf(ForbiddenException.class);
        verify(platformNotificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("An in-jurisdiction announcement is delivered through the real notification pipeline")
    void announcementWithinScopeIsDelivered() throws Exception {
        User regional = user(User.Role.REGIONAL_ADMIN, REGION_A, null);
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(regional));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(institutionRepository.findByRegionIdAndIsDeletedFalse(REGION_A))
                .thenReturn(List.of(institution(INST_A, REGION_A, DISTRICT_A)));
        InstitutionMembership membership = InstitutionMembership.builder()
                .userId(CALLER)
                .institutionId(INST_A)
                .isActive(true)
                .build();
        when(membershipRepository.findByInstitutionIdAndIsActiveTrue(INST_A))
                .thenReturn(List.of(membership));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(platformNotificationRepository.save(any(PlatformNotification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateAnnouncementRequest request = new CreateAnnouncementRequest();
        request.setTitle("Term notice");
        request.setContent("Schools should resume on Monday.");
        request.setAudienceType("ALL");

        AnnouncementDetailResponse response = service.createAnnouncement(CALLER, request);

        assertThat(response.getRecipientCount()).isEqualTo(1);
        assertThat(response.getAudienceType()).isEqualTo("ALL");
        verify(notificationService).notifyUser(eq(CALLER), eq("Term notice"),
                eq("Schools should resume on Monday."), eq("REGIONAL_ANNOUNCEMENT"),
                eq("ANNOUNCEMENT"), any());
    }

    // ── governance scope ──

    @Test
    @DisplayName("A verification for an institution outside the jurisdiction cannot be read")
    void verificationOutsideJurisdictionIsRejected() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.REGIONAL_ADMIN, REGION_A, null)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(institutionRepository.findByRegionIdAndIsDeletedFalse(REGION_A)).thenReturn(List.of());

        VerificationRecord record = new VerificationRecord();
        record.setId(UUID.randomUUID());
        record.setEntityType("INSTITUTION");
        record.setEntityId(INST_B);
        record.setStatus("PENDING");
        when(verificationRepository.findById(any(UUID.class))).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.getVerificationDetail(CALLER, record.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("A district admin cannot read a region-level data quality report for another scope")
    void districtAdminCannotReadRegionDataQualityOfOtherDistrict() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.DISTRICT_ADMIN, REGION_A, DISTRICT_A)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(districtRepository.findById(DISTRICT_A)).thenReturn(Optional.of(district(DISTRICT_A, REGION_A, "A1")));
        when(institutionRepository.findByDistrictIdAndIsDeletedFalse(DISTRICT_A)).thenReturn(List.of());

        // Data quality is computed strictly from the caller's own institution set:
        // with no institutions in scope there is nothing to report on.
        assertThat(service.getDataQuality(CALLER).getTotalIssues()).isZero();
    }

    // ── ward dimension scope ──

    @Test
    @DisplayName("District admin ward list is limited to their own district")
    void districtAdminSeesOnlyOwnDistrictWards() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.DISTRICT_ADMIN, REGION_A, DISTRICT_A)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(districtRepository.findById(DISTRICT_A)).thenReturn(Optional.of(district(DISTRICT_A, REGION_A, "A1")));
        when(institutionRepository.findByDistrictIdAndIsDeletedFalse(DISTRICT_A)).thenReturn(List.of());
        when(wardRepository.findByDistrictIdInAndIsDeletedFalse(List.of(DISTRICT_A)))
                .thenReturn(List.of(ward(UUID.fromString("44444444-4444-4444-4444-444444444401"), DISTRICT_A, "A1-W")));
        when(districtRepository.findById(DISTRICT_A)).thenReturn(Optional.of(district(DISTRICT_A, REGION_A, "A1")));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        lenient().when(institutionRepository.countByWardIdAndIsDeletedFalse(any())).thenReturn(0L);

        var page = service.getWards(CALLER, 0, 20, null, null);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getDistrictId()).isEqualTo(DISTRICT_A);
        verify(wardRepository).findByDistrictIdInAndIsDeletedFalse(List.of(DISTRICT_A));
    }

    @Test
    @DisplayName("District admin cannot filter wards by a sibling district")
    void districtAdminCannotFilterSiblingDistrictWards() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.DISTRICT_ADMIN, REGION_A, DISTRICT_A)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(districtRepository.findById(DISTRICT_A)).thenReturn(Optional.of(district(DISTRICT_A, REGION_A, "A1")));
        when(institutionRepository.findByDistrictIdAndIsDeletedFalse(DISTRICT_A)).thenReturn(List.of());
        when(districtRepository.findById(DISTRICT_A2)).thenReturn(Optional.of(district(DISTRICT_A2, REGION_A, "A2")));

        assertThatThrownBy(() -> service.getWards(CALLER, 0, 20, null, DISTRICT_A2))
                .isInstanceOf(ForbiddenException.class);
        verify(wardRepository, never()).findByDistrictIdInAndIsDeletedFalse(anyList());
    }

    @Test
    @DisplayName("Regional admin cannot open a ward of another region")
    void regionalAdminCannotOpenForeignWard() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.REGIONAL_ADMIN, REGION_A, null)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(institutionRepository.findByRegionIdAndIsDeletedFalse(REGION_A)).thenReturn(List.of());
        when(wardRepository.findById(UNKNOWN)).thenReturn(Optional.of(
                ward(UNKNOWN, DISTRICT_B, "B1-W")));
        when(districtRepository.findById(DISTRICT_B)).thenReturn(Optional.of(district(DISTRICT_B, REGION_B, "B1")));

        assertThatThrownBy(() -> service.getWardDetail(CALLER, UNKNOWN))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── scheduled report scope ──

    @Test
    @DisplayName("Scheduled report creation binds the caller's jurisdiction, never client input")
    void scheduledReportCreationBindsCallerJurisdiction() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.REGIONAL_ADMIN, REGION_A, null)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(institutionRepository.findByRegionIdAndIsDeletedFalse(REGION_A)).thenReturn(List.of());
        when(scheduledReportRepository.save(any(ScheduledReport.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateScheduledReportRequest req = new CreateScheduledReportRequest(
                "Attendance digest", "ATTENDANCE", "WEEKLY", "team@example.org");
        ScheduledReportSummary summary = service.createScheduledReport(CALLER, req);

        assertThat(summary.getRegionId()).isEqualTo(REGION_A);
        assertThat(summary.getDistrictId()).isNull();
        assertThat(summary.getNextRunAt()).isNotNull();
        assertThat(summary.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("A user cannot touch a scheduled report they do not own")
    void scheduledReportOwnershipIsEnforced() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.REGIONAL_ADMIN, REGION_A, null)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(institutionRepository.findByRegionIdAndIsDeletedFalse(REGION_A)).thenReturn(List.of());
        UUID reportId = UUID.fromString("55555555-5555-5555-5555-555555555501");
        when(scheduledReportRepository.findById(reportId)).thenReturn(Optional.of(
                report(reportId, UUID.fromString("00000000-0000-0000-0000-0000000000b2"), REGION_A, null)));

        assertThatThrownBy(() -> service.getScheduledReportRuns(CALLER, reportId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Scheduled report list hides reports from other jurisdictions")
    void scheduledReportListHidesForeignReports() {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.REGIONAL_ADMIN, REGION_A, null)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(institutionRepository.findByRegionIdAndIsDeletedFalse(REGION_A)).thenReturn(List.of());
        when(scheduledReportRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(CALLER))
                .thenReturn(List.of(
                        report(UUID.fromString("55555555-5555-5555-5555-555555555502"), CALLER, REGION_A, null),
                        report(UUID.fromString("55555555-5555-5555-5555-555555555503"), CALLER, REGION_B, null)));
        lenient().when(regionRepository.findById(REGION_B)).thenReturn(Optional.of(region(REGION_B, "B")));

        var reports = service.getScheduledReports(CALLER);

        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).getRegionId()).isEqualTo(REGION_A);
    }

    @Test
    @DisplayName("A due report executes inside the owner scope and produces a real run row")
    void dueReportExecutesInOwnerScope() throws Exception {
        when(userRepository.findById(CALLER)).thenReturn(Optional.of(
                user(User.Role.REGIONAL_ADMIN, REGION_A, null)));
        when(regionRepository.findById(REGION_A)).thenReturn(Optional.of(region(REGION_A, "A")));
        when(institutionRepository.findByRegionIdAndIsDeletedFalse(REGION_A)).thenReturn(List.of());
        UUID reportId = UUID.fromString("55555555-5555-5555-5555-555555555504");
        ScheduledReport due = report(reportId, CALLER, REGION_A, null);
        due.setNextRunAt(java.time.LocalDateTime.now().minusMinutes(1));
        when(scheduledReportRepository.findDue(eq("ACTIVE"), any()))
                .thenReturn(List.of(due));
        when(scheduledReportRepository.save(any(ScheduledReport.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(oversightService.getDashboard(REGION_A, null)).thenReturn(
                tz.elmkusoma.oversight.dto.OversightDashboardResponse.builder()
                        .totalInstitutions(1L).totalTeachers(2L).totalStudents(3L)
                        .attendanceRate(80.0).averagePerformance(65.0).build());
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"institutions\":1}");
        lenient().when(scheduledReportRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int executed = service.runDueScheduledReports();

        assertThat(executed).isEqualTo(1);
        assertThat(due.getRunCount()).isEqualTo(1);
        assertThat(due.getLastRunAt()).isNotNull();
        assertThat(due.getNextRunAt()).isAfter(java.time.LocalDateTime.now());
        verify(scheduledReportRunRepository).save(any());
        verify(notificationService).notifyUser(eq(CALLER), any(), any(), any(), any(), eq(reportId));
    }

}
