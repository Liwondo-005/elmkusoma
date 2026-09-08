package tz.elmkusoma.parent.dto.request;

import lombok.Data;

@Data
public class ParentNotificationPreferenceRequest {

    private Boolean attendanceAlerts;
    private Boolean gradeAlerts;
    private Boolean feeAlerts;
    private Boolean generalAnnouncements;
    private Boolean smsEnabled;
    private Boolean emailEnabled;
    private Boolean pushEnabled;
}
