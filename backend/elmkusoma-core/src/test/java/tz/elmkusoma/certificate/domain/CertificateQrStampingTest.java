package tz.elmkusoma.certificate.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tz.elmkusoma.certificate.qr.CertificateQrCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every certificate is stamped with its QR at persist, whatever produced it.
 *
 * <p>There are four issuance paths - manual, live-class expiry, event attendance, NFE - and a QR
 * written at three of them would mean certificates that scan and certificates that do not, with
 * nothing in the data to tell them apart. The stamp lives in the lifecycle callback precisely
 * because a callback cannot be skipped, so these tests pin the callback rather than the paths.</p>
 *
 * <p>Same package as {@link Certificate} so the package-private callback is exercised directly:
 * this is the exact code Hibernate runs, not a copy of it.</p>
 */
class CertificateQrStampingTest {

    private static final String VERIFICATION_URL =
            "https://learn.elmkusoma.tz/certificates/verify/9F2A4C7B1D6E8A30";

    private Certificate base() {
        return Certificate.builder()
                .templateId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .issuedBy(UUID.randomUUID())
                .serialNumber("CERT-QR-001")
                .certificateType(Certificate.CertificateType.COMPLETION)
                .title("Certificate of Completion")
                .studentName("Amini Mwakalinga")
                .completionDate(LocalDate.of(2026, 3, 1))
                .issueDate(LocalDateTime.now())
                .verificationCode("9F2A4C7B1D6E8A30")
                .verificationUrl(VERIFICATION_URL)
                .build();
    }

    @Test
    @DisplayName("a certificate carries a QR for its verification link")
    void aCertificateCarriesAQr() {
        Certificate certificate = base();

        certificate.onCreate();

        assertNotNull(certificate.getQrCodeUrl(),
                "issuance must leave a scannable code on the row");
        assertTrue(certificate.getQrCodeUrl().startsWith("data:image/svg+xml;base64,"),
                "the stored value is the image itself, ready to render with no second request");
        assertEquals(CertificateQrCode.svgDataUri(VERIFICATION_URL), certificate.getQrCodeUrl(),
                "the QR must encode this certificate's own verification URL");
    }

    @Test
    @DisplayName("a certificate with no verification link is persisted without a QR")
    void aCertificateWithoutAVerificationLinkIsPersisted() {
        Certificate certificate = base();
        certificate.setVerificationUrl(null);

        certificate.onCreate();

        assertNull(certificate.getQrCodeUrl(),
                "there is nothing to encode, and generating a code for a null would be a lie");
    }

    @Test
    @DisplayName("a QR a caller generated deliberately is not overwritten")
    void aDeliberateQrIsNotOverwritten() {
        Certificate certificate = base();
        String existing = "data:image/svg+xml;base64,PRE-GENERATED";
        certificate.setQrCodeUrl(existing);

        certificate.onCreate();

        assertSame(existing, certificate.getQrCodeUrl(),
                "the callback fills an absence, it does not take a value that was already set");
    }
}
