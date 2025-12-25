package com.alimertkaya.digitalwallet.controller;

import com.alimertkaya.digitalwallet.dto.*;
import com.alimertkaya.digitalwallet.dto.enums.VerificationType;
import com.alimertkaya.digitalwallet.entity.User;
import com.alimertkaya.digitalwallet.service.UserService;
import com.alimertkaya.digitalwallet.service.VerificationService;
import com.alimertkaya.digitalwallet.service.impl.VerificationServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final VerificationService verificationService;

    @GetMapping("/me")
    public Mono<UserProfileResponse> getProfile() {
        return userService.getCurrentUserProfile();
    }

    @PostMapping("/change-password")
    public Mono<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
       return userService.changePassword(request);
    }

    @PutMapping("/info")
    public Mono<UserProfileResponse> updateUserInfo(@Valid @RequestBody UpdateUserInfoRequest request) {
        return userService.updateUserInfo(request);
    }

    @PutMapping("/email")
    public Mono<UserProfileResponse> updateEmail(@Valid @RequestBody UpdateEmailRequest request) {
        return userService.updateEmail(request);
    }

    @PutMapping("/phone")
    public Mono<UserProfileResponse> updatePhone(@Valid @RequestBody UpdatePhoneRequest request) {
        return userService.updatePhone(request);
    }

}