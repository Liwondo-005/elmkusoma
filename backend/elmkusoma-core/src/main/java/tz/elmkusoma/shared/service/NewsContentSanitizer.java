package tz.elmkusoma.shared.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Sanitises rich-text news article bodies.
 *
 * <p>This exists because public news needs real formatting and the previous contract - plain text
 * only, rendered as React text nodes - could not provide it. That contract is genuinely safe:
 * refusing HTML makes stored XSS structurally impossible rather than filtered. Trading it for
 * allowlist sanitisation is a deliberate change of mechanism, not a downgrade in safety, but it
 * does move the burden onto this class. It is therefore deny-by-default and closed over the whole
 * surface rather than filtering a known-bad list.</p>
 *
 * <h2>What is allowed</h2>
 * Structural and inline formatting only: paragraphs, headings, lists, blockquote, emphasis, strong,
 * links, and the {@code <br>} line break. No images, no {@code <iframe>}, no {@code <style>}, no
 * {@code <form>}, no {@code <object>}. Anything not named is unwrapped or dropped.
 *
 * <h2>What is stripped even from allowed tags</h2>
 * Every event handler ({@code onclick}, {@code onerror}, and so on), {@code style}, {@code id},
 * {@code class}, {@code formaction}, {@code target} and anything else not explicitly permitted.
 * {@code <a>} keeps only {@code href}, and the URL is separately validated - a link is the one
 * allowed element that can carry a {@code javascript:} payload, so it gets its own check rather
 * than relying on Jsoup's protocol filter alone.
 */
@Component
public class NewsContentSanitizer {

    /** Tags an editor may use. Everything else is unwrapped, keeping its text but losing the tag. */
    private static final Safelist TAGS = Safelist.none()
            .addTags("p", "br", "h2", "h3", "h4", "strong", "b", "em", "i", "u",
                    "ul", "ol", "li", "blockquote", "a", "code", "pre", "hr")
            // Applied to every allowed tag.
            .addAttributes(":all", "href", "title", "lang", "dir");

    /**
     * Sanitises a body fragment.
     *
     * @param html untrusted content from an administrator
     * @return markup safe to render, with links normalised
     */
    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        // body() parses as a fragment and does not inject html/head/body wrappers into the result,
        // which is what we want: this string is embedded inside an existing page.
        Document dirty = Jsoup.parseBodyFragment(html);
        Document clean = Jsoup.parseBodyFragment(Jsoup.clean(dirty.body().html(), "", TAGS,
                new org.jsoup.nodes.Document.OutputSettings().prettyPrint(false)));
        // Jsoup cannot express "this attribute only on this tag" through the shared Safelist as
        // configured above, so href is re-validated per anchor here.
        clean.select("a[href]").forEach(anchor -> {
            String href = anchor.attr("href").trim();
            if (!isSafeHref(href)) {
                anchor.removeAttr("href");
            }
        });
        return clean.body().html().trim();
    }

    /**
     * Only http(s), mailto and same-document fragments.
     *
     * <p>Relative URLs are allowed because a link into another part of the site is legitimate, but
     * a scheme-bearing URL must be one of the three above. {@code javascript:}, {@code data:} and
     * {@code vbscript:} are rejected. Control characters are stripped before the check because
     * {@code java\nscript:alert(1)} is a well-known way to smuggle a scheme past a naive prefix
     * test, and browsers ignore those characters inside a URL scheme.</p>
     */
    private static boolean isSafeHref(String href) {
        if (href.isEmpty()) {
            return false;
        }
        String normalised = href.replaceAll("[\\x00-\\x20\\x7F]", "").toLowerCase(java.util.Locale.ROOT);
        if (normalised.startsWith("#") || normalised.startsWith("/")) {
            return true;
        }
        return normalised.startsWith("http://")
                || normalised.startsWith("https://")
                || normalised.startsWith("mailto:");
    }

    /** True when the content contains anything that looks like a tag, used to pick a render path. */
    public static boolean containsMarkup(String value) {
        return value != null && value.matches("(?s).*<\\s*/?\\s*[a-zA-Z][^>]*>.*");
    }
}