package tz.elmkusoma.parent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentNotificationPreferenceResponse {

    private UUID id;
    private UUID parentId;
    private Boolean attendanceAlerts;
    private Boolean gradeAlerts;
    private Boolean feeAlerts;
    private Boolean generalAnnouncements;
    private Boolean smsEnabled;
    private Boolean emailEnabled;
    private Boolean pushEnabled;
}
