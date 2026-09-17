package tz.elmkusoma.nfe.provider.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderResponse {

    private UUID id;
    private UUID institutionId;
    private String name;
    private String providerType;
    private String description;
    private String logoUrl;
    private String website;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private String contactPersonName;
    private String contactPersonEmail;
    private String contactPersonPhone;
    private Boolean isActive;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}
