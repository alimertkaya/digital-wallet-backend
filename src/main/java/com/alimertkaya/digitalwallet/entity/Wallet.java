package com.alimertkaya.digitalwallet.entity;

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
@Table("wallets")
public class Wallet {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("name")
    private String name;

    @Column("currency_code")
    private String currencyCode;

    @Column("balance")
    private BigDecimal balance;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
