package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkContentActionRequest {
    private String type;     // COURSE | RESOURCE | EVENT
    private String action;   // PUBLISH | UNPUBLISH | ARCHIVE | RESTORE
    private List<UUID> ids;
}
