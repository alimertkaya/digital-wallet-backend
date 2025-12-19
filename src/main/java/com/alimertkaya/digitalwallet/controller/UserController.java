package com.alimertkaya.digitalwallet.controller;

import com.alimertkaya.digitalwallet.dto.UserResponse;
import com.alimertkaya.digitalwallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Mono<ResponseEntity<UserResponse>> getMyProfile() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(username -> userService.findByUsername(username))
                .map(user -> ResponseEntity.ok(UserResponse.fromEntity(user)));
    }
}
