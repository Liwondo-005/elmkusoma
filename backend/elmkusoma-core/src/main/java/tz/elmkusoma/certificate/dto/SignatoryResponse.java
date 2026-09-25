package tz.elmkusoma.certificate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Authorised signatory profile + authorisation scope, as exposed to platform governance. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignatoryResponse {
    private UUID id;
    private UUID institutionId;
    private String institutionName;
    private String fullName;
    private String positionTitle;
    private String organization;
    /** Signature image data URL (data:image/...;base64,...) or https:// URL; null when not configured. */
    private String signatureImage;
    /** Certificate types this signatory is authorised to sign; null = all types. */
    private List<String> certificateTypes;
    private String status;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
