package tz.elmkusoma.certificate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.qr.QrDecodeSupport;
import tz.elmkusoma.certificate.repository.CertificateRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * What is written to the database is what a holder will scan.
 *
 * <p>The unit tests cover the renderer and the lifecycle callback separately, but neither one
 * proves the whole path: the callback firing, the column being wide enough to hold the result,
 * zxing being on the runtime classpath, and the value surviving a round trip through Postgres. A
 * regression in any of those produces certificates that look fine in every other test and cannot
 * be scanned.</p>
 *
 * <p>The column type is asserted directly because the failure it prevents is otherwise
 * confusing: the test profile creates its schema from the entity annotation, so dropping
 * {@code columnDefinition = "text"} silently reverts the column to Hibernate's default of
 * varchar(255), and every issuance then fails with a length overflow on a value nothing else in
 * the system treats as variable-length.</p>
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CertificateQrPersistenceTest {

    @Autowired private CertificateRepository certificateRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("the stored QR survives the round trip through Postgres and still scans")
    void theStoredQrSurvivesTheRoundTrip() {
        String code = "QRDB" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                .toUpperCase();
        String verificationUrl = "https://learn.elmkusoma.tz/certificates/verify/" + code;

        Certificate certificate = Certificate.builder()
                .templateId(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .issuedBy(UUID.randomUUID())
                .serialNumber("CERT-QR-DB-" + UUID.randomUUID().toString().substring(0, 8))
                .certificateType(Certificate.CertificateType.COMPLETION)
                .title("Certificate of Completion")
                .studentName("Amini Mwakalinga")
                .completionDate(LocalDate.of(2026, 3, 1))
                .issueDate(LocalDateTime.now())
                .verificationCode(code)
                .verificationUrl(verificationUrl)
                .build();
        certificate.setInstitutionId(UUID.randomUUID());

        Certificate saved = certificateRepository.save(certificate);
        try {
            Certificate reloaded = certificateRepository.findById(saved.getId()).orElseThrow();

            assertNotNull(reloaded.getQrCodeUrl(),
                    "the lifecycle callback must have stamped a QR at persist");
            assertEquals(verificationUrl, QrDecodeSupport.decodeSvgDataUri(reloaded.getQrCodeUrl()),
                    "the image read back out of the database must still scan to this "
                            + "certificate's own verification URL");
        } finally {
            certificateRepository.delete(saved);
        }
    }

    @Test
    @DisplayName("qr_code_url is wide enough to hold a stored code")
    void qrCodeUrlIsWideEnough() {
        String type = jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns "
                        + "WHERE table_name = 'certificates' AND column_name = 'qr_code_url'",
                String.class);

        assertEquals("text", type,
                "qr_code_url must be text. The test schema is generated from the entity, so this "
                        + "is what catches columnDefinition being dropped and the column silently "
                        + "reverting to varchar(255), which is far too small for any QR.");
    }
}
