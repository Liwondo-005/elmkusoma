package tz.elmkusoma.certificate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.certificate.domain.CertificateApproval;
import tz.elmkusoma.certificate.domain.CertificateApproval.ApprovalStage;
import tz.elmkusoma.certificate.domain.CertificateApproval.DecisionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateApprovalRepository extends JpaRepository<CertificateApproval, UUID> {

    List<CertificateApproval> findByCertificateIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID certificateId);

    /** The single current decision for a stage, if one exists. */
    Optional<CertificateApproval> findByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatusIn(
            UUID certificateId, ApprovalStage stage, List<DecisionStatus> statuses);

    boolean existsByCertificateIdAndStageAndIsDeletedFalseAndDecisionStatus(
            UUID certificateId, ApprovalStage stage, DecisionStatus status);

    /** Institution approval queue: outstanding work, oldest first. */
    List<CertificateApproval> findByInstitutionIdAndIsDeletedFalseAndDecisionStatusOrderByCreatedAtAsc(
            UUID institutionId, DecisionStatus status);

    long countByInstitutionIdAndIsDeletedFalseAndDecisionStatus(UUID institutionId, DecisionStatus status);
}
