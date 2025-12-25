package com.alimertkaya.digitalwallet.service;

import com.alimertkaya.digitalwallet.dto.*;
import com.alimertkaya.digitalwallet.entity.User;
import reactor.core.publisher.Mono;

public interface UserService {
    Mono<User> getCurrentUser();
    Mono<UserProfileResponse> getCurrentUserProfile();
    Mono<Void> changePassword(ChangePasswordRequest request);
    Mono<UserProfileResponse> updateUserInfo(UpdateUserInfoRequest request);
    Mono<UserProfileResponse> updateEmail(UpdateEmailRequest request);
    Mono<UserProfileResponse> updatePhone(UpdatePhoneRequest request);
}