package com.alimertkaya.digitalwallet.service;

import com.alimertkaya.digitalwallet.entity.ExchangeRate;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface ExchangeRateService {
    // doviz cifti icin kuru gunceller veya olusturur
    Mono<ExchangeRate> updateExchangeRate(String sourceCurrency, String targetCurrency, BigDecimal rate);

    // tutari, source -> target a guncel kur ile cevirir
    Mono<BigDecimal> convertCurrency(BigDecimal amount, String sourceCurrency, String targetCurrency);
}
