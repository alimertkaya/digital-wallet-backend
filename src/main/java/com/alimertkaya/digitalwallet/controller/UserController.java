package com.alimertkaya.digitalwallet.controller;

import com.alimertkaya.digitalwallet.dto.ChangePasswordRequest;
import com.alimertkaya.digitalwallet.dto.UserProfileResponse;
import com.alimertkaya.digitalwallet.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Mono<UserProfileResponse> getProfile() {
        return userService.getCurrentUserProfile();
    }

    @PostMapping("/change-password")
    public Mono<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
       return userService.changePassword(request);
    }
}