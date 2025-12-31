package com.alimertkaya.digitalwallet.service.impl;

import com.alimertkaya.digitalwallet.dto.external.ExchangeApiResponse;
import com.alimertkaya.digitalwallet.entity.ExchangeRate;
import com.alimertkaya.digitalwallet.repository.ExchangeRateRepository;
import com.alimertkaya.digitalwallet.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final WebClient webClient;

    @Value("${app.exchange-rate-api.url}")
    private String apiUrl;

    private Mono<BigDecimal> getRate(String src, String tgt) {
        return exchangeRateRepository.findBySourceCurrencyAndTargetCurrency(src, tgt)
                .map(ExchangeRate::getRate)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Kur bilgisi bulunamadı: " + src + "_" + tgt)));
    }

    public Mono<Void> syncExchangeRates() {
        return webClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(ExchangeApiResponse.class)
                .flatMap(response -> {
                    if (response.getRates() == null) {
                        return Mono.error(new RuntimeException("API'den kurlar alınamadı!"));
                    }

                    return Flux.fromIterable(response.getRates().entrySet())
                            .flatMap(entry -> updateExchangeRate("USD", entry.getKey(), entry.getValue()))
                            .then();
                })
                .doOnSuccess(v -> log.info("Dış API üzerinden tüm kurlar başarıyla güncellendi."))
                .doOnError(e -> log.error("Kur senkronizasyonu sırasında hata: ", e));
    }

    @Override
    public Mono<ExchangeRate> updateExchangeRate(String sourceCurrency, String targetCurrency, BigDecimal rate) {
        return exchangeRateRepository.findBySourceCurrencyAndTargetCurrency(sourceCurrency, targetCurrency)
                .defaultIfEmpty(ExchangeRate.builder() // kayit yoksa yeni olusturulur
                        .sourceCurrency(sourceCurrency)
                        .targetCurrency(targetCurrency)
                        .build())
                .flatMap(exchangeRate -> {
                    exchangeRate.setRate(rate);
                    exchangeRate.setUpdatedAt(LocalDateTime.now());
                    return exchangeRateRepository.save(exchangeRate);
                });
    }

    @Override
    public Mono<BigDecimal> convertCurrency(BigDecimal amount, String sourceCurrency, String targetCurrency) {
        String src = sourceCurrency.toUpperCase();
        String tgt = targetCurrency.toUpperCase();

        // birimler aynıya cevirme
        if (src.equals(tgt)) {
            return Mono.just(amount);
        }

        // USD -> TRY direkt kur
        if (src.equals("USD")) {
            return getRate("USD", tgt)
                    .map(rate -> amount.multiply(rate).setScale(2, RoundingMode.HALF_UP));
        }

        // TRY -> USD ters kur
        if (tgt.equals("USD")) {
            return getRate("USD", src)
                    .map(rate -> amount.divide(rate, 2, RoundingMode.HALF_UP));
        }

        // TRY -> EUR carpraz kur
        return Mono.zip(getRate("USD", src), getRate("USD", tgt))
                .map(tuple -> {
                    BigDecimal srcRate = tuple.getT1();
                    BigDecimal tgtRate = tuple.getT2();
                    BigDecimal amountInTry = amount.divide(srcRate, 10, RoundingMode.HALF_UP);
                    return amountInTry.multiply(tgtRate).setScale(2, RoundingMode.HALF_UP);
                });

    }

    @Override
    public Flux<ExchangeRate> getAllRates() {
        return exchangeRateRepository.findAllBySourceCurrency("USD");
    }
}
