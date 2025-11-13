package com.alimertkaya.digitalwallet.service.impl;

import com.alimertkaya.digitalwallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements ReactiveUserDetailsService {
    // circular dependency onlemek icin olustuk bu class ı
    /*
    * SecurityConfig -> JwtAuthenticationFilter'a bağlı.
    * JwtAuthenticationFilter -> UserDetailsServiceImpl'e bağlı.
    * UserDetailsServiceImpl -> UserRepository'e bağlı.
     */
    private final UserRepository userRepository;

    // Security e username uzerinden db deki user u bulmayi gosterir
    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .cast(UserDetails.class)
                .switchIfEmpty(Mono.error(new RuntimeException("Kullanıcı bulunamadı: " + username)));
    }
}