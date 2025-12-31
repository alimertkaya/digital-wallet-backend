package com.alimertkaya.digitalwallet.controller;

import com.alimertkaya.digitalwallet.entity.ExchangeRate;
import com.alimertkaya.digitalwallet.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {
    private final ExchangeRateService exchangeRateService;

    @GetMapping
    public Flux<ExchangeRate> getAllExchangeRates() {
        return exchangeRateService.getAllRates();
    }
}
