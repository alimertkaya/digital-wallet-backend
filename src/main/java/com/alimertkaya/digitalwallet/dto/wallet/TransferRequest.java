package com.alimertkaya.digitalwallet.dto.wallet;

import com.alimertkaya.digitalwallet.dto.enums.TransactionCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotNull(message = "Hedef cüzdan ID'si boş olamaz")
    private Long targetWalletId;

    @NotNull(message = "Tutar boş olamaz")
    @DecimalMin(value = "0.01", message = "Tutar en az 0.01 olmalıdır")
    private BigDecimal amount;
    private TransactionCategory category;
    private String description;
}
