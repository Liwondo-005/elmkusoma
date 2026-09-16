package tz.elmkusoma.workers.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateGenerationEvent implements Serializable {

    private Long id;
    private Long certificateId;
    private Long studentId;
    private String studentName;
    private String courseName;
    private Long templateId;
    private Long institutionId;
    private LocalDateTime timestamp;
}
