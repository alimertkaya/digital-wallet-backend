package com.alimertkaya.digitalwallet.repository;

import com.alimertkaya.digitalwallet.entity.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {

    /**
     * security login page
     * @param username
     * @return User entity içeren bir Mono<User> or blank
     */
    Mono<User> findByUsername(String username);

    /**
     * register page kullanımda mı?
     * @param email
     * @return Mono<User> or blank
     */
    Mono<User> findByEmail(String email);

    /**
     * register page kullanımda mı?
     * @param tckn
     * @return Mono<User> or blank
     */
    Mono<User> findByTckn(String tckn);

    /**
     * register page kullanımda mı?
     * @param phoneNumber
     * @return Mono<User> or blank
     */
    Mono<User> findByPhoneNumber(String phoneNumber);
}