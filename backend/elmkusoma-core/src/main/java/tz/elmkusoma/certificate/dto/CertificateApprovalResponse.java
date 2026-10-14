package tz.elmkusoma.certificate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.domain.CertificateApproval;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One approval decision, as shown to an authorized reviewer.
 *
 * <p>Carries the actor, their role, the template version they approved against and the reason.
 * That combination is what makes the decision auditable after the fact; a bare "approved" flag is
 * not evidence of anything.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateApprovalResponse {

    private UUID id;
    private UUID certificateId;
    private String stage;
    private String decisionStatus;
    private UUID actorUserId;
    private String actorEmail;
    private String actorRole;
    private Integer templateVersion;
    private String reason;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;

    // Certificate context.
    //
    // The approval queue is a list of pending decisions. Without these, a reviewer sees a wall of
    // rows distinguished only by an id and a stage, and has to go and look each certificate up
    // separately before they can judge it - which is to say, they cannot judge it at all. The
    // reviewer needs to know whose award this is and what it is.
    //
    // These are non-identifying for the reviewer's purpose: the award's title and the student's
    // name are exactly what an approver must see to decide. This DTO is never returned by the
    // public verification endpoint.
    private String certificateSerialNumber;
    private String certificateTitle;
    private String certificateType;
    private String studentName;
    private String certificateStatus;

    public static CertificateApprovalResponse from(CertificateApproval approval) {
        return from(approval, null);
    }

    /**
     * Builds the response, optionally denormalising the certificate.
     *
     * <p>The certificate is passed in rather than looked up per row: the queue returns many
     * approvals at once, and one query per row would turn a single page view into an N+1.</p>
     */
    public static CertificateApprovalResponse from(CertificateApproval approval,
                                                   Certificate certificate) {
        CertificateApprovalResponse response = CertificateApprovalResponse.builder()
                .id(approval.getId())
                .certificateId(approval.getCertificateId())
                .stage(approval.getStage() == null ? null : approval.getStage().name())
                .decisionStatus(approval.getDecisionStatus() == null ? null : approval.getDecisionStatus().name())
                .actorUserId(approval.getActorUserId())
                .actorEmail(approval.getActorEmail())
                .actorRole(approval.getActorRole())
                .templateVersion(approval.getTemplateVersion())
                .reason(approval.getReason())
                .decidedAt(approval.getDecidedAt())
                .createdAt(approval.getCreatedAt())
                .build();
        if (certificate != null) {
            response.setCertificateSerialNumber(certificate.getSerialNumber());
            response.setCertificateTitle(certificate.getTitle());
            response.setCertificateType(certificate.getCertificateType() == null
                    ? null : certificate.getCertificateType().name());
            response.setStudentName(certificate.getStudentName());
            response.setCertificateStatus(certificate.getStatus() == null
                    ? null : certificate.getStatus().name());
        }
        return response;
    }
}
