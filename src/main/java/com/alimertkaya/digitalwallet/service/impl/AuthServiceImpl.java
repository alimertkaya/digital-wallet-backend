package com.alimertkaya.digitalwallet.service.impl;

import com.alimertkaya.digitalwallet.dto.auth.AuthResponse;
import com.alimertkaya.digitalwallet.dto.auth.LoginRequest;
import com.alimertkaya.digitalwallet.dto.auth.RegisterRequest;
import com.alimertkaya.digitalwallet.dto.enums.VerificationType;
import com.alimertkaya.digitalwallet.entity.User;
import com.alimertkaya.digitalwallet.repository.UserRepository;
import com.alimertkaya.digitalwallet.service.AuthService;
import com.alimertkaya.digitalwallet.service.JwtService;
import com.alimertkaya.digitalwallet.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor // final fieldslar icin constructor olusturur
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificationService verificationService;

    // mono dolu ise 400, empty ise continue
    private <T> Mono<Void> failIfPresent(Mono<T> mono, String message) {
        return mono
                .flatMap(x -> Mono.<Void>error(new ResponseStatusException(HttpStatus.BAD_REQUEST, message)))
                .switchIfEmpty(Mono.empty());
    }

    @Override
    public Mono<AuthResponse> register(RegisterRequest request) {
        // TCKN yi duz metin aliyoruz daha sonra db ye kayittan once encryption yapicagiz
        String tcknForCheck = request.getTckn();

        // benzersizlik kontrol
        Mono<Void> validation = failIfPresent(userRepository.findByUsername(request.getUsername()),
                "Kullanıcı adı zaten alınmış: " + request.getUsername())
                .then(failIfPresent(userRepository.findByEmail(request.getEmail()),
                        "E-posta zaten kullanımda: " + request.getEmail()))
                .then(failIfPresent(userRepository.findByTckn(tcknForCheck),
                        "TCKN zaten kayıtlı"))
                .then(failIfPresent(userRepository.findByPhoneNumber(request.getPhoneNumber()),
                        "Telefon numarası zaten kayıtlı"));

        return validation.then(Mono.defer(() -> {
                    User newUser = User.builder()
                            .username(request.getUsername())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .email(request.getEmail())
                            .phoneNumber(request.getPhoneNumber())
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .birthDate(request.getBirthDate())
                            .tckn(tcknForCheck)
                            .roles("ROLE_USER")
                            .isEnabled(true) // e-mail verified olana kadar true
                            .isLocked(false)
                            .isEmailVerified(false)
                            .isPhoneVerified(false)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    // save db
                    return userRepository.save(newUser)
                            .flatMap(savedUser -> {
                                return verificationService.sendCode(savedUser.getId(), savedUser.getPhoneNumber(), VerificationType.PHONE_VERIFICATION)
                                        .thenReturn(savedUser);
                            })
                            .map(savedUser -> AuthResponse.builder()
                                    .token(jwtService.generateToken(savedUser))
                                    .username(savedUser.getUsername())
                                    .build());
                }));

    }

    @Override
    public Mono<AuthResponse> login(LoginRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Kullanıcı bulunamadı veya şifre hatalı")))
                .flatMap(user -> {
                    // kullanici bulunduysa password match et
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                "Kullanıcı bulunamadı veya şifre hatalı"));
                    }
                    return Mono.just(AuthResponse.builder()
                            .token(jwtService.generateToken(user))
                            .username(user.getUsername())
                            .firstName(user.getFirstName())
                            .build());
                });
    }
}