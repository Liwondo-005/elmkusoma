package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A staff reply to a contact enquiry, or an internal note that is never shown outward. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageReplyRequest {

    @NotBlank(message = "A reply cannot be empty")
    @Size(max = 5000, message = "A reply must not exceed 5000 characters")
    private String message;

    /** True records a staff-only note. Null is treated as false. */
    private Boolean internal;
}