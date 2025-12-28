package com.alimertkaya.digitalwallet.controller;

import com.alimertkaya.digitalwallet.dto.notification.VerifyCodeRequest;
import com.alimertkaya.digitalwallet.dto.user.*;
import com.alimertkaya.digitalwallet.service.UserService;
import com.alimertkaya.digitalwallet.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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

    @PostMapping("/verify-email")
    public Mono<Void> verifyEmail(@Valid @RequestBody VerifyCodeRequest request) {
        return userService.verifyEmail(request);
    }

    @PostMapping("/verify-phone")
    public Mono<Void> verifyPhone(@Valid @RequestBody VerifyCodeRequest request) {
        return userService.verifyPhone(request);
    }

    @PostMapping("/resend-email-code")
    public Mono<Void> resendEmailCode() {
        return userService.resendEmailCode();
    }

    @PostMapping("/resend-phone-code")
    public Mono<Void> resendPhoneCode() {
        return userService.resendPhoneCode();
    }

    @PostMapping("/deactivate")
    public Mono<Void> deactivateAccount() {
        return userService.deactivateAccount();
    }
}