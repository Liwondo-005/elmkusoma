package tz.elmkusoma.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.academic.domain.Grade;
import tz.elmkusoma.academic.repository.GradeRepository;
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.domain.EventMaterial;
import tz.elmkusoma.event.repository.EventMaterialRepository;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.highereducation.domain.LearningModule;
import tz.elmkusoma.highereducation.domain.ModuleStatus;
import tz.elmkusoma.highereducation.domain.WorkshopSession;
import tz.elmkusoma.highereducation.repository.LearningModuleRepository;
import tz.elmkusoma.highereducation.domain.WorkshopSession;
import tz.elmkusoma.highereducation.repository.WorkshopSessionRepository;
import tz.elmkusoma.learner.domain.LearnerNotification;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Provider/institution isolation + privilege-escalation regression through the real filter
 * chain (JwtAuthenticationFilter → JwtRequestAttributeFilter → OrganizationContextResolver →
 * URL fence → method security → controller scope helpers → GlobalExceptionHandler).
 *
 * <p>Covers the P0/P1 fixes: fail-closed object-level event access (ID manipulation),
 * server-authoritative provider_id on create, scoped material deletion, /v1/admin access for
 * PROVIDER_ADMIN while /v1/platform-admin stays ADMIN-only, cross-institution header denial,
 * academic grades IDOR (by-id + client-institution spoof), learner ownership on modules and
 * workshops, and notification inbox ownership.
 *
 * <p>Two institutions (the seeded tenant A + a fresh tenant B); every mutation-denied case
 * re-reads the row to prove no write happened.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProviderIsolationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstitutionMembershipRepository membershipRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMaterialRepository eventMaterialRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private LearningModuleRepository learningModuleRepository;

    @Autowired
    private WorkshopSessionRepository workshopSessionRepository;

    @Autowired
    private LearnerNotificationRepository notificationRepository;

    private UUID tenantA;
    private Institution instB;

    private User providerA;
    private User studentA;
    private User studentB;

    private Event ownEvent;
    private Event foreignEvent;
    private EventMaterial foreignMaterial;

    private Grade ownGrade;
    private Grade foreignGrade;

    private LearningModule ownModule;
    private LearningModule foreignModule;

    private LearnerNotification notifA;
    private LearnerNotification notifB;

    private String providerAToken;
    private String studentAToken;
    private String studentBToken;

    @BeforeAll
    void createFixtures() {
        String run = UUID.randomUUID().toString().substring(0, 8);
        tenantA = TestDataSeeder.INSTITUTION_ID;
        instB = saveInstitution("Provider Iso B " + run);

        providerA = saveUser("provider-iso-a-" + run + "@test.com", User.Role.PROVIDER_ADMIN, tenantA);
        studentA = saveUser("provider-iso-student-a-" + run + "@test.com", User.Role.STUDENT, tenantA);
        studentB = saveUser("provider-iso-student-b-" + run + "@test.com", User.Role.STUDENT, instB.getId());

        providerAToken = TestTokens.userToken(providerA.getEmail());
        studentAToken = TestTokens.userToken(studentA.getEmail());
        studentBToken = TestTokens.userToken(studentB.getEmail());

        ownEvent = saveEvent("Provider iso own event", tenantA, providerA.getId(),
                providerA.getId().toString());
        foreignEvent = saveEvent("Provider iso foreign event", instB.getId(), providerA.getId(),
                UUID.randomUUID().toString());

        foreignMaterial = eventMaterialRepository.save(EventMaterial.builder()
                .eventId(foreignEvent.getId())
                .title("Provider iso foreign material")
                .materialType("DOCUMENT")
                .fileUrl("/materials/provider-iso-foreign.pdf")
                .isPublic(false)
                .build());

        ownGrade = saveGrade("Provider iso grade A", "PIA-" + run, tenantA);
        foreignGrade = saveGrade("Provider iso grade B", "PIB-" + run, instB.getId());

        ownModule = learningModuleRepository.save(LearningModule.builder()
                .institutionId(tenantA)
                .studentId(studentA.getId())
                .courseId(UUID.randomUUID())
                .moduleTitle("Provider iso own module")
                .moduleCode("PIOM-" + run)
                .status(ModuleStatus.NOT_STARTED)
                .progressPercent(0)
                .build());

        foreignModule = learningModuleRepository.save(LearningModule.builder()
                .institutionId(instB.getId())
                .studentId(studentB.getId())
                .courseId(UUID.randomUUID())
                .moduleTitle("Provider iso foreign module")
                .moduleCode("PIFM-" + run)
                .status(ModuleStatus.NOT_STARTED)
                .progressPercent(0)
                .build());

        notifA = notificationRepository.save(LearnerNotification.builder()
                .userId(studentA.getId())
                .institutionId(tenantA)
                .title("Provider iso own notification")
                .message("own")
                .notificationType("GENERAL")
                .isRead(false)
                .build());
        notifB = notificationRepository.save(LearnerNotification.builder()
                .userId(studentB.getId())
                .institutionId(instB.getId())
                .title("Provider iso foreign notification")
                .message("foreign")
                .notificationType("GENERAL")
                .isRead(false)
                .build());
    }

    // ── events: object-level (ID-manipulation) isolation ──

    @Test
    void providerAdmin_readsOwnEvent_200() throws Exception {
        mockMvc.perform(get("/v1/events/" + ownEvent.getId())
                        .header("Authorization", "Bearer " + providerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ownEvent.getId().toString()));
    }

    @Test
    void providerAdmin_readsForeignEvent_404() throws Exception {
        mockMvc.perform(get("/v1/events/" + foreignEvent.getId())
                        .header("Authorization", "Bearer " + providerAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void providerAdmin_updatesForeignEvent_403_andRowUnchanged() throws Exception {
        mockMvc.perform(put("/v1/events/" + foreignEvent.getId())
                        .header("Authorization", "Bearer " + providerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody("Tampered title")))
                .andExpect(status().isForbidden());

        Event reloaded = eventRepository.findById(foreignEvent.getId()).orElseThrow();
        assertEquals("Provider iso foreign event", reloaded.getTitle());
    }

    @Test
    void providerAdmin_deletesForeignEvent_403_andRowUnchanged() throws Exception {
        mockMvc.perform(delete("/v1/events/" + foreignEvent.getId())
                        .header("Authorization", "Bearer " + providerAToken))
                .andExpect(status().isForbidden());

        Event reloaded = eventRepository.findById(foreignEvent.getId()).orElseThrow();
        assertFalse(reloaded.getIsDeleted());
    }

    @Test
    void providerAdmin_deletesForeignMaterial_403_andRowUnchanged() throws Exception {
        mockMvc.perform(delete("/v1/events/materials/" + foreignMaterial.getId())
                        .header("Authorization", "Bearer " + providerAToken))
                .andExpect(status().isForbidden());

        EventMaterial reloaded = eventMaterialRepository.findById(foreignMaterial.getId()).orElseThrow();
        assertFalse(reloaded.getIsDeleted());
    }

    @Test
    void providerAdmin_createEvent_spoofedProviderId_forcedToOwnIdentity() throws Exception {
        String body = "{\"title\":\"Provider iso spoof event\",\"eventType\":\"WORKSHOP\","
                + "\"startsAt\":\"2027-01-10T10:00:00\","
                + "\"providerId\":\"spoofed-provider-" + UUID.randomUUID() + "\"}";

        MvcResult result = mockMvc.perform(post("/v1/events")
                        .header("Authorization", "Bearer " + providerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.data.providerId").value(providerA.getId().toString()))
                .andReturn();

        String createdId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        Event created = eventRepository.findById(UUID.fromString(createdId)).orElseThrow();
        assertEquals(providerA.getId().toString(), created.getProviderId());
        assertEquals(tenantA, created.getInstitutionId());
    }

    @Test
    void providerAdmin_pagedEventList_scopedToOwnEvents() throws Exception {
        mockMvc.perform(get("/v1/events?page=0&size=50")
                        .header("Authorization", "Bearer " + providerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id").value(hasItem(ownEvent.getId().toString())))
                .andExpect(jsonPath("$.data[*].id").value(not(hasItem(foreignEvent.getId().toString()))));
    }

    // ── platform vs provider authority separation ──

    @Test
    void providerAdmin_platformAdminEndpoint_403() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/dashboard")
                        .header("Authorization", "Bearer " + providerAToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void providerAdmin_ownOrgAdminEndpoint_200() throws Exception {
        mockMvc.perform(get("/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + providerAToken))
                .andExpect(status().isOk());
    }

    @Test
    void providerAdmin_foreignInstitutionHeader_403() throws Exception {
        mockMvc.perform(get("/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + providerAToken)
                        .header("X-Institution-Id", instB.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ── academic grades: by-id IDOR + client-institution spoof ──

    @Test
    void admin_readsOwnGrade_200_andForeignGrade_404() throws Exception {
        mockMvc.perform(get("/v1/academic/grades/" + ownGrade.getId())
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ownGrade.getId().toString()));

        mockMvc.perform(get("/v1/academic/grades/" + foreignGrade.getId())
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void admin_createGrade_clientInstitutionIgnored() throws Exception {
        String body = "{\"institutionId\":\"" + instB.getId() + "\","
                + "\"educationLevel\":\"SECONDARY\","
                + "\"name\":\"Provider iso spoof grade\","
                + "\"code\":\"PI-SPOOF-1\","
                + "\"sortOrder\":9}";

        mockMvc.perform(post("/v1/academic/grades")
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.data.institutionId").value(tenantA.toString()));
    }

    @Test
    void admin_gradesList_clientInstitutionParamIgnored() throws Exception {
        mockMvc.perform(get("/v1/academic/grades?institutionId=" + instB.getId())
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id").value(hasItem(ownGrade.getId().toString())))
                .andExpect(jsonPath("$.data[*].id").value(not(hasItem(foreignGrade.getId().toString()))));
    }

    // ── learner ownership: modules and workshops ──

    @Test
    void student_readsForeignModule_404() throws Exception {
        mockMvc.perform(get("/v1/college/learner/modules/" + foreignModule.getId())
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void student_listsForeignStudentsModules_403() throws Exception {
        mockMvc.perform(get("/v1/college/learner/modules/student/" + studentB.getId())
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void student_listsOwnModules_200() throws Exception {
        mockMvc.perform(get("/v1/college/learner/modules/student/" + studentA.getId())
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isOk());
    }

    @Test
    void student_updatesOwnModuleProgress_200() throws Exception {
        mockMvc.perform(put("/v1/college/learner/modules/" + ownModule.getId() + "/progress")
                        .header("Authorization", "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progressPercent\":55,\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.progressPercent").value(55))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        LearningModule reloaded = learningModuleRepository.findById(ownModule.getId()).orElseThrow();
        assertEquals(55, reloaded.getProgressPercent().intValue());
        assertEquals(ModuleStatus.IN_PROGRESS, reloaded.getStatus());
    }

    @Test
    void student_updatesForeignModuleProgress_404_andRowUnchanged() throws Exception {
        int before = learningModuleRepository.findById(foreignModule.getId())
                .orElseThrow().getProgressPercent();

        mockMvc.perform(put("/v1/college/learner/modules/" + foreignModule.getId() + "/progress")
                        .header("Authorization", "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progressPercent\":99}"))
                .andExpect(status().isNotFound());

        assertEquals(before, learningModuleRepository.findById(foreignModule.getId())
                .orElseThrow().getProgressPercent().intValue());
    }

    @Test
    void student_updatesOwnModuleProgress_invalidPercent_400() throws Exception {
        int before = learningModuleRepository.findById(ownModule.getId())
                .orElseThrow().getProgressPercent();

        mockMvc.perform(put("/v1/college/learner/modules/" + ownModule.getId() + "/progress")
                        .header("Authorization", "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progressPercent\":150}"))
                .andExpect(status().isBadRequest());

        assertEquals(before, learningModuleRepository.findById(ownModule.getId())
                .orElseThrow().getProgressPercent().intValue());
    }

    @Test
    void student_updatesOwnModuleProgress_dropped_400_andRowUnchanged() throws Exception {
        int before = learningModuleRepository.findById(ownModule.getId())
                .orElseThrow().getProgressPercent();

        mockMvc.perform(put("/v1/college/learner/modules/" + ownModule.getId() + "/progress")
                        .header("Authorization", "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progressPercent\":100,\"status\":\"DROPPED\"}"))
                .andExpect(status().isBadRequest());

        assertEquals(before, learningModuleRepository.findById(ownModule.getId())
                .orElseThrow().getProgressPercent().intValue());
    }

    @Test
    void student_listsForeignStudentsWorkshops_403() throws Exception {
        mockMvc.perform(get("/v1/college/learner/workshops/student/" + studentB.getId())
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacher_createWorkshop_clientInstitutionIgnored() throws Exception {
        String body = "{\"institutionId\":\"" + instB.getId() + "\","
                + "\"studentId\":\"" + studentA.getId() + "\","
                + "\"title\":\"Provider iso workshop\","
                + "\"workshopType\":\"WORKSHOP\","
                + "\"scheduledAt\":\"2027-01-11T09:00:00\"}";

        mockMvc.perform(post("/v1/college/learner/workshops")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.institutionId").value(tenantA.toString()));
    }

    // ── learner workshop create/delete ownership ──

    @Test
    void student_createOwnWorkshop_forcedStudentIdAndInstitution_200() throws Exception {
        String body = "{\"institutionId\":\"" + instB.getId() + "\","
                + "\"studentId\":\"" + studentA.getId() + "\","
                + "\"title\":\"Provider iso learner workshop\","
                + "\"workshopType\":\"Workshop\","
                + "\"scheduledAt\":\"2027-02-11T09:00:00\"}";

        mockMvc.perform(post("/v1/college/learner/workshops")
                        .header("Authorization", "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value(studentA.getId().toString()))
                .andExpect(jsonPath("$.data.institutionId").value(tenantA.toString()));
    }

    @Test
    void student_createWorkshop_forForeignStudent_403() throws Exception {
        String body = "{\"studentId\":\"" + studentB.getId() + "\","
                + "\"title\":\"Provider iso spoofed learner workshop\","
                + "\"workshopType\":\"Workshop\"}";

        mockMvc.perform(post("/v1/college/learner/workshops")
                        .header("Authorization", "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void student_deletesOwnWorkshop_200() throws Exception {
        UUID workshopId = createWorkshopFor(studentA.getId());

        mockMvc.perform(delete("/v1/college/learner/workshops/" + workshopId)
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Audit B-56: workshop deletion is a soft delete (BaseEntity.is_deleted), so retention
        // sweeps and the audit trail can still see the removal. The row must survive but be flagged.
        WorkshopSession deleted = workshopSessionRepository.findById(workshopId).orElse(null);
        assertNotNull(deleted, "soft delete must keep the row for the audit trail");
        assertTrue(Boolean.TRUE.equals(deleted.getIsDeleted()), "workshop must be soft deleted");
    }

    @Test
    void student_deletesForeignLearnersWorkshop_403_andRowUnchanged() throws Exception {
        UUID workshopId = createWorkshopFor(studentA.getId());

        mockMvc.perform(delete("/v1/college/learner/workshops/" + workshopId)
                        .header("Authorization", "Bearer " + TestTokens.otherStudentToken()))
                .andExpect(status().isForbidden());

        assertTrue(workshopSessionRepository.existsById(workshopId));
    }

    @Test
    void foreignInstitutionStudent_deletesWorkshop_403_andRowUnchanged() throws Exception {
        UUID workshopId = createWorkshopFor(studentA.getId());

        mockMvc.perform(delete("/v1/college/learner/workshops/" + workshopId)
                        .header("Authorization", "Bearer " + studentBToken))
                .andExpect(status().isForbidden());

        assertTrue(workshopSessionRepository.existsById(workshopId));
    }

    @Test
    void teacher_deleteWorkshop_403_roleGateUnchanged() throws Exception {
        UUID workshopId = createWorkshopFor(studentA.getId());

        mockMvc.perform(delete("/v1/college/learner/workshops/" + workshopId)
                        .header("Authorization", "Bearer " + TestTokens.teacherToken()))
                .andExpect(status().isForbidden());

        assertTrue(workshopSessionRepository.existsById(workshopId));
    }

    @Test
    void institutionAdmin_deletesLearnersWorkshop_200() throws Exception {
        UUID workshopId = createWorkshopFor(studentA.getId());

        mockMvc.perform(delete("/v1/college/learner/workshops/" + workshopId)
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Audit B-56: soft delete, consistent with every other entity in the project.
        WorkshopSession deleted = workshopSessionRepository.findById(workshopId).orElse(null);
        assertNotNull(deleted, "soft delete must keep the row for the audit trail");
        assertTrue(Boolean.TRUE.equals(deleted.getIsDeleted()), "workshop must be soft deleted");
    }

    private UUID createWorkshopFor(UUID studentId) throws Exception {
        String body = "{\"institutionId\":\"" + instB.getId() + "\","
                + "\"studentId\":\"" + studentId + "\","
                + "\"title\":\"Provider iso delete target\","
                + "\"workshopType\":\"WORKSHOP\","
                + "\"scheduledAt\":\"2027-03-11T09:00:00\"}";

        MvcResult result = mockMvc.perform(post("/v1/college/learner/workshops")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        String createdId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return UUID.fromString(createdId);
    }

    // ── notification inbox ownership ──

    @Test
    void student_markForeignNotificationRead_403_andOwn_200() throws Exception {
        mockMvc.perform(put("/v1/notifications/" + notifB.getId() + "/read")
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isForbidden());
        assertFalse(notificationRepository.findById(notifB.getId()).orElseThrow().getIsRead());

        mockMvc.perform(put("/v1/notifications/" + notifA.getId() + "/read")
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isOk());
        assertTrue(notificationRepository.findById(notifA.getId()).orElseThrow().getIsRead());
    }

    // ── fixtures ──

    private String eventBody(String title) {
        return "{\"title\":\"" + title + "\",\"eventType\":\"WORKSHOP\","
                + "\"startsAt\":\"2027-01-12T10:00:00\"}";
    }

    private Institution saveInstitution(String name) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .code("PI-" + UUID.randomUUID().toString().substring(0, 8))
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());
    }

    private User saveUser(String email, User.Role role, UUID institutionId) {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("ProviderIso")
                .lastName("Tester")
                .role(role)
                .institutionId(institutionId)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        InstitutionMembership.Role membershipRole = switch (role) {
            case STUDENT -> InstitutionMembership.Role.STUDENT;
            case PROVIDER_ADMIN, PROVIDER_STAFF -> InstitutionMembership.Role.ADMIN;
            default -> InstitutionMembership.Role.ADMIN;
        };
        membershipRepository.save(InstitutionMembership.builder()
                .userId(user.getId())
                .institutionId(institutionId)
                .role(membershipRole)
                .isActive(true)
                .build());
        return user;
    }

    private Event saveEvent(String title, UUID institutionId, UUID organizerId, String providerId) {
        return eventRepository.save(Event.builder()
                .institutionId(institutionId)
                .organizerId(organizerId)
                .title(title)
                .eventType("WORKSHOP")
                .startsAt(LocalDateTime.now().plusDays(7))
                .providerId(providerId)
                .status("PUBLISHED")
                // @SuperBuilder bypasses field initializers — set NOT NULL defaults explicitly
                .isFree(true)
                .requiresApproval(false)
                .build());
    }

    private Grade saveGrade(String name, String code, UUID institutionId) {
        return gradeRepository.save(Grade.builder()
                .institutionId(institutionId)
                .educationLevel(EducationLevel.SECONDARY)
                .name(name)
                .code(code)
                .sortOrder(99)
                .isActive(true)
                .build());
    }
}
