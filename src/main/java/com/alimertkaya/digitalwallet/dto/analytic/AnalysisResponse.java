package com.alimertkaya.digitalwallet.dto.analytic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    private BigDecimal totalBalanceInUSD;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpense;
    private List<CategorySpendResponse> categoryDistribution;
}