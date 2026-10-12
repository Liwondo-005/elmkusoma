package tz.elmkusoma.certificate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.certificate.domain.Certificate;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Shared issuance primitives: verification links, verification codes, and the audit record for an
 * automatic issuance.
 *
 * <p>Certificate issuance happens in four places - the manual service, live-class expiry, event
 * attendance, and course completion. Before this existed the three automatic paths each built their
 * own verification URL, and none of them wrote an audit row at all, so certificates appeared with
 * no record of what produced them. One component keeps the four paths from drifting.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateIssuanceSupport {

    /**
     * Actor recorded when issuance was performed by the platform rather than by a person.
     *
     * <p>An explicit system identity, not a fabricated user. A real "actor" that did not act, or a
     * null, would both make the audit trail misleading.
     */
    public static final String SYSTEM_ACTOR_EMAIL = "system@elmkusoma.internal";
    public static final String SYSTEM_ACTOR_ROLE = "SYSTEM";

    private final AuditService auditService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String publicFrontendUrl;

    /**
     * Canonical public verification URL.
     *
     * <p>Falls back to the relative route when no base URL is configured, so a missing setting
     * degrades rather than aborting an otherwise valid issuance.</p>
     */
    public String verificationUrl(String verificationCode) {
        String path = "/certificates/verify/" + verificationCode;
        if (publicFrontendUrl == null || publicFrontendUrl.isBlank()) {
            return path;
        }
        return publicFrontendUrl.replaceAll("/+$", "") + path;
    }

    /**
     * Cryptographically unpredictable verification code.
     *
     * <p>Public verification is keyed on this alone, so it must not be guessable or sequential.
     * Uniqueness is additionally enforced by the database.</p>
     */
    public String newVerificationCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16)
                .toUpperCase(Locale.ROOT);
    }

    /**
     * Records an automatic issuance in the audit trail.
     *
     * <p>Automatic issuance is legitimate and policy-permitted, but "the platform decided this"
     * is exactly the fact an auditor needs recorded. {@code actorUserId} is the real user when a
     * person's action triggered the sweep (a teacher ending a session), and null when it was a
     * scheduler. The system identity is recorded either way, and never invented.</p>
     *
     * @param source stable identifier for the code path, e.g. {@code live-class-expiry}
     * @param detail short human-readable note; must not contain personal data
     */
    public void recordAutomaticIssuance(Certificate certificate, UUID actorUserId,
                                        String source, String detail) {
        try {
            auditService.recordAuditLog(
                    certificate.getInstitutionId(),
                    actorUserId,
                    SYSTEM_ACTOR_EMAIL,
                    SYSTEM_ACTOR_ROLE,
                    "Certificate",
                    certificate.getId(),
                    certificate.getSerialNumber(),
                    AuditLog.AuditAction.CREATE,
                    null,
                    Map.of(
                            "status", certificate.getStatus() == null ? "ISSUED" : certificate.getStatus().name(),
                            "issuanceMode", "AUTOMATIC",
                            "source", source,
                            "detail", detail == null ? "" : detail));
        } catch (Exception e) {
            // The certificate is already saved; failing the caller's transaction here would roll
            // back an expiry sweep or an attendance record that is independently correct. The
            // failure is logged loudly rather than silently swallowed.
            log.error("Failed to audit automatic certificate issuance {} ({}): {}",
                    certificate.getSerialNumber(), source, e.getMessage());
        }
    }
}
