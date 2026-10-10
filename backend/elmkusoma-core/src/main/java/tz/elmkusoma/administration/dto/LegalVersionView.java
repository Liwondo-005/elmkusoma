package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One immutable published version of a legal document, with the author who published it.
 *
 * <p>Retained for the record even when a newer version supersedes it: an archived version is
 * never edited, which is what lets the platform answer "what were the terms in force on this
 * date".</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalVersionView {

    private UUID id;
    private Integer version;
    private String title;
    private String content;
    private LocalDate effectiveDate;
    private LocalDateTime publishedAt;
    private String publishedBy;
}