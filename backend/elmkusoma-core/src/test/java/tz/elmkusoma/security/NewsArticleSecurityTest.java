package tz.elmkusoma.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.administration.domain.PlatformConfigEntry;
import tz.elmkusoma.administration.repository.PlatformConfigRepository;
import tz.elmkusoma.shared.domain.NewsArticle;
import tz.elmkusoma.shared.repository.NewsArticleRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Public news and announcements.
 *
 * <p>The invariants worth defending are all about the gap between what an administrator can see
 * and what an anonymous visitor can see. A draft that leaks is the serious failure, so most of
 * these tests assert absence: that a draft is not listed, not fetchable by slug, and that the
 * public payload carries no working-copy fields at all.</p>
 *
 * <p>Time-dependent rules (the NEW window, scheduled publication, expiry) are exercised by
 * writing rows with backdated timestamps straight through the repository. The service refuses to
 * create an already-expired article, which is correct behaviour and would otherwise make the
 * expiry rule untestable.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewsArticleSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NewsArticleRepository newsRepository;

    @Autowired
    private PlatformConfigRepository configRepository;

    @Autowired
    private tz.elmkusoma.shared.repository.UserRepository userRepository;

    @Autowired
    private tz.elmkusoma.shared.repository.InstitutionMembershipRepository membershipRepository;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private String platformToken;
    private String institutionAdminToken;
    private UUID tenantId;

    @BeforeAll
    void fixtures() {
        tenantId = TestDataSeeder.INSTITUTION_ID;
        platformToken = TestTokens.userToken(createUser("news-platform-admin", tz.elmkusoma.shared.domain.User.Role.ADMIN));
        // An institution admin, to prove the Platform Admin fence rejects a merely privileged
        // account rather than trusting the client's own idea of who it is.
        institutionAdminToken = TestTokens.userToken(
                createUser("news-org-admin", tz.elmkusoma.shared.domain.User.Role.INSTITUTION_ADMIN));
    }

    private String createUser(String prefix, tz.elmkusoma.shared.domain.User.Role role) {
        String email = prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        var user = userRepository.save(tz.elmkusoma.shared.domain.User.builder()
                .email(email)
                .passwordHash("test-hash")
                .firstName("News")
                .lastName("Admin")
                .role(role)
                .institutionId(tenantId)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .build());
        membershipRepository.save(tz.elmkusoma.shared.domain.InstitutionMembership.builder()
                .userId(user.getId())
                .institutionId(tenantId)
                .role(tz.elmkusoma.shared.domain.InstitutionMembership.Role.ADMIN)
                .isActive(true)
                .build());
        return email;
    }

    @BeforeEach
    void reset() {
        // Hard delete between tests: soft-deleted rows keep their slug, which would make the
        // duplicate-slug assertions depend on execution order.
        newsRepository.deleteAll();
        setConfig("news.newWindowDays", "7");
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────────────────

    @Test
    void platformAdmin_canCreateDraft() throws Exception {
        mockMvc.perform(post("/v1/platform-admin/news")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("Term one begins in January", "Summary of the term notice.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.slug").value("term-one-begins-in-january"))
                .andExpect(jsonPath("$.data.priority").value("NORMAL"))
                .andExpect(jsonPath("$.data.featured").value(false));
    }

    @Test
    void draft_isNeverVisibleOnPublicListOrBySlug() throws Exception {
        String id = createDraft("Confidential draft about staffing");
        publish(id);

        String draftId = createDraft("Secret internal draft headline");
        String slug = adminGet(draftId).slug();

        String listBody = mockMvc.perform(get("/v1/public/news"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(listBody.contains("Secret internal draft headline"),
                "a draft must never appear in public listings");

        mockMvc.perform(get("/v1/public/news/" + slug))
                .andExpect(status().isNotFound());
    }

    @Test
    void publishing_makesTheArticlePublic() throws Exception {
        String id = createDraft("Library opens on Saturdays");
        publish(id);
        String slug = adminGet(id).slug();

        mockMvc.perform(get("/v1/public/news/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Library opens on Saturdays"))
                .andExpect(jsonPath("$.data.body").exists())
                .andExpect(jsonPath("$.data.publishedAt").exists());
    }

    @Test
    void unpublishing_removesItFromThePublicSite() throws Exception {
        String id = createDraft("Temporary closure notice");
        String slug = adminGet(id).slug();
        publish(id);

        mockMvc.perform(post("/v1/platform-admin/news/" + id + "/unpublish")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(get("/v1/public/news/" + slug)).andExpect(status().isNotFound());
    }

    @Test
    void archiving_removesItFromThePublicSite() throws Exception {
        String id = createDraft("Old policy that is superseded");
        String slug = adminGet(id).slug();
        publish(id);

        mockMvc.perform(post("/v1/platform-admin/news/" + id + "/archive")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        mockMvc.perform(get("/v1/public/news/" + slug)).andExpect(status().isNotFound());
    }

    @Test
    void archivedArticle_cannotBePublishedDirectly() throws Exception {
        String id = createDraft("Archived then republished by mistake");
        publish(id);
        archive(id);

        mockMvc.perform(post("/v1/platform-admin/news/" + id + "/publish")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_removesItFromEverywhere() throws Exception {
        String id = createDraft("Article that will be deleted");
        String slug = adminGet(id).slug();
        publish(id);

        mockMvc.perform(delete("/v1/platform-admin/news/" + id)
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/public/news/" + slug)).andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/platform-admin/news/" + id)
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isNotFound());
    }

    // ── NEW window ─────────────────────────────────────────────────────────────────────────

    @Test
    void newBadge_isSuppressedWhenWindowIsZero() throws Exception {
        String id = createDraft("Something published moments ago");
        publish(id);

        setConfig("news.newWindowDays", "0");
        String slug = adminGet(id).slug();

        mockMvc.perform(get("/v1/public/news/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.new").value(false));
    }

    @Test
    void newBadge_appliesWithinTheConfiguredWindow() throws Exception {
        String id = createDraft("Inside the window");
        publish(id);
        String slug = adminGet(id).slug();

        mockMvc.perform(get("/v1/public/news/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.new").value(true));
    }

    @Test
    void newBadge_expiresOnceOutsideTheWindow() throws Exception {
        String slug = seedPublished("stale-article", "Published long ago", 30);

        mockMvc.perform(get("/v1/public/news/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.new").value(false));
    }

    @Test
    void aLongerWindowBringsBackTheBadge() throws Exception {
        String slug = seedPublished("thirty-day-old", "Published thirty days ago", 30);

        setConfig("news.newWindowDays", "60");
        mockMvc.perform(get("/v1/public/news/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.new").value(true));
    }

    @Test
    void malformedWindowFallsBackInsteadOfBreakingTheListing() throws Exception {
        String id = createDraft("Still listable with a broken config value");
        publish(id);
        String slug = adminGet(id).slug();

        setConfig("news.newWindowDays", "not-a-number");

        mockMvc.perform(get("/v1/public/news/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.new").value(true));
    }

    @Test
    void publishedAt_isServerStampedAndCannotBeClientSupplied() throws Exception {
        // A fabricated publication date is the whole way "NEW" could be faked, so the request DTO
        // must not accept one at all.
        String body = articleJson("Anti spoofing", "Summary text")
                .replace("}", ",\"publishedAt\":\"2001-01-01T00:00:00\"}");

        mockMvc.perform(post("/v1/platform-admin/news")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        String list = mockMvc.perform(get("/v1/public/news"))
                .andReturn().getResponse().getContentAsString();
        assertFalse(list.contains("2001"), "a client-supplied publication date must be ignored");
    }

    // ── featured and priority ──────────────────────────────────────────────────────────────

    @Test
    void featuredArticlesSortAheadOfUnfeaturedRegardlessOfAge() throws Exception {
        seedPublished("old-but-featured", "Old but featured", 200, true, NewsArticle.PRIORITY_NORMAL);
        seedPublished("recent-normal", "Recent normal", 1, false, NewsArticle.PRIORITY_NORMAL);

        List<String> slugs = publicOrder();
        assertEquals("old-but-featured", slugs.get(0),
                "featured is an editorial choice and outranks recency");
    }

    @Test
    void urgentSortsAheadOfNormalAmongUnfeatured() throws Exception {
        seedPublished("normal-one", "Normal one", 5, false, NewsArticle.PRIORITY_NORMAL);
        seedPublished("urgent-one", "Urgent one", 3, false, NewsArticle.PRIORITY_URGENT);

        List<String> slugs = publicOrder();
        assertEquals("urgent-one", slugs.get(0), "priority must rank above recency");
    }

    @Test
    void newArticleIsNotAutomaticallyFeatured() throws Exception {
        String id = createDraft("Just published, nothing special");
        publish(id);

        mockMvc.perform(get("/v1/platform-admin/news/" + id)
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(jsonPath("$.data.featured").value(false));
    }

    @Test
    void featuredFlagCanBeSetAndCleared() throws Exception {
        String id = createDraft("Toggle me");
        publish(id);

        mockMvc.perform(post("/v1/platform-admin/news/" + id + "/featured")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"featured\":true}"))
                .andExpect(jsonPath("$.data.featured").value(true));

        mockMvc.perform(get("/v1/public/news/" + adminGet(id).slug()))
                .andExpect(jsonPath("$.data.featured").value(true));

        mockMvc.perform(post("/v1/platform-admin/news/" + id + "/featured")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"featured\":false}"))
                .andExpect(jsonPath("$.data.featured").value(false));

        mockMvc.perform(get("/v1/public/news/" + adminGet(id).slug()))
                .andExpect(jsonPath("$.data.featured").value(false));
    }

    @Test
    void priorityAndFeaturedAreIndependent() throws Exception {
        String id = createDraft("Urgent but not featured");
        publish(id);
        setPriority(id, NewsArticle.PRIORITY_URGENT);

        mockMvc.perform(get("/v1/platform-admin/news/" + id)
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(jsonPath("$.data.priority").value("URGENT"))
                .andExpect(jsonPath("$.data.featured").value(false));
    }

    // ── scheduling and expiry ─────────────────────────────────────────────────────────────

    @Test
    void scheduledArticle_isHiddenUntilDue() throws Exception {
        String id = createDraft("Future announcement");
        setScheduled(id, LocalDateTime.now().plusDays(2));
        publish(id);
        String slug = adminGet(id).slug();

        mockMvc.perform(get("/v1/public/news/" + slug)).andExpect(status().isNotFound());
    }

    @Test
    void scheduledArticle_appearsOnceItIsDue() throws Exception {
        String id = createDraft("Announcement that just came due");
        publish(id);
        String slug = adminGet(id).slug();

        // Backdate the gate rather than sleeping.
        NewsArticle article = newsRepository.findById(UUID.fromString(id)).orElseThrow();
        article.setScheduledAt(LocalDateTime.now().minusMinutes(1));
        newsRepository.save(article);

        mockMvc.perform(get("/v1/public/news/" + slug)).andExpect(status().isOk());
    }

    @Test
    void expiredArticle_leavesPublicListings() throws Exception {
        NewsArticle article = newsRepository.save(NewsArticle.builder()
                .title("Expired notice")
                .summary("Was relevant last term")
                .body("Body of the expired notice.")
                .slug("expired-notice")
                .status(NewsArticle.STATUS_PUBLISHED)
                .publishedAt(LocalDateTime.now().minusDays(40))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .priority(NewsArticle.PRIORITY_NORMAL)
                .isFeatured(false)
                .sortOrder(0)
                .build());

        mockMvc.perform(get("/v1/public/news/" + article.getSlug())).andExpect(status().isNotFound());

        String body = mockMvc.perform(get("/v1/public/news"))
                .andReturn().getResponse().getContentAsString();
        assertFalse(body.contains("expired-notice"), "an expired article must not be listed");
    }

    @Test
    void expiryMustBeInTheFuture() throws Exception {
        String body = articleJson("Already expired", "Summary")
                .replace("}", ",\"expiresAt\":\"2001-01-01T00:00:00\"}");

        mockMvc.perform(post("/v1/platform-admin/news")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── authorization ──────────────────────────────────────────────────────────────────────

    @Test
    void anonymousCannotReachAnyAdminEndpoint() throws Exception {
        // 403 rather than 401, matching every other /v1/platform-admin/** endpoint in this codebase:
        // the request is refused by method security before any authentication semantics are reached.
        mockMvc.perform(get("/v1/platform-admin/news")).andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/platform-admin/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("Nope", "Nope")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/platform-admin/news/" + UUID.randomUUID() + "/publish"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/v1/platform-admin/news/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void institutionAdminCannotManagePlatformNews() throws Exception {
        mockMvc.perform(get("/v1/platform-admin/news")
                        .header("Authorization", "Bearer " + institutionAdminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/platform-admin/news")
                        .header("Authorization", "Bearer " + institutionAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("Nope", "Nope")))
                .andExpect(status().isForbidden());
    }

    @Test
    void editingAnArticleYouCannotReachIsRefusedNotSilentlyIgnored() throws Exception {
        String id = createDraft("Owned by platform");

        mockMvc.perform(put("/v1/platform-admin/news/" + id)
                        .header("Authorization", "Bearer " + institutionAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("Hijacked", "Hijacked summary")))
                .andExpect(status().isForbidden());

        // And nothing changed.
        mockMvc.perform(get("/v1/platform-admin/news/" + id)
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(jsonPath("$.data.title").value("Owned by platform"));
    }

    // ── public payload hygiene ────────────────────────────────────────────────────────────

    @Test
    void publicPayloadExposesNoWorkingCopyMetadata() throws Exception {
        String id = createDraft("Hygiene check");
        publish(id);

        String body = mockMvc.perform(get("/v1/public/news"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        for (String field : List.of("scheduledAt", "expiresAt", "sortOrder",
                "lastModifiedBy", "publishedBy", "isDeleted", "createdBy", "institutionId")) {
            assertFalse(body.contains(field),
                    "the public news payload must not expose " + field);
        }
        // 'status' is checked separately: the word appears in no other field name here.
        assertFalse(body.contains("\"status\""), "the public news payload must not expose status");
    }

    @Test
    void publicArticleBySlug_hasNoIdOrAdminFields() throws Exception {
        String id = createDraft("Clean public payload");
        publish(id);
        String slug = adminGet(id).slug();

        mockMvc.perform(get("/v1/public/news/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value(slug))
                .andExpect(jsonPath("$.data.sortOrder").doesNotExist())
                .andExpect(jsonPath("$.data.lastModifiedBy").doesNotExist());
    }

    @Test
    void unknownSlugIs404Not500() throws Exception {
        mockMvc.perform(get("/v1/public/news/there-is-no-such-article"))
                .andExpect(status().isNotFound());
    }

    // ── content safety ─────────────────────────────────────────────────────────────────────

    @Test
    void scriptTagsAreStrippedFromTheStoredBody() throws Exception {
        // Sanitisation, not rejection: the article is saved, and what is stored must contain no
        // executable markup. Asserting on the stored body is what makes this meaningful - a test
        // that only checked the HTTP status would pass even if the script tag had been persisted.
        String body = "{\"title\":\"Formatted\",\"summary\":\"S\","
                + "\"body\":\"<p>Safe text</p><script>alert(1)</script>\"}";

        String created = mockMvc.perform(post("/v1/platform-admin/news")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String stored = JsonPath.read(created, "$.data.body");
        assertFalse(stored.toLowerCase(java.util.Locale.ROOT).contains("<script"),
                "a script tag must never survive into the stored body: " + stored);
        assertTrue(stored.contains("Safe text"),
                "sanitisation must keep the surrounding text, not discard the whole body");
    }

    @Test
    void allowedFormattingIsPreserved() throws Exception {
        String body = "{\"title\":\"Formatted\",\"summary\":\"S\",\"body\":"
                + "\"<p>Read <strong>this</strong> and <a href=\\\"https://example.org/a\\\">this link</a>.</p>\"}";

        String created = mockMvc.perform(post("/v1/platform-admin/news")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String stored = JsonPath.read(created, "$.data.body");
        assertTrue(stored.contains("<strong>"), "bold must survive: " + stored);
        assertTrue(stored.contains("href=\"https://example.org/a\""), "safe links must survive: " + stored);
    }

    @Test
    void xssCorpusIsNeutralised() throws Exception {
        // Each payload is something that executes if the allowlist leaks. Asserted on the stored
        // value, because a 201 alone would say nothing about what was actually persisted.
        String[] payloads = {
                "<img src=x onerror=alert(1)>",
                "<a href=\"javascript:alert(1)\">click</a>",
                "<a href=\"JaVaScRiPt:alert(1)\">click</a>",
                "<a href=\"java\nscript:alert(1)\">click</a>",
                "<iframe src=\"https://evil.example\"></iframe>",
                "<style>body{display:none}</style>",
                "<object data=\"x\"></object>",
                "<svg onload=alert(1)></svg>",
                "<div style=\"background:url(javascript:alert(1))\">x</div>",
                "<form action=\"https://evil.example\"><input name=a></form>",
                "<a href=\"https://ok.example\" target=\"_blank\" onclick=\"alert(1)\">x</a>",
                "<p onmouseover=\"alert(1)\">x</p>",
                "<body onload=alert(1)>",
                "<math><mtext><script>alert(1)</script></mtext></math>",
        };

        for (int index = 0; index < payloads.length; index++) {
            String payload = payloads[index];
            // Serialised through Jackson rather than concatenated: one of the payloads contains a
            // literal newline, and hand-built JSON with an unescaped control character is invalid,
            // which would have tested the JSON parser rather than the sanitiser.
            //
            // Each title is unique because the slug is derived from it, and fourteen identical
            // titles would exhaust the collision suffixes and fail for the wrong reason.
            String requestJson = objectMapper.writeValueAsString(java.util.Map.of(
                    "title", "Corpus case " + index,
                    "summary", "S",
                    "body", payload));

            String created = mockMvc.perform(post("/v1/platform-admin/news")
                            .header("Authorization", "Bearer " + platformToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            String stored = JsonPath.<String>read(created, "$.data.body")
                    .toLowerCase(java.util.Locale.ROOT);

            assertFalse(stored.contains("onerror"), "event handler survived for: " + payload + " -> " + stored);
            assertFalse(stored.contains("onload"), "event handler survived for: " + payload + " -> " + stored);
            assertFalse(stored.contains("onclick"), "event handler survived for: " + payload + " -> " + stored);
            assertFalse(stored.contains("onmouseover"), "event handler survived for: " + payload + " -> " + stored);
            assertFalse(stored.contains("javascript:"), "javascript: survived for: " + payload + " -> " + stored);
            assertFalse(stored.contains("<script"), "script tag survived for: " + payload + " -> " + stored);
            assertFalse(stored.contains("<iframe"), "iframe survived for: " + payload + " -> " + stored);
            assertFalse(stored.contains("<object"), "object survived for: " + payload + " -> " + stored);
            assertFalse(stored.contains("<svg"), "svg survived for: " + payload + " -> " + stored);
            assertFalse(stored.contains("<style"), "style tag survived for: " + payload + " -> " + stored);
            assertFalse(stored.contains("<form"), "form survived for: " + payload + " -> " + stored);
        }
    }

    @Test
    void ordinaryProseContainingAngleBracketsIsAccepted() throws Exception {
        // The plain-text rule must not be so blunt that "grades below 5" becomes unpublishable.
        mockMvc.perform(post("/v1/platform-admin/news")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Reading age 5 < 7\",\"summary\":\"S\","
                                + "\"body\":\"Candidates sit an exam if their age is < 7.\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void javascriptCoverImageUrlIsRejected() throws Exception {
        String body = "{\"title\":\"T\",\"summary\":\"S\",\"body\":\"B\","
                + "\"coverImageUrl\":\"javascript:alert(1)\"}";

        mockMvc.perform(post("/v1/platform-admin/news")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidSlugIsRejectedRatherThanSilentlyRewritten() throws Exception {
        mockMvc.perform(post("/v1/platform-admin/news")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"summary\":\"S\",\"body\":\"B\","
                                + "\"slug\":\"Not A Slug\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateSlugIsRejectedWithConflict() throws Exception {
        mockMvc.perform(post("/v1/platform-admin/news")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"First\",\"summary\":\"S\",\"body\":\"B\","
                                + "\"slug\":\"taken-slug\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/platform-admin/news")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Second\",\"summary\":\"S\",\"body\":\"B\","
                                + "\"slug\":\"taken-slug\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void autoSlugCollisionsAreResolvedRatherThanFailing() throws Exception {
        createDraft("Same headline");
        String secondId = createDraft("Same headline");
        String secondSlug = adminGet(secondId).slug();

        assertFalse(secondSlug.equals("same-headline"), "the second article must not take the same slug");
        assertEquals("same-headline-2", secondSlug);
    }

    @Test
    void editingKeepsTheExistingSlug() throws Exception {
        String id = createDraft("Original headline here");
        String originalSlug = adminGet(id).slug();

        mockMvc.perform(put("/v1/platform-admin/news/" + id)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("Completely different headline", "New summary text")))
                .andExpect(status().isOk());

        assertEquals(originalSlug, adminGet(id).slug(),
                "an edit must not silently break a URL that has already been shared");
    }

    @Test
    void emptyTitleIsRejected() throws Exception {
        mockMvc.perform(post("/v1/platform-admin/news")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"summary\":\"S\",\"body\":\"B\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── admin search and pagination ────────────────────────────────────────────────────────

    @Test
    void adminCanSearchAndFilter() throws Exception {
        createDraft("Mathematics revision classes");
        String otherId = createDraft("Sports day fixtures");
        archive(otherId);

        mockMvc.perform(get("/v1/platform-admin/news?q=sports")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/v1/platform-admin/news?status=ARCHIVED")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/v1/platform-admin/news")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void publicListingPaginates() throws Exception {
        for (int i = 0; i < 5; i++) {
            seedPublished("page-article-" + i, "Article number " + i, i);
        }

        mockMvc.perform(get("/v1/public/news?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.first").value(true));

        mockMvc.perform(get("/v1/public/news?page=2&size=2"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void emptyNewsReturnsAnEmptyPageRatherThanAnError() throws Exception {
        mockMvc.perform(get("/v1/public/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/v1/public/news/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void listingOrderIsStableAcrossRepeatedRequests() throws Exception {
        // Same publication instant for all three, so only the deterministic tiebreak can order them.
        LocalDateTime same = LocalDateTime.now().minusHours(5);
        seedPublished("tie-a", "Tie A", same, false, NewsArticle.PRIORITY_NORMAL);
        seedPublished("tie-b", "Tie B", same, false, NewsArticle.PRIORITY_NORMAL);
        seedPublished("tie-c", "Tie C", same, false, NewsArticle.PRIORITY_NORMAL);

        List<String> first = publicOrder();
        for (int attempt = 0; attempt < 3; attempt++) {
            assertEquals(first, publicOrder(),
                    "identical articles must not reshuffle between requests");
        }
        assertEquals(3, first.size());
    }

    // ── cover image upload ──────────────────────────────────────────────────────────────────

    @Test
    void platformAdminCanUploadACoverImage() throws Exception {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

        String body = mockMvc.perform(multipart("/v1/platform-admin/news/cover")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "cover.png",
                                "image/png", png))
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/v1/content/news-covers/")))
                .andReturn().getResponse().getContentAsString();

        // The returned URL must actually serve, or the admin has been handed a broken image.
        String url = JsonPath.read(body, "$.data.url");
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void svgIsRefusedBecauseItCanCarryScript() throws Exception {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);

        mockMvc.perform(multipart("/v1/platform-admin/news/cover")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "cover.svg", "image/svg+xml", svg))
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aFileWhoseBytesAreNotAnImageIsRefused() throws Exception {
        // Named .png, declared image/png, but actually HTML. The extension and content-type checks
        // would both pass; only looking at the bytes catches this.
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        mockMvc.perform(multipart("/v1/platform-admin/news/cover")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "cover.png", "image/png", html))
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonImageContentTypeIsRefused() throws Exception {
        mockMvc.perform(multipart("/v1/platform-admin/news/cover")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "cover.png", "text/html", "<html></html>".getBytes()))
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyPlatformAdminMayUploadACover() throws Exception {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        mockMvc.perform(multipart("/v1/platform-admin/news/cover")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "cover.png", "image/png", png)))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/v1/platform-admin/news/cover")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "cover.png", "image/png", png))
                        .header("Authorization", "Bearer " + institutionAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void coverServingRejectsPathTraversal() throws Exception {
        // Asserted as 400/404 specifically rather than is4xxClientError(): a generic 4xx assertion
        // also passes on 405, which is what an unmapped path returns - so the original version of
        // this test would have passed while the endpoint did not exist at all.
        mockMvc.perform(get("/v1/content/news-covers/..%2F..%2F..%2Fapplication.properties"))
                .andExpect(result -> assertTrue(
                        result.getResponse().getStatus() == 400 || result.getResponse().getStatus() == 404,
                        "traversal must be refused, got " + result.getResponse().getStatus()));
        mockMvc.perform(get("/v1/content/news-covers/not-a-uuid.png"))
                .andExpect(status().isBadRequest());
    }

    // ── helpers ────────────────────────────────────────────────────────────────────────────

    private String createDraft(String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/platform-admin/news")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson(title, "Summary for " + title)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
    }

    private void publish(String id) throws Exception {
        mockMvc.perform(post("/v1/platform-admin/news/" + id + "/publish")
                .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk());
    }

    private void archive(String id) throws Exception {
        mockMvc.perform(post("/v1/platform-admin/news/" + id + "/archive")
                .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk());
    }

    private void setPriority(String id, String priority) throws Exception {
        NewsArticle article = newsRepository.findById(UUID.fromString(id)).orElseThrow();
        article.setPriority(priority);
        newsRepository.save(article);
    }

    private void setScheduled(String id, LocalDateTime when) throws Exception {
        NewsArticle article = newsRepository.findById(UUID.fromString(id)).orElseThrow();
        article.setScheduledAt(when);
        newsRepository.save(article);
    }

    /** Writes a published row with a controlled publication date, for time-window assertions. */
    private String seedPublished(String slug, String title, int daysAgo) {
        return seedPublished(slug, title, LocalDateTime.now().minusDays(daysAgo),
                false, NewsArticle.PRIORITY_NORMAL);
    }

    private String seedPublished(String slug, String title, int daysAgo, boolean featured, String priority) {
        return seedPublished(slug, title, LocalDateTime.now().minusDays(daysAgo), featured, priority);
    }

    private String seedPublished(String slug, String title, LocalDateTime publishedAt,
                                 boolean featured, String priority) {
        newsRepository.save(NewsArticle.builder()
                .title(title)
                .summary("Summary for " + title)
                .body("Body of " + title)
                .slug(slug)
                .status(NewsArticle.STATUS_PUBLISHED)
                .publishedAt(publishedAt)
                .isFeatured(featured)
                .priority(priority)
                .sortOrder(0)
                .build());
        return slug;
    }

    private record AdminView(String id, String slug, String status) {}

    private AdminView adminGet(String id) throws Exception {
        String body = mockMvc.perform(get("/v1/platform-admin/news/" + id)
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new AdminView(
                JsonPath.read(body, "$.data.id"),
                JsonPath.read(body, "$.data.slug"),
                JsonPath.read(body, "$.data.status"));
    }

    private List<String> publicOrder() throws Exception {
        String body = mockMvc.perform(get("/v1/public/news?size=50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.content[*].slug");
    }

    private void setConfig(String key, String value) {
        PlatformConfigEntry entry = configRepository.findByConfigKeyAndIsDeletedFalse(key)
                .orElseGet(() -> PlatformConfigEntry.builder().configKey(key).build());
        entry.setConfigValue(value);
        configRepository.save(entry);
    }

    private static String articleJson(String title, String summary) {
        return "{\"title\":\"" + title + "\",\"summary\":\"" + summary + "\","
                + "\"body\":\"Body text for this article.\"}";
    }
}