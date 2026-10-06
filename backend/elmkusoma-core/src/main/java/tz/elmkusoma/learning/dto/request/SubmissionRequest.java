package tz.elmkusoma.learning.dto.request;

import lombok.Data;

@Data
public class SubmissionRequest {

    /** Learner's typed answer body. Persisted as assignment_submissions.submission_text. */
    private String content;

    /** Canonical alias of {@link #content} (frontend payload field name). */
    private String submissionText;

    /** Attachment reference (URL or uploaded object key). */
    private String fileUrl;

    /** When true, saves the submission as a draft (not counted as a final submission). */
    private Boolean draft;

    private Integer grade;

    private String feedback;
}
