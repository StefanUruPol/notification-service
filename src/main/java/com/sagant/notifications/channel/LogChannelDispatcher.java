package com.sagant.notifications.channel;

import com.sagant.notifications.entity.Notification;
import com.sagant.notifications.entity.NotificationChannel;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class LogChannelDispatcher implements ChannelDispatcher {

    private static final Logger log = LoggerFactory.getLogger("notification.dispatch.log-channel");

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.LOG;
    }

    @Override
    public void send(Notification notification) {
        log.info("Notification despachada via LOG channel",
                StructuredArguments.keyValue("recipient", notification.getRecipient()),
                StructuredArguments.keyValue("subject", notification.getSubject()),
                StructuredArguments.keyValue("priority", notification.getPriority()),
                StructuredArguments.keyValue("metadata", notification.getMetadata()));
    }
}
