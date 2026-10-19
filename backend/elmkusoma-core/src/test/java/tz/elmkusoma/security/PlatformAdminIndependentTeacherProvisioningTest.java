package tz.elmkusoma.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.testutil.TestTokens;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Platform Admin provisioning of teacher account types through the existing
 * {@code POST /v1/platform-admin/users} workflow — the server half of the independent-teacher
 * option in the user form.
 *
 * <p>The contract pinned here (CreateUserRequest.independentTeacher + PlatformAdminService):</p>
 * <ul>
 *   <li>An independent teacher is created with the explicit flag and NO organization: the
 *       persisted account has no institution and no membership row — no affiliation is ever
 *       invented on the admin's behalf.</li>
 *   <li>School-affiliated teacher creation is untouched: organization given (or required)
 *       means a real membership for that real organization.</li>
 *   <li>The waiver is TEACHER-only: no other organization-scoped role may drop its school
 *       by claiming the flag.</li>
 *   <li>A teacher with neither a school nor the flag is rejected — the pre-existing
 *       organization-scoped rule keeps holding.</li>
 *   <li>The flag alongside an explicit organization does not detach it: a school the admin
 *       actually chose still produces its real membership.</li>
 * </ul>
 *
 * <p>Every fixture is created fresh per test with a random run suffix and removed again in
 * {@code @AfterEach}; no pre-existing row is touched.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class PlatformAdminIndependentTeacherProvisioningTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private InstitutionRepository institutionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InstitutionMembershipRepository membershipRepository;

    private String run;
    private Institution school;
    private User platformAdmin;

    private final List<Institution> institutions = new ArrayList<>();
    private final List<User> users = new ArrayList<>();
    private final List<InstitutionMembership> memberships = new ArrayList<>();

    @BeforeEach
    void setUp() {
        run = UUID.randomUUID().toString().substring(0, 8);
        institutions.clear();
        users.clear();
        memberships.clear();

        platformAdmin = saveAdmin("prov-admin-" + run + "@test.com");
        school = saveInstitution("Provision " + run);
        // Mirror the accepted platform-admin fixture (G-tests): the account resolves an
        // organization context like any other admin of the platform's own workflows.
        memberships.add(membershipRepository.save(InstitutionMembership.builder()
                .userId(platformAdmin.getId())
                .institutionId(school.getId())
                .role(InstitutionMembership.Role.ADMIN)
                .isActive(true)
                .build()));
    }

    @AfterEach
    void cleanUpOwnRowsOnly() {
        if (!memberships.isEmpty()) membershipRepository.deleteAll(memberships);
        if (!users.isEmpty()) userRepository.deleteAll(users);
        if (!institutions.isEmpty()) institutionRepository.deleteAll(institutions);
    }

    @Test
    void independentTeacher_isCreatedWithoutSchoolAndWithoutAnyMembership() throws Exception {
        String email = "prov-teacher-ind-" + run + "@test.com";

        mockMvc.perform(post("/v1/platform-admin/users")
                        .header("Authorization", token(platformAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Independent\",\"lastName\":\"Teacher\","
                                + "\"email\":\"" + email + "\",\"password\":\"password123\","
                                + "\"role\":\"TEACHER\",\"independentTeacher\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("TEACHER"));

        User created = userRepository.findByEmailAndIsDeletedFalse(email).orElseThrow();
        users.add(created);
        assertTrue(created.getInstitutionId() == null,
                "an independent teacher must persist with no institution");
        assertTrue(membershipRepository.findByUserIdAndIsActiveTrue(created.getId()).isEmpty(),
                "an independent teacher must get no organization membership — none is invented");
    }

    @Test
    void schoolAffiliatedTeacher_creationIsUnchangedAndGetsARealMembership() throws Exception {
        String email = "prov-teacher-school-" + run + "@test.com";

        mockMvc.perform(post("/v1/platform-admin/users")
                        .header("Authorization", token(platformAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"School\",\"lastName\":\"Teacher\","
                                + "\"email\":\"" + email + "\",\"password\":\"password123\","
                                + "\"role\":\"TEACHER\",\"institutionId\":\"" + school.getId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("TEACHER"));

        User created = userRepository.findByEmailAndIsDeletedFalse(email).orElseThrow();
        users.add(created);
        assertEquals(school.getId(), created.getInstitutionId(),
                "a school-affiliated teacher keeps the chosen school");
        List<InstitutionMembership> own = membershipRepository.findByUserIdAndIsActiveTrue(created.getId());
        memberships.addAll(own);
        assertEquals(1, own.size(), "the school membership must actually be created");
        assertEquals(school.getId(), own.get(0).getInstitutionId());
        assertEquals(InstitutionMembership.Role.TEACHER, own.get(0).getRole());
    }

    @Test
    void teacherWithNeitherSchoolNorFlag_isRejected() throws Exception {
        String email = "prov-teacher-bare-" + run + "@test.com";

        mockMvc.perform(post("/v1/platform-admin/users")
                        .header("Authorization", token(platformAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Bare\",\"lastName\":\"Teacher\","
                                + "\"email\":\"" + email + "\",\"password\":\"password123\","
                                + "\"role\":\"TEACHER\"}"))
                .andExpect(status().isBadRequest());

        assertFalse(userRepository.existsByEmailAndIsDeletedFalse(email),
                "a rejected provisioning attempt must not leave an account behind");
    }

    @Test
    void independentFlag_isHonouredOnlyForTeachers() throws Exception {
        String email = "prov-admin-ind-" + run + "@test.com";

        // INSTITUTION_ADMIN is organization-scoped; the waiver is TEACHER-only, so the flag
        // must not become a back door for dropping the school requirement of another role.
        mockMvc.perform(post("/v1/platform-admin/users")
                        .header("Authorization", token(platformAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Claimed\",\"lastName\":\"Independent\","
                                + "\"email\":\"" + email + "\",\"password\":\"password123\","
                                + "\"role\":\"INSTITUTION_ADMIN\",\"independentTeacher\":true}"))
                .andExpect(status().isBadRequest());

        assertFalse(userRepository.existsByEmailAndIsDeletedFalse(email),
                "a non-teacher may not provision itself without an organization");
    }

    @Test
    void independentFlagWithExplicitSchool_stillCreatesTheRealMembership() throws Exception {
        String email = "prov-teacher-flag-school-" + run + "@test.com";

        mockMvc.perform(post("/v1/platform-admin/users")
                        .header("Authorization", token(platformAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Waiver\",\"lastName\":\"WithSchool\","
                                + "\"email\":\"" + email + "\",\"password\":\"password123\","
                                + "\"role\":\"TEACHER\",\"independentTeacher\":true,"
                                + "\"institutionId\":\"" + school.getId() + "\"}"))
                .andExpect(status().isCreated());

        User created = userRepository.findByEmailAndIsDeletedFalse(email).orElseThrow();
        users.add(created);
        List<InstitutionMembership> own = membershipRepository.findByUserIdAndIsActiveTrue(created.getId());
        memberships.addAll(own);
        // The flag only waives the requirement; a school the admin explicitly chose is never
        // silently detached — the account lands in that real membership like any teacher.
        assertEquals(school.getId(), created.getInstitutionId(),
                "an explicitly chosen school must persist even with the waiver set");
        assertEquals(1, own.size(), "the explicitly chosen school's membership must exist");
        assertEquals(school.getId(), own.get(0).getInstitutionId());
    }

    // ── fixtures ───────────────────────────────────────────────────────────────────────────

    private Institution saveInstitution(String name) {
        Institution saved = institutionRepository.save(Institution.builder()
                .name(name)
                .code("PROV-" + UUID.randomUUID().toString().substring(0, 8))
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());
        institutions.add(saved);
        return saved;
    }

    private User saveAdmin(String email) {
        User admin = userRepository.save(User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("Provision")
                .lastName("Admin")
                .role(User.Role.ADMIN)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        users.add(admin);
        return admin;
    }

    /** TestTokens emits the raw JWT; the filter only accepts {@code Authorization: Bearer <jwt>}. */
    private String token(User user) {
        return "Bearer " + TestTokens.userToken(user.getEmail());
    }
}
