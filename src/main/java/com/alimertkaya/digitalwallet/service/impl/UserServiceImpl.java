package com.alimertkaya.digitalwallet.service.impl;

import com.alimertkaya.digitalwallet.entity.User;
import com.alimertkaya.digitalwallet.repository.UserRepository;
import com.alimertkaya.digitalwallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public Mono<User> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kullanıcı bulunamadı")));
    }
}