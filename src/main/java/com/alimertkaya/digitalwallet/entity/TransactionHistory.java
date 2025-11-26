package com.alimertkaya.digitalwallet.entity;

import com.alimertkaya.digitalwallet.dto.enums.HistoryDirection;
import com.alimertkaya.digitalwallet.dto.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("transaction_history")
public class TransactionHistory {

    @Id
    private Long id;

    @Column("wallet_id")
    private Long walletId;

    @Column("related_wallet_id")
    private Long relatedWalletId;

    @Column("type")
    private TransactionType type;

    @Column("direction")
    private HistoryDirection direction;

    @Column("amount")
    private BigDecimal amount;

    @Column("balance_before")
    private BigDecimal balanceBefore;

    @Column("balance_after")
    private BigDecimal balanceAfter;

    @Column("currency_code")
    private String currencyCode;

    @Column("description")
    private String description;

    @Column("created_at")
    private LocalDateTime createdAt;
}