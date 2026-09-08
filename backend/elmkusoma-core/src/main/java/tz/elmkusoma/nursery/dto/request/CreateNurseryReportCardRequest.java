package tz.elmkusoma.nursery.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateNurseryReportCardRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Term ID is required")
    private UUID termId;

    private String generalRemarks;

    private String teacherComments;

    private String physicalDevelopment;

    private String cognitiveDevelopment;

    private String socialDevelopment;

    private String emotionalDevelopment;

    private String languageDevelopment;

    private String areasOfStrength;

    private String areasForImprovement;

    private String recommendationsForParents;
}