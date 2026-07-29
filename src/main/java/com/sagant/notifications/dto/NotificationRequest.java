package com.sagant.notifications.dto;

import com.sagant.notifications.entity.NotificationChannel;
import com.sagant.notifications.entity.NotificationPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record NotificationRequest(

        @NotBlank(message = "recipient es obligatorio (email o URL segun el canal)")
        @Size(max = 500, message = "recipient no puede superar los 500 caracteres")
        String recipient,

        @NotNull(message = "channel es obligatorio y debe ser uno de: LOG, SERVICE")
        NotificationChannel channel,

        @NotBlank(message = "subject es obligatorio")
        @Size(max = 200, message = "subject no puede superar los 200 caracteres")
        String subject,

        @NotBlank(message = "body es obligatorio")
        String body,

        @NotNull(message = "priority es obligatorio y debe ser uno de: LOW, MEDIUM, HIGH")
        NotificationPriority priority,

        Map<String, String> metadata
) {
}
