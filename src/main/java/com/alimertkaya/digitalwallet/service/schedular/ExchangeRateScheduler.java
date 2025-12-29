package com.alimertkaya.digitalwallet.service.schedular;

import com.alimertkaya.digitalwallet.service.impl.ExchangeRateServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateScheduler {
    private final ExchangeRateServiceImpl exchangeRateService;

    // 6 saatte
    @Scheduled(fixedRate = 21600000)
    public void scheduleRateSync() {
        log.info("Döviz kurları API üzerinden güncelleniyor...");
        exchangeRateService.syncExchangeRates()
                .subscribe();
    }
}