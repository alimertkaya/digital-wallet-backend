package com.alimertkaya.digitalwallet.entity;

import com.alimertkaya.digitalwallet.dto.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notifications")
public class Notification {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    private String title;
    private String message;
    private NotificationType type;
    @Column("is_read")
    private boolean isRead;
    @Column("created_at")
    private LocalDateTime createdAt;
}