package com.sagant.notifications.channel;

import com.sagant.notifications.entity.Notification;
import com.sagant.notifications.entity.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;


@Component
public class ServiceChannelDispatcher implements ChannelDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ServiceChannelDispatcher.class);

    private final RestClient restClient;

    public ServiceChannelDispatcher(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SERVICE;
    }

    @Override
    public void send(Notification notification) {
        Map<String, Object> payload = Map.of(
                "id", notification.getId().toString(),
                "subject", notification.getSubject(),
                "body", notification.getBody(),
                "priority", notification.getPriority().name(),
                "metadata", notification.getMetadata()
        );

        HttpStatusCode status = restClient.post()
                .uri(notification.getRecipient())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity()
                .getStatusCode();

        if (status.isError()) {
            throw new IllegalStateException("El destino SERVICE respondio con status " + status.value());
        }

        log.debug("Notification {} despachada via SERVICE a {}", notification.getId(), notification.getRecipient());
    }
}
