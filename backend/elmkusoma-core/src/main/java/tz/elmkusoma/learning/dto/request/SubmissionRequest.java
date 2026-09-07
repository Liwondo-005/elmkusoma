package tz.elmkusoma.learning.dto.request;

import lombok.Data;

@Data
public class SubmissionRequest {

    private String fileUrl;

    private Integer grade;

    private String feedback;
}
