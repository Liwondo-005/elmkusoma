package tz.elmkusoma.oversight.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JurisdictionSummary {
    private String type;
    private String name;
    private String code;
}