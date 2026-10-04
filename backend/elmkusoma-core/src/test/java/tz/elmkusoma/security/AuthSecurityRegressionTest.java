package tz.elmkusoma.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.identity.domain.PasswordResetToken;
import tz.elmkusoma.identity.domain.VerificationCode;
import tz.elmkusoma.identity.repository.PasswordResetTokenRepository;
import tz.elmkusoma.identity.repository.VerificationCodeRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level authentication + OTP security regression tests through the real
 * filter chain (JwtAuthenticationFilter -&gt; ... -&gt; GlobalExceptionHandler).
 *
 * <p>Contracts pinned here (verified against {@code AuthServiceImpl} and
 * {@code GlobalExceptionHandler} before writing):</p>
 * <ul>
 *   <li>valid login -&gt; 200 with access + refresh tokens;</li>
 *   <li>wrong password AND unknown email -&gt; identical 401
 *       {@code "Invalid credentials"} (no account-enumeration oracle;
 *       {@code DaoAuthenticationProvider} hides unknown users by default);</li>
 *   <li>duplicate register -&gt; neutral 400
 *       {@code "Unable to complete registration with the provided details"};</li>
 *   <li>reset-password with unknown vs expired token -&gt; identical 400
 *       {@code "Reset token is invalid or has expired"}. NOTE:
 *       {@code ResetPasswordRequest} validates UUID shape, so the "garbage"
 *       probe must be UUID-shaped; non-UUID input is rejected by bean
 *       validation instead (still 400, different body);</li>
 *   <li>verify-code with wrong vs expired code -&gt; identical 400
 *       {@code "Invalid or expired verification code"};</li>
 *   <li>lockout row (attempts=5) -&gt; 400
 *       {@code "Too many failed attempts. Please request a new code."};</li>
 *   <li>send-code within 60s -&gt; 400 cooldown message;</li>
 *   <li>wrong current password on change-password throws the same
 *       {@code BadCredentialsException} as login, so both render the identical
 *       401 {@code "Invalid credentials"} body;</li>
 *   <li>service responses never echo the 5-digit code (bodies scanned).</li>
 * </ul>
 *
 * <p>All fixtures use random emails: hermetic against other suites on the
 * shared test database. The seeded users' passwords are never changed here
 * (only the wrong-current path is exercised at HTTP level; the success path
 * is covered by {@code AuthServiceChangePasswordTest}).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AuthSecurityRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    private static String freshEmail(String prefix) {
        return (prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com").toLowerCase();
    }

    private static String errorOf(String body) {
        return JsonPath.read(body, "$.error");
    }

    private MvcResult postJson(String url, String json, String bearer) throws Exception {
        var req = post(url).contentType("application/json").content(json);
        if (bearer != null) {
            req.header("Authorization", "Bearer " + bearer);
        }
        return mockMvc.perform(req).andReturn();
    }

    // ── login ──

    @Test
    void validLogin_returns200WithBothTokens() throws Exception {
        MvcResult result = postJson("/v1/auth/login",
                "{\"email\":\"teacher@elmkusoma.tz\",\"password\":\"password\"}", null);
        assertEquals(200, result.getResponse().getStatus());
        String body = result.getResponse().getContentAsString();
        assertTrue((Boolean) JsonPath.read(body, "$.success"));
        assertNotNull(JsonPath.read(body, "$.data.accessToken"));
        assertNotNull(JsonPath.read(body, "$.data.refreshToken"));
    }

    @Test
    void invalidLogin_wrongPasswordAndUnknownEmail_yieldIdentical401() throws Exception {
        MvcResult wrongPassword = postJson("/v1/auth/login",
                "{\"email\":\"teacher@elmkusoma.tz\",\"password\":\"wrong-password-123\"}", null);
        assertEquals(401, wrongPassword.getResponse().getStatus());
        String wrongPasswordBody = wrongPassword.getResponse().getContentAsString();
        assertEquals(false, (Boolean) JsonPath.read(wrongPasswordBody, "$.success"));
        assertEquals("Invalid credentials", errorOf(wrongPasswordBody));

        MvcResult unknownEmail = postJson("/v1/auth/login",
                "{\"email\":\"" + freshEmail("nobody") + "\",\"password\":\"wrong-password-123\"}", null);
        assertEquals(401, unknownEmail.getResponse().getStatus());
        // Same generic body: the response must not reveal whether the email exists.
        assertEquals(errorOf(wrongPasswordBody), errorOf(unknownEmail.getResponse().getContentAsString()));
    }

    // ── register ──

    @Test
    void register_duplicateEmail_returnsNeutral400() throws Exception {
        MvcResult result = postJson("/v1/auth/register",
                "{\"firstName\":\"Test\",\"lastName\":\"Dup\",\"email\":\"teacher@elmkusoma.tz\","
                        + "\"password\":\"Password123\"}", null);
        assertEquals(400, result.getResponse().getStatus());
        String body = result.getResponse().getContentAsString();
        assertEquals("Unable to complete registration with the provided details", errorOf(body));
        // Neutral: must not echo the email or confirm existence.
        assertFalse(body.contains("teacher@elmkusoma.tz"));
        assertFalse(body.toLowerCase().contains("already exists"));
    }

    // ── reset-password: unknown vs expired token ──

    @Test
    void resetPassword_garbageVsExpiredToken_yieldIdentical400() throws Exception {
        String expiredToken = UUID.randomUUID().toString();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .token(expiredToken)
                .userId(TestDataSeeder.STUDENT_USER_ID)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .used(false)
                .build());
        String garbageToken = UUID.randomUUID().toString();

        MvcResult garbage = postJson("/v1/auth/reset-password",
                "{\"token\":\"" + garbageToken + "\",\"newPassword\":\"NewPassword123\"}", null);
        MvcResult expired = postJson("/v1/auth/reset-password",
                "{\"token\":\"" + expiredToken + "\",\"newPassword\":\"NewPassword123\"}", null);

        assertEquals(400, garbage.getResponse().getStatus());
        assertEquals(400, expired.getResponse().getStatus());
        assertEquals(errorOf(garbage.getResponse().getContentAsString()),
                errorOf(expired.getResponse().getContentAsString()));
        assertEquals("Reset token is invalid or has expired",
                errorOf(expired.getResponse().getContentAsString()));
    }

    // ── verify-code: wrong vs expired ──

    @Test
    void verifyCode_wrongVsExpiredCode_yieldIdentical400() throws Exception {
        String email = freshEmail("otp");
        verificationCodeRepository.save(VerificationCode.builder()
                .email(email)
                .code("12345")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .attempts(0)
                .build());

        MvcResult wrong = postJson("/v1/auth/verify-code",
                "{\"email\":\"" + email + "\",\"code\":\"99999\"}", null);
        MvcResult expired = postJson("/v1/auth/verify-code",
                "{\"email\":\"" + email + "\",\"code\":\"12345\"}", null);

        assertEquals(400, wrong.getResponse().getStatus());
        assertEquals(400, expired.getResponse().getStatus());
        assertEquals(errorOf(wrong.getResponse().getContentAsString()),
                errorOf(expired.getResponse().getContentAsString()));
        assertEquals("Invalid or expired verification code",
                errorOf(expired.getResponse().getContentAsString()));

        // Neither failure response may echo a code.
        assertFalse(wrong.getResponse().getContentAsString().contains("99999"));
        assertFalse(expired.getResponse().getContentAsString().contains("12345"));
    }

    @Test
    void verifyCode_sixthAttempt_returnsLockoutMessage() throws Exception {
        String email = freshEmail("otp-lock");
        verificationCodeRepository.save(VerificationCode.builder()
                .email(email)
                .code("54321")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .attempts(5)
                .build());

        MvcResult result = postJson("/v1/auth/verify-code",
                "{\"email\":\"" + email + "\",\"code\":\"54321\"}", null);

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Too many failed attempts. Please request a new code.",
                errorOf(result.getResponse().getContentAsString()));
        assertFalse(result.getResponse().getContentAsString().contains("54321"));
    }

    @Test
    void sendCode_within60s_returnsCooldownMessage_andNeverReturnsCode() throws Exception {
        String email = freshEmail("otp-cool");
        verificationCodeRepository.save(VerificationCode.builder()
                .email(email)
                .code("11111")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .attempts(0)
                .build());

        MvcResult result = postJson("/v1/auth/send-code",
                "{\"email\":\"" + email + "\"}", null);

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("Please wait 60 seconds before requesting a new code",
                errorOf(result.getResponse().getContentAsString()));
        assertFalse(result.getResponse().getContentAsString().contains("11111"));
    }

    @Test
    void sendCode_successResponse_doesNotContainIssuedCode() throws Exception {
        String email = freshEmail("otp-fresh");

        MvcResult result = postJson("/v1/auth/send-code",
                "{\"email\":\"" + email + "\"}", null);

        assertEquals(200, result.getResponse().getStatus());
        String storedCode = verificationCodeRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow()
                .getCode();
        assertEquals(5, storedCode.length());
        assertFalse(result.getResponse().getContentAsString().contains(storedCode),
                "send-code response must never echo the issued verification code");
    }

    // ── change-password ──

    @Test
    void changePassword_wrongCurrentPassword_401IdenticalToBadLogin() throws Exception {
        MvcResult badLogin = postJson("/v1/auth/login",
                "{\"email\":\"student@elmkusoma.tz\",\"password\":\"wrong-password-123\"}", null);
        assertEquals(401, badLogin.getResponse().getStatus());

        MvcResult change = postJson("/v1/auth/change-password",
                "{\"currentPassword\":\"WrongPassword123\",\"newPassword\":\"BrandNewPass123\"}",
                TestTokens.studentToken());
        assertEquals(401, change.getResponse().getStatus());
        assertEquals(errorOf(badLogin.getResponse().getContentAsString()),
                errorOf(change.getResponse().getContentAsString()));
        assertEquals("Invalid credentials",
                errorOf(change.getResponse().getContentAsString()));
    }

    @Test
    void changePassword_unauthenticated_isRejected() throws Exception {
        // SecurityConfig pins POST /v1/auth/change-password to authenticated()
        // ahead of the /v1/auth/** permitAll. The exact denial code (401 vs 403)
        // is Spring's default entry-point behaviour, so any 4xx counts as rejected.
        mockMvc.perform(post("/v1/auth/change-password")
                        .contentType("application/json")
                        .content("{\"currentPassword\":\"x\",\"newPassword\":\"NewPassword123\"}"))
                .andExpect(status().is4xxClientError());
    }
}
