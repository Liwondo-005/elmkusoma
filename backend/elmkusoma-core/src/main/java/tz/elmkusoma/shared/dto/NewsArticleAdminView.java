package tz.elmkusoma.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A news article as Platform Admin sees it, including every working-copy field the public view
 * deliberately hides.
 *
 * <p>Separated from {@link NewsArticleView} rather than reusing it with nullable extras, so that
 * adding a field to the admin payload can never accidentally publish it.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticleAdminView {

    private String id;

    private String slug;

    private String title;

    private String summary;

    private String body;

    private String category;

    private String coverImageUrl;

    private String authorName;

    /** DRAFT | PUBLISHED | ARCHIVED. */
    private String status;

    private LocalDateTime publishedAt;

    private LocalDateTime scheduledAt;

    private LocalDateTime expiresAt;

    private boolean featured;

    private String priority;

    private Integer sortOrder;

    private String lastModifiedBy;

    private String publishedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}