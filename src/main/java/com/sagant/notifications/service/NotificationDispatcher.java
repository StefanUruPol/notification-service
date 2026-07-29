package com.sagant.notifications.service;

import com.sagant.notifications.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationQueueService queueService;
    private final NotificationDispatchWorker worker;
    private final int batchSize;

    public NotificationDispatcher(
            NotificationQueueService queueService,
            NotificationDispatchWorker worker,
            @Value("${notifications.dispatch.batch-size:20}") int batchSize) {
        this.queueService = queueService;
        this.worker = worker;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${notifications.dispatch.poll-interval-ms:2000}")
    public void pollAndDispatch() {
        List<Notification> batch = queueService.claimBatch(batchSize);

        if (batch.isEmpty()) {
            return;
        }

        log.info("Dispatcher tomo un lote de {} notificaciones para procesar", batch.size());
        batch.forEach(worker::dispatch);
    }
}
