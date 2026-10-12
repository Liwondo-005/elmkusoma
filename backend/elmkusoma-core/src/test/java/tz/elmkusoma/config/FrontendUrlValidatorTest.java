package tz.elmkusoma.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validation rules for the public frontend base URL that account-recovery links are built
 * from. The point is that a bad value is caught at startup instead of producing an emailed
 * link the user cannot open.
 */
class FrontendUrlValidatorTest {

    private static final String LOCALHOST = "http://localhost:3000";

    // ---------------------------------------------------------------- accepted

    @Test
    void localDefault_isAcceptedOutsideProduction() {
        assertNull(FrontendUrlValidator.problem(LOCALHOST, false),
                "development keeps the documented localhost default");
    }

    @Test
    void publicHttpsOrigin_isAccepted() {
        assertNull(FrontendUrlValidator.problem("https://elmkusoma.co.tz", true));
        assertNull(FrontendUrlValidator.problem("http://school.example.org:3000", false));
    }

    // ------------------------------------------------------------- production

    @Test
    void production_rejectsLocalhost() {
        String problem = FrontendUrlValidator.problem(LOCALHOST, true);
        assertNotNull(problem, "production must never silently emit localhost recovery links");
        assertTrue(problem.contains("prod"), "the message should name the profile: " + problem);
    }

    @Test
    void production_rejectsLoopbackIpAndLocalAliases() {
        assertNotNull(FrontendUrlValidator.problem("http://127.0.0.1:3000", true));
        assertNotNull(FrontendUrlValidator.problem("http://0.0.0.0:3000", true));
        assertNotNull(FrontendUrlValidator.problem("http://[::1]:3000", true));
        assertNotNull(FrontendUrlValidator.problem("http://app.localhost", true));
    }

    @Test
    void localhostRemainsAcceptableOutsideProduction() {
        assertNull(FrontendUrlValidator.problem("http://127.0.0.1:3000", false));
    }

    // --------------------------------------------------------------- malformed

    @Test
    void blankValue_isRejected() {
        assertNotNull(FrontendUrlValidator.problem(null, false));
        assertNotNull(FrontendUrlValidator.problem("", false));
        assertNotNull(FrontendUrlValidator.problem("   ", false));
    }

    @Test
    void missingScheme_isRejected() {
        assertNotNull(FrontendUrlValidator.problem("elmkusoma.co.tz", false));
        assertNotNull(FrontendUrlValidator.problem("www.elmkusoma.co.tz", false));
    }

    @Test
    void nonHttpScheme_isRejected() {
        assertNotNull(FrontendUrlValidator.problem("ftp://elmkusoma.co.tz", false));
        assertNotNull(FrontendUrlValidator.problem("javascript:alert(1)", false));
    }

    /**
     * Links are built as {@code base + "/reset-password?token=…"}, so a trailing slash would
     * yield {@code https://host//reset-password}.
     */
    @Test
    void trailingSlash_isRejectedBecauseItWouldDoubleTheSeparator() {
        String problem = FrontendUrlValidator.problem("https://elmkusoma.co.tz/", false);
        assertNotNull(problem);
        assertTrue(problem.contains("trailing"), problem);
    }

    @Test
    void queryOrFragment_isRejected() {
        assertNotNull(FrontendUrlValidator.problem("https://elmkusoma.co.tz?x=1", false));
        assertNotNull(FrontendUrlValidator.problem("https://elmkusoma.co.tz#frag", false));
    }

    @Test
    void hostlessValue_isRejected() {
        assertNotNull(FrontendUrlValidator.problem("http://", false));
    }

    // ----------------------------------------------------------------- output

    /** Diagnostics may echo the URL, but must stay actionable and never grow a stack of noise. */
    @Test
    void everyRejectionExplainsWhatToDo() {
        String[] bad = {null, "", "elmkusoma.co.tz", "https://elmkusoma.co.tz/", "http://"};
        for (String value : bad) {
            String problem = FrontendUrlValidator.problem(value, false);
            assertNotNull(problem, "expected a rejection for: " + value);
            assertTrue(problem.contains("FRONTEND_URL") || problem.contains("app.frontend-url"),
                    "diagnostic should say which setting to fix: " + problem);
        }
    }
}
