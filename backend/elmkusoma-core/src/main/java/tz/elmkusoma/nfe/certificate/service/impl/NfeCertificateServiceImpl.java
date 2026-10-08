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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NfeCertificateServiceImpl implements NfeCertificateService {

    private final NfeCertificateRepository certificateRepository;
    private final tz.elmkusoma.nfe.provider.service.NfeScopeValidator scopeValidator;

    @Override
    public NfeCertificateResponse createCertificate(UUID institutionId, UUID providerId, NfeCertificateRequest request) {
        // B-15: provider and learner must both belong to the caller's institution. Previously the
        // providerId came straight from the path and learnerId was never checked, so a row could
        // reference another tenant.
        UUID ownedProviderId = scopeValidator.requireOwnedProvider(institutionId, providerId).getId();
        UUID learnerId = scopeValidator.requireOwnedLearner(institutionId, request.getLearnerId());
        UUID programId = scopeValidator.requireOwnedProgram(institutionId, request.getProgramId());

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
                .status(request.getStatus() != null ? NfeCertificate.CertificateStatus.valueOf(request.getStatus()) : NfeCertificate.CertificateStatus.DRAFT)
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
        if (request.getCertificateType() != null) certificate.setCertificateType(NfeCertificate.CertificateType.valueOf(request.getCertificateType()));
        if (request.getTitle() != null) certificate.setTitle(request.getTitle());
        if (request.getStudentName() != null) certificate.setStudentName(request.getStudentName());
        if (request.getSerialNumber() != null) certificate.setSerialNumber(request.getSerialNumber());
        if (request.getVerificationCode() != null) certificate.setVerificationCode(request.getVerificationCode());
        if (request.getIssuedAt() != null) certificate.setIssuedAt(request.getIssuedAt());
        if (request.getExpiryDate() != null) certificate.setExpiryDate(request.getExpiryDate());
        if (request.getStatus() != null) certificate.setStatus(NfeCertificate.CertificateStatus.valueOf(request.getStatus()));
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
                .orElseThrow(() -> new ResourceNotFoundException("NFE Certificate", "verificationCode", verificationCode));
        return mapToResponse(certificate);
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
