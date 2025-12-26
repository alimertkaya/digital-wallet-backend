package com.alimertkaya.digitalwallet.entity;

import com.alimertkaya.digitalwallet.dto.enums.VerificationType;
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
@Table("verification_codes")
public class VerificationCode {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    private String code;
    private VerificationType type;
    @Column("expiry_date")
    private LocalDateTime expiryDate;
}