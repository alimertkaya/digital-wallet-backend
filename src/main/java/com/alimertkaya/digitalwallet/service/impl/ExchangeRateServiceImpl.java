package com.alimertkaya.digitalwallet.service.impl;

import com.alimertkaya.digitalwallet.entity.ExchangeRate;
import com.alimertkaya.digitalwallet.repository.ExchangeRateRepository;
import com.alimertkaya.digitalwallet.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    public Mono<ExchangeRate> updateExchangeRate(String sourceCurrency, String targetCurrency, BigDecimal rate) {
        String src = sourceCurrency.toUpperCase();
        String tgt = targetCurrency.toUpperCase();

        return exchangeRateRepository.findBySourceCurrencyAndTargetCurrency(src, tgt)
                .defaultIfEmpty(ExchangeRate.builder() // kayit yoksa yeni olusturulur
                        .sourceCurrency(src)
                        .targetCurrency(tgt)
                        .build())
                .flatMap(exchangeRate -> {
                    exchangeRate.setRate(rate);
                    exchangeRate.setUpdatedAt(LocalDateTime.now());
                    return exchangeRateRepository.save(exchangeRate);
                })
                .doOnSuccess(er -> log.info("Döviz kuru güncellendi: {} -> {} = {}", sourceCurrency, targetCurrency, rate));
    }

    @Override
    public Mono<BigDecimal> convertCurrency(BigDecimal amount, String sourceCurrency, String targetCurrency) {
        String src = sourceCurrency.toUpperCase();
        String tgt = targetCurrency.toUpperCase();

        // birimler aynıya cevirme
        if (src.equals(tgt)) {
            return Mono.just(amount);
        }

        return exchangeRateRepository.findBySourceCurrencyAndTargetCurrency(src, tgt)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Döviz kuru bulunamadı: " + src + " -> " + tgt)))
                .map(exchangeRate -> {
                    BigDecimal convertedAmount = amount.multiply(exchangeRate.getRate());
                    return convertedAmount.setScale(2, RoundingMode.HALF_UP);
                });
    }
}
