package com.alimertkaya.digitalwallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Eski şifre zorunludur")
    private String oldPassword;

    @NotBlank(message = "Yeni şifre zorunludur")
    @Size(min = 8, message = "Şifre en az 8 karakter olmalıdır")
    private String newPassword;
}