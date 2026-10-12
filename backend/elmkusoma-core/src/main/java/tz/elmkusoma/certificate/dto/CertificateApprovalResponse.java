package tz.elmkusoma.certificate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    public static CertificateApprovalResponse from(CertificateApproval approval) {
        return CertificateApprovalResponse.builder()
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
    }
}
