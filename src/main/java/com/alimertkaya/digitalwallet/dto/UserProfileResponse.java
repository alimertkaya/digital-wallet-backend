package com.alimertkaya.digitalwallet.dto;

import com.alimertkaya.digitalwallet.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String tckn;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
    private boolean isEmailVerified;
    private boolean isPhoneVerified;
    private boolean isKycVerified;

    public static UserProfileResponse fromEntity(User user) {
        return UserProfileResponse.builder()
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .tckn(user.getTckn())
                .birthDate(user.getBirthDate())
                .isEmailVerified(user.isEmailVerified())
                .isPhoneVerified(user.isPhoneVerified())
                .isKycVerified(user.isKycVerified())
                .build();
    }
}