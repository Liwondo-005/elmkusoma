package tz.elmkusoma.shared.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Public contact submission.
 *
 * <p>{@code website} and {@code formStartedAt} are bot traps, not real fields. Both are optional
 * so a legitimate client omits them; a client that fills the hidden field in, or submits faster
 * than a person could, is quietly discarded.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 120, message = "Name must be 2-120 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    @NotBlank(message = "Category is required")
    @Pattern(regexp = "GENERAL|SUPPORT|PARTNERSHIP|FEEDBACK|COMPLAINT",
            message = "Category must be one of: GENERAL, SUPPORT, PARTNERSHIP, FEEDBACK, COMPLAINT")
    private String category;

    @NotBlank(message = "Subject is required")
    @Size(min = 4, max = 160, message = "Subject must be 4-160 characters")
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(min = 20, max = 4000, message = "Message must be 20-4000 characters")
    private String message;

    /** Honeypot. Hidden from people, irresistible to bots. */
    private String website;

    /** Honeypot: epoch millis the form was rendered. */
    private Long formStartedAt;
}