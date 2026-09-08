package tz.elmkusoma.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TransferRequest {

    @NotNull(message = "Target class group ID is required")
    private UUID toClassGroupId;

    private String reason;
}
