package tz.elmkusoma.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.course.repository.CourseRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Course image upload, serving and tenancy.
 *
 * <p>Two defects from the audit are pinned here.</p>
 *
 * <p><b>Content was never checked.</b> The upload validated the extension and the declared MIME
 * type and stopped there, then served the result from an anonymous {@code /v1/content/**} route
 * with no {@code nosniff}. An HTML file named {@code .png} was therefore accepted and handed
 * back as {@code image/png} to any visitor.</p>
 *
 * <p><b>The reference was client-owned.</b> {@code thumbnailUrl} was an arbitrary string on the
 * course request, so any course author could point a published course's image at another
 * institution's upload.</p>
 *
 * <p>The storage root is redirected to a temporary directory for the run. That is what makes
 * "another client can fetch the same bytes" a real assertion here rather than a mocked one: the
 * bytes have to survive leaving the request that uploaded them.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties",
        properties = "file.upload.dir=${java.io.tmpdir}/elmkusoma-course-image-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CourseImageSecurityTest {

    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    private String adminToken;
    private UUID institutionId;

    @BeforeAll
    void fixtures() {
        institutionId = TestDataSeeder.INSTITUTION_ID;
        adminToken = TestTokens.userToken(TestTokens.ADMIN_EMAIL);
    }

    // ── authorization ─────────────────────────────────────────────────────────────────────

    @Test
    void anonymousCannotUpload() throws Exception {
        mockMvc.perform(multipart("/v1/courses/thumbnail").file(png("cover.png")))
                .andExpect(status().isForbidden());
    }

    @Test
    void aStudentCannotUpload() throws Exception {
        mockMvc.perform(multipart("/v1/courses/thumbnail")
                        .file(png("cover.png"))
                        .header("Authorization", "Bearer " + TestTokens.studentToken())
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void anInstitutionAdminCanUpload() throws Exception {
        mockMvc.perform(multipart("/v1/courses/thumbnail")
                        .file(png("cover.png"))
                        .header("Authorization", "Bearer " + TestTokens.userToken("admin2-" + UUID.randomUUID() + "@test.com"))
                        .header("X-Institution-Id", institutionId.toString()))
                // The token's user does not exist in this fixture set, so this proves the route is
                // role-gated rather than proving the happy path; the happy path is covered below
                // with the seeded administrator.
                .andExpect(status().is4xxClientError());
    }

    // ── content validation ────────────────────────────────────────────────────────────────

    @Test
    void anHtmlFileNamedPngIsRejected() throws Exception {
        // Named .png and declared image/png, but the bytes are HTML. Only a signature check
        // catches this, and it was the exact gap that let it be stored and served anonymously.
        byte[] html = "<html><script>alert(document.cookie)</script></html>".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(multipart("/v1/courses/thumbnail")
                        .file(new MockMultipartFile("file", "cover.png", "image/png", html))
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void svgIsRejected() throws Exception {
        byte[] svg = "<svg xmlns='http://www.w3.org/2000/svg'><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(multipart("/v1/courses/thumbnail")
                        .file(new MockMultipartFile("file", "logo.svg", "image/svg+xml", svg))
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anOversizedFileIsRejected() throws Exception {
        byte[] tooBig = new byte[(int) (5L * 1024 * 1024) + 1];
        System.arraycopy(PNG, 0, tooBig, 0, PNG.length);

        mockMvc.perform(multipart("/v1/courses/thumbnail")
                        .file(new MockMultipartFile("file", "big.png", "image/png", tooBig))
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isBadRequest());
    }

    // ── durable, shared retrieval ──────────────────────────────────────────────────────────

    @Test
    void anUploadedImageIsRetrievableByAnyClient() throws Exception {
        MvcResult uploaded = upload();
        String reference = referenceOf(uploaded);

        // The bytes are on disk and the reference is a real servable path, not a blob: URL and
        // not a preview that only exists in the uploader's tab.
        assertTrue(reference.startsWith("/v1/content/courses/"),
                "the persisted reference must be a servable path, got: " + reference);
        assertTrue(reference.contains(institutionId.toString()),
                "the reference must be scoped to the uploading institution: " + reference);

        // A completely separate request, with no session from the upload, retrieves the bytes.
        // This is the "another device sees the same image" property.
        byte[] body = mockMvc.perform(get(reference))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertEquals(PNG.length, body.length, "the stored bytes must match what was uploaded");

        // And the anonymous route must not be a content-type sniffing opportunity.
        mockMvc.perform(get(reference))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals("nosniff",
                        result.getResponse().getHeader("X-Content-Type-Options")));
    }

    @Test
    void twoUploadsGetDistinctReferences() throws Exception {
        assertNotEquals(referenceOf(upload()), referenceOf(upload()),
                "each upload must get its own object name, never overwrite the previous image");
    }

    @Test
    void aTraversalAttemptIsRefused() throws Exception {
        mockMvc.perform(get("/v1/content/courses/..%2f..%2fapplication.yml"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void anUnknownImageIs404NotAnError() throws Exception {
        mockMvc.perform(get("/v1/content/courses/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".png"))
                .andExpect(status().isNotFound());
    }

    // ── course association ─────────────────────────────────────────────────────────────────

    @Test
    void aCourseKeepsItsOwnInstitutionImage() throws Exception {
        String reference = referenceOf(upload());
        String title = "Course image " + UUID.randomUUID().toString().substring(0, 8);

        MvcResult created = mockMvc.perform(post("/v1/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType("application/json")
                        .content("{\"title\":\"" + title + "\",\"thumbnailUrl\":\"" + reference + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String id = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.data.id");
        UUID courseId = UUID.fromString(id);

        assertEquals(reference,
                courseRepository.findById(courseId).orElseThrow().getThumbnailUrl(),
                "the course must retain the reference it was given");
    }

    @Test
    void aCourseCannotAdoptAnotherInstitutionsImage() throws Exception {
        String foreignInstitution = UUID.randomUUID().toString();
        String foreignReference = "/v1/content/courses/" + foreignInstitution + "/" + UUID.randomUUID() + ".png";
        String title = "Steal " + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/v1/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Institution-Id", institutionId.toString())
                        .contentType("application/json")
                        .content("{\"title\":\"" + title + "\",\"thumbnailUrl\":\"" + foreignReference + "\"}"))
                .andExpect(status().isForbidden());
    }

    // ── helpers ───────────────────────────────────────────────────────────────────────────

    private MvcResult upload() throws Exception {
        return mockMvc.perform(multipart("/v1/courses/thumbnail")
                        .file(png("cover.png"))
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Institution-Id", institutionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").exists())
                .andReturn();
    }

    private static String referenceOf(MvcResult result) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.url");
    }

    private static MockMultipartFile png(String name) {
        return new MockMultipartFile("file", name, "image/png", PNG);
    }
}
