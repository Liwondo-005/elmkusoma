package tz.elmkusoma.certificate.pdf;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.domain.CertificateTemplate;
import tz.elmkusoma.certificate.repository.CertificateRepository;
import tz.elmkusoma.certificate.repository.CertificateSignatorySnapshotRepository;
import tz.elmkusoma.certificate.repository.CertificateTemplateRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.repository.InstitutionRepository;

import java.io.IOException;
import java.util.UUID;

/**
 * Exports an issued certificate as a PDF.
 *
 * <p>The orphaned {@code elmkusoma-workers} module has its own PDF service behind a message queue
 * this application never publishes to. Rather than deploy a second service whose renderer cannot
 * see the approval state, the signatory snapshots, the lifecycle or the QR code, the export lives
 * here and reads the same rows everything else does.</p>
 *
 * <p>Authorisation is not repeated here. The controller runs the same role, tenant and ownership
 * checks as reading the certificate; this class only re-checks the one thing reading does not
 * care about - that the certificate is in a state where a document exists at all.</p>
 */
@Service
@RequiredArgsConstructor
public class CertificatePdfService {

    private final CertificateRepository certificateRepository;
    private final CertificateTemplateRepository templateRepository;
    private final CertificateSignatorySnapshotRepository signatorySnapshotRepository;
    private final InstitutionRepository institutionRepository;
    private final CertificatePdfRenderer renderer;

    /**
     * Renders one certificate to PDF bytes.
     *
     * <p>Only ISSUED and REVOKED certificates export. A draft, a certificate in review and an
     * approved-but-unreleased one are not yet documents: issuing is the act that makes them one,
     * and a file saved outside the system cannot later discover that release was refused. A
     * revoked certificate still exports, watermarked, because a holder may legitimately need the
     * record of what was withdrawn.</p>
     *
     * @throws ResourceNotFoundException the certificate does not exist, or is soft-deleted
     * @throws ForbiddenException         the certificate belongs to another institution
     * @throws IllegalStateException      the certificate has not been released
     */
    @Transactional(readOnly = true)
    public byte[] exportCertificate(UUID certificateId, UUID institutionId) {
        Certificate certificate = certificateRepository.findByIdAndIsDeletedFalse(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", "id", certificateId));

        if (certificate.getInstitutionId() == null || !certificate.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("certificate", "export");
        }

        if (certificate.getStatus() != Certificate.CertificateStatus.ISSUED
                && certificate.getStatus() != Certificate.CertificateStatus.REVOKED) {
            throw new IllegalStateException(
                    "Only an issued certificate can be exported as a document (this one is "
                            + certificate.getStatus() + "). Issue it first.");
        }

        String institutionName = certificate.getInstitutionId() == null ? null
                : institutionRepository.findById(certificate.getInstitutionId())
                        .map(Institution::getName)
                        .orElse(null);

        String logoUrl = templateRepository.findByIdAndIsDeletedFalse(certificate.getTemplateId())
                .map(CertificateTemplate::getLogoUrl)
                .orElse(null);

        // Frozen at issuance. Certificates issued before snapshots existed, or the automatic
        // participation routes that never link a template, have none - the document then falls
        // back to the "authorised issuer" line, exactly as the browser renderer does.
        var signatories = certificate.getTemplateId() == null
                ? java.util.List.<tz.elmkusoma.certificate.domain.CertificateSignatorySnapshot>of()
                : signatorySnapshotRepository
                        .findByCertificateIdAndIsDeletedFalseOrderByDisplayOrderAsc(certificateId);

        try {
            return renderer.render(certificate, signatories, institutionName, logoUrl);
        } catch (IOException e) {
            throw new IllegalStateException("The certificate could not be rendered to PDF", e);
        }
    }
}
