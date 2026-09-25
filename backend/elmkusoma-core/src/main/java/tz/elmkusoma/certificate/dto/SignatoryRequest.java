package tz.elmkusoma.certificate.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Create/update payload for a certificate signatory (profile + authorisation scope). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignatoryRequest {
    @Size(max = 200)
    private String fullName;
    @Size(max = 200)
    private String positionTitle;
    @Size(max = 200)
    private String organization;
    /** Image data URL or https:// URL only — filesystem paths are rejected. */
    private String signatureImage;
    /** Subset of COMPLETION, ACHIEVEMENT, PARTICIPATION, TRANSCRIPT; null/empty = all types. */
    private List<String> certificateTypes;
    /** ACTIVE or INACTIVE; defaults to ACTIVE on create. */
    private String status;
    private LocalDate validFrom;
    private LocalDate validUntil;
    /** Institution scope; null = platform-wide signatory. */
    private UUID institutionId;
}
