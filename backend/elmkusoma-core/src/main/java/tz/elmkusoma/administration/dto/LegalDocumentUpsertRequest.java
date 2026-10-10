package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Create or edit a legal document's working copy.
 *
 * <p>{@code type} is only honoured on create; an edit cannot retype a document, because the
 * published history is keyed on the original type and silently retyping would orphan it.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalDocumentUpsertRequest {

    @Pattern(regexp = "TERMS|PRIVACY|COOKIE|SUPPORT_POLICY",
            message = "Type must be one of: TERMS, PRIVACY, COOKIE, SUPPORT_POLICY")
    private String type;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 200000, message = "Content must not exceed 200000 characters")
    private String content;

    private LocalDate effectiveDate;
}