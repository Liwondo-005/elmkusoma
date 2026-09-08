package tz.elmkusoma.teacher.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeacherQualificationRequest {

    @NotBlank(message = "Qualification name is required")
    private String qualificationName;

    @NotBlank(message = "Institution name is required")
    private String institutionName;

    private String fieldOfStudy;

    private Integer yearObtained;

    private String certificateUrl;
}
