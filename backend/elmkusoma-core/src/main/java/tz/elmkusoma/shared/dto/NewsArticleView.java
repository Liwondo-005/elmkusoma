package tz.elmkusoma.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A news article as the public sees it.
 *
 * <p>Deliberately not the entity. {@code isNew} is computed on the server from
 * {@code publishedAt} against a configurable window, because a client that computed it itself
 * could mark a two-year-old article as new; and the working-copy fields ({@code status},
 * {@code scheduledAt}, {@code sortOrder}, {@code lastModifiedBy}, {@code publishedBy}) are absent
 * because none of them are the visitor's business.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticleView {

    private String id;

    private String slug;

    private String title;

    private String summary;

    /** Omitted from list responses; only populated for the single-article endpoint. */
    private String body;

    private String category;

    private String coverImageUrl;

    private String authorName;

    private LocalDateTime publishedAt;

    /** Null unless the article has actually been edited since publication. */
    private LocalDateTime updatedAt;

    private boolean featured;

    private String priority;

    /**
     * Server-computed against the configured NEW window. Never trusted from the client.
     *
     * <p>Pinned to {@code "new"} rather than {@code "isNew"}: {@code new} is a Java keyword so it
     * cannot be the field name, and relying on Jackson stripping the {@code is} prefix off a
     * Lombok-generated boolean getter makes the wire format a side effect of Lombok's naming.
     */
    @JsonProperty("new")
    private boolean isNew;
}