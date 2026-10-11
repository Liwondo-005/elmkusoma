package tz.elmkusoma.shared.domain;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;

import java.util.UUID;

/**
 * A reply on a contact enquiry.
 *
 * <p>{@code isInternal} marks a note that only staff may read. The public receipt endpoint and
 * the enquirer's own view never select internal rows at all — the filter is in the repository
 * query rather than in response mapping, so a future endpoint cannot leak one by forgetting.</p>
 */
@Entity
@Table(name = "contact_message_replies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ContactMessageReply extends tz.elmkusoma.common.BaseEntity {


    @Column(name = "contact_message_id", nullable = false)
    private UUID contactMessageId;

    @Column(name = "author_id")
    private UUID authorId;

    @Builder.Default
    @Column(name = "is_internal", nullable = false)
    private Boolean isInternal = false;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Whether this reply was actually emailed to the enquirer. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean delivered = false;
}