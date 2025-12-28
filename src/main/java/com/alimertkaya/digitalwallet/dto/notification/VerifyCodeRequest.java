package com.alimertkaya.digitalwallet.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyCodeRequest {
    @NotBlank(message = "Doğrulama kodu boş olamaz!")
    @Size(min = 6, max = 6, message = "Kod 6 haneli olmalıdır")
    private String code;
}