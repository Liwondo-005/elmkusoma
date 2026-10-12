package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The admin's view of a legal document: the working copy plus where publication stands.
 *
 * <p>{@code draftVersion} and {@code publishedVersion} are separate on purpose - they differ
 * whenever an edit has not been published yet, which is exactly the state an editor needs to be
 * able to see.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalDocumentAdminView {

    private UUID id;
    private String type;
    private String title;
    private String content;
    private LocalDate effectiveDate;
    private Integer draftVersion;
    private Integer publishedVersion;
    private String lastModifiedBy;
    private LocalDateTime updatedAt;
}