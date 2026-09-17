package tz.elmkusoma.nfe.certificate.service;

import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.nfe.certificate.dto.NfeCertificateRequest;
import tz.elmkusoma.nfe.certificate.dto.NfeCertificateResponse;

import java.util.List;
import java.util.UUID;

public interface NfeCertificateService {

    NfeCertificateResponse createCertificate(UUID institutionId, UUID providerId, NfeCertificateRequest request);

    NfeCertificateResponse getCertificate(UUID institutionId, UUID certificateId);

    PageResponse<NfeCertificateResponse> listCertificates(UUID institutionId, UUID providerId, int page, int size);

    NfeCertificateResponse updateCertificate(UUID institutionId, UUID certificateId, NfeCertificateRequest request);

    void deleteCertificate(UUID institutionId, UUID certificateId);

    List<NfeCertificateResponse> getCertificatesByLearner(UUID institutionId, UUID learnerId);

    NfeCertificateResponse verifyCertificate(UUID institutionId, String verificationCode);
}
