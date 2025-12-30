package com.alimertkaya.digitalwallet.service;

import com.alimertkaya.digitalwallet.dto.analytic.AnalysisResponse;
import reactor.core.publisher.Mono;

public interface AnalyticsService {
    Mono<AnalysisResponse> getMonthlyAnalysis();
}