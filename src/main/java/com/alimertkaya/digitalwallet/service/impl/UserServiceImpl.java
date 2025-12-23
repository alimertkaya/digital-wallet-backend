package com.alimertkaya.digitalwallet.service.impl;

import com.alimertkaya.digitalwallet.dto.ChangePasswordRequest;
import com.alimertkaya.digitalwallet.dto.UserProfileResponse;
import com.alimertkaya.digitalwallet.entity.User;
import com.alimertkaya.digitalwallet.repository.UserRepository;
import com.alimertkaya.digitalwallet.service.UserService;
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
                    if (!passwordEncoder.matches(request.getOldPassword(), request.getNewPassword())) {
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
}