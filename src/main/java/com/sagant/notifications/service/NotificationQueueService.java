package com.sagant.notifications.service;

import com.sagant.notifications.entity.Notification;
import com.sagant.notifications.entity.NotificationStatus;
import com.sagant.notifications.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Service
public class NotificationQueueService {

    private final NotificationRepository repository;

    public NotificationQueueService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<Notification> claimBatch(int batchSize) {
        List<Notification> batch = repository.findBatchForDispatch(Instant.now(), batchSize);
        batch.forEach(n -> n.setStatus(NotificationStatus.PROCESSING));
        return repository.saveAll(batch);
    }

    @Transactional
    public void markSent(UUID id) {
        repository.findById(id).ifPresent(n -> {
            n.setStatus(NotificationStatus.SENT);
            n.setAttempts(n.getAttempts() + 1);
        });
    }

    @Transactional
    public void markFailed(UUID id, String errorMessage) {
        repository.findById(id).ifPresent(n -> {
            n.setStatus(NotificationStatus.FAILED);
            n.setAttempts(n.getAttempts() + 1);
            n.setLastError(errorMessage);
        });
    }
}
