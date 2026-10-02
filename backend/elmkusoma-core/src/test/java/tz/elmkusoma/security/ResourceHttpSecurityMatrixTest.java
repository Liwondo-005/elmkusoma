package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.learner.domain.LearnerEnrollment;
import tz.elmkusoma.learner.repository.LearnerEnrollmentRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end HTTP security matrix for resource access — the wiring proof for
 * the service-level gates (jurisdiction, institution, eligibility, analytics
 * scope) behind the real filter chain: JwtAuthenticationFilter →
 * JwtRequestAttributeFilter → OrganizationContextResolver → method security →
 * GlobalExceptionHandler.
 *
 * <p>Every response code below is part of the delivery contract:</p>
 * <ul>
 *   <li>Regional/District Admin → own jurisdiction: 200 (governance read),</li>
 *   <li>Regional/District Admin → other jurisdiction: 404 (no existence
 *       oracle, even with the exact resource UUID),</li>
 *   <li>Regional/District Admin → write endpoints: 403 (jurisdiction is
 *       read-only governance),</li>
 *   <li>institution-scoped caller → foreign institution: 404,</li>
 *   <li>learner → course-linked resource without enrollment: 403, with
 *       enrollment: 200 (backend authoritative),</li>
 *   <li>analytics → owner/admin: 200, other teacher: 403, learner: 403.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResourceHttpSecurityMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private LearnerEnrollmentRepository learnerEnrollmentRepository;

    // jurisdiction fixtures (created once per class — random scope ids and
    // emails keep them hermetic against other suites/runs on the shared test
    // database)
    private UUID regionDar;
    private UUID districtIlalaId;
    private UUID regionAru;
    private UUID districtArusha;
    private Institution instDar;
    private Institution instAru;
    private User regionalDar;
    private User regionalAru;
    private User districtIlala;
    private Resource resDar;
    private Resource resAru;

    @BeforeAll
    void createJurisdictionFixtures() {
        String run = UUID.randomUUID().toString().substring(0, 8);
        regionDar = UUID.randomUUID();
        districtIlalaId = UUID.randomUUID();
        regionAru = UUID.randomUUID();
        districtArusha = UUID.randomUUID();

        instDar = saveInstitution("Dar Jurisdiction " + run, regionDar, districtIlalaId);
        instAru = saveInstitution("Aru Jurisdiction " + run, regionAru, districtArusha);

        regionalDar = saveUser("regional-dar-matrix-" + run + "@test.com", User.Role.REGIONAL_ADMIN,
                regionDar, null, null);
        regionalAru = saveUser("regional-aru-matrix-" + run + "@test.com", User.Role.REGIONAL_ADMIN,
                regionAru, null, null);
        districtIlala = saveUser("district-ilala-matrix-" + run + "@test.com", User.Role.DISTRICT_ADMIN,
                null, districtIlalaId, null);

        resDar = saveResource(instDar, "Dar region resource");
        resAru = saveResource(instAru, "Aru region resource");
    }

    // ── fixtures ──

    private Institution saveInstitution(String name, UUID regionId, UUID districtId) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .code("JUR-" + UUID.randomUUID().toString().substring(0, 8))
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .regionId(regionId)
                .districtId(districtId)
                .isActive(true)
                .isDeleted(false)
                .build());
    }

    private User saveUser(String email, User.Role role, UUID regionId, UUID districtId,
                          UUID institutionId) {
        User user = User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("Matrix")
                .lastName("Tester")
                .role(role)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build();
        user.setRegionId(regionId);
        user.setDistrictId(districtId);
        user.setInstitutionId(institutionId);
        return userRepository.save(user);
    }

    private Resource saveResource(Institution institution, String title) {
        return resourceRepository.save(Resource.builder()
                .institutionId(institution.getId())
                .uploadedBy(UUID.randomUUID())
                .title(title)
                .resourceType(Resource.ResourceType.PDF)
                .storageUrl("https://storage.test/matrix.pdf")
                .visibility(Resource.ResourceVisibility.PUBLIC)
                .sortOrder(0)
                .isDeleted(false)
                .build());
    }

    // ── jurisdiction read governance ──

    @Test
    void regionalAdmin_readsOwnRegionResource_200() throws Exception {
        mockMvc.perform(get("/v1/resources/" + resDar.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(regionalDar.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Dar region resource"));
    }

    @Test
    void regionalAdmin_readsOtherRegionResource_404_evenWithExactUuid() throws Exception {
        mockMvc.perform(get("/v1/resources/" + resAru.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(regionalDar.getEmail())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/v1/resources/" + resAru.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(regionalAru.getEmail())))
                .andExpect(status().isOk());
    }

    @Test
    void districtAdmin_readsOwnDistrictResource_200_otherDistrict_404() throws Exception {
        mockMvc.perform(get("/v1/resources/" + resDar.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(districtIlala.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Dar region resource"));

        mockMvc.perform(get("/v1/resources/" + resAru.getId())
                        .header("Authorization", "Bearer " + TestTokens.userToken(districtIlala.getEmail())))
                .andExpect(status().isNotFound());
    }

    @Test
    void regionalAdmin_listScopedToOwnRegionOnly() throws Exception {
        mockMvc.perform(get("/v1/resources")
                        .header("Authorization", "Bearer " + TestTokens.userToken(regionalDar.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Dar region resource"));
    }

    @Test
    void regionalAdmin_querySearchStaysScopedToOwnRegion() throws Exception {
        mockMvc.perform(get("/v1/resources")
                        .param("q", "Dar region")
                        .header("Authorization", "Bearer " + TestTokens.userToken(regionalDar.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Dar region resource"));

        mockMvc.perform(get("/v1/resources")
                        .param("q", "Aru region")
                        .header("Authorization", "Bearer " + TestTokens.userToken(regionalDar.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void regionalAdmin_writeEndpoints_areForbidden() throws Exception {
        // jurisdiction grants governance READS only — writes stay with the
        // institution's teachers/admins
        mockMvc.perform(post("/v1/resources")
                        .header("Authorization", "Bearer " + TestTokens.userToken(regionalDar.getEmail()))
                        .contentType("application/json")
                        .content("{\"title\":\"region shouldn't publish\",\"resourceType\":\"PDF\","
                                + "\"storageUrl\":\"https://storage.test/x.pdf\",\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownResourceUuid_is404ForJurisdictionAdmin() throws Exception {
        mockMvc.perform(get("/v1/resources/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + TestTokens.userToken(regionalDar.getEmail())))
                .andExpect(status().isNotFound());
    }

    // ── institution scope (unchanged for everyone else) ──

    @Test
    void teacher_ofAnotherInstitution_readsForeignResource_404() throws Exception {
        mockMvc.perform(get("/v1/resources/" + resDar.getId())
                        .header("Authorization", "Bearer " + TestTokens.teacherToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void student_readsStandalonePublicResource_200_regression() throws Exception {
        Resource standalone = saveResource(
                institutionRepository.findById(TestDataSeeder.INSTITUTION_ID).orElseThrow(),
                "Standalone matrix resource");

        mockMvc.perform(get("/v1/resources/" + standalone.getId())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Standalone matrix resource"));
    }

    // ── instructor role admission (ResourceService treats INSTRUCTOR as a
    //    teacher for visibility, so the HTTP layer must admit them for
    //    authoring — while learner-facing APIs stay closed) ──

    @Test
    void instructor_createsAndReadsOwnResource_butLearnerApisStayClosed() throws Exception {
        Institution testInstitution = institutionRepository
                .findById(TestDataSeeder.INSTITUTION_ID).orElseThrow();
        User instructor = saveUser(
                "instructor-matrix-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com",
                User.Role.INSTRUCTOR, null, null, testInstitution.getId());

        var created = mockMvc.perform(post("/v1/resources")
                        .header("Authorization", "Bearer " + TestTokens.userToken(instructor.getEmail()))
                        .contentType("application/json")
                        .content("{\"title\":\"Instructor matrix resource\",\"resourceType\":\"PDF\","
                                + "\"storageUrl\":\"https://storage.test/instructor.pdf\","
                                + "\"visibility\":\"INSTITUTION\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID instructorResource = UUID.fromString(com.jayway.jsonpath.JsonPath
                .read(created.getResponse().getContentAsString(), "$.data.id"));

        mockMvc.perform(get("/v1/resources/" + instructorResource)
                        .header("Authorization", "Bearer " + TestTokens.userToken(instructor.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Instructor matrix resource"));

        // narrow admission: instructor still rejected by learner-facing APIs
        mockMvc.perform(get("/v1/learner/resources")
                        .header("Authorization", "Bearer " + TestTokens.userToken(instructor.getEmail())))
                .andExpect(status().isForbidden());
    }

    // ── learner eligibility (course link → enrollment) ──

    @Test
    void courseLinkedResource_deniedWithoutEnrollment_allowedWithIt() throws Exception {
        Institution testInstitution = institutionRepository
                .findById(TestDataSeeder.INSTITUTION_ID).orElseThrow();
        UUID courseScope = UUID.randomUUID();
        Resource courseResource = resourceRepository.save(Resource.builder()
                .institutionId(testInstitution.getId())
                .uploadedBy(TestDataSeeder.TEACHER_USER_ID)
                .title("Course matrix resource")
                .resourceType(Resource.ResourceType.PDF)
                .storageUrl("https://storage.test/course.pdf")
                .courseId(courseScope)
                .visibility(Resource.ResourceVisibility.PUBLIC)
                .sortOrder(0)
                .isDeleted(false)
                .build());

        // same institution, but the learner has no entitlement for this course
        mockMvc.perform(get("/v1/resources/" + courseResource.getId())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("not enrolled")));

        // with the enrollment in place the backend authorizes the read
        LearnerEnrollment enrollment = new LearnerEnrollment();
        enrollment.setUserId(TestDataSeeder.STUDENT_USER_ID);
        enrollment.setCourseId(courseScope);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setIsDeleted(false);
        learnerEnrollmentRepository.save(enrollment);

        mockMvc.perform(get("/v1/resources/" + courseResource.getId())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Course matrix resource"));
    }

    // ── analytics scope (owner-or-admin) ──

    @Test
    void analytics_ownerAndAdmin_200_otherTeacher_403_learner_403() throws Exception {
        Institution testInstitution = institutionRepository
                .findById(TestDataSeeder.INSTITUTION_ID).orElseThrow();
        Resource owned = resourceRepository.save(Resource.builder()
                .institutionId(testInstitution.getId())
                .uploadedBy(TestDataSeeder.TEACHER_USER_ID)
                .title("Owned matrix resource")
                .resourceType(Resource.ResourceType.PDF)
                .storageUrl("https://storage.test/owned.pdf")
                .visibility(Resource.ResourceVisibility.PUBLIC)
                .sortOrder(0)
                .isDeleted(false)
                .build());

        // another teacher in the same institution has no access to these numbers
        User otherTeacher = saveUser(
                "teacher-two-matrix-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com",
                User.Role.TEACHER, null, null, testInstitution.getId());

        mockMvc.perform(get("/v1/resources/" + owned.getId() + "/analytics")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/resources/" + owned.getId() + "/analytics")
                        .header("Authorization", "Bearer " + TestTokens.userToken(otherTeacher.getEmail())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/resources/" + owned.getId() + "/analytics")
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/resources/" + owned.getId() + "/analytics")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isForbidden());
    }
}
