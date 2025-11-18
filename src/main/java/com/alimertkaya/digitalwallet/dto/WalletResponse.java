package com.alimertkaya.digitalwallet.dto;

import com.alimertkaya.digitalwallet.entity.Wallet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {

    private Long id;
    private Long userId;
    private String name;
    private String currencyCode;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    // from Entity to DTO
    public static WalletResponse fromEntity(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .name(wallet.getName())
                .currencyCode(wallet.getCurrencyCode())
                .balance(wallet.getBalance())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}