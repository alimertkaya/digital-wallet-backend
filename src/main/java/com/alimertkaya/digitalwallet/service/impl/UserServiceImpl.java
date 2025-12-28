package com.alimertkaya.digitalwallet.service.impl;

import com.alimertkaya.digitalwallet.dto.enums.VerificationType;
import com.alimertkaya.digitalwallet.dto.notification.VerifyCodeRequest;
import com.alimertkaya.digitalwallet.dto.user.*;
import com.alimertkaya.digitalwallet.entity.User;
import com.alimertkaya.digitalwallet.repository.UserRepository;
import com.alimertkaya.digitalwallet.service.UserService;
import com.alimertkaya.digitalwallet.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;

    @Override
    public Mono<User> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(userRepository::findByUsername)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kullanıcı bulunamadı")));
    }

    @Override
    public Mono<UserProfileResponse> getCurrentUserProfile() {
        return getCurrentUser()
                .map(UserProfileResponse::fromEntity);
    }

    @Override
    public Mono<Void> changePassword(ChangePasswordRequest request) {
        return getCurrentUser()
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mevcut şifreniz hatalı!"));
                    }

                    // new password ile old aynı olmasın
                    if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yeni şifre eskisiyle aynı olamaz."));
                    }

                    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                    return userRepository.save(user);
                })
                .then();
    }

    @Override
    public Mono<UserProfileResponse> updateUserInfo(UpdateUserInfoRequest request) {
        return getCurrentUser()
                .flatMap(user -> {
                    boolean isChanged = false;
                    if (request.getFirstName() != null && !request.getFirstName().equals(user.getFirstName())) {
                        user.setFirstName(request.getFirstName());
                        isChanged = true;
                    }
                    if (request.getLastName() != null && !request.getLastName().equals(user.getLastName())) {
                        user.setLastName(request.getLastName());
                        isChanged = true;
                    }

                    if (request.getBirthDate() != null && !request.getBirthDate().equals(user.getBirthDate())) {
                        user.setBirthDate(request.getBirthDate());
                        isChanged = true;
                    }

                    if (!isChanged) return Mono.just(UserProfileResponse.fromEntity(user));

                    return userRepository.save(user).map(UserProfileResponse::fromEntity);
                });
    }

    @Override
    public Mono<UserProfileResponse> updateEmail(UpdateEmailRequest request) {
        return getCurrentUser()
                .flatMap(user -> {
                    // ayni mail mi
                    if (request.getNewEmail().equals(user.getEmail())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yeni e-posta eskisiyle aynı olmaz."));
                    }

                    // mail kullanimda mi
                    return userRepository.findByEmail(request.getNewEmail())
                            .flatMap(existing -> Mono.<User>error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bu e-posta kullanımda.")))
                            .switchIfEmpty(Mono.just(user));
                })
                .flatMap(user -> {
                    user.setEmail(request.getNewEmail());
                    user.setEmailVerified(false); // mail degisti, dogrulama gerek
                    return userRepository.save(user)
                            .flatMap(savedUser -> verificationService.sendCode(savedUser.getId(), savedUser.getEmail(), VerificationType.EMAIL_VERIFICATION)
                                    .thenReturn(savedUser))
                            .map(UserProfileResponse::fromEntity);
                });
    }

    @Override
    public Mono<UserProfileResponse> updatePhone(UpdatePhoneRequest request) {
        return getCurrentUser()
                .flatMap(user -> {
                    if (request.getNewPhoneNumber().equals(user.getPhoneNumber())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yeni numara eskisiyle aynı olamaz."));
                    }

                    return userRepository.findByPhoneNumber(request.getNewPhoneNumber())
                            .flatMap(existing -> Mono.<User>error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bu numara kullanımda.")))
                            .switchIfEmpty(Mono.just(user));
                })
                .flatMap(user -> {
                    user.setPhoneNumber(request.getNewPhoneNumber());
                    user.setPhoneVerified(false);
                    return userRepository.save(user)
                            .flatMap(savedUser -> verificationService.sendCode(savedUser.getId(), savedUser.getPhoneNumber(), VerificationType.PHONE_VERIFICATION)
                                    .thenReturn(savedUser))
                            .map(UserProfileResponse::fromEntity);
                });
    }

    @Override
    public Mono<Void> verifyEmail(VerifyCodeRequest request) {
        return getCurrentUser()
                .flatMap(user -> {
                    if (user.isEmailVerified()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zaten doğrulanmış."));
                    }

                    return verificationService.verifyCode(user.getId(), request.getCode(), VerificationType.EMAIL_VERIFICATION)
                            .flatMap(isValid -> {
                                user.setEmailVerified(true);
                                return userRepository.save(user);
                            });
                })
                .then();
    }

    @Override
    public Mono<Void> resendEmailCode() {
        return getCurrentUser()
                .flatMap(user -> verificationService.sendCode(user.getId(), user.getEmail(), VerificationType.EMAIL_VERIFICATION));
    }

    @Override
    public Mono<Void> verifyPhone(VerifyCodeRequest request) {
        return getCurrentUser()
                .flatMap(user -> {
                    if (user.isPhoneVerified()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telefon numarası zaten doğrulanmış."));
                    }

                    return verificationService.verifyCode(user.getId(), request.getCode(), VerificationType.PHONE_VERIFICATION)
                            .flatMap(isValid -> {
                                user.setPhoneVerified(true);
                                return userRepository.save(user);
                            });
                })
                .then();
    }

    @Override
    public Mono<Void> resendPhoneCode() {
        return getCurrentUser()
                .flatMap(user -> verificationService.sendCode(user.getId(), user.getPhoneNumber(), VerificationType.PHONE_VERIFICATION));
    }

    @Override
    public Mono<Void> deactivateAccount() {
        return getCurrentUser()
                .flatMap(user -> {
                    user.setEnabled(false);
                    return userRepository.save(user);
                })
                .then();
    }
}