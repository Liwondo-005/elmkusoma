package tz.elmkusoma.certificate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Template markup has been accepted, stored and returned unvalidated since the columns were added.
 *
 * <p>Nothing rendered it, so nothing was exploited. These rules are what makes rendering it later
 * safe, and they are written down here because the interesting cases are the ones that look
 * harmless: an event handler, a style that reaches out, a link that is not a link.</p>
 */
class CertificateTemplateContentSanitizerTest {

    private final CertificateTemplateContentSanitizer sanitizer =
            new CertificateTemplateContentSanitizer();

    // ---- Markup ----

    @Test
    @DisplayName("a script tag is removed, not merely emptied")
    void scriptTagsAreRemoved() {
        String clean = sanitizer.sanitizeHtml(
                "<p>good</p><script>alert(document.cookie)</script><p>also good</p>");

        assertFalse(clean.contains("script"), clean);
        assertFalse(clean.contains("alert"), clean);
        assertTrue(clean.contains("good"), "the legitimate content must survive");
    }

    @Test
    @DisplayName("event handler attributes are stripped even on allowed tags")
    void eventHandlersAreStripped() {
        String clean = sanitizer.sanitizeHtml(
                "<p onclick=\"steal()\" onmouseover=\"steal()\">hello</p>");

        assertFalse(clean.contains("onclick"), clean);
        assertFalse(clean.contains("onmouseover"), clean);
        assertTrue(clean.contains("hello"));
        assertTrue(clean.contains("<p"), "the element itself is allowed");
    }

    @Test
    @DisplayName("elements that can execute or frame are dropped entirely")
    void executableElementsAreDropped() {
        String clean = sanitizer.sanitizeHtml(
                "<iframe src=\"https://evil.example\"></iframe>"
                        + "<object data=\"x\"></object>"
                        + "<embed src=\"x\">"
                        + "<form action=\"x\"><input name=\"a\"></form>"
                        + "<svg onload=\"steal()\"><use href=\"x\"/></svg>");

        assertFalse(clean.contains("iframe"), clean);
        assertFalse(clean.contains("object"), clean);
        assertFalse(clean.contains("embed"), clean);
        assertFalse(clean.contains("form"), clean);
        assertFalse(clean.contains("input"), clean);
        assertFalse(clean.contains("svg"), clean);
        assertFalse(clean.contains("steal"), clean);
    }

    @Test
    @DisplayName("a javascript: link loses its href but keeps its text")
    void javascriptLinksAreNeutralised() {
        String clean = sanitizer.sanitizeHtml("<a href=\"javascript:steal()\">click me</a>");

        assertFalse(clean.toLowerCase().contains("javascript"), clean);
        assertTrue(clean.contains("click me"), "the label is content and must remain");
    }

    @Test
    @DisplayName("a link obfuscated with tabs and newlines is caught too")
    void obfuscatedLinksAreNeutralised() {
        String clean = sanitizer.sanitizeHtml(
                "<a href=\"java\tscript:alert(1)\">x</a>");

        assertFalse(clean.toLowerCase().contains("script:"), clean);
    }

    @Test
    @DisplayName("an image pointing at an executable or insecure source loses its src")
    void unsafeImagesAreNeutralised() {
        String clean = sanitizer.sanitizeHtml("<img src=\"javascript:alert(1)\" alt=\"logo\">");

        assertFalse(clean.toLowerCase().contains("javascript"), clean);
        assertTrue(clean.contains("alt=\"logo\""), "the alternative text is harmless");
    }

    @Test
    @DisplayName("the formatting a certificate actually needs is preserved")
    void legitimateFormattingSurvives() {
        String clean = sanitizer.sanitizeHtml(
                "<div class=\"award\"><h2>Title</h2><p><strong>Bold</strong> and "
                        + "<span style=\"color:#c02020\">red</span></p>"
                        + "<ul><li>one</li><li>two</li></ul></div>");

        assertTrue(clean.contains("<h2>Title</h2>"), clean);
        assertTrue(clean.contains("<strong>Bold</strong>"), clean);
        assertTrue(clean.contains("color:#c02020"), "colour is the point of a template builder");
        assertTrue(clean.contains("<li>one</li>"), clean);
        assertTrue(clean.contains("class=\"award\""), clean);
    }

    @Test
    @DisplayName("markup that does not parse is returned as text rather than thrown away")
    void malformedMarkupDoesNotThrow() {
        String clean = sanitizer.sanitizeHtml("<p>unclosed <b>bold <i>nested");

        assertFalse(clean.contains("<script"));
        assertTrue(clean.length() > 0);
    }

    @Test
    @DisplayName("an empty template stays empty rather than becoming a stray tag")
    void emptyInputStaysEmpty() {
        assertEquals("", sanitizer.sanitizeHtml(null));
        assertEquals("", sanitizer.sanitizeHtml("   "));
    }

    @Test
    @DisplayName("markup beyond the size of any certificate is refused outright")
    void oversizedMarkupIsRefused() {
        String oversized = "<p>" + "a".repeat(CertificateTemplateContentSanitizer.MAX_HTML_LENGTH) + "</p>";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sanitizer.sanitizeHtml(oversized));
        assertTrue(ex.getMessage().contains("too large"));
    }

    // ---- Stylesheet ----

    @Test
    @DisplayName("@import cannot make the renderer fetch another stylesheet")
    void importIsRemoved() {
        String clean = sanitizer.sanitizeCss(
                "@import url(\"https://evil.example/x.css\"); body { color: red }");

        assertFalse(clean.toLowerCase().contains("@import"), clean);
        assertTrue(clean.contains("color: red"), "ordinary declarations must survive");
    }

    @Test
    @DisplayName("expression() and its relatives are removed")
    void expressionIsRemoved() {
        assertFalse(sanitizer.sanitizeCss("width: expression(alert(1))")
                .toLowerCase().contains("expression"));
        assertFalse(sanitizer.sanitizeCss("behavior: url(x.htc)").toLowerCase().contains("behavior"));
        assertFalse(sanitizer.sanitizeCss("-moz-binding: url(x)").toLowerCase().contains("binding"));
    }

    @Test
    @DisplayName("a url() to an arbitrary host is removed - a preview must not beacon out")
    void remoteUrlsAreRemoved() {
        String clean = sanitizer.sanitizeCss(
                "body { background: url(https://evil.example/pixel.gif) } h1 { color: blue }");

        assertFalse(clean.contains("evil.example"), clean);
        assertTrue(clean.contains("color: blue"), clean);
    }

    @Test
    @DisplayName("a data image stays, so a self-contained background still works")
    void dataImagesAreKept() {
        assertTrue(sanitizer.sanitizeCss(
                        "background: url(data:image/png;base64,AAAA)").contains("data:image/png"));
        assertTrue(sanitizer.sanitizeCss(
                        "background: url(\"data:image/png;base64,AAAA\")").contains("data:image/png"));
    }

    @Test
    @DisplayName("a remote image in an img tag is allowed - that is where a logo belongs")
    void remoteImagesInMarkupAreKept() {
        String clean = sanitizer.sanitizeHtml(
                "<img src=\"https://cdn.example/logo.png\" alt=\"logo\">");

        assertTrue(clean.contains("src=\"https://cdn.example/logo.png\""), clean);
        assertTrue(clean.contains("alt=\"logo\""), clean);
    }

    @Test
    @DisplayName("the same remote image inside a stylesheet is not, because it beacons")
    void remoteImagesInStylesheetsAreRemoved() {
        String clean = sanitizer.sanitizeCss(
                "background: url(\"https://cdn.example/logo.png\")");

        assertFalse(clean.contains("cdn.example"), clean);
    }

    @Test
    @DisplayName("a style value cannot break out of its attribute")
    void styleCannotEscapeItsAttribute() {
        String clean = sanitizer.sanitizeHtml("<p style=\"color:red\\\" onmouseover=\\\"steal()\">x</p>");

        assertFalse(clean.contains("onmouseover"), clean);
    }

    @Test
    @DisplayName("an inline style is scrubbed by the same rules as the stylesheet")
    void inlineStylesAreScrubbed() {
        String clean = sanitizer.sanitizeHtml(
                "<p style=\"color:red; background:url(https://evil.example/x); "
                        + "width:expression(alert(1))\">x</p>");

        assertFalse(clean.contains("evil.example"), clean);
        assertFalse(clean.toLowerCase().contains("expression"), clean);
        assertTrue(clean.contains("color:red"), "the useful part of the declaration stays");
    }

    @Test
    @DisplayName("a stylesheet beyond any plausible certificate is refused")
    void oversizedStylesheetIsRefused() {
        String oversized = "x".repeat(CertificateTemplateContentSanitizer.MAX_CSS_LENGTH + 1);

        assertThrows(IllegalArgumentException.class, () -> sanitizer.sanitizeCss(oversized));
    }

    // ---- Ways of not looking like a url() ----

    @Test
    @DisplayName("a space between url and the bracket does not make the rule miss")
    void urlsSeparatedByWhitespaceAreStillScanned() {
        // Detection allowed this and the scan did not, so the whole declaration came through
        // untouched: one space was enough to reach out from a preview.
        String clean = sanitizer.sanitizeCss(
                "body { background: url (https://evil.example/track.gif) } h1 { color: blue }");

        assertFalse(clean.contains("evil.example"), clean);
        assertTrue(clean.contains("color: blue"), clean);
    }

    @Test
    @DisplayName("a keyword split by a comment is still caught once the comment is gone")
    void commentsCannotSplitAKeywordPastTheRules() {
        String clean = sanitizer.sanitizeCss(
                "body { background: u/*x*/rl(https://evil.example/track.gif) }");

        assertFalse(clean.contains("evil.example"), clean);
        assertFalse(clean.contains("url("), clean);
    }

    @Test
    @DisplayName("comments are removed before anything is judged, and the rules still apply")
    void commentsAreStrippedAndDeclarationsSurvive() {
        String clean = sanitizer.sanitizeCss("/* header */ h1 { color: blue } /* tail */");

        assertFalse(clean.contains("/*"), clean);
        assertTrue(clean.contains("color: blue"), clean);
    }

    @Test
    @DisplayName("an unterminated comment drops the rest rather than leaving it unexamined")
    void unterminatedCommentsDropTheTail() {
        String clean = sanitizer.sanitizeCss(
                "h1 { color: blue } /* never closes, and hides url(https://evil.example/x)");

        assertFalse(clean.contains("evil.example"), clean);
        assertTrue(clean.contains("color: blue"), clean);
    }

    @Test
    @DisplayName("a benign data image still survives the comment pass")
    void dataImagesSurviveCommentStripping() {
        assertTrue(sanitizer.sanitizeCss(
                        "/* bg */ background: url(data:image/png;base64,AAAA)").contains("data:image/png"));
    }
}
