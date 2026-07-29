package com.sagant.notifications.controller;

import com.sagant.notifications.dto.NotificationRequest;
import com.sagant.notifications.dto.NotificationResponse;
import com.sagant.notifications.entity.Notification;
import com.sagant.notifications.repository.NotificationRepository;
import com.sagant.notifications.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationService notificationService, NotificationRepository notificationRepository) {
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationRequest request) {
        Notification saved = notificationService.create(request);
        NotificationResponse body = NotificationResponse.from(saved);
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/notifications/" + saved.getId()))
                .body(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getById(@PathVariable UUID id) {
        return notificationRepository.findById(id)
                .map(NotificationResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

