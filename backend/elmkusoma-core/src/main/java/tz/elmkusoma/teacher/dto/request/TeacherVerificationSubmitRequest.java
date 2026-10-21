package tz.elmkusoma.teacher.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Self-service teacher verification submission. The service categories declare which
 * documents are required (preliminary automated check); the Platform Admin's review
 * remains the only authority that grants formal verification.
 */
@Data
public class TeacherVerificationSubmitRequest {

    /** Service categories the teacher intends to deliver (e.g. LIVE_CLASS, COURSE). */
    @NotEmpty(message = "At least one service type is required")
    private List<String> serviceTypes;

    /** Supporting documents, validated against the service-category matrix. */
    @NotEmpty(message = "At least one document is required")
    private List<DocumentSubmission> documents;

    /** Optional free-text note for the reviewer (stored as the record's notes). */
    private String notes;

    @Data
    public static class DocumentSubmission {

        /** Document type, e.g. TEACHING_QUALIFICATION, CERTIFICATE, COMPETENCY_EVIDENCE. */
        @NotBlank(message = "Document type is required")
        private String type;

        /** Link to the uploaded document (e.g. a media-library URL). Optional. */
        private String url;

        /** Optional note describing the document. */
        private String notes;
    }
}
