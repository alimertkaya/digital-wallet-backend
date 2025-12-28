package com.alimertkaya.digitalwallet.service;

import com.alimertkaya.digitalwallet.dto.auth.AuthResponse;
import com.alimertkaya.digitalwallet.dto.auth.LoginRequest;
import com.alimertkaya.digitalwallet.dto.auth.RegisterRequest;
import reactor.core.publisher.Mono;

public interface AuthService {

    // yeni kullaniciyi kaydetme
    // @param request kayit bilgileri (username, password. email etc.) iceren DTO
    // return JWT token iceren AuthResponse
    Mono<AuthResponse> register(RegisterRequest request);

    // @param request giris bilgileri iceren DTO
    // @return JWT token iceren AuthResponse
    Mono<AuthResponse> login(LoginRequest request);
}