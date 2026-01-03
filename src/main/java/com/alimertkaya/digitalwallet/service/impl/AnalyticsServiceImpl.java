package com.alimertkaya.digitalwallet.service.impl;

import com.alimertkaya.digitalwallet.dto.analytic.AnalysisResponse;
import com.alimertkaya.digitalwallet.dto.analytic.CategorySpendResponse;
import com.alimertkaya.digitalwallet.dto.enums.HistoryDirection;
import com.alimertkaya.digitalwallet.dto.enums.TransactionCategory;
import com.alimertkaya.digitalwallet.entity.TransactionHistory;
import com.alimertkaya.digitalwallet.entity.Wallet;
import com.alimertkaya.digitalwallet.repository.TransactionHistoryRepository;
import com.alimertkaya.digitalwallet.repository.WalletRepository;
import com.alimertkaya.digitalwallet.service.AnalyticsService;
import com.alimertkaya.digitalwallet.service.ExchangeRateService;
import com.alimertkaya.digitalwallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final TransactionHistoryRepository transactionHistoryRepository;
    private final WalletRepository walletRepository;
    private final ExchangeRateService exchangeRateService;
    private final UserService userService;

    @Override
    public Mono<AnalysisResponse> getMonthlyAnalysis() {
        return userService.getCurrentUser().flatMap(user -> walletRepository.findByUserId(user.getId())
                .collectList()
                .flatMap(wallets -> {
                    if (wallets.isEmpty()) {
                        return Mono.just(AnalysisResponse.builder()
                                .totalBalanceInUSD(BigDecimal.ZERO)
                                .totalBalanceInTL(BigDecimal.ZERO)
                                .build());
                    }
                    List<Long> walletIds = wallets.stream().map(Wallet::getId).toList();
                    LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);

                    Mono<BigDecimal> totalUsdBalance = Flux.fromIterable(wallets)
                            .flatMap(w -> exchangeRateService.convertCurrency(w.getBalance(), w.getCurrencyCode(),
                                    "USD"))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    Mono<BigDecimal> totalTlBalance = Flux.fromIterable(wallets)
                            .flatMap(w -> exchangeRateService.convertCurrency(w.getBalance(), w.getCurrencyCode(),
                                    "TRY"))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    Mono<List<TransactionHistory>> historyList = transactionHistoryRepository
                            .findByWalletIdInAndCreatedAtAfter(walletIds, startOfMonth)
                            .collectList();

                    return Mono.zip(totalUsdBalance, totalTlBalance, historyList)
                            .flatMap(tuple -> {
                                BigDecimal netWorthUsd = tuple.getT1();
                                BigDecimal netWorthTl = tuple.getT2();
                                List<TransactionHistory> histories = tuple.getT3();

                                // Her işlemi TL çevirir
                                Mono<BigDecimal> incomeMono = Flux.fromIterable(histories)
                                        .filter(h -> h.getDirection() == HistoryDirection.IN)
                                        .flatMap(h -> exchangeRateService.convertCurrency(h.getAmount(),
                                                h.getCurrencyCode(), "TRY"))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                Mono<BigDecimal> expenseMono = Flux.fromIterable(histories)
                                        .filter(h -> h.getDirection() == HistoryDirection.OUT)
                                        .flatMap(h -> exchangeRateService.convertCurrency(h.getAmount(),
                                                h.getCurrencyCode(), "TRY"))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                // Kategori bazlı harcamaları TL cevirir
                                Mono<Map<TransactionCategory, BigDecimal>> categoryMapMono = Flux
                                        .fromIterable(histories)
                                        .filter(h -> h.getDirection() == HistoryDirection.OUT
                                                && h.getCategory() != null)
                                        .flatMap(h -> exchangeRateService
                                                .convertCurrency(h.getAmount(), h.getCurrencyCode(), "TRY")
                                                .map(convertedAmount -> Map.entry(h.getCategory(), convertedAmount)))
                                        .reduce(new HashMap<TransactionCategory, BigDecimal>(), (map, entry) -> {
                                            map.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
                                            return map;
                                        });

                                return Mono.zip(incomeMono, expenseMono, categoryMapMono)
                                        .map(result -> {
                                            BigDecimal income = result.getT1();
                                            BigDecimal expense = result.getT2();
                                            Map<TransactionCategory, BigDecimal> categoryMap = result.getT3();

                                            List<CategorySpendResponse> distribution = categoryMap.entrySet().stream()
                                                    .map(e -> {
                                                        double percentage = 0;

                                                        if (expense.compareTo(BigDecimal.ZERO) > 0) {
                                                            percentage = e.getValue()
                                                                    .divide(expense, 4, RoundingMode.HALF_UP)
                                                                    .doubleValue() * 100;
                                                        }
                                                        return new CategorySpendResponse(e.getKey(), e.getValue(),
                                                                percentage);
                                                    })
                                                    .toList();

                                            return AnalysisResponse.builder()
                                                    .totalBalanceInUSD(netWorthUsd)
                                                    .totalBalanceInTL(netWorthTl)
                                                    .monthlyIncome(income)
                                                    .monthlyExpense(expense)
                                                    .categoryDistribution(distribution)
                                                    .build();
                                        });
                            });
                }));
    }
}
