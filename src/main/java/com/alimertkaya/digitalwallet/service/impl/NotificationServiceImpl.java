package com.alimertkaya.digitalwallet.service.impl;

import com.alimertkaya.digitalwallet.dto.enums.NotificationType;
import com.alimertkaya.digitalwallet.entity.Notification;
import com.alimertkaya.digitalwallet.repository.NotificationRepository;
import com.alimertkaya.digitalwallet.service.NotificationService;
import com.alimertkaya.digitalwallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    @Override
    public Mono<Void> createNotification(Long userId, String title, String message, NotificationType type) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        return notificationRepository.save(notification)
                .then();
    }

    @Override
    public Flux<Notification> getMyNotifications() {
        return userService.getCurrentUser()
                .flatMapMany(user -> notificationRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()));
    }

    @Override
    public Mono<Void> markAsRead(Long notificationId) {
        return userService.getCurrentUser()
                .flatMap(user -> notificationRepository.findById(notificationId)
                        .flatMap(notification -> {
                            if (!notification.getUserId().equals(user.getId())) {
                                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz yok."));
                            }
                            notification.setRead(true);
                            return notificationRepository.save(notification);
                        }))
                .then();
    }
}
