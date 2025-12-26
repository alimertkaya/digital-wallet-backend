package com.alimertkaya.digitalwallet.service;

import com.alimertkaya.digitalwallet.dto.enums.VerificationType;
import reactor.core.publisher.Mono;

public interface VerificationService {
    // kodu uretir ve db save yapar
    Mono<Void> sendCode(Long userId, String destination, VerificationType type);
    // kodu kontrol eder ve db delete yapar
    Mono<Boolean> verifyCode(Long userId, String code, VerificationType type);
}