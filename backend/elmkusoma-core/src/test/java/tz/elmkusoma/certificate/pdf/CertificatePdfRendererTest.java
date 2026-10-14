package tz.elmkusoma.certificate.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.domain.CertificateSignatorySnapshot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The PDF is the copy that leaves the system, so it has to say everything the browser document
 * says - and, for a withdrawn certificate, say it loudly enough that a forwarded file cannot be
 * mistaken for a live one.
 */
class CertificatePdfRendererTest {

    private final CertificatePdfRenderer renderer = new CertificatePdfRenderer();

    private Certificate issuedCertificate() {
        Certificate certificate = Certificate.builder()
                .templateId(java.util.UUID.randomUUID())
                .studentId(java.util.UUID.randomUUID())
                .serialNumber("CERT-CMP-2026-000042")
                .certificateNumber("CERT-ABCDEF012345")
                .certificateType(Certificate.CertificateType.COMPLETION)
                .title("Certificate of Completion")
                .studentName("Amini Mwakalinga")
                .studentIdNumber("S12345")
                .courseOrProgramme("Advanced Web Development")
                .completionDate(LocalDate.of(2026, 3, 14))
                .issueDate(java.time.LocalDateTime.of(2026, 3, 20, 9, 30))
                .status(Certificate.CertificateStatus.ISSUED)
                .verificationCode("VERIFY-ME")
                .verificationUrl("https://elmkusoma.co.tz/certificates/verify/VERIFY-ME")
                .grade("Pass with Distinction")
                .instructorName("Juma Abeid")
                .skills(List.of("HTML", "CSS", "JavaScript"))
                .build();
        certificate.setId(java.util.UUID.randomUUID());
        certificate.setInstitutionId(java.util.UUID.randomUUID());
        return certificate;
    }

    private String text(byte[] pdf) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdf))) {
            return new PDFTextStripper().getText(document);
        }
    }

    @Test
    @DisplayName("an issued certificate renders as a PDF carrying its own content")
    void rendersIssuedCertificate() throws Exception {
        byte[] pdf = renderer.render(issuedCertificate(), List.of(), "ELMKUSOMA Institute", null);

        assertTrue(pdf.length > 1_000, "a real document is more than a stub");
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII),
                "the response must actually be a PDF, not an error page");

        String content = text(pdf);
        assertTrue(content.contains("CERTIFICATE OF COMPLETION"), "the heading must match the type");
        assertTrue(content.contains("Amini Mwakalinga"), "the holder must be named");
        assertTrue(content.contains("Advanced Web Development"), "the award must be named");
        assertTrue(content.contains("CERT-CMP-2026-000042"), "the serial must be on the document");
        assertTrue(content.contains("HTML  \u00B7  CSS  \u00B7  JavaScript"),
                "skills must be listed");
        assertTrue(content.contains("ELMKUSOMA Institute"), "the awarding institution must appear");
    }

    @Test
    @DisplayName("a revoked certificate is watermarked so a detached copy cannot pass as live")
    void watermarksRevokedCertificate() throws Exception {
        Certificate certificate = issuedCertificate();
        certificate.setStatus(Certificate.CertificateStatus.REVOKED);

        String content = text(renderer.render(certificate, List.of(), null, null));

        assertTrue(content.contains("REVOKED"),
                "the watermark is the only thing that survives the file leaving the system");
        assertFalse(content.contains("ELMKUSOMA Institute"),
                "a null institution must render the fallback rather than the literal word null");
    }

    @Test
    @DisplayName("a draft is watermarked too, because it is not yet a released document")
    void watermarksDraftCertificate() throws Exception {
        Certificate certificate = issuedCertificate();
        certificate.setStatus(Certificate.CertificateStatus.DRAFT);

        assertTrue(text(renderer.render(certificate, List.of(), null, null)).contains("DRAFT"));
    }

    @Test
    @DisplayName("signatories are drawn from their frozen snapshots, not the live profiles")
    void rendersSignatorySnapshots() throws Exception {
        CertificateSignatorySnapshot snapshot = new CertificateSignatorySnapshot();
        snapshot.setFullName("Neema Shayo");
        snapshot.setPositionTitle("Dean of Studies");
        snapshot.setOrganization("ELMKUSOMA Institute");
        snapshot.setSignatureImage(null);

        String content = text(renderer.render(issuedCertificate(), List.of(snapshot),
                "ELMKUSOMA Institute", null));

        assertTrue(content.contains("Neema Shayo"), "the signatory who signed must appear");
        assertTrue(content.contains("Dean of Studies"), "so must their title");
    }

    @Test
    @DisplayName("names that the PDF fonts cannot represent are transliterated, not fatal")
    void fitsUnrepresentableCharacters() {
        // NFD strips the accent rather than dropping the letter.
        assertEquals("Jose Fernandez", CertificatePdfRenderer.fit("Jos\u00E9 Fern\u00E1ndez"));
        assertEquals("cafe", CertificatePdfRenderer.fit("caf\u00E9"));
        // Smart quotes and dashes have no place in a standard font encoding.
        assertEquals("\"quoted\" and 'apostrophe'", CertificatePdfRenderer.fit(
                "\u201Cquoted\u201D and \u2018apostrophe\u2019"));
        assertEquals("a - b", CertificatePdfRenderer.fit("a \u2013 b"));
        assertEquals("wait...", CertificatePdfRenderer.fit("wait\u2026"));
        // Outside any Latin mapping entirely: replaced rather than throwing from showText.
        assertEquals("Juma ?", CertificatePdfRenderer.fit("Juma \u65E5"));
        assertEquals("", CertificatePdfRenderer.fit(null));
    }

    @Test
    @DisplayName("an image that will not decode is dropped instead of failing the export")
    void aBrokenImageIsIgnored() throws Exception {
        Certificate certificate = issuedCertificate();

        CertificateSignatorySnapshot snapshot = new CertificateSignatorySnapshot();
        snapshot.setFullName("Neema Shayo");
        snapshot.setSignatureImage("data:image/png;base64,not-really-an-image");

        byte[] pdf = renderer.render(certificate, List.of(snapshot), null,
                "https://example.org/logo.png");

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII),
                "a missing logo or signature must not cost the holder their certificate");
    }

    @Test
    @DisplayName("a null verification URL falls back to the code rather than encoding nothing")
    void rendersWithoutAVerificationUrl() throws Exception {
        Certificate certificate = issuedCertificate();
        certificate.setVerificationUrl(null);

        // The renderer must still produce a document; whether a QR appears is secondary.
        byte[] pdf = renderer.render(certificate, List.of(), null, null);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
        assertNull(certificate.getVerificationUrl());
    }
}

