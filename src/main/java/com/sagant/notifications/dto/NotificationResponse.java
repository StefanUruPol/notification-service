package com.sagant.notifications.dto;

import com.sagant.notifications.entity.Notification;
import com.sagant.notifications.entity.NotificationChannel;
import com.sagant.notifications.entity.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationStatus status,
        NotificationChannel channel,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getStatus(), n.getChannel(), n.getCreatedAt());
    }
}
