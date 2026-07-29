package com.sagant.notifications.service;

import com.sagant.notifications.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Component
public class NotificationDispatchWorker {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchWorker.class);

    private final ChannelSendExecutor channelSendExecutor;
    private final NotificationQueueService queueService;

    public NotificationDispatchWorker(ChannelSendExecutor channelSendExecutor, NotificationQueueService queueService) {
        this.channelSendExecutor = channelSendExecutor;
        this.queueService = queueService;
    }

    @Async("dispatchExecutor")
    public void dispatch(Notification notification) {
        MDC.put("notificationId", notification.getId().toString());
        try {
            channelSendExecutor.sendWithRetry(notification);
            queueService.markSent(notification.getId());
            log.info("Notification despachada con exito via {}", notification.getChannel());
        } catch (Exception ex) {
            queueService.markFailed(notification.getId(), ex.getMessage());
            log.error("Notification fallo tras agotar reintentos: {}", ex.getMessage());
        } finally {
            MDC.remove("notificationId");
        }
    }
}
