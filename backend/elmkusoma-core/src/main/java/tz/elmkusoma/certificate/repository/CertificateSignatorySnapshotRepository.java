package tz.elmkusoma.certificate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.certificate.domain.CertificateSignatorySnapshot;

import java.util.List;
import java.util.UUID;

@Repository
public interface CertificateSignatorySnapshotRepository extends JpaRepository<CertificateSignatorySnapshot, UUID> {

    List<CertificateSignatorySnapshot> findByCertificateIdAndIsDeletedFalseOrderByDisplayOrderAsc(
            UUID certificateId);
}
