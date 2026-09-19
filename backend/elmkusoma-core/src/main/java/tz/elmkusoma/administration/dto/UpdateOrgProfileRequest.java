package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrgProfileRequest {
    private String name;
    private String description;
    private String address;
    private String city;
    private String region;
    private String phone;
    private String email;
    private String website;
    private String logoUrl;
    private String bannerUrl;
    private String motto;
    private Integer foundedYear;
    private Integer totalCapacity;
}
