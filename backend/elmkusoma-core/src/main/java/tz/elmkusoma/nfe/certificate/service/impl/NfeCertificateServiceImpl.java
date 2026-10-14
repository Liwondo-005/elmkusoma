package tz.elmkusoma.nfe.certificate.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nfe.certificate.domain.NfeCertificate;
import tz.elmkusoma.nfe.certificate.dto.NfeCertificateRequest;
import tz.elmkusoma.nfe.certificate.dto.NfeCertificateResponse;
import tz.elmkusoma.nfe.certificate.repository.NfeCertificateRepository;
import tz.elmkusoma.nfe.certificate.service.NfeCertificateService;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NfeCertificateServiceImpl implements NfeCertificateService {

    private final NfeCertificateRepository certificateRepository;
    private final tz.elmkusoma.nfe.provider.service.NfeScopeValidator scopeValidator;

    /**
     * Which statuses a certificate may be created in.
     *
     * <p>REVOKED is not one of them: a withdrawal describes something that happened to a
     * certificate that already existed, so creating one that way records an event that never
     * occurred.</p>
     */
    private static final Set<NfeCertificate.CertificateStatus> CREATION_STATUSES =
            EnumSet.of(NfeCertificate.CertificateStatus.DRAFT, NfeCertificate.CertificateStatus.ISSUED);

    /**
     * Enforces the only moves a certificate's status may make.
     *
     * <p>Both writes used to be plain field assignments. A provider admin could create a
     * certificate already marked ISSUED, and - more seriously - could PUT an ISSUED certificate
     * back to DRAFT, or lift a REVOKED one. For a document that may already have been printed and
     * handed to a learner, the difference between an issued certificate and a draft is the whole
     * point of the status: a credential that can be quietly un-issued by a second request is not
     * a credential, and a withdrawal that a second request can undo is not a withdrawal.</p>
     *
     * <p>Returns the status to store, so the caller does not have to reason about it twice.</p>
     *
     * @param from the status already on the row, or {@code null} when creating
     * @param to   the status being asked for
     */
    private static NfeCertificate.CertificateStatus transition(
            NfeCertificate.CertificateStatus from, NfeCertificate.CertificateStatus to) {
        if (from == null) {
            if (!CREATION_STATUSES.contains(to)) {
                throw new IllegalStateException(
                        "A certificate cannot be created as " + to + "; only DRAFT or ISSUED");
            }
            return to;
        }
        if (to == from) {
            return to;
        }
        if (from == NfeCertificate.CertificateStatus.REVOKED) {
            throw new IllegalStateException(
                    "A revoked certificate cannot be moved back to " + to);
        }
        if (to == NfeCertificate.CertificateStatus.DRAFT) {
            throw new IllegalStateException(
                    "An issued certificate cannot be returned to DRAFT; it can only be revoked");
        }
        return to;
    }


    @Override
    public NfeCertificateResponse createCertificate(UUID institutionId, UUID providerId, NfeCertificateRequest request) {
        // B-15: provider and learner must both belong to the caller's institution. Previously the
        // providerId came straight from the path and learnerId was never checked, so a row could
        // reference another tenant.
        UUID ownedProviderId = scopeValidator.requireOwnedProvider(institutionId, providerId).getId();
        UUID learnerId = scopeValidator.requireOwnedLearner(institutionId, request.getLearnerId());
        UUID programId = scopeValidator.requireOwnedProgram(institutionId, request.getProgramId());

        NfeCertificate.CertificateStatus requestedStatus =
                request.getStatus() != null
                        ? NfeCertificate.CertificateStatus.valueOf(request.getStatus())
                        : NfeCertificate.CertificateStatus.DRAFT;

        NfeCertificate certificate = NfeCertificate.builder()
                .providerId(ownedProviderId)
                .learnerId(learnerId)
                .programId(programId)
                .certificateType(NfeCertificate.CertificateType.valueOf(request.getCertificateType()))
                .title(request.getTitle())
                .studentName(request.getStudentName())
                // B-17: human-facing identifiers are generated server-side. They were client-supplied
                // with no uniqueness check, and a duplicate made the public verification endpoint
                // throw IncorrectResultSizeDataAccessException (500).
                .serialNumber(resolveSerial(request.getSerialNumber(), institutionId))
                .verificationCode(resolveVerificationCode(request.getVerificationCode(), institutionId))
                .issuedAt(request.getIssuedAt())
                .expiryDate(request.getExpiryDate())
                .status(transition(null, requestedStatus))
                .issuedBy(request.getIssuedBy())
                .build();
        certificate.setInstitutionId(institutionId);

        NfeCertificate saved;
        try {
            saved = certificateRepository.save(certificate);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // The unique indexes added in V130 now make this a clean conflict instead of a 500.
            throw new IllegalStateException("A certificate with this serial number or verification code already exists");
        }
        return mapToResponse(saved);
    }

    private String resolveSerial(String requested, UUID institutionId) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        return "NFE/" + java.time.Year.now().getValue() + "/"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String resolveVerificationCode(String requested, UUID institutionId) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        return "NFE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    @Override
    @Transactional(readOnly = true)
    public NfeCertificateResponse getCertificate(UUID institutionId, UUID certificateId) {
        NfeCertificate certificate = certificateRepository.findByIdAndInstitutionId(certificateId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("NFE Certificate", "id", certificateId));
        return mapToResponse(certificate);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NfeCertificateResponse> listCertificates(UUID institutionId, UUID providerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<NfeCertificate> allCertificates = certificateRepository.findByProviderId(providerId, institutionId);

        int start = Math.min(page * size, allCertificates.size());
        int end = Math.min((page + 1) * size, allCertificates.size());
        List<NfeCertificate> paged = allCertificates.subList(start, end);

        PageImpl<NfeCertificate> certificatePage = new PageImpl<>(
                paged, pageable, allCertificates.size());

        List<NfeCertificateResponse> content = certificatePage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                content,
                certificatePage.getNumber(),
                certificatePage.getSize(),
                certificatePage.getTotalElements(),
                certificatePage.getTotalPages(),
                certificatePage.isFirst(),
                certificatePage.isLast()
        );
    }

    @Override
    public NfeCertificateResponse updateCertificate(UUID institutionId, UUID certificateId, NfeCertificateRequest request) {
        NfeCertificate certificate = certificateRepository.findByIdAndInstitutionId(certificateId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("NFE Certificate", "id", certificateId));

        if (request.getLearnerId() != null) {
            certificate.setLearnerId(scopeValidator.requireOwnedLearner(institutionId, request.getLearnerId()));
        }
        if (request.getProgramId() != null) {
            certificate.setProgramId(scopeValidator.requireOwnedProgram(institutionId, request.getProgramId()));
        }
        // The serial and the verification code are the document's identity, fixed at creation
        // under B-17. Rewriting either here would turn an already-printed certificate into
        // something its holder can no longer verify, and would let one request re-point the code
        // printed on one learner's document at another learner's record.
        if (request.getSerialNumber() != null && !request.getSerialNumber().equals(certificate.getSerialNumber())) {
            throw new IllegalStateException("A certificate's serial number cannot be changed after it is created");
        }
        if (request.getVerificationCode() != null
                && !request.getVerificationCode().equals(certificate.getVerificationCode())) {
            throw new IllegalStateException("A certificate's verification code cannot be changed after it is created");
        }
        if (request.getCertificateType() != null) certificate.setCertificateType(NfeCertificate.CertificateType.valueOf(request.getCertificateType()));
        if (request.getTitle() != null) certificate.setTitle(request.getTitle());
        if (request.getStudentName() != null) certificate.setStudentName(request.getStudentName());
        if (request.getIssuedAt() != null) certificate.setIssuedAt(request.getIssuedAt());
        if (request.getExpiryDate() != null) certificate.setExpiryDate(request.getExpiryDate());
        if (request.getStatus() != null) {
            certificate.setStatus(transition(
                    certificate.getStatus(), NfeCertificate.CertificateStatus.valueOf(request.getStatus())));
        }
        if (request.getIssuedBy() != null) certificate.setIssuedBy(request.getIssuedBy());

        NfeCertificate saved = certificateRepository.save(certificate);
        return mapToResponse(saved);
    }

    @Override
    public void deleteCertificate(UUID institutionId, UUID certificateId) {
        NfeCertificate certificate = certificateRepository.findByIdAndInstitutionId(certificateId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("NFE Certificate", "id", certificateId));
        certificate.setIsDeleted(true);
        certificateRepository.save(certificate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NfeCertificateResponse> getCertificatesByLearner(UUID institutionId, UUID learnerId) {
        return certificateRepository.findByLearnerId(learnerId, institutionId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NfeCertificateResponse verifyCertificate(UUID institutionId, String verificationCode) {
        NfeCertificate certificate = certificateRepository.findByVerificationCode(verificationCode, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Nfe Certificate", "verificationCode", verificationCode));
        return mapToVerificationResponse(certificate);
    }

    /**
     * The verification answer: the state of the document <em>today</em>, which is a different
     * question from the state the institution last recorded.
     *
     * <p>The stored status is an event log entry - someone issued it, someone revoked it. What a
     * holder or an employer needs at the moment of checking is whether it is good now, and an
     * expiry date that has passed makes that "no" while the row still says ISSUED. Reporting
     * ISSUED for a certificate that lapsed last month is the quiet sort of untruth a verifier
     * exists to prevent.</p>
     */
    static String effectiveVerificationStatus(NfeCertificate certificate) {
        NfeCertificate.CertificateStatus status = certificate.getStatus();
        if (status == NfeCertificate.CertificateStatus.REVOKED) {
            return status.name();
        }
        if (status == NfeCertificate.CertificateStatus.DRAFT) {
            return status.name();
        }
        LocalDate expiry = certificate.getExpiryDate();
        if (expiry != null && expiry.isBefore(LocalDate.now())) {
            return "EXPIRED";
        }
        return status.name();
    }

    private NfeCertificateResponse mapToVerificationResponse(NfeCertificate certificate) {
        NfeCertificateResponse response = mapToResponse(certificate);
        response.setStatus(effectiveVerificationStatus(certificate));
        return response;
    }

    private NfeCertificateResponse mapToResponse(NfeCertificate certificate) {
        return NfeCertificateResponse.builder()
                .id(certificate.getId())
                .institutionId(certificate.getInstitutionId())
                .providerId(certificate.getProviderId())
                .learnerId(certificate.getLearnerId())
                .programId(certificate.getProgramId())
                .certificateType(certificate.getCertificateType().name())
                .title(certificate.getTitle())
                .studentName(certificate.getStudentName())
                .serialNumber(certificate.getSerialNumber())
                .verificationCode(certificate.getVerificationCode())
                .issuedAt(certificate.getIssuedAt())
                .expiryDate(certificate.getExpiryDate())
                .status(certificate.getStatus().name())
                .issuedBy(certificate.getIssuedBy())
                .createdAt(certificate.getCreatedAt())
                .build();
    }
}
