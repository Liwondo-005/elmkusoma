package tz.elmkusoma.identity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaStatusResponse {

    private boolean enrolled;
    private long remainingCodes;
    private List<String> recoveryCodes;
}
