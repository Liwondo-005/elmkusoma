package tz.elmkusoma.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be 2-100 characters")
    private String firstName;

    @Size(max = 255, message = "Middle name must not exceed 255 characters")
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 100, message = "Last name must be 2-100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be 8-128 characters")
    private String password;

    @Size(max = 255, message = "Phone must not exceed 255 characters")
    private String phone;

    // Deliberately excludes PROVIDER_ADMIN/PROVIDER_STAFF and every platform role. Those are
    // granted by platform invitation, never chosen by whoever fills in this public form. The
    // pattern is the client-visible contract, so it must not advertise roles the service will
    // reject - AuthServiceImpl.PUBLIC_REGISTRATION_ROLES is the authoritative allowlist and
    // re-checks this server-side.
    @Pattern(regexp = "STUDENT|TEACHER|PARENT|OTHER_LEARNER|LEARNER",
            message = "Role must be one of: STUDENT, TEACHER, PARENT, OTHER_LEARNER, LEARNER")
    private String role;

    private String learningLevel;

    private String secondaryStage;

    private String form;
}
