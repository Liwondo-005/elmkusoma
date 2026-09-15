package tz.elmkusoma.liveclass.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveClassAnalytics {
    private int totalParticipants;
    private int currentOnline;
    private int peakParticipants;
    private long totalChatMessages;
    private long averageDurationSeconds;
    private int totalSessions;
}
