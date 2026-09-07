package tz.elmkusoma.institution.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateInstitutionRequest {

    @NotBlank(message = "Institution name is required")
    @Size(min = 2, max = 255, message = "Name must be 2-255 characters")
    private String name;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @NotBlank(message = "Institution type is required")
    private String type;

    private String logoUrl;

    private String website;

    private String email;

    private String phone;

    private String address;

    private String city;

    private String country;
}
