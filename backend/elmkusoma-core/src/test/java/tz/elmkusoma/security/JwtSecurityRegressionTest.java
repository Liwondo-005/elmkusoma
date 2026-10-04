package tz.elmkusoma.security;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.config.security.JwtTokenProvider;
import tz.elmkusoma.testutil.TestTokens;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JWT acceptance/rejection regression tests through the real filter chain.
 *
 * <p>Contracts pinned here (verified against {@code JwtTokenProvider} and
 * {@code JwtAuthenticationFilter}): strict access validation requires
 * signature + expiry + {@code alg=HS256} + {@code iss=elmkusoma}; the refresh
 * path is lenient about a missing {@code iss} (pre-hardening 7d tokens) but
 * still pins {@code alg} and enforces a present {@code iss}; rotation revokes
 * the presented refresh token so replay yields 403.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class JwtSecurityRegressionTest {

    private static final SecretKey KEY = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode("dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLTIwMjQ="));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private int getWithToken(String token) throws Exception {
        // /v1/courses requires authentication (unlike permitAll /v1/auth/me,
        // which answers 404 for a null principal) — bad tokens must 401/403.
        return mockMvc.perform(get("/v1/courses")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();
    }

    private static void assertDenied(int status, String tokenKind) {
        assertTrue(status == 401 || status == 403,
                tokenKind + " must be denied, got HTTP " + status);
    }

    @Test
    void validToken_accessesProtectedEndpoint_200_control() throws Exception {
        mockMvc.perform(get("/v1/courses")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken()))
                .andExpect(status().isOk());
    }

    @Test
    void expiredToken_isDenied() throws Exception {
        String expired = Jwts.builder()
                .subject(TestTokens.STUDENT_EMAIL)
                .issuer("elmkusoma")
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000L))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000L))
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();
        assertDenied(getWithToken(expired), "expired token");
    }

    @Test
    void tamperedSignature_isDenied() throws Exception {
        String valid = TestTokens.studentToken();
        char last = valid.charAt(valid.length() - 1);
        char replacement = last == 'a' ? 'b' : 'a';
        String tampered = valid.substring(0, valid.length() - 1) + replacement;
        assertDenied(getWithToken(tampered), "tampered token");
    }

    @Test
    void algNoneToken_isDenied() throws Exception {
        String header = base64Url("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = base64Url(("{\"sub\":\"" + TestTokens.STUDENT_EMAIL + "\",\"iss\":\"elmkusoma\","
                + "\"exp\":" + ((System.currentTimeMillis() / 1000) + 3600) + "}")
                .getBytes(StandardCharsets.UTF_8));
        assertDenied(getWithToken(header + "." + payload + "."), "alg=none token");
    }

    @Test
    void foreignIssuerToken_isDeniedOnAccessPaths() throws Exception {
        String foreign = Jwts.builder()
                .subject(TestTokens.STUDENT_EMAIL)
                .issuer("evil-issuer")
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();
        assertDenied(getWithToken(foreign), "foreign-iss token");
    }

    @Test
    void oldShapeAccessTokenWithoutIss_isDeniedOnStrictPaths() throws Exception {
        String oldShape = Jwts.builder()
                .subject(TestTokens.STUDENT_EMAIL)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();
        assertDenied(getWithToken(oldShape), "pre-hardening (no iss) access token");
    }

    @Test
    void preIssShapeRefresh_isAcceptedAndRotated_thenReplayIsRevoked403() throws Exception {
        String legacyRefresh = Jwts.builder()
                .subject(TestTokens.STUDENT_EMAIL)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();

        MvcResult first = mockMvc.perform(post("/v1/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + legacyRefresh + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String firstBody = first.getResponse().getContentAsString();
        String newAccess = JsonPath.read(firstBody, "$.data.accessToken");
        String newRefresh = JsonPath.read(firstBody, "$.data.refreshToken");
        assertNotEquals(legacyRefresh, newRefresh);

        // Rotation transparently upgrades: replacements carry iss and validate strictly.
        assertTrue(jwtTokenProvider.validateToken(newAccess));
        assertTrue(jwtTokenProvider.validateRefreshToken(newRefresh));

        MvcResult replay = mockMvc.perform(post("/v1/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + legacyRefresh + "\"}"))
                .andExpect(status().isForbidden())
                .andReturn();
        assertEquals("Refresh token has been revoked",
                (String) JsonPath.read(replay.getResponse().getContentAsString(), "$.error"));
    }

    @Test
    void foreignIssuerRefresh_isRejected() throws Exception {
        String foreignRefresh = Jwts.builder()
                .subject(TestTokens.STUDENT_EMAIL)
                .issuer("evil-issuer")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();

        MvcResult result = mockMvc.perform(post("/v1/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + foreignRefresh + "\"}"))
                .andExpect(status().isForbidden())
                .andReturn();
        assertEquals("Invalid or expired refresh token",
                (String) JsonPath.read(result.getResponse().getContentAsString(), "$.error"));
    }
}
