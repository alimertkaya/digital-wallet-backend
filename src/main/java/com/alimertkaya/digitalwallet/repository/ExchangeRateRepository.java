package com.alimertkaya.digitalwallet.repository;

import com.alimertkaya.digitalwallet.entity.ExchangeRate;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ExchangeRateRepository extends R2dbcRepository<ExchangeRate, Long> {
    Mono<ExchangeRate> findBySourceCurrencyAndTargetCurrency(String sourceCurrency, String targetCurrency);

    Flux<ExchangeRate> findAllBySourceCurrency(String sourceCurrency);
}