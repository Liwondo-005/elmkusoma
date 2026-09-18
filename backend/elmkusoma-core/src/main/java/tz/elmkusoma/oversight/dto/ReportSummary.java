package tz.elmkusoma.oversight.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSummary {
    private String id;
    private String title;
    private String description;
    private String type;
    private String icon;
    private String color;
    private Boolean available;
}