package tz.elmkusoma.liveclass.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveSessionJoinResponse {
    private String liveKitToken;
    private String liveKitUrl;
    private String roomName;
    private boolean liveKitAvailable;
    private String classStatus;
    private String message;
}
