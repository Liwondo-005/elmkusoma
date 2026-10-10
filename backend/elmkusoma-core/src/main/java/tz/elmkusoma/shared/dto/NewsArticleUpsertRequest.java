package tz.elmkusoma.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Create/update payload for a news article.
 *
 * <p>Three fields are deliberately absent and cannot be supplied by a caller:</p>
 *
 * <ul>
 *   <li>{@code status} - publishing, unpublishing and archiving are separate endpoints, so the
 *       lifecycle cannot be reached by accident with an ordinary save.</li>
 *   <li>{@code publishedAt} - written by the server on publish. It is what the NEW indicator is
 *       measured from, so accepting it here would let any article claim to be freshly published.</li>
 *   <li>{@code id} / {@code createdBy} - set by the persistence layer.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticleUpsertRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Summary is required")
    @Size(max = 500, message = "Summary must not exceed 500 characters")
    private String summary;

    @NotBlank(message = "Body is required")
    @Size(max = 200000, message = "Body must not exceed 200000 characters")
    private String body;

    /**
     * Optional. Blank means "derive from the title"; a supplied value must be a clean slug.
     * Rejected with a pattern rather than silently corrected, because an administrator who typed
     * "Breaking News!" deserves to be told it is not a valid URL segment.
     */
    @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "Slug must be lower-case words separated by single hyphens, e.g. term-start-2026")
    @Size(max = 160, message = "Slug must not exceed 160 characters")
    private String slug;

    @Size(max = 40, message = "Category must not exceed 40 characters")
    private String category;

    /**
     * Absolute (http/https) or root-relative (/...) only. Explicitly rejects javascript:, data: and
     * vbscript:, which a cover image URL is the classic way to smuggle a script onto a page.
     */
    @Pattern(regexp = "^$|^(https?://|/)[\\x20-\\x7E]+$",
            message = "Cover image URL must start with http://, https:// or /")
    @Size(max = 1000, message = "Cover image URL must not exceed 1000 characters")
    private String coverImageUrl;

    @Size(max = 160, message = "Author must not exceed 160 characters")
    private String authorName;

    @Pattern(regexp = "NORMAL|IMPORTANT|URGENT",
            message = "Priority must be one of: NORMAL, IMPORTANT, URGENT")
    private String priority;

    private Boolean featured;

    private Integer sortOrder;

    private LocalDateTime scheduledAt;

    private LocalDateTime expiresAt;
}