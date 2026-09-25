package tz.elmkusoma.certificate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Replace-the-set request for a template's authorised signatories (ordered). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSignatoriesRequest {
    /** Ordered signatory ids; empty list clears all links. */
    private List<UUID> signatoryIds;
}
