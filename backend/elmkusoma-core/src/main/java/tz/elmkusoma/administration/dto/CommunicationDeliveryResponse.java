package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationDeliveryResponse {
    private Long platformNotifications;
    private Long learnerNotifications;
    private Long learnerRead;
    private Long learnerUnread;
    private Double learnerReadRate;
    private Long platformReadCountSum;
    private String deliveryNote;
}
