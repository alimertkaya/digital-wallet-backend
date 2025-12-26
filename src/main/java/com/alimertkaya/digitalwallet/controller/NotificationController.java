package com.alimertkaya.digitalwallet.controller;

import com.alimertkaya.digitalwallet.entity.Notification;
import com.alimertkaya.digitalwallet.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public Flux<Notification> getMyNotifications() {
        return notificationService.getMyNotifications();
    }

    @PutMapping("/{id}/read")
    public Mono<Void> markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(id);
    }
}