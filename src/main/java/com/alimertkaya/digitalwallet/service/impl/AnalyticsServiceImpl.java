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
                                    .build());
                        }
                        List<Long> walletIds = wallets.stream().map(Wallet::getId).toList();
                        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);

                        Mono<BigDecimal> totalUsdBalance = Flux.fromIterable(wallets)
                                .flatMap(w -> exchangeRateService.convertCurrency(w.getBalance(), w.getCurrencyCode(), "USD"))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        Mono<List<TransactionHistory>> historyList = transactionHistoryRepository.findByWalletIdInAndCreatedAtAfter(walletIds, startOfMonth)
                                .collectList();

                        return Mono.zip(totalUsdBalance, historyList)
                                .map(tuple -> {
                                    BigDecimal netWorth = tuple.getT1();
                                    List<TransactionHistory> histories = tuple.getT2();

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

                                    List<CategorySpendResponse> distribution = categoryMap.entrySet().stream()
                                            .map(e -> new CategorySpendResponse(e.getKey(), e.getValue(), 0))
                                            .toList();

                                    return AnalysisResponse.builder()
                                            .totalBalanceInUSD(netWorth)
                                            .monthlyIncome(income)
                                            .monthlyExpense(expense)
                                            .categoryDistribution(distribution)
                                            .build();
                                });
                    })
        );
    }
}
