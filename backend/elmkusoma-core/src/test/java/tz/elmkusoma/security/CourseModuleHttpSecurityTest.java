package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tz.elmkusoma.course.domain.Course;
import tz.elmkusoma.course.domain.CourseLesson;
import tz.elmkusoma.course.domain.CourseModule;
import tz.elmkusoma.course.repository.CourseLessonRepository;
import tz.elmkusoma.course.repository.CourseModuleRepository;
import tz.elmkusoma.course.repository.CourseRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.testutil.TestTokens;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP security regression for the course module/lesson routes
 * ({@code /v1/courses/**}) through the real filter chain. CourseService denies
 * cross-institution access with 403 (deny-by-scope, no existence oracle via
 * 404), so every cross-institution probe below must yield 403 and leave the
 * foreign row untouched.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CourseModuleHttpSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstitutionMembershipRepository membershipRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseModuleRepository moduleRepository;

    @Autowired
    private CourseLessonRepository lessonRepository;

    private Institution instA;
    private Institution instB;

    private User teacherA;

    private Course courseA;
    private Course courseB;
    private CourseModule moduleA;
    private CourseModule moduleB;
    private CourseLesson lessonA;
    private CourseLesson lessonB;

    @BeforeAll
    void createFixtures() {
        String run = UUID.randomUUID().toString().substring(0, 8);

        instA = saveInstitution("Course Sec A " + run);
        instB = saveInstitution("Course Sec B " + run);

        teacherA = saveUser("course-teacher-a-" + run + "@test.com", User.Role.TEACHER, instA);

        courseA = saveCourse(instA, "Course Sec A " + run);
        courseB = saveCourse(instB, "Course Sec B " + run);

        moduleA = saveModule(courseA, "Module A " + run);
        moduleB = saveModule(courseB, "Module B " + run);

        lessonA = saveLesson(moduleA, "Lesson A " + run);
        lessonB = saveLesson(moduleB, "Lesson B " + run);
    }

    // ── fixtures ──

    private Institution saveInstitution(String name) {
        return institutionRepository.save(Institution.builder()
                .name(name)
                .code("CRS-" + UUID.randomUUID().toString().substring(0, 8))
                .type(Institution.InstitutionType.SECONDARY)
                .country("Tanzania")
                .isActive(true)
                .isDeleted(false)
                .build());
    }

    private User saveUser(String email, User.Role role, Institution institution) {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("Course")
                .lastName("Tester")
                .role(role)
                .institutionId(institution.getId())
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        membershipRepository.save(InstitutionMembership.builder()
                .userId(user.getId())
                .institutionId(institution.getId())
                .role(InstitutionMembership.Role.TEACHER)
                .isActive(true)
                .build());
        return user;
    }

    private Course saveCourse(Institution institution, String title) {
        return courseRepository.save(Course.builder()
                .institutionId(institution.getId())
                .title(title)
                .level("SECONDARY")
                .isPublished(false)
                .isFeatured(false)
                .isDeleted(false)
                .build());
    }

    private CourseModule saveModule(Course course, String title) {
        return moduleRepository.save(CourseModule.builder()
                .institutionId(course.getInstitutionId())
                .courseId(course.getId())
                .title(title)
                .sortOrder(0)
                .isDeleted(false)
                .build());
    }

    private CourseLesson saveLesson(CourseModule module, String title) {
        return lessonRepository.save(CourseLesson.builder()
                .institutionId(module.getInstitutionId())
                .moduleId(module.getId())
                .title(title)
                .contentType("VIDEO")
                .sortOrder(0)
                .isFree(false)
                .isDeleted(false)
                .build());
    }

    private String bearer() {
        return "Bearer " + TestTokens.userToken(teacherA.getEmail());
    }

    // ── cross-institution reads ──

    @Test
    void getModules_foreignCourse_403() throws Exception {
        mockMvc.perform(get("/v1/courses/" + courseB.getId() + "/modules")
                        .header("Authorization", bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLessons_foreignModule_403() throws Exception {
        mockMvc.perform(get("/v1/courses/modules/" + moduleB.getId() + "/lessons")
                        .header("Authorization", bearer()))
                .andExpect(status().isForbidden());
    }

    // ── cross-institution deletes (deny + no write) ──

    @Test
    void deleteModule_foreignModule_403_andNotDeleted() throws Exception {
        mockMvc.perform(delete("/v1/courses/modules/" + moduleB.getId())
                        .header("Authorization", bearer()))
                .andExpect(status().isForbidden());

        CourseModule reloaded = moduleRepository.findById(moduleB.getId()).orElseThrow();
        assertFalse(reloaded.getIsDeleted());
    }

    @Test
    void deleteLesson_foreignLesson_403_andNotDeleted() throws Exception {
        mockMvc.perform(delete("/v1/courses/lessons/" + lessonB.getId())
                        .header("Authorization", bearer()))
                .andExpect(status().isForbidden());

        CourseLesson reloaded = lessonRepository.findById(lessonB.getId()).orElseThrow();
        assertFalse(reloaded.getIsDeleted());
    }

    // ── cross-institution create (deny + nothing persisted) ──

    @Test
    void createLesson_intoForeignModule_403_andNothingPersisted() throws Exception {
        long before = lessonRepository.countByModuleIdAndIsDeletedFalse(moduleB.getId());

        mockMvc.perform(post("/v1/courses/modules/" + moduleB.getId() + "/lessons")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Smuggled lesson\",\"contentType\":\"VIDEO\","
                                + "\"durationMinutes\":5,\"sortOrder\":1,\"isFree\":false}"))
                .andExpect(status().isForbidden());

        assertEquals(before, lessonRepository.countByModuleIdAndIsDeletedFalse(moduleB.getId()));
    }

    // ── same-institution happy paths ──

    @Test
    void getModules_ownCourse_200() throws Exception {
        mockMvc.perform(get("/v1/courses/" + courseA.getId() + "/modules")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].courseId").value(courseA.getId().toString()));
    }

    @Test
    void getLessons_ownModule_200() throws Exception {
        mockMvc.perform(get("/v1/courses/modules/" + moduleA.getId() + "/lessons")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].moduleId").value(moduleA.getId().toString()));
    }

    @Test
    void createModule_ownCourse_201() throws Exception {
        mockMvc.perform(post("/v1/courses/" + courseA.getId() + "/modules")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Fresh module "
                                + UUID.randomUUID().toString().substring(0, 8)
                                + "\",\"description\":\"happy path\",\"sortOrder\":9}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createLesson_ownModule_201() throws Exception {
        mockMvc.perform(post("/v1/courses/modules/" + moduleA.getId() + "/lessons")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Fresh lesson "
                                + UUID.randomUUID().toString().substring(0, 8)
                                + "\",\"contentType\":\"VIDEO\",\"durationMinutes\":7}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }
}
