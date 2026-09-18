package tz.elmkusoma.nfe.provider.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderRequest {

    @NotBlank(message = "Provider name is required")
    private String name;

    @NotBlank(message = "Provider type is required")
    private String providerType;

    private String description;

    private String logoUrl;

    private String website;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    private String address;

    private String city;

    private String country;

    private String contactPersonName;

    @Email(message = "Invalid contact person email format")
    private String contactPersonEmail;

    private String contactPersonPhone;
}
