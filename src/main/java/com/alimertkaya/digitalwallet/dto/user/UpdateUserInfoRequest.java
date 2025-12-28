package com.alimertkaya.digitalwallet.dto.user;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateUserInfoRequest {
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
}