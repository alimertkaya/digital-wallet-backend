package com.alimertkaya.digitalwallet.service;

import com.alimertkaya.digitalwallet.dto.ChangePasswordRequest;
import com.alimertkaya.digitalwallet.dto.UserProfileResponse;
import com.alimertkaya.digitalwallet.entity.User;
import reactor.core.publisher.Mono;

public interface UserService {
    Mono<User> getCurrentUser();

    Mono<UserProfileResponse> getCurrentUserProfile();
    Mono<Void> changePassword(ChangePasswordRequest request);
}