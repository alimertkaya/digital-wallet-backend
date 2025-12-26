package com.alimertkaya.digitalwallet.service.impl;

import com.alimertkaya.digitalwallet.dto.enums.VerificationType;
import com.alimertkaya.digitalwallet.entity.VerificationCode;
import com.alimertkaya.digitalwallet.repository.UserRepository;
import com.alimertkaya.digitalwallet.repository.VerificationCodeRepository;
import com.alimertkaya.digitalwallet.service.NotificationService;
import com.alimertkaya.digitalwallet.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements VerificationService {
    private final VerificationCodeRepository codeRepository;

    private static final SecureRandom secureRandom = new SecureRandom();

    // kod gonder
    @Override
    public Mono<Void> sendCode(Long userId, String destination, VerificationType type) {
        String code = String.format("%06d", secureRandom.nextInt(1000000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        return codeRepository.findByUserIdAndType(userId, type)
                .flatMap(existing -> {
                    existing.setCode(code);
                    existing.setExpiryDate(expiry);
                    return codeRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> codeRepository.save(VerificationCode.builder()
                        .userId(userId)
                        .code(code)
                        .type(type)
                        .expiryDate(expiry)
                        .build())))
                .doOnSuccess(c -> log.info("📢 [MOCK] Kod Gönderildi -> Tip: {}, Hedef: {}, Kod: {}", type, destination, code))
                .then();
    }

    // kodu dogrula
    @Override
    public Mono<Boolean> verifyCode(Long userId, String inputCode, VerificationType type) {
        return codeRepository.findByUserIdAndType(userId, type)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçersiz veya hatalı kod.")))
                .flatMap(vc -> {
                    if (vc.getExpiryDate().isBefore(LocalDateTime.now())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kodun süresi dolmuş."));
                    }
                    if (!vc.getCode().equals(inputCode)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hatalı kod."));
                    }
                    return codeRepository.delete(vc).thenReturn(true);
                });
        }
}