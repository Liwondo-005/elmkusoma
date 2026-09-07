package tz.elmkusoma.institution.dto.response;

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
public class InstitutionResponse {

    private UUID id;
    private String name;
    private String description;
    private String type;
    private String status;
    private String logoUrl;
    private String website;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private LocalDateTime createdAt;
}
