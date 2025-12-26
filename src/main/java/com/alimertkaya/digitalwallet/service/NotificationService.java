package com.alimertkaya.digitalwallet.service;

import com.alimertkaya.digitalwallet.dto.enums.NotificationType;
import com.alimertkaya.digitalwallet.entity.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationService {
    Mono<Void> createNotification(Long userId, String title, String message, NotificationType type);
    Flux<Notification> getMyNotifications();
    Mono<Void> markAsRead(Long notificationId);
}