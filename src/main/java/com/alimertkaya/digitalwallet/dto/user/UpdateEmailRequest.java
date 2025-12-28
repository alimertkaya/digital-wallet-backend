package com.alimertkaya.digitalwallet.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateEmailRequest {
    @NotBlank(message = "E-posta boş bırakılamaz")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    private String newEmail;
}