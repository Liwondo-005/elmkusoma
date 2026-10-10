package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Restores an earlier published version into the draft, for the author to review and publish. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalRevertRequest {

    private Integer version;
}