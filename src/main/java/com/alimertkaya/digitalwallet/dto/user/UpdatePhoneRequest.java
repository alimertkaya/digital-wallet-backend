package com.alimertkaya.digitalwallet.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdatePhoneRequest {
    @NotBlank(message = "Telefon numarası boş bırakılamaz")
    @Pattern(regexp = "^[1-9][0-9]{9}$", message = "Telefon numarası 10 haneli olmalı ve başında 0 olmamalıdır")
    private String newPhoneNumber;
}