package tz.elmkusoma.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The published legal document a visitor is allowed to see.
 *
 * <p>Built only from an archived {@code LegalDocumentVersion}, so a draft cannot reach this
 * shape by accident.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalDocumentView {

    private String type;
    private String title;
    private Integer version;

    /** Plain text. Rendered as text nodes, never as markup. */
    private String content;

    private LocalDate effectiveDate;
    private LocalDateTime publishedAt;
}