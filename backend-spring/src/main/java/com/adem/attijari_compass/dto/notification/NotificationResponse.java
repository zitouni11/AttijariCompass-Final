package com.adem.attijari_compass.dto.notification;

import com.adem.attijari_compass.entity.NotificationSeverity;
import com.adem.attijari_compass.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String id;
    private NotificationType type;
    private NotificationSeverity severity;
    private String title;
    private String message;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime timestamp;
    private boolean read;
    private String actionLabel;
    private String actionRoute;
}
