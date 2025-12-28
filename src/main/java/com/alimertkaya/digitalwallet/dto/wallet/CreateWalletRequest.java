package com.alimertkaya.digitalwallet.dto.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWalletRequest {

    @NotBlank(message = "Cüzdan adı boş bırakılamaz")
    @Size(max = 100, message = "Cüzdan adı en fazla 100 karakter olabilir.")
    private String name;

    @NotBlank(message = "Para birimi kodu boş bırakılamaz")
    @Size(min = 3, max = 3, message = "Para birimi kodu 3 karakter olmalıdır. (örn: TRY, USD)")
    private String currencyCode;
}
