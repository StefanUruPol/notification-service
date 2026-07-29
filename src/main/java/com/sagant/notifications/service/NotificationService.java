package com.sagant.notifications.service;

import com.sagant.notifications.dto.NotificationRequest;
import com.sagant.notifications.entity.Notification;
import com.sagant.notifications.entity.NotificationStatus;
import com.sagant.notifications.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public Notification create(NotificationRequest request) {
        Notification notification = Notification.builder()
                .recipient(request.recipient())
                .channel(request.channel())
                .subject(request.subject())
                .body(request.body())
                .priority(request.priority())
                .metadata(request.metadata() == null ? java.util.Map.of() : request.metadata())
                .status(NotificationStatus.PENDING)
                .build();

        Notification saved = repository.save(notification);

        log.info("Notification {} creada y encolada (channel={}, priority={})",
                saved.getId(), saved.getChannel(), saved.getPriority());

        return saved;
    }
}
