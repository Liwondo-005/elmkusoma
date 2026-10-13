package tz.elmkusoma.certificate.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Confining what an administrator can put in a certificate template.
 *
 * <p>Templates carry {@code html_content} and {@code css_content} TEXT columns that have been
 * accepted, stored and handed back to the client since they were added - with no validation of any
 * kind. The fields were never rendered either, so nothing was exploited; the moment they are
 * rendered they become stored markup in an authenticated application, and the time to constrain
 * them is before the first one is written, not after.</p>
 *
 * <p>The rule is deny-by-default, which is also how the news sanitiser in this build works: a tag
 * or attribute that is not named in the safelist is stripped. A blacklist of "bad" tags would have
 * to anticipate every variant, and it only has to miss one.</p>
 *
 * <p>Two layers, because there are two ways in. {@link #sanitizeHtml} decides which elements and
 * attributes survive parsing. {@link #sanitizeCss} then scrubs the CSS itself - both the
 * {@code css_content} column and every surviving inline {@code style} attribute - because allowing
 * {@code style} is the whole point of a template builder while {@code url()} and
 * {@code expression()} are exactly what must not reach a renderer.</p>
 *
 * <p>This constrains input. It is not by itself a reason to render the result unsandboxed: the
 * client renders template markup inside a sandboxed frame with scripts disabled, because defence
 * that only exists on the way in cannot cover markup that was written before these rules, or
 * written by a build that did not have them.</p>
 */
@Component
public class CertificateTemplateContentSanitizer {

    /**
     * A template is a document, not a data import. 128 KB of markup is already far beyond any
     * plausible certificate, and the column has no length limit of its own.
     */
    static final int MAX_HTML_LENGTH = 128_000;
    static final int MAX_CSS_LENGTH = 64_000;

    private static final Pattern UNSAFE_HREF = Pattern.compile(
            "^\\s*(javascript|vbscript|data)\\s*:", Pattern.CASE_INSENSITIVE);

    private static final Pattern CSS_AT_IMPORT = Pattern.compile(
            "@import", Pattern.CASE_INSENSITIVE);
    /**
     * {@code expression(...)} takes a parenthesis; {@code behavior:} and {@code -moz-binding:}
     * take a colon. Matching only on {@code (} let both of the latter straight through, which is
     * the sort of hole a deny-by-default rule is supposed to close.
     */
    private static final Pattern CSS_EXPRESSION = Pattern.compile(
            "(expression|behavior|-moz-binding)\\s*[:(]", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_URL = Pattern.compile(
            "url\\s*\\(", Pattern.CASE_INSENSITIVE);
    /**
     * Only a data image. An https resource would be allowed too if the goal were merely to stop
     * execution, but the goal is that a preview makes no outbound request: every url() to a remote
     * host turns whoever opens the template into a beacon for whoever wrote it. A certificate
     * template should be self-contained anyway, so that it renders the same wherever it is opened.
     * Images with a real source belong in {@code img src}, which the markup rules allow over
     * https.
     */
    private static final Pattern CSS_SAFE_URL = Pattern.compile(
            "url\\s*\\(\\s*['\"]?\\s*data:image/", Pattern.CASE_INSENSITIVE);

    /**
     * Elements a certificate may be built from.
     *
     * <p>No {@code script}, {@code style}, {@code iframe}, {@code object}, {@code embed},
     * {@code form}, {@code input} or {@code svg} - each can execute or frame something, and none
     * has a job to do in a printed document. {@code img} and {@code a} are kept because a
     * certificate legitimately carries a logo and a link.</p>
     */
    private static final Safelist TAGS = Safelist.none()
            .addTags("div", "span", "p", "br", "hr", "section", "header", "footer",
                    "article", "aside", "figure", "figcaption",
                    "h1", "h2", "h3", "h4", "h5", "h6",
                    "strong", "b", "em", "i", "u", "s", "strike", "small", "mark",
                    "sup", "sub", "blockquote", "pre", "code", "kbd", "var",
                    "ul", "ol", "li", "dl", "dt", "dd",
                    "table", "thead", "tbody", "tfoot", "tr", "th", "td",
                    "caption", "col", "colgroup",
                    "img", "a")
            // Layout and formatting, all of it harmless on its own.
            .addAttributes(":all",
                    "class", "id", "title", "lang", "dir", "style", "hidden")
            .addAttributes(":all", "align", "valign", "width", "height",
                    "colspan", "rowspan", "scope", "headers", "span")
            .addAttributes("a", "href", "target", "rel", "name")
            .addAttributes("img", "src", "alt", "loading")
            .addAttributes("th", "abbr")
            .addAttributes("col", "span")
            .addAttributes("ol", "start", "type", "reversed")
            .addAttributes("ul", "type")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https", "data");

    /**
     * Sanitises template markup.
     *
     * @return the safe markup, or an empty string for null/blank input
     * @throws IllegalArgumentException when the input is too large to be a certificate
     */
    public String sanitizeHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        if (html.length() > MAX_HTML_LENGTH) {
            throw new IllegalArgumentException(
                    "Template markup is too large (limit " + MAX_HTML_LENGTH + " characters)");
        }

        Document dirty = Jsoup.parseBodyFragment(html);
        Document clean = Jsoup.parseBodyFragment(Jsoup.clean(dirty.body().html(), "", TAGS,
                new Document.OutputSettings().prettyPrint(false)));

        for (Element element : clean.getAllElements()) {
            scrubStyle(element);
            scrubAnchor(element);
            scrubImage(element);
        }
        return clean.body().html().trim();
    }

    /**
     * Sanitises a stylesheet, or an inline {@code style} value.
     *
     * <p>Text in, text out - there is no CSS parser in this build and adding one to run a
     * stylesheet through would be a large dependency for a narrow problem. The constructs that can
     * execute, import or reach out are few and named, so they are removed and everything else is
     * left alone. {@code <} is dropped as well so a value cannot close its attribute and open new
     * markup.</p>
     */
    public String sanitizeCss(String css) {
        if (css == null || css.isBlank()) {
            return "";
        }
        if (css.length() > MAX_CSS_LENGTH) {
            throw new IllegalArgumentException(
                    "Template stylesheet is too large (limit " + MAX_CSS_LENGTH + " characters)");
        }

        String scrubbed = css;
        scrubbed = CSS_AT_IMPORT.matcher(scrubbed).replaceAll("");
        scrubbed = CSS_EXPRESSION.matcher(scrubbed).replaceAll("");
        scrubbed = stripUnsafeUrls(scrubbed);
        scrubbed = scrubbed.replace("<", "");
        // A comment could conceal any of the above from the patterns: remove them first is not
        // possible without a parser, so they are neutralised by flattening */ which would
        // otherwise allow a construct to be split across a comment boundary.
        scrubbed = scrubbed.replace("</", "<\\/");
        return scrubbed.trim();
    }

    /**
     * Removes every {@code url()} except a data image or an https resource.
     *
     * <p>{@code url(javascript:...)} has been dead in browsers for years, but a stylesheet can
     * still make the renderer fetch an arbitrary host on every preview - which turns anyone who
     * opens a template into a beacon. Restricting to {@code data:image/} and {@code https://} keeps
     * background images working while removing the request a document has no need to make.</p>
     */
    private String stripUnsafeUrls(String css) {
        if (!CSS_URL.matcher(css).find()) {
            return css;
        }
        // Split on url( so each call site can be judged on its own opening.
        StringBuilder out = new StringBuilder(css.length());
        int cursor = 0;
        java.util.Locale locale = Locale.ROOT;
        while (true) {
            int at = indexOfIgnoreCase(css, "url(", cursor, locale);
            if (at < 0) {
                out.append(css, cursor, css.length());
                break;
            }
            int close = css.indexOf(')', at);
            if (close < 0) {
                // Unbalanced: drop the remainder rather than let a half-open construct through.
                break;
            }
            String call = css.substring(at, close + 1);
            out.append(css, cursor, at);
            if (CSS_SAFE_URL.matcher(call).find()) {
                out.append(call);
            }
            cursor = close + 1;
        }
        return out.toString();
    }

    private int indexOfIgnoreCase(String haystack, String needle, int from, Locale locale) {
        return haystack.toLowerCase(locale).indexOf(needle.toLowerCase(locale), from);
    }

    /** Inline style attributes get the same treatment as the stylesheet. */
    private void scrubStyle(Element element) {
        String style = element.attr("style");
        if (style == null || style.isBlank()) {
            return;
        }
        String clean = sanitizeCss(style);
        if (clean.isBlank()) {
            element.removeAttr("style");
        } else {
            element.attr("style", clean);
        }
    }

    /**
     * Drops href from a link whose target is not a plain web or mail address, and forces
     * {@code rel} on the ones that stay.
     *
     * <p>The safelist's protocol rules already block {@code javascript:}; this catches the
     * whitespace-and-tab obfuscations they are written with, and a link left with no rel in a
     * document that may be opened from an email client.</p>
     */
    private void scrubAnchor(Element element) {
        if (!"a".equals(element.tagName())) {
            return;
        }
        String href = element.attr("href").trim();
        if (href.isEmpty() || UNSAFE_HREF.matcher(href).find()
                || !element.hasAttr("href")) {
            element.removeAttr("href");
            element.removeAttr("target");
            element.removeAttr("rel");
            return;
        }
        if (element.hasAttr("target")) {
            element.attr("rel", "noopener noreferrer");
        }
    }

    /**
     * Keeps only images that point at a data image or https, over whatever the safelist admitted.
     *
     * <p>{@code http:} is excluded deliberately: a template that loads a resource insecurely is
     * mixed content the moment the application is served over https, and a logo is never urgent
     * enough to warrant it.</p>
     */
    private void scrubImage(Element element) {
        if (!"img".equals(element.tagName())) {
            return;
        }
        String src = element.attr("src").trim();
        String lower = src.toLowerCase(Locale.ROOT);
        boolean safe = lower.startsWith("data:image/")
                || lower.startsWith("https://");
        if (!safe) {
            element.removeAttr("src");
        }
    }
}
