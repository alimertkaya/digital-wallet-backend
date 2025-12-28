package com.alimertkaya.digitalwallet.dto.wallet;

import com.alimertkaya.digitalwallet.dto.enums.HistoryDirection;
import com.alimertkaya.digitalwallet.dto.enums.TransactionType;
import com.alimertkaya.digitalwallet.entity.TransactionHistory;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class TransactionHistoryResponse {
    private Long id;
    private TransactionType type;
    private HistoryDirection direction;
    private BigDecimal amount;
    private String currencyCode;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private Long relatedWalletId;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("date")
    private LocalDateTime createdAt;

    public static TransactionHistoryResponse fromEntity(TransactionHistory entity) {
        return TransactionHistoryResponse.builder()
                .id(entity.getId())
                .type(entity.getType())
                .direction(entity.getDirection())
                .amount(entity.getAmount())
                .currencyCode(entity.getCurrencyCode())
                .balanceBefore(entity.getBalanceBefore())
                .balanceAfter(entity.getBalanceAfter())
                .relatedWalletId(entity.getRelatedWalletId())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}