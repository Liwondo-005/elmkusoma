package tz.elmkusoma.learning.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.dto.ResourceResponse;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.learning.repository.ResourceTagRepository;
import tz.elmkusoma.learning.repository.ResourceTaggingRepository;
import tz.elmkusoma.learning.repository.StudentSavedResourceRepository;
import tz.elmkusoma.learner.repository.LearnerEnrollmentRepository;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Jurisdiction governance for resource access: the same region/district-match
 * rule {@code OversightController.verifyInstitutionJurisdiction} applies to
 * institutions, enforced server-side on every resource read.
 *
 * <p>Verification matrix (backend authoritative — frontend filters are never
 * trusted):</p>
 * <ul>
 *   <li>Regional Admin → own region: allowed (governance read),</li>
 *   <li>Regional Admin → another region: denied even with the exact resource
 *       UUID (404 — no existence oracle),</li>
 *   <li>District Admin → own district allowed / other district denied,</li>
 *   <li>no region/district assignment → no jurisdiction at all,</li>
 *   <li>everyone else stays behind the institution scope; without an
 *       institution context there is no read.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ResourceJurisdictionSecurityTest {

    @Mock private ResourceRepository resourceRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private ResourceTagRepository tagRepository;
    @Mock private ResourceTaggingRepository taggingRepository;
    @Mock private StudentSavedResourceRepository savedResourceRepository;
    @Mock private UserRepository userRepository;
    @Mock private tz.elmkusoma.course.repository.CourseRepository courseRepository;
    @Mock private tz.elmkusoma.course.repository.CourseModuleRepository courseModuleRepository;
    @Mock private tz.elmkusoma.liveclass.service.MediaProxyService mediaProxyService;
    @Mock private ResourceMetadataExtractor metadataExtractor;
    @Mock private tz.elmkusoma.audit.service.AuditService auditService;
    @Mock private ClassAccessGuard classAccessGuard;
    @Mock private LearnerEnrollmentRepository learnerEnrollmentRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ResourceService service;

    private static final UUID REGION_DAR = UUID.randomUUID();
    private static final UUID REGION_ARU = UUID.randomUUID();
    private static final UUID DISTRICT_ILALA = UUID.randomUUID();
    private static final UUID DISTRICT_ARUSHA = UUID.randomUUID();

    private UUID resourceId;
    private UUID darInstitutionId;
    private UUID aruInstitutionId;
    private UUID regionalDarId;
    private UUID regionalAruId;
    private UUID districtIlalaId;
    private UUID districtArushaId;
    private UUID teacherId;

    @BeforeEach
    void setUp() {
        resourceId = UUID.randomUUID();
        darInstitutionId = UUID.randomUUID();
        aruInstitutionId = UUID.randomUUID();
        regionalDarId = UUID.randomUUID();
        regionalAruId = UUID.randomUUID();
        districtIlalaId = UUID.randomUUID();
        districtArushaId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
    }

    // ── helpers ──

    private Resource resourceIn(UUID institutionId, Resource.ResourceVisibility visibility) {
        return Resource.builder()
                .id(resourceId)
                .institutionId(institutionId)
                .uploadedBy(UUID.randomUUID())
                .title("Governed resource")
                .resourceType(Resource.ResourceType.PDF)
                .storageUrl("https://storage.test/governed.pdf")
                .visibility(visibility)
                .sortOrder(0)
                .isDeleted(false)
                .build();
    }

    private void stubUserRegion(UUID userId, UUID regionId) {
        User user = new User();
        user.setRegionId(regionId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    private void stubUserDistrict(UUID userId, UUID districtId) {
        User user = new User();
        user.setDistrictId(districtId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    private void stubInstitutionJurisdiction(UUID institutionId, UUID regionId, UUID districtId) {
        Institution institution = new Institution();
        institution.setId(institutionId);
        institution.setRegionId(regionId);
        institution.setDistrictId(districtId);
        when(institutionRepository.findByIdAndIsDeletedFalse(institutionId))
                .thenReturn(Optional.of(institution));
    }

    // ── Regional Admin ──

    @Test
    void regionalAdmin_readsResourcesInsideOwnRegion() {
        Resource resource = resourceIn(darInstitutionId, Resource.ResourceVisibility.PRIVATE);
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        stubInstitutionJurisdiction(darInstitutionId, REGION_DAR, DISTRICT_ILALA);
        stubUserRegion(regionalDarId, REGION_DAR);

        // institutionId is null: oversight accounts hold no membership — the
        // jurisdiction check alone must authorize the governance read.
        ResourceResponse response =
                service.getResource(resourceId, null, regionalDarId, "REGIONAL_ADMIN");

        assertNotNull(response);
        assertEquals(resourceId, response.getId());
    }

    @Test
    void regionalAdmin_otherRegion_deniedEvenWithExactResourceUuid() {
        Resource resource = resourceIn(aruInstitutionId, Resource.ResourceVisibility.PUBLIC);
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        stubInstitutionJurisdiction(aruInstitutionId, REGION_ARU, DISTRICT_ARUSHA);
        stubUserRegion(regionalDarId, REGION_DAR);

        // Same institution scope would otherwise match — the Arusha resource is
        // invisible to the Dar es Salaam regional admin despite the UUID.
        assertThrows(ResourceNotFoundException.class,
                () -> service.requireVisible(resourceId, null, regionalDarId, "REGIONAL_ADMIN"));
        assertThrows(ResourceNotFoundException.class,
                () -> service.getResource(resourceId, null, regionalDarId, "REGIONAL_ADMIN"));
    }

    @Test
    void regionalAdmin_withoutRegionAssignment_hasNoJurisdiction() {
        Resource resource = resourceIn(darInstitutionId, Resource.ResourceVisibility.PUBLIC);
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        stubInstitutionJurisdiction(darInstitutionId, REGION_DAR, DISTRICT_ILALA);
        stubUserRegion(regionalDarId, null);

        assertThrows(ResourceNotFoundException.class,
                () -> service.requireVisible(resourceId, null, regionalDarId, "REGIONAL_ADMIN"));
    }

    // ── District Admin ──

    @Test
    void districtAdmin_ownDistrict_allowed() {
        Resource resource = resourceIn(darInstitutionId, Resource.ResourceVisibility.PUBLIC);
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        stubInstitutionJurisdiction(darInstitutionId, REGION_DAR, DISTRICT_ILALA);
        stubUserDistrict(districtIlalaId, DISTRICT_ILALA);

        assertNotNull(service.requireVisible(resourceId, null, districtIlalaId, "DISTRICT_ADMIN"));
    }

    @Test
    void districtAdmin_otherDistrict_denied() {
        Resource resource = resourceIn(darInstitutionId, Resource.ResourceVisibility.PUBLIC);
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        stubInstitutionJurisdiction(darInstitutionId, REGION_DAR, DISTRICT_ILALA);
        stubUserDistrict(districtArushaId, DISTRICT_ARUSHA);

        assertThrows(ResourceNotFoundException.class,
                () -> service.requireVisible(resourceId, null, districtArushaId, "DISTRICT_ADMIN"));
    }

    // ── jurisdiction-scoped listing ──

    @Test
    void regionalAdmin_listContainsOnlyOwnRegionInstitutions() {
        Resource darPublic = resourceIn(darInstitutionId, Resource.ResourceVisibility.PUBLIC);
        Resource darPrivate = resourceIn(darInstitutionId, Resource.ResourceVisibility.PRIVATE);

        stubUserRegion(regionalDarId, REGION_DAR);
        when(institutionRepository.findByRegionIdAndIsDeletedFalse(REGION_DAR))
                .thenReturn(List.of(institution(darInstitutionId)));
        when(resourceRepository.findByInstitutionIdInAndIsDeletedFalse(List.of(darInstitutionId)))
                .thenReturn(List.of(darPublic, darPrivate));

        List<ResourceResponse> result = service.listResources(
                null, regionalDarId, "REGIONAL_ADMIN", null, null, null, null, null, 0, 20);

        assertEquals(2, result.size(), "both in-region resources (incl. PRIVATE governance read) are listed");
        // the institution-scoped path must never run for a jurisdiction role
        verify(resourceRepository, never()).findByInstitutionIdAndVisibilities(any(), any());
    }

    // ── everyone else stays institution-scoped ──

    @Test
    void nonJurisdictionRole_withoutInstitutionContext_denied() {
        Resource resource = resourceIn(darInstitutionId, Resource.ResourceVisibility.PUBLIC);
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));

        assertThrows(ResourceNotFoundException.class,
                () -> service.requireVisible(resourceId, null, teacherId, "TEACHER"));
    }

    @Test
    void nonJurisdictionRole_listWithoutInstitution_denied() {
        assertThrows(SecurityException.class,
                () -> service.listResources(null, teacherId, "TEACHER",
                        null, null, null, null, null, 0, 20));
    }

    @Test
    void unknownResourceUuid_neverResolves_forJurisdictionRole() {
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.requireVisible(resourceId, null, regionalDarId, "REGIONAL_ADMIN"));
        verify(institutionRepository, never()).findByIdAndIsDeletedFalse(any());
    }

    private static Institution institution(UUID id) {
        Institution institution = new Institution();
        institution.setId(id);
        return institution;
    }
}
