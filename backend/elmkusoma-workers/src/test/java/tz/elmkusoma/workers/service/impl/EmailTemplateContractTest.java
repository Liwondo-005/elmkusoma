package tz.elmkusoma.workers.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Contract between the templates workers owns and the template names core publishes.
 *
 * <p>This exists because of a real outage that no test caught: core published
 * {@code contact-enquiry} and {@code contact-reply}, but workers shipped only three
 * other templates. Rendering then threw inside the listener, and because the queue has
 * no dead-letter destination and {@code defaultRequeueRejected} defaults to true, the
 * message was requeued forever on a single-consumer listener and starved every other
 * email. A missing template is therefore a queue-wide incident, not a cosmetic gap.</p>
 *
 * <p>No SMTP session is opened: this renders templates in-process only.</p>
 */
class EmailTemplateContractTest {

    private static TemplateEngine engine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/email/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        // Spring's engine, matching production: it evaluates th:text with SpEL. A bare TemplateEngine
        // defaults to the OGNL compiler, which is not what actually runs here.
        org.thymeleaf.spring6.SpringTemplateEngine engine = new org.thymeleaf.spring6.SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    // ------------------------------------------------------- rendering

    @Test
    @DisplayName("contact-enquiry renders for the support/admin recipient")
    void contactEnquiryRendersWithProducerVariables() {
        // Exactly the keys ContactMessageService puts into its variables map.
        Map<String, Object> vars = new HashMap<>();
        vars.put("reference", "ENQ-2026-0001");
        vars.put("name", "Grace M.");
        vars.put("email", "grace@example.test");
        vars.put("category", "Admissions");
        vars.put("subject", "Fee schedule for term one");
        vars.put("message", "Could you share the current fee schedule?");

        String html = render("contact-enquiry", vars);

        assertTrue(html.contains("ENQ-2026-0001"));
        assertTrue(html.contains("Grace M."));
        assertTrue(html.contains("grace@example.test"));
        assertTrue(html.contains("Admissions"));
        assertTrue(html.contains("Fee schedule for term one"));
        assertTrue(html.contains("Could you share the current fee schedule?"));
    }

    @Test
    @DisplayName("contact-reply renders for the person who submitted the enquiry")
    void contactReplyRendersWithProducerVariables() {
        // Exactly the keys ContactMessageService.publishReply puts into its map.
        Map<String, Object> vars = new HashMap<>();
        vars.put("reference", "ENQ-2026-0001");
        vars.put("name", "Grace M.");
        vars.put("message", "Here is the fee schedule you asked for.");

        String html = render("contact-reply", vars);

        assertTrue(html.contains("ENQ-2026-0001"));
        assertTrue(html.contains("Grace M."));
        assertTrue(html.contains("Here is the fee schedule you asked for."));
    }

    @Test
    @DisplayName("Visitor-supplied text is escaped, never rendered as markup")
    void untrustedContentIsEscaped() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("reference", "ENQ-1");
        vars.put("name", "<script>alert('xss')</script>");
        vars.put("email", "a@b.test");
        vars.put("category", "General");
        vars.put("subject", "<img src=x onerror=alert(1)>");
        vars.put("message", "<b>bold</b>");

        String html = render("contact-enquiry", vars);

        assertFalse(html.contains("<script>alert('xss')</script>"),
                "a submission must never be able to inject script into an admin's inbox");
        assertFalse(html.contains("<img src=x onerror=alert(1)>"));
        assertTrue(html.contains("&lt;script&gt;"), "the value should appear escaped instead");
    }

    @Test
    @DisplayName("A template that does not exist fails loudly instead of rendering empty")
    void unknownTemplateFailsRatherThanSilentlyRendering() {
        boolean threw = false;
        try {
            render("no-such-template", new HashMap<>());
        } catch (RuntimeException expected) {
            threw = true;
        }
        assertTrue(threw,
                "silently rendering nothing would report undelivered mail as sent");
    }
    /**
     * Thymeleaf 3 has no Context(Map) constructor, so variables are set individually -
     * the same way EmailServiceImpl does it in production.
     */
    private static String render(String template, Map<String, Object> vars) {
        org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
        context.setVariables(vars);
        return engine().process(template, context);
    }

    // ------------------------------------------------------- inventory
    /**
     * Every template name core publishes must exist in the workers classpath.
     *
     * <p>Producer references are discovered by scanning core's source for
     * {@code publishEmailEvent(recipient, subject, "template", ...)} and reading the
     * third argument.</p>
     *
     * <p><b>Known limitation:</b> this is static scanning, so a template name built at
     * runtime (a variable or concatenation) would not be discovered. No such producer
     * exists today - all call sites pass literals - but a future dynamic name would
     * need either a literal or an addition to the list asserted here.</p>
     */
    @Test
    @DisplayName("Every template core publishes exists in the workers classpath")
    void everyPublishedTemplateExists() throws IOException {
        Path backend = locateBackendRoot();
        assumeTrue(backend != null, "elmkusoma-core sources not visible from the workers module");

        Set<String> published = publishedTemplateNames(backend.resolve("elmkusoma-core"));
        assertFalse(published.isEmpty(), "scanning core found no publishEmailEvent template names");

        Set<String> available = availableTemplateNames();

        Set<String> missing = new LinkedHashSet<>(published);
        missing.removeAll(available);
        assertEquals(Set.of(), missing,
                "these templates are published by core but absent from workers, which would "
                        + "poison the single-consumer email queue: " + missing);
    }

    private static Set<String> publishedTemplateNames(Path coreModule) throws IOException {
        Path sources = coreModule.resolve("src/main/java");
        if (!Files.isDirectory(sources)) {
            return Set.of();
        }
        // A template name is the only lowercase-dash literal in the argument list, so
        // this tolerates a subject built by concatenation such as
        //   "New enquiry " + ref + ": " + subject(), "contact-enquiry", ...
        // which a fixed-position regular expression would miss.
        Pattern call = Pattern.compile("publishEmailEvent\\s*\\(");
        Pattern literal = Pattern.compile("\"([^\"]*)\"");

        Set<String> names = new LinkedHashSet<>();
        try (Stream<Path> files = Files.walk(sources)) {
            List<Path> javaFiles = files.filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
            for (Path file : javaFiles) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                Matcher m = call.matcher(source);
                while (m.find()) {
                    int from = m.end();
                    int depth = 1;
                    int i = from;
                    while (i < source.length() && depth > 0 && i - from < 2000) {
                        char c = source.charAt(i);
                        if (c == '(') depth++;
                        else if (c == ')') depth--;
                        i++;
                    }
                    String args = source.substring(from, Math.min(i, source.length()));
                    Matcher lit = literal.matcher(args);
                    while (lit.find()) {
                        String candidate = lit.group(1);
                        if (candidate.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
                            names.add(candidate);
                        }
                    }
                }
            }
        }
        return names;
    }

    private static Set<String> availableTemplateNames() throws IOException {
        Path dir = Paths.get("src/main/resources/templates/email");
        assumeTrue(Files.isDirectory(dir), "templates directory not visible");
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".html"))
                    .map(n -> n.substring(0, n.length() - ".html".length()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    private static Path locateBackendRoot() {
        Path current = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && current != null; i++) {
            Path candidate = current.resolve("elmkusoma-core");
            if (Files.isDirectory(candidate)) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }
}