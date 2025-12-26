package com.alimertkaya.digitalwallet.repository;

import com.alimertkaya.digitalwallet.entity.Notification;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface NotificationRepository extends R2dbcRepository<Notification, Long> {
    // tum bildirimler yeniden eskiye
    Flux<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    // sadece okunmamis bildirimler
    Flux<Notification> findByUserIdAndIsReadFalse(Long userId);
}