package com.alimertkaya.digitalwallet.controller;

import com.alimertkaya.digitalwallet.dto.AuthResponse;
import com.alimertkaya.digitalwallet.dto.LoginRequest;
import com.alimertkaya.digitalwallet.dto.RegisterRequest;
import com.alimertkaya.digitalwallet.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor // for AuthService
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED) // basarili olursa, return 201 Created
    public Mono<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request)
                .map(authResponse -> ResponseEntity.ok(authResponse));
    }
}
