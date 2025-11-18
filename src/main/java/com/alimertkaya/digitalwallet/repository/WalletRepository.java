package com.alimertkaya.digitalwallet.repository;

import com.alimertkaya.digitalwallet.entity.Wallet;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface WalletRepository extends R2dbcRepository<Wallet, Long> {

    // user a tum wallet leri listeler
    Flux<Wallet> findByUserId(Long userId);

    // kullanicinin kendi cuzdanini gosterir
    Mono<Wallet> findByIdAndUserId(Long id, Long userId);
}
