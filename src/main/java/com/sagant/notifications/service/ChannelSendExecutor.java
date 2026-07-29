package com.sagant.notifications.service;

import com.sagant.notifications.channel.ChannelDispatcher;
import com.sagant.notifications.entity.Notification;
import com.sagant.notifications.entity.NotificationChannel;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Component
public class ChannelSendExecutor {

    private final Map<NotificationChannel, ChannelDispatcher> dispatchersByChannel;

    public ChannelSendExecutor(List<ChannelDispatcher> dispatchers) {
        this.dispatchersByChannel = dispatchers.stream()
                .collect(Collectors.toMap(ChannelDispatcher::channel, Function.identity()));
    }

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendWithRetry(Notification notification) throws Exception {
        ChannelDispatcher dispatcher = dispatchersByChannel.get(notification.getChannel());
        if (dispatcher == null) {
            throw new IllegalStateException("No hay ChannelDispatcher registrado para " + notification.getChannel());
        }
        dispatcher.send(notification);
    }
}
