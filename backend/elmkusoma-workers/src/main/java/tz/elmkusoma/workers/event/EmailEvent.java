package tz.elmkusoma.workers.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent implements Serializable {

    private Long id;
    private String toEmail;
    private String subject;
    private String templateName;
    private Map<String, Object> templateVariables;
    /**
     * Opaque tenant marker (core uses UUID strings). Informational only: the
     * email consumer never authorizes on it.
     */
    private String institutionId;
    private long timestamp;
}
