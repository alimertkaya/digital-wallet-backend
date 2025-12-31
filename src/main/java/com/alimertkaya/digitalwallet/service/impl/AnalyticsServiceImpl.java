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
        return userService.getCurrentUser().flatMap(user ->
            walletRepository.findByUserId(user.getId())
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
                                .flatMap(w -> exchangeRateService.convertCurrency(w.getBalance(), w.getCurrencyCode(), "USD"))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        Mono<BigDecimal> totalTlBalance = Flux.fromIterable(wallets)
                                .flatMap(w -> exchangeRateService.convertCurrency(w.getBalance(), w.getCurrencyCode(), "TRY"))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        Mono<List<TransactionHistory>> historyList = transactionHistoryRepository.findByWalletIdInAndCreatedAtAfter(walletIds, startOfMonth)
                                .collectList();

                        return Mono.zip(totalUsdBalance, totalTlBalance, historyList)
                                .map(tuple -> {
                                    BigDecimal netWorthUsd = tuple.getT1();
                                    BigDecimal netWorthTl = tuple.getT2();
                                    List<TransactionHistory> histories = tuple.getT3();

                                    BigDecimal income = BigDecimal.ZERO;
                                    BigDecimal expense = BigDecimal.ZERO;
                                    Map<TransactionCategory, BigDecimal> categoryMap = new HashMap<>();

                                    for (TransactionHistory h : histories) {
                                        if (h.getDirection() == HistoryDirection.IN) {
                                            income = income.add(h.getAmount());
                                        } else {
                                            expense = expense.add(h.getAmount());
                                            categoryMap.merge(h.getCategory(), h.getAmount(), BigDecimal::add);
                                        }
                                    }

                                    BigDecimal finalTotalExpense = expense;
                                    List<CategorySpendResponse> distribution = categoryMap.entrySet().stream()
                                            .map(e -> {
                                                double percentage = 0;

                                                if (finalTotalExpense.compareTo(BigDecimal.ZERO) > 0) {
                                                    percentage = e.getValue()
                                                            .divide(finalTotalExpense, 4, RoundingMode.HALF_UP)
                                                            .doubleValue() * 100;
                                                }
                                                return new CategorySpendResponse(e.getKey(), e.getValue(), percentage);
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
                    })
        );
    }
}
