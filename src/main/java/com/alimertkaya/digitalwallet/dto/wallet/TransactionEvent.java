package com.alimertkaya.digitalwallet.dto.wallet;

import com.alimertkaya.digitalwallet.dto.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private LocalDateTime eventTimestamp = LocalDateTime.now();

    // DEPOSIT, WITHDRAW, TRANSFER
    private TransactionType type;

    // para gonderen veya paranin yatacagi hesap
    private Long sourceWalletId;

    // parayi alan cuzdan
    private Long targetWalletId;

    // islem tutari
    private BigDecimal sourceAmount;
    private BigDecimal targetAmount;

    private String sourceCurrency;
    private String targetCurrency;
}
