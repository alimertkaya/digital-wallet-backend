package com.alimertkaya.digitalwallet.service;

import com.alimertkaya.digitalwallet.entity.User;
import reactor.core.publisher.Mono;

public interface UserService {
   Mono<User> findByUsername(String username);
}
