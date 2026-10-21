package tz.elmkusoma.teacher.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The current teacher's verification view: derived effective state, the document
 * requirements per service category, and the full review history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherVerificationStatusResponse {

    private UUID teacherId;
    private UUID userId;

    /** PROVISIONAL | PENDING | CHANGES_REQUIRED | REJECTED | APPROVED | SUSPENDED. */
    private String effectiveStatus;

    /** True only when the Platform Admin has granted formal verification (state APPROVED). */
    private boolean formallyVerified;

    /** Documents required per service category (preliminary check matrix). */
    private Map<String, List<String>> requiredDocumentsByServiceType;

    /** Review history, oldest first. */
    private List<Record> records;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Record {
        private UUID id;
        private String status;
        private String verificationType;
        private String notes;
        /** Raw JSON document array as submitted. */
        private String documents;
        /** Raw JSON metadata, e.g. {"serviceTypes":[...]}. */
        private String metadata;
        private LocalDateTime submittedAt;
        private LocalDateTime reviewedAt;
        private UUID reviewedBy;
    }
}
