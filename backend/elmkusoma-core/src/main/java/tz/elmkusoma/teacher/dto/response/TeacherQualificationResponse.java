package tz.elmkusoma.teacher.dto.response;

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
public class TeacherQualificationResponse {

    private UUID id;
    private UUID teacherId;
    private String qualificationName;
    private String institutionName;
    private String fieldOfStudy;
    private Integer yearObtained;
    private String certificateUrl;
    private LocalDateTime createdAt;
}
